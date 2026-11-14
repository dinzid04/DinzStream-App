package com.yuranime.dinz.data.repository

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.yuranime.dinz.data.firebase.BorderDefinition
import com.yuranime.dinz.data.firebase.ProfileEffectDefinition
import java.io.InputStreamReader

object DiscordCosmeticsRepository {

    private data class RawDiscordDecorationDto(
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

    private data class RawDiscordEffectDto(
        @SerializedName("id") val id: String?,
        @SerializedName("uuid") val uuid: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("category") val category: String?,
        @SerializedName("product_category") val productCategory: String?,
        @SerializedName("icon") val icon: String?,
        @SerializedName("animated_icon") val animatedIcon: String?,
        @SerializedName("label") val label: String?,
        @SerializedName("accessibility_label") val accessibilityLabel: String?,
        @SerializedName("bakedCardUrl") val bakedCardUrl: String?
    )

    private data class CatalogLayerDto(
        @SerializedName("src") val src: String?,
        @SerializedName("loop") val loop: Boolean?,
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
        @SerializedName("layers") val layers: List<CatalogLayerDto>?
    )

    private data class CatalogCollectionDto(
        @SerializedName("category") val category: String?,
        @SerializedName("effects") val effects: List<CatalogEffectDto>?
    )

    private data class CatalogRootDto(
        @SerializedName("collections") val collections: List<CatalogCollectionDto>?
    )

    @Volatile
    private var cachedBorders: List<BorderDefinition>? = null

    @Volatile
    private var cachedEffects: List<ProfileEffectDefinition>? = null

    private val gson = GsonBuilder().setLenient().serializeNulls().create()

    /**
     * Membaca dan mem-parse seluruh 985 Avatar Decoration Discord asli dari asset JSON
     */
    fun getAllBorders(context: Context): List<BorderDefinition> {
        cachedBorders?.let { return it }

        synchronized(this) {
            cachedBorders?.let { return it }

            return try {
                context.assets.open("discord_assets/discord_avatar_decorations_complete_985.json").use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                        val itemType = object : com.google.gson.reflect.TypeToken<List<RawDiscordDecorationDto>>() {}.type
                        val rawList: List<RawDiscordDecorationDto> = gson.fromJson(reader, itemType) ?: emptyList()

                        val total = rawList.size
                        val starterEnd = (total * 0.15).toInt() // ~15% Starter
                        val highEnd = (total * 0.80).toInt()    // ~65% Standard & High (Lv. 35+)

                        val result = rawList.mapIndexed { index, raw ->
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
                                ?: "Dekorasi Avatar Discord Premium"
                            val category = raw.category?.takeIf { it.isNotBlank() }
                                ?: raw.productCategory?.takeIf { it.isNotBlank() }
                                ?: "Discord"

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
                        result
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    private fun loadCatalogEffects(context: Context): Map<String, CatalogEffectDto> {
        val map = mutableMapOf<String, CatalogEffectDto>()
        try {
            context.assets.open("discord_assets/discord_profile_effects_catalog.json").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    val root = gson.fromJson(reader, CatalogRootDto::class.java)
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
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    /**
     * Membaca dan mem-parse seluruh Profile Effects Discord asli dari asset JSON (396 lengkap + catalog)
     */
    fun getAllProfileEffects(context: Context): List<ProfileEffectDefinition> {
        cachedEffects?.let { return it }

        synchronized(this) {
            cachedEffects?.let { return it }

            return try {
                val catalogMap = loadCatalogEffects(context)

                context.assets.open("discord_assets/discord_profile_effects_complete_396.json").use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                        val itemType = object : com.google.gson.reflect.TypeToken<List<RawDiscordEffectDto>>() {}.type
                        val rawList: List<RawDiscordEffectDto> = gson.fromJson(reader, itemType) ?: emptyList()

                        val total = rawList.size
                        val starterEnd = (total * 0.15).toInt() // ~15% Starter
                        val highEnd = (total * 0.80).toInt()    // ~65% Standard & High (Lv. 35+)

                        val result = rawList.mapIndexed { index, raw ->
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

                            val catalogLoopLayer = catalogItem?.layers?.firstOrNull { it.loop == true }?.src
                                ?: catalogItem?.layers?.lastOrNull()?.src
                            val animUrl = catalogLoopLayer?.takeIf { it.isNotBlank() }
                                ?: raw.animatedIcon?.takeIf { it.isNotBlank() }
                                ?: raw.icon.orEmpty()

                            val layerUrls = catalogItem?.layers?.mapNotNull { it.src?.takeIf { s -> s.isNotBlank() } }
                                ?.ifEmpty { listOf(animUrl).filter { it.isNotBlank() } }
                                ?: listOf(animUrl).filter { it.isNotBlank() }

                            val desc = raw.accessibilityLabel?.takeIf { it.isNotBlank() }
                                ?: catalogItem?.accessibilityLabel?.takeIf { it.isNotBlank() }
                                ?: catalogItem?.description?.takeIf { it.isNotBlank() }
                                ?: raw.label?.takeIf { it.isNotBlank() }
                                ?: raw.category?.takeIf { it.isNotBlank() }
                                ?: "Efek Animasi Profil Discord Premium"
                            val category = raw.category?.takeIf { it.isNotBlank() }
                                ?: raw.productCategory?.takeIf { it.isNotBlank() }
                                ?: "Discord"

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
                                        layerUrls = layerUrls
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
                                        layerUrls = layerUrls
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
                                        layerUrls = layerUrls
                                    )
                                }
                            }
                        }
                        cachedEffects = result
                        result
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Optimasi ukuran resolusi dan status animasi URL CDN Discord
     * Hanya berlaku untuk avatar-decoration-presets (DO NOT append query params to profile_effects / collectibles-shop)
     */
    fun getOptimizedUrl(url: String, size: Int = 160, animated: Boolean = true): String {
        if (url.isBlank()) return ""
        return if (url.contains("cdn.discordapp.com/avatar-decoration-presets/")) {
            val base = url.substringBefore("?")
            "$base?size=$size&passthrough=$animated"
        } else {
            url
        }
    }

    fun getBorderById(context: Context, id: String): BorderDefinition? {
        if (id.isBlank()) return null
        return getAllBorders(context).firstOrNull { it.id == id || it.imageUrl == id }
    }

    fun getProfileEffectById(context: Context, id: String): ProfileEffectDefinition? {
        if (id.isBlank()) return null
        return getAllProfileEffects(context).firstOrNull { it.id == id || it.animationUrl == id }
    }
}
