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
        @SerializedName("accessibility_label") val accessibilityLabel: String?
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
                                    // Starter Tier: 500 - 1.500 Poin, Level 1
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
                                    // Standard & High Tier: 3.500 - 25.000 Poin, Wajib Level 35
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
                                    // Mythic / Limited Tier: Redeem Code Only
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

    /**
     * Membaca dan mem-parse seluruh 396 Profile Effects Discord asli dari asset JSON
     */
    fun getAllProfileEffects(context: Context): List<ProfileEffectDefinition> {
        cachedEffects?.let { return it }

        synchronized(this) {
            cachedEffects?.let { return it }

            return try {
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
                            val animUrl = raw.animatedIcon?.takeIf { it.isNotBlank() }
                                ?: raw.icon.orEmpty()
                            val desc = raw.accessibilityLabel?.takeIf { it.isNotBlank() }
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
                                        category = category
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
                                        category = category
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
                                        category = category
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
     * Default size=160 untuk grid shop/edit profile agar instan dimuat dalam hitungan milidetik
     * size=240 untuk halaman profil utama
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
