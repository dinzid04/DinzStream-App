package com.yuranime.dinz.data.api

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://xyz-api.animein.net"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 ANIMEIN/5.2.2"
    private const val PROXY_SECRET = "animein-secure-proxy-key-123"

    val gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("x-proxy-secret", PROXY_SECRET)
            .header("Accept", "application/json")
            .header("Connection", "keep-alive")
            .build()
        chain.proceed(request)
    }

    private val responseSanitizerInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: okhttp3.Response? = null
        var lastException: Exception? = null

        // Auto retry hingga 2 kali jika koneksi bermasalah atau server 5xx
        for (attempt in 0..2) {
            try {
                response = chain.proceed(request)
                if (response.isSuccessful) break
                if (attempt < 2 && (response.code in 500..599 || response.code == 429)) {
                    response.close()
                    try { Thread.sleep((attempt + 1) * 300L) } catch (_: InterruptedException) {}
                    continue
                }
                break
            } catch (e: Exception) {
                lastException = e
                if (attempt < 2) {
                    try { Thread.sleep((attempt + 1) * 300L) } catch (_: InterruptedException) {}
                    continue
                } else {
                    throw e
                }
            }
        }

        if (response == null) {
            throw lastException ?: IOException("Gagal menghubungi server AnimeIn")
        }

        val body = response.body
        if (body != null) {
            val contentType = body.contentType()
            val raw = body.string()

            // Bersihkan string body response: hapus Byte Order Mark (BOM), spasi/baris kosong atau teks sampah sebelum { atau [
            var cleanText = raw.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
            val firstBrace = cleanText.indexOfAny(charArrayOf('{', '['))
            if (firstBrace > 0) {
                cleanText = cleanText.substring(firstBrace)
            } else if (firstBrace < 0 && (cleanText.startsWith("<") || cleanText.isBlank())) {
                // Server mengembalikan HTML error page / Cloudflare challenge
                cleanText = "{\"status\":\"error\",\"message\":\"Server sedang sibuk, memuat data fallback...\",\"data\":{}}"
            }

            val newBody = cleanText.toResponseBody(contentType)
            response = response.newBuilder().body(newBody).build()
        }

        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(responseSanitizerInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val apiService: AnimeInApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AnimeInApiService::class.java)
    }
}
