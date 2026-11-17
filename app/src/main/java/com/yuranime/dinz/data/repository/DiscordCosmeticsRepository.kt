package com.yuranime.dinz.data.repository

import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.yuranime.dinz.data.firebase.BorderDefinition
import com.yuranime.dinz.data.firebase.ProfileEffectDefinition
import com.yuranime.dinz.data.firebase.ProfileEffectLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

object DiscordCosmeticsRepository {

    private const val TAG = "DiscordCosmeticsRepo"
    private const val COLLECTION_CONFIG = "app_config"
    private const val DOC_REMOTE_CONFIG = "discord"
    private const val COLLECTION_ASSETS = "discord_assets"
    private const val FETCH_TIMEOUT_MS = 12_000L

    data class RemoteCosmeticsConfig(
        val isActive: Boolean = true,
        val catalogVersion: Int = 1,
        val cdnHosts: List<String> = emptyList(),
        val presetPathMarker: String = "",
        val queryParamTemplate: String = "size={size}&passthrough={animated}",
        val inviteUrl: String = "",
        val webhookUrl: String = "",
        val guildId: String = "",
        val channelId: String = "",
        val defaultBorderDesc: String = "Dekorasi Avatar Premium",
        val defaultEffectDesc: String = "Efek Animasi Profil Premium",
        val defaultCategory: String = "Koleksi Spesial",
        val effectsCatalogDoc: String = "",
        val borderChunkDocs: List<String> = emptyList(),
        val effectChunkDocs: List<String> = emptyList()
    )

    private data class RawDecorationDto(
        @SerializedName("id") val id: String?,
        @SerializedName("uuid") val uuid: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("category") val category: String?,
        @SerializedName("product_category") val productCategory: String?,
        @SerializedName("icon") val icon: String?,
        @SerializedName("animated_icon") val animatedIcon: String?,
        @SerializedName("asset") val asset: String?,
        @SerializedName("label") val label: String?
    )

    private data class RawEffectDto(
        @SerializedName("id") val id: String?,
        @SerializedName("uuid") val uuid: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("category") val category: String?,
        @SerializedName("product_category") val productCategory: String?,
        @SerializedName("icon") val icon: String?,
        @SerializedName("animated_icon") val animatedIcon: String?,
        @SerializedName("label") val label: String?,
        @SerializedName("accessibility_label") val accessibilityLabel: String?,
        @SerializedName("bakedCardUrl") val bakedCardUrl: String?,
        @SerializedName("effects") val effects: List<CatalogLayerDto>?,
        @SerializedName("layers") val layers: List<CatalogLayerDto>?
    )

    private data class CatalogPositionDto(
        @SerializedName("x") val x: Int?,
        @SerializedName("y") val y: Int?
    )

    private data class CatalogLayerDto(
        @SerializedName("src") val src: String?,
        @SerializedName("loop") val loop: Boolean?,
        @SerializedName("duration") val duration: Long?,
        @SerializedName("start") val start: Long?,
        @SerializedName("loopDelay") val loopDelay: Long?,
        @SerializedName("position") val position: CatalogPositionDto?,
        @SerializedName("zIndex") val zIndex: Int?
    )

    private data class CatalogEffectDto(
        @SerializedName("id") val id: String?,
        @SerializedName("title") val title: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("accessibilityLabel") val accessibilityLabel: String?,
        @SerializedName("staticFrameSrc") val staticFrameSrc: String?,
        @SerializedName("thumbnailPreviewSrc") val thumbnailPreviewSrc: String?,
        @SerializedName("reducedMotionSrc") val reducedMotionSrc: String?,
        @SerializedName("layers") val layers: List<CatalogLayerDto>?,
        @SerializedName("effects") val effects: List<CatalogLayerDto>?
    )

    private data class CatalogCollectionDto(
        @SerializedName("category") val category: String?,
        @SerializedName("effects") val effects: List<CatalogEffectDto>?
    )

    private data class CatalogRootDto(
        @SerializedName("collections") val collections: List<CatalogCollectionDto>?
    )

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedConfig: RemoteCosmeticsConfig? = null

    @Volatile
    private var cachedBorders: List<BorderDefinition>? = null

    @Volatile
    private var cachedEffects: List<ProfileEffectDefinition>? = null

    @Volatile
    private var lastConfigAttemptMs: Long = 0L

    private val _catalogRevision = MutableStateFlow(0)
    val catalogRevision: StateFlow<Int> = _catalogRevision.asStateFlow()

    private val gson = GsonBuilder().setLenient().serializeNulls().create()

    private val restClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Pra-muat konfigurasi dinamis dari Firebase secara asinkron (aman offline/null).
     */
    fun prefetchConfig(context: Context) {
        repoScope.launch {
            try {
                loadRemoteConfigBlocking(context)
            } catch (t: Throwable) {
                Log.w(TAG, "Prefetch konfigurasi dinamis dilewati: ${t.message}")
            }
        }
    }

    fun getRemoteConfig(): RemoteCosmeticsConfig? = cachedConfig

    fun getInviteUrl(): String? = cachedConfig?.inviteUrl?.takeIf { it.isNotBlank() }

    fun getWebhookUrl(): String? = cachedConfig?.webhookUrl?.takeIf { it.isNotBlank() }

    /**
     * Mengecek apakah hostname termasuk daftar CDN kosmetik yang dikonfigurasi di Firebase.
     */
    fun isManagedCdnHost(host: String): Boolean {
        if (host.isBlank()) return false
        val cfg = cachedConfig ?: return false
        val normalized = host.lowercase(Locale.ROOT)
        return cfg.cdnHosts.any { candidate ->
            candidate.isNotBlank() && normalized.contains(candidate.trim().lowercase(Locale.ROOT))
        }
    }

    private fun resolveProjectId(context: Context): String? {
        return try {
            FirebaseApp.getInstance().options.projectId?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            try {
                val resId = context.resources.getIdentifier("project_id", "string", context.packageName)
                if (resId != 0) context.getString(resId).takeIf { it.isNotBlank() } else null
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun fetchFirestoreDocJsonRest(
        context: Context,
        collection: String,
        document: String
    ): JSONObject? {
        return try {
            val projectId = resolveProjectId(context) ?: return null
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection/$document"
            val req = Request.Builder().url(url).build()
            restClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                val body = res.body?.string()?.takeIf { it.isNotBlank() } ?: return null
                JSONObject(body).optJSONObject("fields")
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun readRestString(fields: JSONObject, key: String): String {
        return try {
            fields.optJSONObject(key)?.optString("stringValue", "")?.trim().orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun readRestStringList(fields: JSONObject, key: String): List<String> {
        return try {
            val values: JSONArray = fields.optJSONObject(key)
                ?.optJSONObject("arrayValue")
                ?.optJSONArray("values")
                ?: return emptyList()
            buildList {
                for (i in 0 until values.length()) {
                    val v = values.optJSONObject(i)?.optString("stringValue", "")?.trim()
                    if (!v.isNullOrEmpty()) add(v)
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseConfigFromSnapshot(snap: DocumentSnapshot): RemoteCosmeticsConfig? {
        if (!snap.exists()) return null
        return try {
            val isActive = snap.getBoolean("isActive") ?: true
            val version = (snap.getLong("catalogVersion") ?: 1L).toInt()
            val cdnHosts = (snap.get("cdnHosts") as? List<*>)
                ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                .orEmpty()
            val borderChunks = (snap.get("borderChunkDocs") as? List<*>)
                ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                .orEmpty()
            val effectChunks = (snap.get("effectChunkDocs") as? List<*>)
                ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                .orEmpty()

            RemoteCosmeticsConfig(
                isActive = isActive,
                catalogVersion = version,
                cdnHosts = cdnHosts,
                presetPathMarker = snap.getString("presetPathMarker").orEmpty().trim(),
                queryParamTemplate = snap.getString("queryParamTemplate")
                    ?.takeIf { it.isNotBlank() }
                    ?: "size={size}&passthrough={animated}",
                inviteUrl = snap.getString("inviteUrl").orEmpty().trim(),
                webhookUrl = snap.getString("webhookUrl").orEmpty().trim(),
                guildId = snap.getString("guildId").orEmpty().trim(),
                channelId = snap.getString("channelId").orEmpty().trim(),
                defaultBorderDesc = snap.getString("defaultBorderDesc")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Dekorasi Avatar Premium",
                defaultEffectDesc = snap.getString("defaultEffectDesc")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Efek Animasi Profil Premium",
                defaultCategory = snap.getString("defaultCategory")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Koleksi Spesial",
                effectsCatalogDoc = snap.getString("effectsCatalogDoc").orEmpty().trim(),
                borderChunkDocs = borderChunks,
                effectChunkDocs = effectChunks
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Gagal parse RemoteCosmeticsConfig snapshot: ${t.message}")
            null
        }
    }

    private fun parseConfigFromRest(fields: JSONObject): RemoteCosmeticsConfig? {
        return try {
            val isActive = fields.optJSONObject("isActive")?.optBoolean("booleanValue", true) ?: true
            val version = fields.optJSONObject("catalogVersion")?.optInt("integerValue", 1) ?: 1
            RemoteCosmeticsConfig(
                isActive = isActive,
                catalogVersion = version,
                cdnHosts = readRestStringList(fields, "cdnHosts"),
                presetPathMarker = readRestString(fields, "presetPathMarker"),
                queryParamTemplate = readRestString(fields, "queryParamTemplate")
                    .ifBlank { "size={size}&passthrough={animated}" },
                inviteUrl = readRestString(fields, "inviteUrl"),
                webhookUrl = readRestString(fields, "webhookUrl"),
                guildId = readRestString(fields, "guildId"),
                channelId = readRestString(fields, "channelId"),
                defaultBorderDesc = readRestString(fields, "defaultBorderDesc")
                    .ifBlank { "Dekorasi Avatar Premium" },
                defaultEffectDesc = readRestString(fields, "defaultEffectDesc")
                    .ifBlank { "Efek Animasi Profil Premium" },
                defaultCategory = readRestString(fields, "defaultCategory")
                    .ifBlank { "Koleksi Spesial" },
                effectsCatalogDoc = readRestString(fields, "effectsCatalogDoc"),
                borderChunkDocs = readRestStringList(fields, "borderChunkDocs"),
                effectChunkDocs = readRestStringList(fields, "effectChunkDocs")
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Gagal parse RemoteCosmeticsConfig REST: ${t.message}")
            null
        }
    }

    private fun loadRemoteConfigBlocking(context: Context): RemoteCosmeticsConfig? {
        cachedConfig?.let { return it }
        val now = System.currentTimeMillis()
        if (now - lastConfigAttemptMs < 3_000L && cachedConfig == null) {
            // Hindari spam berulang dalam rentang detik yang sama jika baru saja gagal
        }
        lastConfigAttemptMs = now

        val sdkConfig = try {
            runBlocking {
                withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                    val snap = FirebaseFirestore.getInstance()
                        .collection(COLLECTION_CONFIG)
                        .document(DOC_REMOTE_CONFIG)
                        .get()
                        .await()
                    parseConfigFromSnapshot(snap)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Firestore SDK gagal membaca config: ${t.message}")
            null
        }

        val resolved = sdkConfig ?: try {
            fetchFirestoreDocJsonRest(context, COLLECTION_CONFIG, DOC_REMOTE_CONFIG)
                ?.let { parseConfigFromRest(it) }
        } catch (_: Throwable) {
            null
        }

        if (resolved != null) {
            cachedConfig = resolved
            _catalogRevision.value = _catalogRevision.value + 1
        }
        return resolved
    }

    private fun fetchPayloadDocBlocking(context: Context, docId: String): String? {
        if (docId.isBlank()) return null
        val fromSdk = try {
            runBlocking {
                withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                    val snap = FirebaseFirestore.getInstance()
                        .collection(COLLECTION_ASSETS)
                        .document(docId)
                        .get()
                        .await()
                    snap.getString("payload")?.takeIf { it.isNotBlank() }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Firestore SDK gagal membaca dokumen $docId: ${t.message}")
            null
        }

        if (!fromSdk.isNullOrBlank()) return fromSdk

        return try {
            val fields = fetchFirestoreDocJsonRest(context, COLLECTION_ASSETS, docId)
                ?: return null
            readRestString(fields, "payload").takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            Log.w(TAG, "REST gagal membaca dokumen $docId: ${t.message}")
            null
        }
    }

    /**
     * Mengambil daftar Avatar Decoration secara dinamis dari Firebase Firestore.
     * Aman terhadap null, data kosong, maupun kondisi offline tanpa menyebabkan force close.
     */
    fun getAllBorders(context: Context): List<BorderDefinition> {
        cachedBorders?.let { return it }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            repoScope.launch { runCatching { getAllBorders(context) } }
            return emptyList()
        }

        synchronized(this) {
            cachedBorders?.let { return it }

            return try {
                val cfg = loadRemoteConfigBlocking(context)
                if (cfg == null || !cfg.isActive || cfg.borderChunkDocs.isEmpty()) {
                    return emptyList()
                }

                val itemType = object : TypeToken<List<RawDecorationDto>>() {}.type
                val combinedRaw = mutableListOf<RawDecorationDto>()

                for (docName in cfg.borderChunkDocs) {
                    val payload = fetchPayloadDocBlocking(context, docName) ?: continue
                    val parsed: List<RawDecorationDto>? = try {
                        gson.fromJson(payload, itemType)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Gagal parse chunk border $docName: ${e.message}")
                        null
                    }
                    if (!parsed.isNullOrEmpty()) {
                        combinedRaw.addAll(parsed)
                    }
                }

                if (combinedRaw.isEmpty()) return emptyList()

                val total = combinedRaw.size
                val starterEnd = (total * 0.15).toInt()
                val highEnd = (total * 0.80).toInt()

                val result = combinedRaw.mapIndexed { index, raw ->
                    val resolvedId = raw.id?.takeIf { it.isNotBlank() }
                        ?: raw.asset?.takeIf { it.isNotBlank() }
                        ?: raw.uuid
                        ?: "border_$index"
                    val resolvedName = raw.name?.takeIf { it.isNotBlank() } ?: "Avatar Border #$index"
                    val animUrl = raw.animatedIcon?.takeIf { it.isNotBlank() }
                        ?: raw.icon.orEmpty()
                    val staticUrl = raw.icon?.takeIf { it.isNotBlank() }
                        ?: raw.animatedIcon.orEmpty()
                    val desc = raw.label?.takeIf { it.isNotBlank() }
                        ?: raw.category?.takeIf { it.isNotBlank() }
                        ?: cfg.defaultBorderDesc
                    val category = raw.category?.takeIf { it.isNotBlank() }
                        ?: raw.productCategory?.takeIf { it.isNotBlank() }
                        ?: cfg.defaultCategory

                    when {
                        index < starterEnd -> {
                            val cost = 500L + ((index % 11) * 100L)
                            BorderDefinition(
                                id = resolvedId,
                                name = resolvedName,
                                imageUrl = animUrl,
                                staticImageUrl = staticUrl,
                                pointCost = cost,
                                description = desc,
                                tier = "A",
                                requiredLevel = 1,
                                category = category
                            )
                        }
                        index < highEnd -> {
                            val cost = 3500L + ((index % 44) * 500L)
                            BorderDefinition(
                                id = resolvedId,
                                name = resolvedName,
                                imageUrl = animUrl,
                                staticImageUrl = staticUrl,
                                pointCost = cost,
                                description = desc,
                                tier = "B",
                                requiredLevel = 35,
                                category = category
                            )
                        }
                        else -> {
                            BorderDefinition(
                                id = resolvedId,
                                name = resolvedName,
                                imageUrl = animUrl,
                                staticImageUrl = staticUrl,
                                pointCost = 0L,
                                description = desc,
                                tier = "C",
                                requiredLevel = 0,
                                category = category
                            )
                        }
                    }
                }

                cachedBorders = result
                _catalogRevision.value = _catalogRevision.value + 1
                result
            } catch (t: Throwable) {
                Log.w(TAG, "Gagal memuat katalog border dari Firebase: ${t.message}")
                emptyList()
            }
        }
    }

    private fun loadCatalogEffects(
        context: Context,
        catalogDocName: String
    ): Map<String, CatalogEffectDto> {
        val map = mutableMapOf<String, CatalogEffectDto>()
        if (catalogDocName.isBlank()) return map
        try {
            val payload = fetchPayloadDocBlocking(context, catalogDocName) ?: return map
            val root = gson.fromJson(payload, CatalogRootDto::class.java)
            root?.collections?.forEach { col ->
                col.effects?.forEach { eff ->
                    eff.id?.takeIf { it.isNotBlank() }?.let { id ->
                        map[id] = eff
                    }
                    eff.title?.takeIf { it.isNotBlank() }?.let { title ->
                        map[title.trim().lowercase()] = eff
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Gagal memuat katalog detail effect dari Firebase: ${t.message}")
        }
        return map
    }

    /**
     * Mengambil daftar Profile Effects secara dinamis dari Firebase Firestore.
     * Aman terhadap null, data kosong, maupun kondisi offline tanpa menyebabkan force close.
     */
    fun getAllProfileEffects(context: Context): List<ProfileEffectDefinition> {
        cachedEffects?.let { return it }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            repoScope.launch { runCatching { getAllProfileEffects(context) } }
            return emptyList()
        }

        synchronized(this) {
            cachedEffects?.let { return it }

            return try {
                val cfg = loadRemoteConfigBlocking(context)
                if (cfg == null || !cfg.isActive || cfg.effectChunkDocs.isEmpty()) {
                    return emptyList()
                }

                val catalogMap = loadCatalogEffects(context, cfg.effectsCatalogDoc)
                val itemType = object : TypeToken<List<RawEffectDto>>() {}.type
                val combinedRaw = mutableListOf<RawEffectDto>()

                for (docName in cfg.effectChunkDocs) {
                    val payload = fetchPayloadDocBlocking(context, docName) ?: continue
                    val parsed: List<RawEffectDto>? = try {
                        gson.fromJson(payload, itemType)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Gagal parse chunk effect $docName: ${e.message}")
                        null
                    }
                    if (!parsed.isNullOrEmpty()) {
                        combinedRaw.addAll(parsed)
                    }
                }

                if (combinedRaw.isEmpty()) return emptyList()

                val total = combinedRaw.size
                val starterEnd = (total * 0.15).toInt()
                val highEnd = (total * 0.80).toInt()

                val result = combinedRaw.mapIndexed { index, raw ->
                    val resolvedId = raw.id?.takeIf { it.isNotBlank() }
                        ?: raw.uuid
                        ?: "effect_$index"
                    val resolvedName = raw.name?.takeIf { it.isNotBlank() } ?: "Profile Effect #$index"

                    val catalogItem = (raw.id?.let { catalogMap[it] })
                        ?: (raw.name?.let { catalogMap[it.trim().lowercase()] })

                    val staticUrl = catalogItem?.thumbnailPreviewSrc?.takeIf { it.isNotBlank() }
                        ?: catalogItem?.reducedMotionSrc?.takeIf { it.isNotBlank() }
                        ?: catalogItem?.staticFrameSrc?.takeIf { it.isNotBlank() }
                        ?: raw.icon?.takeIf { it.isNotBlank() }
                        ?: raw.bakedCardUrl?.takeIf { it.isNotBlank() }
                        ?: raw.animatedIcon.orEmpty()

                    val rawLayers = raw.effects ?: raw.layers
                    val catalogLayers = catalogItem?.layers ?: catalogItem?.effects
                    val bestLayers = when {
                        !rawLayers.isNullOrEmpty() && (catalogLayers.isNullOrEmpty() || rawLayers.size >= catalogLayers.size) -> rawLayers
                        !catalogLayers.isNullOrEmpty() -> catalogLayers
                        else -> rawLayers
                    }

                    val loopLayer = bestLayers?.firstOrNull { it.loop == true }?.src
                        ?: bestLayers?.lastOrNull()?.src
                    val animUrl = loopLayer?.takeIf { it.isNotBlank() }
                        ?: raw.animatedIcon?.takeIf { it.isNotBlank() }
                        ?: raw.icon.orEmpty()

                    val parsedLayers = mutableListOf<ProfileEffectLayer>()
                    if (!bestLayers.isNullOrEmpty()) {
                        bestLayers.forEachIndexed { idx, lay ->
                            val s = lay.src?.takeIf { it.isNotBlank() }
                            if (s != null) {
                                parsedLayers.add(
                                    ProfileEffectLayer(
                                        src = s,
                                        loop = lay.loop ?: true,
                                        start = lay.start ?: 0L,
                                        duration = lay.duration ?: 3000L,
                                        loopDelay = lay.loopDelay ?: 0L,
                                        positionY = lay.position?.y ?: 0,
                                        zIndex = lay.zIndex ?: (100 + idx)
                                    )
                                )
                            }
                        }
                    }

                    if (parsedLayers.isEmpty() && animUrl.isNotBlank()) {
                        parsedLayers.add(
                            ProfileEffectLayer(
                                src = animUrl,
                                loop = true,
                                start = 0L,
                                duration = 3800L,
                                loopDelay = 0L,
                                positionY = 0,
                                zIndex = 100
                            )
                        )
                    }

                    val layerUrls = parsedLayers.map { it.src }.ifEmpty {
                        listOf(animUrl).filter { it.isNotBlank() }
                    }

                    val desc = raw.accessibilityLabel?.takeIf { it.isNotBlank() }
                        ?: catalogItem?.accessibilityLabel?.takeIf { it.isNotBlank() }
                        ?: catalogItem?.description?.takeIf { it.isNotBlank() }
                        ?: raw.label?.takeIf { it.isNotBlank() }
                        ?: raw.category?.takeIf { it.isNotBlank() }
                        ?: cfg.defaultEffectDesc
                    val category = raw.category?.takeIf { it.isNotBlank() }
                        ?: raw.productCategory?.takeIf { it.isNotBlank() }
                        ?: cfg.defaultCategory

                    when {
                        index < starterEnd -> {
                            val cost = 500L + ((index % 11) * 100L)
                            ProfileEffectDefinition(
                                id = resolvedId,
                                name = resolvedName,
                                animationUrl = animUrl,
                                pointCost = cost,
                                description = desc,
                                tier = "A",
                                requiredLevel = 1,
                                category = category,
                                staticImageUrl = staticUrl,
                                layerUrls = layerUrls,
                                layers = parsedLayers
                            )
                        }
                        index < highEnd -> {
                            val cost = 3500L + ((index % 44) * 500L)
                            ProfileEffectDefinition(
                                id = resolvedId,
                                name = resolvedName,
                                animationUrl = animUrl,
                                pointCost = cost,
                                description = desc,
                                tier = "B",
                                requiredLevel = 35,
                                category = category,
                                staticImageUrl = staticUrl,
                                layerUrls = layerUrls,
                                layers = parsedLayers
                            )
                        }
                        else -> {
                            ProfileEffectDefinition(
                                id = resolvedId,
                                name = resolvedName,
                                animationUrl = animUrl,
                                pointCost = 0L,
                                description = desc,
                                tier = "C",
                                requiredLevel = 0,
                                category = category,
                                staticImageUrl = staticUrl,
                                layerUrls = layerUrls,
                                layers = parsedLayers
                            )
                        }
                    }
                }

                cachedEffects = result
                _catalogRevision.value = _catalogRevision.value + 1
                result
            } catch (t: Throwable) {
                Log.w(TAG, "Gagal memuat katalog profile effects dari Firebase: ${t.message}")
                emptyList()
            }
        }
    }

    /**
     * Optimasi ukuran resolusi dan status animasi URL kosmetik memakai parameter dinamis dari Firebase.
     */
    fun getOptimizedUrl(url: String, size: Int = 160, animated: Boolean = true): String {
        if (url.isBlank()) return ""
        val cfg = cachedConfig
        val marker = cfg?.presetPathMarker?.takeIf { it.isNotBlank() }
        val matchesPresetMarker = marker != null && url.contains(marker, ignoreCase = true)
        val hasPassthroughParam = url.contains("passthrough=", ignoreCase = true)

        if (matchesPresetMarker || hasPassthroughParam) {
            val base = url.substringBefore("?")
            val template = cfg?.queryParamTemplate?.takeIf { it.isNotBlank() }
                ?: "size={size}&passthrough={animated}"
            val query = template
                .replace("{size}", size.toString())
                .replace("{animated}", animated.toString())
            return "$base?$query"
        }
        return url
    }

    fun getBorderById(context: Context, id: String): BorderDefinition? {
        if (id.isBlank()) return null
        return try {
            getAllBorders(context).firstOrNull { it.id == id || it.imageUrl == id }
        } catch (_: Throwable) {
            null
        }
    }

    fun getProfileEffectById(context: Context, id: String): ProfileEffectDefinition? {
        if (id.isBlank()) return null
        return try {
            getAllProfileEffects(context).firstOrNull { it.id == id || it.animationUrl == id }
        } catch (_: Throwable) {
            null
        }
    }

    fun findEffect(context: Context, urlOrId: String): ProfileEffectDefinition? {
        if (urlOrId.isBlank()) return null
        return try {
            getAllProfileEffects(context).firstOrNull {
                it.id == urlOrId ||
                    it.animationUrl == urlOrId ||
                    it.layerUrls.contains(urlOrId) ||
                    it.staticImageUrl == urlOrId
            }
        } catch (_: Throwable) {
            null
        }
    }
}
