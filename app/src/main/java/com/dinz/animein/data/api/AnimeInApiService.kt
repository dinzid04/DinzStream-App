package com.dinz.animein.data.api

import com.dinz.animein.data.model.AnimeHomeResponse
import com.dinz.animein.data.model.CuplixResponse
import retrofit2.http.GET
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
}
