package com.yuranime.dinz.data.model

import androidx.compose.runtime.Immutable

enum class SkipSegmentType {
    INTRO,
    OUTRO
}

@Immutable
data class ExternalAnimeMapping(
    val cacheKey: String,
    val localTitle: String,
    val matchedTitle: String,
    val anilistId: Int? = null,
    val malId: Int? = null,
    val tmdbId: Int? = null,
    val tmdbSeason: Int? = null,
    val anidbId: Int? = null,
    val kitsuId: Int? = null,
    val seasonHint: Int? = null,
    val confidence: Float = 0f,
    val resolvedAtMs: Long = System.currentTimeMillis()
)

@Immutable
data class SkipSegment(
    val type: SkipSegmentType,
    val startMs: Long,
    val endMs: Long,
    val source: String
) {
    fun containsPosition(positionMs: Long): Boolean {
        return endMs > startMs && positionMs in startMs until endMs
    }
}

@Immutable
data class EpisodeSkipData(
    val cacheKey: String,
    val intro: SkipSegment? = null,
    val outro: SkipSegment? = null,
    val sourceSummary: String? = null,
    val mappingUsed: ExternalAnimeMapping? = null,
    val fetchedAtMs: Long = System.currentTimeMillis()
)

@Immutable
data class AniListVoiceActorInfo(
    val id: Int,
    val name: String,
    val nativeName: String? = null,
    val language: String,
    val imageUrl: String? = null
)

@Immutable
data class AniListCharacterEntry(
    val id: Int,
    val name: String,
    val nativeName: String? = null,
    val role: String,
    val imageUrl: String? = null,
    val voiceActors: List<AniListVoiceActorInfo> = emptyList()
) {
    fun formattedRole(): String = when (role.uppercase()) {
        "MAIN" -> "Main"
        "SUPPORTING" -> "Supporting"
        "BACKGROUND" -> "Background"
        else -> role.lowercase().replaceFirstChar { it.uppercase() }
    }
}

@Immutable
data class AniListStaffEntry(
    val id: Int,
    val name: String,
    val nativeName: String? = null,
    val role: String,
    val imageUrl: String? = null
)

@Immutable
data class AniListCreditsData(
    val anilistId: Int,
    val matchedTitle: String,
    val characters: List<AniListCharacterEntry> = emptyList(),
    val staff: List<AniListStaffEntry> = emptyList(),
    val availableVaLanguages: List<String> = emptyList(),
    val fetchedAtMs: Long = System.currentTimeMillis()
)
