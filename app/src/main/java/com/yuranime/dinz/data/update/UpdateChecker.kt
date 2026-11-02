package com.yuranime.dinz.data.update

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val htmlUrl: String
)

object UpdateChecker {

    private const val GITHUB_OWNER = "dinzid04"
    private const val GITHUB_REPO = "DinzStream-App"
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val RAW_README_URL = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/README.md"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    suspend fun checkUpdate(currentVersionName: String, forceCheck: Boolean = false): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_API_URL)
                .header("User-Agent", "Yuranime-App/$currentVersionName")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)

            val tagName = jsonObject.get("tag_name")?.asString ?: ""
            val releaseTitle = jsonObject.get("name")?.asString ?: "Versi Terbaru $tagName"
            var releaseBody = jsonObject.get("body")?.asString ?: ""
            val htmlUrl = jsonObject.get("html_url")?.asString ?: "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"

            // Cari URL download APK langsung dari assets
            var apkDownloadUrl = ""
            val assetsArray = jsonObject.getAsJsonArray("assets")
            if (assetsArray != null && assetsArray.size() > 0) {
                for (i in 0 until assetsArray.size()) {
                    val asset = assetsArray.get(i).asJsonObject
                    val assetName = asset.get("name")?.asString ?: ""
                    val downloadUrl = asset.get("browser_download_url")?.asString ?: ""
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = downloadUrl
                        break
                    }
                }
            }

            if (apkDownloadUrl.isBlank()) {
                apkDownloadUrl = htmlUrl
            }

            // Jika releaseBody kosong atau singkat, ambil isi README.md repo untuk melengkapi catatan
            if (releaseBody.isBlank() || releaseBody.length < 50) {
                val readmeContent = fetchReadmeContent()
                if (!readmeContent.isNullOrBlank()) {
                    releaseBody = readmeContent
                }
            }

            val isNewer = isNewerVersion(remote = tagName, local = currentVersionName)

            AppUpdateInfo(
                isUpdateAvailable = if (forceCheck) true else isNewer,
                latestVersion = cleanVersion(tagName),
                releaseTitle = releaseTitle,
                releaseNotes = releaseBody,
                downloadUrl = apkDownloadUrl,
                htmlUrl = htmlUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchReadmeContent(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RAW_README_URL)
                .header("User-Agent", "Yuranime-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun cleanVersion(versionStr: String): String {
        var clean = versionStr.trim()
            .removePrefix("release-")
            .removePrefix("Release-")
            .removePrefix("v")
            .removePrefix("V")
        clean = clean.replace(Regex("(?i)[-_]build[-_]|[-_]b[-_]?"), ".")
        return clean
    }

    fun parseVersionSegments(versionStr: String): List<Int> {
        val cleaned = cleanVersion(versionStr)
        if (cleaned.isBlank()) return emptyList()
        return cleaned.split(".")
            .mapNotNull { segment ->
                segment.filter { it.isDigit() }.toIntOrNull()
            }
    }

    /**
     * Membandingkan strict semantic versioning numerik segmen demi segmen:
     * Menghitung apakah remoteVersion > localVersion.
     * Return true HANYA jika remote secara numerik lebih tinggi dari local.
     * Jika sama atau lebih rendah, atau gagal parsing, WAJIB return false.
     */
    fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteSegments = parseVersionSegments(remote)
        val localSegments = parseVersionSegments(local)

        if (remoteSegments.isEmpty() || localSegments.isEmpty()) return false

        val maxLen = maxOf(remoteSegments.size, localSegments.size)
        for (i in 0 until maxLen) {
            val r = remoteSegments.getOrElse(i) { 0 }
            val l = localSegments.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
