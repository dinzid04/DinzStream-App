package com.dinz.animein.data.model

import com.google.gson.annotations.SerializedName

fun normalizeImageUrl(url: String?): String {
    if (url.isNullOrBlank()) return ""
    val trimmed = url.trim()
    val direct = if (trimmed.startsWith("//")) {
        "https:$trimmed"
    } else if (trimmed.startsWith("/")) {
        "https://xyz-api.animein.net$trimmed"
    } else {
        trimmed
    }

    return if (direct.startsWith("http")) {
        "https://cf1.dinzid.my.id/?url=" + java.net.URLEncoder.encode(direct, "UTF-8")
    } else direct
}

// ==================== HOME MODELS ====================

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
    @SerializedName("genre") val genre: Any? = null,
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
}

// ==================== CUPLIX MODELS ====================

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
    @SerializedName(value = "count_views", alternate = ["views", "view", "total_views", "count_view"]) val countViews: String? = null,
    @SerializedName(value = "count_likes", alternate = ["likes", "like", "total_likes", "count_like"]) val countLikes: String? = null,
    @SerializedName(value = "count_comments", alternate = ["comments", "comment", "total_comments", "count_comment"]) val countComments: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("pokemon") val pokemon: String? = null,
    @SerializedName("time_start") val timeStart: String? = null,
    @SerializedName("time_end") val timeEnd: String? = null,
    @SerializedName(value = "is_like", alternate = ["is_liked", "liked"]) val isLike: String? = null
) {
    fun getEffectiveThumbnail(): String {
        val raw = urlThumbnail ?: poster ?: ""
        return normalizeImageUrl(raw)
    }

    fun getEffectivePoster(): String {
        val raw = poster ?: urlThumbnail ?: ""
        return normalizeImageUrl(raw)
    }

    fun getPokemonAvatarUrl(): String {
        val p = pokemon?.toIntOrNull()?.coerceAtLeast(1) ?: 25
        return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/showdown/$p.gif"
    }

    fun getTimeStartMs(): Long = timeStart?.toLongOrNull() ?: 0L
    fun getTimeEndMs(): Long = timeEnd?.toLongOrNull() ?: 0L
}

data class ContinueWatchingItem(
    val id: String,
    val idMovie: String,
    val title: String,
    val episodeText: String,
    val imageUrl: String,
    val progressFraction: Float,
    val episodeId: String = "",
    val positionMs: Long = 0L
)

// ==================== DETAIL MODELS ====================

data class MovieDetailResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: MovieDetailData? = null
)

data class MovieDetailData(
    @SerializedName("movie") val movie: MovieDetail? = null,
    @SerializedName("episode") val latestEpisode: EpisodeItem? = null,
    @SerializedName("season") val seasons: List<SeasonItem>? = null
)

data class MovieDetail(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("synopsis") val synopsis: String? = null,
    @SerializedName("synonyms") val synonyms: String? = null,
    @SerializedName("image_poster") val imagePoster: String? = null,
    @SerializedName("image_cover") val imageCover: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("day") val day: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("views") val views: String? = null,
    @SerializedName("favorites") val favorites: String? = null,
    @SerializedName("studio") val studio: String? = null,
    @SerializedName("aired_start") val airedStart: String? = null,
    @SerializedName("aired_end") val airedEnd: String? = null,
    @SerializedName("genre") val genre: Any? = null,
    @SerializedName("is_fav") val isFav: String? = null
) {
    fun getDisplayGenre(): String {
        return when (genre) {
            is List<*> -> genre.filterIsInstance<String>().joinToString(", ")
            is String -> genre
            else -> "Anime"
        }.ifBlank { "Anime" }
    }

    fun getGenreList(): List<String> {
        return when (genre) {
            is List<*> -> genre.filterIsInstance<String>().filter { it.isNotBlank() }
            is String -> genre.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    fun getEffectivePoster(): String = normalizeImageUrl(imagePoster ?: imageCover ?: "")
    fun getEffectiveCover(): String = normalizeImageUrl(imageCover ?: imagePoster ?: "")
}

data class EpisodeListResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: EpisodeListData? = null
)

data class EpisodeListData(
    @SerializedName("episode") val episodes: List<EpisodeItem>? = null
)

data class EpisodeItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("index") val index: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("views") val views: String? = null,
    @SerializedName("id_movie") val idMovie: String? = null,
    @SerializedName("key_time") val keyTime: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("is_new") val isNew: String? = null
) {
    fun getEffectiveImage(): String = normalizeImageUrl(image ?: "")
}

data class SeasonItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("season") val season: String? = null,
    @SerializedName("views") val views: String? = null,
    @SerializedName("favorites") val favorites: String? = null,
    @SerializedName("image_cover") val imageCover: String? = null,
    @SerializedName("image_poster") val imagePoster: String? = null,
    @SerializedName("studio") val studio: String? = null,
    @SerializedName("aired_start") val airedStart: String? = null,
    @SerializedName("aired_end") val airedEnd: String? = null,
    @SerializedName("status") val status: String? = null
) {
    fun getEffectiveCover(): String = normalizeImageUrl(imageCover ?: imagePoster ?: "")
    fun getEffectivePoster(): String = normalizeImageUrl(imagePoster ?: imageCover ?: "")
}

data class CommunityPosterResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: CommunityPosterData? = null
)

data class CommunityPosterData(
    @SerializedName("poster") val posters: List<CommunityMediaItem>? = null
)

data class CommunityCoverResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: CommunityCoverData? = null
)

data class CommunityCoverData(
    @SerializedName("cover") val covers: List<CommunityMediaItem>? = null
)

data class CommunityMediaItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("point") val point: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("pokemon") val pokemon: String? = null,
    @SerializedName("is_pro") val isPro: String? = null,
    @SerializedName("rank") val rank: String? = null,
    @SerializedName("admin_choice") val adminChoice: String? = null
) {
    fun getEffectiveImage(): String = normalizeImageUrl(image ?: "")
}

data class DiscussionResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: DiscussionData? = null
)

data class DiscussionData(
    @SerializedName("discussion") val discussions: List<DiscussionItem>? = null
)

data class DiscussionItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("user_name") val userName: String? = null,
    @SerializedName("user_pokemon") val userPokemon: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("pro") val pro: String? = null
)

data class ContributorResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: ContributorData? = null
)

data class ContributorData(
    @SerializedName("contributor") val contributors: List<ContributorItem>? = null
)

data class ContributorItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("id_user") val idUser: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("views") val views: String? = null,
    @SerializedName("likes") val likes: String? = null,
    @SerializedName("pokemon") val pokemon: String? = null,
    @SerializedName("pro") val pro: String? = null,
    @SerializedName("rank") val rank: String? = null,
    @SerializedName("point") val point: String? = null
)

// ==================== STREAMING & COMMENTS ====================

data class StreamResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: StreamData? = null
)

data class StreamData(
    @SerializedName("episode") val episode: EpisodeItem? = null,
    @SerializedName("episode_next") val episodeNext: EpisodeItem? = null,
    @SerializedName("server") val servers: List<ServerItem>? = null
)

data class ServerItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("server_id") val serverId: String? = null
)

data class EpisodeCommentResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: EpisodeCommentData? = null
)

data class EpisodeCommentData(
    @SerializedName("comment") val comments: List<EpisodeCommentItem>? = null
)

data class EpisodeCommentItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName(value = "user_id", alternate = ["id_user", "userId"]) val userId: String? = null,
    @SerializedName(value = "user_name", alternate = ["username", "userName", "name"]) val userName: String? = null,
    @SerializedName(value = "user_pokemon", alternate = ["pokemon", "userPokemon"]) val userPokemon: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName(value = "time", alternate = ["key_time", "time_ago", "time_created"]) val time: String? = null,
    @SerializedName("like") val like: String? = null,
    @SerializedName("dislike") val dislike: String? = null,
    @SerializedName("score") val score: String? = null,
    @SerializedName("replay") val replay: String? = null,
    @SerializedName("pro") val pro: String? = null,
    @SerializedName("rank") val rank: String? = null
) {
    fun getPokemonAvatarUrl(): String {
        val p = userPokemon?.toIntOrNull() ?: 1
        return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/showdown/$p.gif"
    }
}

data class CommentReplyResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: CommentReplyData? = null
)

data class CommentReplyData(
    @SerializedName("comment_replay") val replies: List<EpisodeCommentItem>? = null
)

// ==================== SCHEDULE MODELS ====================

data class ScheduleResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: ScheduleData? = null
)

data class ScheduleData(
    @SerializedName("movie") val movies: List<AnimeItem>? = null
)

// ==================== EXPLORE / SEARCH MODELS ====================

data class ExploreGenreResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: ExploreGenreData? = null
)

data class ExploreGenreData(
    @SerializedName("genre") val genres: List<ExploreGenreItem>? = null
)

data class ExploreGenreItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("group") val group: String? = null
) {
    fun getEffectiveImage(): String = normalizeImageUrl(image)
}

data class ExploreYearResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: ExploreYearData? = null
)

data class ExploreYearData(
    @SerializedName("year") val years: List<ExploreYearItem>? = null
)

data class ExploreYearItem(
    @SerializedName("name") val name: String? = null,
    @SerializedName("image") val image: String? = null
) {
    fun getEffectiveImage(): String = normalizeImageUrl(image)
}

data class ExploreCharacterResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Boolean? = null,
    @SerializedName("data") val data: ExploreCharacterData? = null
)

data class ExploreCharacterData(
    @SerializedName("character") val characters: List<ExploreCharacterItem>? = null
)

data class ExploreCharacterItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("name_full") val nameFull: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image") val image: String? = null
) {
    fun getEffectiveImage(): String = normalizeImageUrl(image)
}


