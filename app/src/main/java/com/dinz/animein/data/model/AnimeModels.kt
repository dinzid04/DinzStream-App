package com.dinz.animein.data.model

import com.google.gson.annotations.SerializedName

data class AnimeHomeResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: AnimeHomeData? = null
)

data class AnimeHomeData(
    @SerializedName("slider") val slider: List<SliderItem>? = null,
    @SerializedName("new") val newEpisodes: List<AnimeItem>? = null,
    @SerializedName("hot") val hotAnime: List<AnimeItem>? = null,
    @SerializedName("today") val todayAnime: List<AnimeItem>? = null,
    @SerializedName("fyp") val fypAnime: List<AnimeItem>? = null,
    @SerializedName("waiting") val waitingAnime: List<AnimeItem>? = null,
    @SerializedName("popular") val popularAnime: List<AnimeItem>? = null,
    @SerializedName("movie") val movies: List<AnimeItem>? = null
)

data class SliderItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("id_movie") val idMovie: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("synopsis") val synopsis: String? = null
)

data class AnimeItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("image_poster") val imagePoster: String? = null,
    @SerializedName("image_cover") val imageCover: String? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("index") val index: String? = null,
    @SerializedName("views") val views: String? = null,
    @SerializedName("favorites") val favorites: String? = null,
    @SerializedName("genre") val genre: Any? = null, // Can be String or List<String>
    @SerializedName("time") val time: String? = null,
    @SerializedName("aired_start") val airedStart: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("synopsis") val synopsis: String? = null
) {
    fun getDisplayGenre(): String {
        return when (genre) {
            is List<*> -> genre.filterIsInstance<String>().joinToString(", ")
            is String -> genre
            else -> "Action"
        }.ifBlank { "Action" }
    }

    fun getEffectivePoster(): String {
        val raw = imagePoster ?: poster ?: imageCover ?: ""
        return normalizeImageUrl(raw)
    }

    fun getEffectiveCover(): String {
        val raw = imageCover ?: imagePoster ?: poster ?: ""
        return normalizeImageUrl(raw)
    }

    private fun normalizeImageUrl(url: String): String {
        if (url.isBlank()) return ""
        val trimmed = url.trim()
        val direct = if (trimmed.startsWith("//")) "https:$trimmed"
        else if (trimmed.startsWith("/")) "https://xyz-api.animein.net$trimmed"
        else trimmed

        return if (direct.startsWith("http")) {
            "https://cf1.dinzid.my.id/?url=" + java.net.URLEncoder.encode(direct, "UTF-8")
        } else direct
    }
}

data class CuplixResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: CuplixData? = null
)

data class CuplixData(
    @SerializedName("fyp") val fypList: List<CuplixItem>? = null
)

data class CuplixItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("url_thumbnail") val urlThumbnail: String? = null,
    @SerializedName("id_episode") val idEpisode: String? = null,
    @SerializedName("id_movie") val idMovie: String? = null,
    @SerializedName("anime") val anime: String? = null,
    @SerializedName("episode") val episode: String? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("count_views") val countViews: String? = null,
    @SerializedName("count_likes") val countLikes: String? = null,
    @SerializedName("count_comments") val countComments: String? = null,
    @SerializedName("username") val username: String? = null
) {
    fun getEffectiveThumbnail(): String {
        val raw = urlThumbnail ?: poster ?: ""
        if (raw.isBlank()) return ""
        val direct = if (raw.startsWith("//")) "https:$raw" else raw
        return if (direct.startsWith("http")) {
            "https://cf1.dinzid.my.id/?url=" + java.net.URLEncoder.encode(direct, "UTF-8")
        } else direct
    }
}

data class ContinueWatchingItem(
    val id: String,
    val idMovie: String,
    val title: String,
    val episodeText: String,
    val imageUrl: String,
    val progressFraction: Float
)
