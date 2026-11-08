package com.yuranime.dinz

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.Locale
import java.util.concurrent.TimeUnit

class YuranimeApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            try {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:400464992596:web:68fde66c88d351699610c1")
                    .setApiKey("AIzaSyAPZsUFl1RoV3bfMWWOsJ0M9yt_YPqrRSg")
                    .setProjectId("dinz-ai-gemini-copy")
                    .setStorageBucket("dinz-ai-gemini-copy.firebasestorage.app")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            } catch (ignored: Exception) {}
        }
        com.yuranime.dinz.data.local.AppPreferences.init(this)
        try {
            coil.Coil.setImageLoader(this)
        } catch (ignored: Exception) {}
    }

    override fun newImageLoader(): ImageLoader {
        val imageOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val host = originalRequest.url.host.lowercase(Locale.ROOT)
                val requestBuilder = originalRequest.newBuilder()

                val isComicDomain = host.contains("shngm") ||
                    host.contains("shinigami") ||
                    host.contains("komik")

                if (isComicDomain) {
                    requestBuilder
                        .header("Referer", "https://app.shinigami.asia/")
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                        )
                } else {
                    requestBuilder
                        .removeHeader("Referer")
                        .removeHeader("x-proxy-secret")
                        .header("User-Agent", "okhttp/4.12.0")
                }

                chain.proceed(requestBuilder.build())
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageOkHttpClient)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache_v3"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

