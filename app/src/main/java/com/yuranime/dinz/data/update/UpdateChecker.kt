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

            val isNewer = isNewerVersion(currentVersionName, tagName)

            AppUpdateInfo(
                isUpdateAvailable = if (forceCheck) true else isNewer,
                latestVersion = tagName.removePrefix("v").removePrefix("V"),
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

    /**
     * Membandingkan semver current vs latestTag
     * Mendukung format semver seperti 1.9.0 vs v1.9.1, v1.9.0-build-17 vs 1.9.0, dsb.
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        if (latest.isBlank()) return false
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")
        val cleanLatest = latest.trim().removePrefix("v").removePrefix("V")

        if (cleanCurrent == cleanLatest) return false

        val currentSemVer = parseSemVer(cleanCurrent)
        val latestSemVer = parseSemVer(cleanLatest)

        return latestSemVer > currentSemVer
    }

    private data class ParsedVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val build: Int
    ) : Comparable<ParsedVersion> {
        override fun compareTo(other: ParsedVersion): Int {
            if (this.major != other.major) return this.major.compareTo(other.major)
            if (this.minor != other.minor) return this.minor.compareTo(other.minor)
            if (this.patch != other.patch) return this.patch.compareTo(other.patch)
            return this.build.compareTo(other.build)
        }
    }

    private fun parseSemVer(ver: String): ParsedVersion {
        var buildNumber = 0
        var mainPart = ver

        // Cek pola -build-17 atau .build.17 atau sejenisnya
        if (ver.contains("-build-", ignoreCase = true)) {
            val parts = ver.split("-build-", ignoreCase = true)
            mainPart = parts[0]
            buildNumber = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        } else if (ver.contains("-b", ignoreCase = true)) {
            val parts = ver.split("-b", ignoreCase = true)
            mainPart = parts[0]
            buildNumber = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        }

        val numbers = mainPart.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }
        val major = numbers.getOrNull(0) ?: 0
        val minor = numbers.getOrNull(1) ?: 0
        val patch = numbers.getOrNull(2) ?: 0

        return ParsedVersion(major, minor, patch, buildNumber)
    }
}
