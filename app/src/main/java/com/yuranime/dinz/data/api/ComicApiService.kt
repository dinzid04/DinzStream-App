package com.yuranime.dinz.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// ==================== DTO MODELS ====================

data class ShngmBaseResponse<T>(
    @SerializedName("retcode") val retcode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)

data class ShngmSliderResponse(
    @SerializedName("data") val data: List<ShngmSliderDto>? = null
)

data class ShngmSliderDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("rating") val rating: Any? = null,
    @SerializedName("background_image") val backgroundImage: String? = null,
    @SerializedName("chara_image") val charaImage: String? = null,
    @SerializedName("slider_link") val sliderLink: String? = null,
    @SerializedName("detail") val detail: String? = null
)

data class ShngmMangaDto(
    @SerializedName("manga_id") val mangaId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("alternative_title") val alternativeTitle: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("country_id") val countryId: String? = null,
    @SerializedName("cover_portrait_url") val coverPortraitUrl: String? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("user_rate") val userRate: Any? = null,
    @SerializedName("view_count") val viewCount: Long? = null,
    @SerializedName("bookmark_count") val bookmarkCount: Long? = null,
    @SerializedName("status") val status: Any? = null,
    @SerializedName("release_year") val releaseYear: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("latest_chapter_id") val latestChapterId: String? = null,
    @SerializedName("latest_chapter_number") val latestChapterNumber: Any? = null,
    @SerializedName("latest_chapter_time") val latestChapterTime: String? = null,
    @SerializedName("taxonomy") val taxonomy: Any? = null
)

data class ShngmChapterDto(
    @SerializedName("chapter_id") val chapterId: String? = null,
    @SerializedName("manga_id") val mangaId: String? = null,
    @SerializedName("chapter_title") val chapterTitle: String? = null,
    @SerializedName("chapter_number") val chapterNumber: Any? = null,
    @SerializedName("thumbnail_image_url") val thumbnailImageUrl: String? = null,
    @SerializedName("view_count") val viewCount: Long? = null,
    @SerializedName("release_date") val releaseDate: String? = null
)

data class ShngmChapterDetailDto(
    @SerializedName("chapter_id") val chapterId: String? = null,
    @SerializedName("manga_id") val mangaId: String? = null,
    @SerializedName("chapter_number") val chapterNumber: Any? = null,
    @SerializedName("chapter_title") val chapterTitle: String? = null,
    @SerializedName("prev_chapter_id") val prevChapterId: String? = null,
    @SerializedName("next_chapter_id") val nextChapterId: String? = null,
    @SerializedName("base_url") val baseUrl: String? = null,
    @SerializedName("chapter") val chapter: ShngmChapterImagesDto? = null
)

data class ShngmChapterImagesDto(
    @SerializedName("path") val path: String? = null,
    @SerializedName("data") val data: List<String>? = null
)

// ==================== RETROFIT SERVICE ====================

interface ComicApiService {

    @GET
    suspend fun getBanners(
        @Url url: String = "https://slider.shinigami.io/v1/slider/web"
    ): ShngmSliderResponse

    @GET("manga/list")
    suspend fun getLatestManga(
        @Query("sort") sort: String = "latest",
        @Query("sort_order") sortOrder: String = "desc",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 12
    ): ShngmBaseResponse<List<ShngmMangaDto>>

    @GET("manga/list")
    suspend fun getTrendingManga(
        @Query("is_recommended") isRecommended: Boolean = true,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 12
    ): ShngmBaseResponse<List<ShngmMangaDto>>

    @GET("manga/top")
    suspend fun getPopularManga(
        @Query("filter") filter: String = "all_time",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 12
    ): ShngmBaseResponse<List<ShngmMangaDto>>

    @GET("manga/list")
    suspend fun searchManga(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ShngmBaseResponse<List<ShngmMangaDto>>

    @GET("manga/list")
    suspend fun getMangaList(
        @Query("q") query: String? = null,
        @Query("sort") sort: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("status") status: String? = null,
        @Query("format") format: String? = null,
        @Query("genre") genre: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 24
    ): ShngmBaseResponse<List<ShngmMangaDto>>

    @GET("chapter/{mangaId}/list")
    suspend fun getMangaChapters(
        @Path("mangaId") mangaId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("sort_by") sortBy: String = "chapter_number",
        @Query("sort_order") sortOrder: String = "desc"
    ): ShngmBaseResponse<List<ShngmChapterDto>>

    @GET("manga/detail/{mangaId}")
    suspend fun getMangaDetail(
        @Path("mangaId") mangaId: String
    ): ShngmBaseResponse<ShngmMangaDto>

    @GET("chapter/detail/{chapterId}")
    suspend fun getChapterDetail(
        @Path("chapterId") chapterId: String
    ): ShngmBaseResponse<ShngmChapterDetailDto>

    companion object {
        private const val BASE_URL = "https://api.shngm.io/v1/"

        fun create(): ComicApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Accept", "application/json, text/plain, */*")
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                        )
                        .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Referer", "https://app.shinigami.asia/")
                        .header("Origin", "https://app.shinigami.asia")
                        .header("sec-ch-ua", "\"Chromium\";v=\"126\", \"Not?A_Brand\";v=\"24\"")
                        .header("sec-ch-ua-mobile", "?1")
                        .header("sec-ch-ua-platform", "\"Android\"")
                        .header("Sec-Fetch-Dest", "empty")
                        .header("Sec-Fetch-Mode", "cors")
                        .header("Sec-Fetch-Site", "cross-site")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ComicApiService::class.java)
        }
    }
}
