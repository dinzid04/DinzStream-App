package com.dinz.animein.data.api

import com.dinz.animein.data.model.AnimeHomeResponse
import com.dinz.animein.data.model.CommentReplyResponse
import com.dinz.animein.data.model.CommunityCoverResponse
import com.dinz.animein.data.model.CommunityPosterResponse
import com.dinz.animein.data.model.ContributorResponse
import com.dinz.animein.data.model.CuplixResponse
import com.dinz.animein.data.model.DiscussionResponse
import com.dinz.animein.data.model.EpisodeCommentResponse
import com.dinz.animein.data.model.EpisodeListResponse
import com.dinz.animein.data.model.ExploreCharacterResponse
import com.dinz.animein.data.model.ExploreGenreResponse
import com.dinz.animein.data.model.ExploreYearResponse
import com.dinz.animein.data.model.MovieDetailResponse
import com.dinz.animein.data.model.ScheduleResponse
import com.dinz.animein.data.model.StreamResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeInApiService {

    @GET("/3/2/home/data")
    suspend fun getHomeData(
        @Query("day") day: String = "SENIN",
        @Query("limit") limit: Int = 16
    ): AnimeHomeResponse

    @GET("/3/2/home/new")
    suspend fun getHomeNewTitles(): AnimeHomeResponse

    @GET("/3/2/home/random")
    suspend fun getHomeRandom(): AnimeHomeResponse

    @GET("/data/fyp2/list_scroll")
    suspend fun getCuplixFeed(
        @Query("limit") limit: Int = 30,
        @Query("sort") sort: String = "views"
    ): CuplixResponse

    @GET("/data/fyp/comment")
    suspend fun getCuplixComments(
        @Query("id_fyp") idFyp: String
    ): EpisodeCommentResponse

    @GET("/3/2/explore/movie")
    suspend fun getExplore(
        @Query("page") page: Int = 0,
        @Query("sort") sort: String = "views",
        @Query("keyword") keyword: String? = null,
        @Query("genre_in") genreIn: String? = null
    ): AnimeHomeResponse

    // ==================== DETAIL ENDPOINTS ====================

    @GET("/3/2/movie/detail/{id}")
    suspend fun getMovieDetail(
        @Path("id") id: String
    ): MovieDetailResponse

    @GET("/3/2/movie/episode/{id}")
    suspend fun getMovieEpisodes(
        @Path("id") id: String,
        @Query("page") page: Int = 0
    ): EpisodeListResponse

    @GET("/data/movie/fyp/list_new")
    suspend fun getMovieCuplix(
        @Query("id_movie") idMovie: String,
        @Query("page") page: Int = 1
    ): CuplixResponse

    @GET("/3/2/movie_poster/data")
    suspend fun getMoviePosters(
        @Query("id_movie") idMovie: String
    ): CommunityPosterResponse

    @GET("/3/2/movie_cover/data")
    suspend fun getMovieCovers(
        @Query("id_movie") idMovie: String
    ): CommunityCoverResponse

    @GET("/data/movie/discussion/list")
    suspend fun getMovieDiscussions(
        @Query("id_movie") idMovie: String,
        @Query("page") page: Int = 1
    ): DiscussionResponse

    @GET("/data/movie/contributor/list")
    suspend fun getMovieContributors(
        @Query("id_movie") idMovie: String,
        @Query("page") page: Int = 1
    ): ContributorResponse

    // ==================== STREAMING & COMMENTS ====================

    @GET("/3/2/episode/streamnew/{id}")
    suspend fun getEpisodeStream(
        @Path("id") idEpisode: String
    ): StreamResponse

    @GET("/3/2/comment/data")
    suspend fun getEpisodeComments(
        @Query("id_episode") idEpisode: String,
        @Query("page") page: Int = 0,
        @Query("sort") sort: String = "new"
    ): EpisodeCommentResponse

    @GET("/data/comment_replay/data")
    suspend fun getCommentReplies(
        @Query("id_comment") idComment: String,
        @Query("page") page: Int = 1
    ): CommentReplyResponse

    // ==================== SCHEDULE ====================

    @GET("/3/2/schedule/data")
    suspend fun getSchedule(
        @Query("day") day: String
    ): ScheduleResponse

    // ==================== EXPLORE / SEARCH ====================

    @GET("/3/2/explore/genre")
    suspend fun getExploreGenres(): ExploreGenreResponse

    @GET("/3/2/explore/year")
    suspend fun getExploreYears(): ExploreYearResponse

    @GET("/3/2/explore/manra_npc")
    suspend fun getExploreCharacters(): ExploreCharacterResponse

    @GET("/3/2/explore/movie")
    suspend fun searchMovies(
        @Query("keyword") keyword: String? = null,
        @Query("genre_in") genreIn: String? = null,
        @Query("type") type: String? = null,
        @Query("sort") sort: String = "views",
        @Query("page") page: Int = 0
    ): AnimeHomeResponse

    @GET("/3/2/explore/movie_genre")
    suspend fun getExploreMovieGenre(
        @Query("id_genre") idGenre: String? = null,
        @Query("genre_in") genreIn: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 0
    ): AnimeHomeResponse

    @GET("/3/2/explore/movie_studio")
    suspend fun getExploreMovieStudio(
        @Query("studio") studio: String,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 0
    ): AnimeHomeResponse

    @GET("/3/2/explore/movie_type")
    suspend fun getExploreMovieType(
        @Query("type") type: String,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 0
    ): AnimeHomeResponse

    @GET("/3/2/explore/movie_year")
    suspend fun getExploreMovieYear(
        @Query("year") year: String,
        @Query("season") season: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 0
    ): AnimeHomeResponse
}
