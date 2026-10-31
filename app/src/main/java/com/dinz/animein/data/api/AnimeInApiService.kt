package com.dinz.animein.data.api

import com.dinz.animein.data.model.AnimeHomeResponse
import com.dinz.animein.data.model.CommunityCoverResponse
import com.dinz.animein.data.model.CommunityPosterResponse
import com.dinz.animein.data.model.ContributorResponse
import com.dinz.animein.data.model.CuplixResponse
import com.dinz.animein.data.model.DiscussionResponse
import com.dinz.animein.data.model.EpisodeListResponse
import com.dinz.animein.data.model.MovieDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeInApiService {

    @GET("/3/2/home/data")
    suspend fun getHomeData(
        @Query("day") day: String = "SENIN",
        @Query("limit") limit: Int = 16
    ): AnimeHomeResponse

    @GET("/data/fyp2/list_scroll")
    suspend fun getCuplixFeed(
        @Query("limit") limit: Int = 15,
        @Query("sort") sort: String = "new"
    ): CuplixResponse

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
}
