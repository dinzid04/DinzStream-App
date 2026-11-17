package com.yuranime.dinz.data.update

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.Inet6Address
import java.net.InetAddress
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
    private const val RELEASES_WEB_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val RAW_README_URL = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/README.md"

    private val ipv4PreferredDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return runCatching {
                Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet6Address) 1 else 0 }
            }.getOrElse { Dns.SYSTEM.lookup(hostname) }
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(ipv4PreferredDns)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val gson = GsonBuilder().setLenient().serializeNulls().create()

    suspend fun checkUpdate(currentVersionName: String, forceCheck: Boolean = false): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // 1. Utama: GitHub REST API (retry sekali jika timeout/5xx; skip retry jika rate-limit 403/429)
            fetchFromGitHubApi(currentVersionName, forceCheck)?.let { return@withContext it }

            // 2. Fallback Web: halaman publik GitHub Releases (bebas batas rate-limit API 60 req/jam)
            fetchFromGitHubWeb(currentVersionName, forceCheck)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchFromGitHubApi(
        currentVersionName: String,
        forceCheck: Boolean
    ): AppUpdateInfo? {
        for (attempt in 0..1) {
            try {
                val request = Request.Builder()
                    .url(RELEASES_API_URL)
                    .header("User-Agent", "Yuranime-App/$currentVersionName")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val (code, responseBody) = httpClient.newCall(request).execute().use { resp ->
                    resp.code to (if (resp.isSuccessful) resp.body?.string() else null)
                }

                if (code == 403 || code == 429) {
                    return null
                }
                if (!responseBody.isNullOrBlank()) {
                    val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                    val tagName = jsonObject.get("tag_name")?.asString ?: ""
                    if (tagName.isNotBlank()) {
                        val releaseTitle = jsonObject.get("name")?.asString ?: "Versi Terbaru $tagName"
                        var releaseBody = jsonObject.get("body")?.asString ?: ""
                        val htmlUrl = jsonObject.get("html_url")?.asString
                            ?: "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"

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

                        if (releaseBody.isBlank() || releaseBody.length < 50) {
                            val readmeContent = fetchReadmeContent()
                            if (!readmeContent.isNullOrBlank()) {
                                releaseBody = readmeContent
                            }
                        }

                        val isNewer = isNewerVersion(remote = tagName, local = currentVersionName)
                        return AppUpdateInfo(
                            isUpdateAvailable = if (forceCheck) true else isNewer,
                            latestVersion = cleanVersion(tagName),
                            releaseTitle = releaseTitle,
                            releaseNotes = releaseBody,
                            downloadUrl = apkDownloadUrl,
                            htmlUrl = htmlUrl
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Coba ulang sekali atau lanjut ke fallback Web
            }
            if (attempt == 0) delay(900L)
        }
        return null
    }

    private suspend fun fetchFromGitHubWeb(
        currentVersionName: String,
        forceCheck: Boolean
    ): AppUpdateInfo? {
        try {
            val req = Request.Builder()
                .url(RELEASES_WEB_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val pair = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null
                else resp.request.url.toString() to resp.body?.string().orEmpty()
            } ?: return null

            val (finalUrl, html) = pair
            if (html.isBlank()) return null

            val doc = Jsoup.parse(html)
            var tagName = finalUrl.substringAfter("/releases/tag/", missingDelimiterValue = "")
                .substringBefore("?")
                .substringBefore("#")
                .trim()
            if (tagName.isBlank()) {
                tagName = doc.selectFirst("a[href*='/releases/tag/']")
                    ?.attr("href")
                    ?.substringAfter("/releases/tag/", missingDelimiterValue = "")
                    ?.substringBefore("?")
                    ?.trim()
                    .orEmpty()
            }
            if (tagName.isBlank()) return null

            val htmlUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/tag/$tagName"
            val releaseTitle = doc.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotBlank() }
                ?: "Versi Terbaru $tagName"

            var releaseBody = doc.select(".markdown-body p, .markdown-body li")
                .mapNotNull { it.text().trim().takeIf { txt -> txt.isNotBlank() } }
                .joinToString("\n")
            if (releaseBody.isBlank() || releaseBody.length < 40) {
                releaseBody = fetchReadmeContent().orEmpty()
            }

            var apkDownloadUrl = ""
            runCatching {
                val assetsReq = Request.Builder()
                    .url("https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/expanded_assets/$tagName")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build()
                val assetsHtml = httpClient.newCall(assetsReq).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
                }
                if (assetsHtml.isNotBlank()) {
                    val apkHref = Jsoup.parse(assetsHtml)
                        .selectFirst("a[href\$=.apk]")
                        ?.attr("href")
                        ?.trim()
                    if (!apkHref.isNullOrBlank()) {
                        apkDownloadUrl = if (apkHref.startsWith("http")) apkHref else "https://github.com$apkHref"
                    }
                }
            }
            if (apkDownloadUrl.isBlank()) {
                apkDownloadUrl = htmlUrl
            }

            val isNewer = isNewerVersion(remote = tagName, local = currentVersionName)
            return AppUpdateInfo(
                isUpdateAvailable = if (forceCheck) true else isNewer,
                latestVersion = cleanVersion(tagName),
                releaseTitle = releaseTitle,
                releaseNotes = releaseBody,
                downloadUrl = apkDownloadUrl,
                htmlUrl = htmlUrl
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return null
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
