package com.yuranime.dinz

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

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
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
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
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

