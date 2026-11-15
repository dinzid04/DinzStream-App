package com.yuranime.dinz.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.yuranime.dinz.data.local.AnimeExternalCacheStore
import com.yuranime.dinz.data.model.AniListCharacterEntry
import com.yuranime.dinz.data.model.AniListCreditsData
import com.yuranime.dinz.data.model.AniListStaffEntry
import com.yuranime.dinz.data.model.AniListVoiceActorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Mengambil data Karakter, Seiyuu (Voice Actor), dan Staff dari AniList GraphQL API
 * menggunakan deteksi judul/sinonim otomatis oleh AnimeIdentityResolver.
 */
object AniListCharacterRepository {

    private const val ANILIST_ENDPOINT = "https://graphql.anilist.co"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    private val CREDITS_BY_ID_QUERY = """
        query (${'$'}id: Int) {
          Media(id: ${'$'}id, type: ANIME) {
            id
            title {
              romaji
              english
              native
            }
            characters(sort: [ROLE, RELEVANCE, ID], perPage: 30) {
              edges {
                role
                node {
                  id
                  name {
                    full
                    native
                  }
                  image {
                    large
                    medium
                  }
                }
                voiceActors(sort: [LANGUAGE, RELEVANCE, ID]) {
                  id
                  name {
                    full
                    native
                  }
                  languageV2
                  image {
                    large
                    medium
                  }
                }
              }
            }
            staff(sort: [RELEVANCE, ID], perPage: 25) {
              edges {
                role
                node {
                  id
                  name {
                    full
                    native
                  }
                  image {
                    large
                    medium
                  }
                }
              }
            }
          }
        }
    """.trimIndent()

    private val CREDITS_BY_MAL_QUERY = CREDITS_BY_ID_QUERY.replace(
        "Media(id: \$id,",
        "Media(idMal: \$id,"
    )

    suspend fun getCreditsForAnime(
        title: String,
        synonymsRaw: String? = null,
        yearHint: String? = null,
        airedStartHint: String? = null,
        expectedEpisodeCount: Int? = null,
        formatHint: String? = null,
        movieId: String? = null,
        forceRefresh: Boolean = false
    ): Result<AniListCreditsData> = withContext(Dispatchers.IO) {
        val mapping = AnimeIdentityResolver.resolveMapping(
            title = title,
            synonymsRaw = synonymsRaw,
            yearHint = yearHint,
            airedStartHint = airedStartHint,
            expectedEpisodeCount = expectedEpisodeCount,
            formatHint = formatHint,
            movieId = movieId,
            forceRefresh = forceRefresh
        ) ?: return@withContext Result.failure(
            IllegalStateException("Anime tidak ditemukan otomatis di AniList")
        )

        val anilistId = mapping.anilistId
        if (!forceRefresh && anilistId != null && anilistId > 0) {
            AnimeExternalCacheStore.getCredits(anilistId)?.let { cached ->
                return@withContext Result.success(cached)
            }
        }

        val queryToRun: String
        val idToUse: Int
        if (anilistId != null && anilistId > 0) {
            queryToRun = CREDITS_BY_ID_QUERY
            idToUse = anilistId
        } else if (mapping.malId != null && mapping.malId > 0) {
            queryToRun = CREDITS_BY_MAL_QUERY
            idToUse = mapping.malId
        } else {
            return@withContext Result.failure(
                IllegalStateException("ID AniList tidak tersedia untuk anime ini")
            )
        }

        val payload = JsonObject().apply {
            addProperty("query", queryToRun)
            add("variables", JsonObject().apply { addProperty("id", idToUse) })
        }
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val req = Request.Builder()
            .url(ANILIST_ENDPOINT)
            .post(body)
            .header("Accept", "application/json")
            .build()

        runCatching {
            AnimeIdentityResolver.externalHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Gagal memuat data AniList (${resp.code})")
                }
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) {
                    throw IllegalStateException("Respon AniList kosong")
                }
                val root = JsonParser.parseString(raw).asJsonObject
                val media = root.objOrNull("data")?.objOrNull("Media")
                    ?: throw IllegalStateException("Media tidak ditemukan di AniList")

                val resolvedId = media.intOrNull("id") ?: idToUse
                val titleObj = media.objOrNull("title")
                val matchedTitle = titleObj?.strOrNull("romaji")?.takeIf { it.isNotBlank() }
                    ?: titleObj?.strOrNull("english")?.takeIf { it.isNotBlank() }
                    ?: mapping.matchedTitle

                val characters = parseCharacters(media.objOrNull("characters")?.arrOrNull("edges"))
                val staff = parseStaff(media.objOrNull("staff")?.arrOrNull("edges"))
                val languages = extractOrderedLanguages(characters)

                val data = AniListCreditsData(
                    anilistId = resolvedId,
                    matchedTitle = matchedTitle,
                    characters = characters,
                    staff = staff,
                    availableVaLanguages = languages
                )
                AnimeExternalCacheStore.putCredits(data)
                data
            }
        }
    }

    private fun parseCharacters(edges: JsonArray?): List<AniListCharacterEntry> {
        if (edges == null) return emptyList()
        val list = mutableListOf<AniListCharacterEntry>()
        for (elem in edges) {
            if (!elem.isJsonObject) continue
            val edge = elem.asJsonObject
            val role = edge.strOrNull("role") ?: "SUPPORTING"
            val node = edge.objOrNull("node") ?: continue
            val charId = node.intOrNull("id") ?: continue
            val nameObj = node.objOrNull("name")
            val fullName = nameObj?.strOrNull("full")?.takeIf { it.isNotBlank() } ?: continue
            val nativeName = nameObj.strOrNull("native")?.takeIf { it.isNotBlank() }
            val imageObj = node.objOrNull("image")
            val charImg = imageObj?.strOrNull("large")?.takeIf { it.isNotBlank() }
                ?: imageObj?.strOrNull("medium")?.takeIf { it.isNotBlank() }

            val vaArr = edge.arrOrNull("voiceActors")
            val vas = mutableListOf<AniListVoiceActorInfo>()
            if (vaArr != null) {
                for (vaElem in vaArr) {
                    if (!vaElem.isJsonObject) continue
                    val vaObj = vaElem.asJsonObject
                    val vaId = vaObj.intOrNull("id") ?: continue
                    val vaNameObj = vaObj.objOrNull("name")
                    val vaName = vaNameObj?.strOrNull("full")?.takeIf { it.isNotBlank() } ?: continue
                    val vaNative = vaNameObj.strOrNull("native")?.takeIf { it.isNotBlank() }
                    val lang = vaObj.strOrNull("languageV2")?.takeIf { it.isNotBlank() } ?: "Japanese"
                    val vaImgObj = vaObj.objOrNull("image")
                    val vaImg = vaImgObj?.strOrNull("large")?.takeIf { it.isNotBlank() }
                        ?: vaImgObj?.strOrNull("medium")?.takeIf { it.isNotBlank() }

                    vas.add(
                        AniListVoiceActorInfo(
                            id = vaId,
                            name = vaName,
                            nativeName = vaNative,
                            language = lang,
                            imageUrl = vaImg
                        )
                    )
                }
            }

            list.add(
                AniListCharacterEntry(
                    id = charId,
                    name = fullName,
                    nativeName = nativeName,
                    role = role,
                    imageUrl = charImg,
                    voiceActors = vas
                )
            )
        }
        return list
    }

    private fun parseStaff(edges: JsonArray?): List<AniListStaffEntry> {
        if (edges == null) return emptyList()
        val grouped = linkedMapOf<Int, MutableList<Pair<AniListStaffEntry, String>>>()

        for (elem in edges) {
            if (!elem.isJsonObject) continue
            val edge = elem.asJsonObject
            val role = edge.strOrNull("role")?.trim().takeUnless { it.isNullOrBlank() } ?: "Staff"
            val node = edge.objOrNull("node") ?: continue
            val staffId = node.intOrNull("id") ?: continue
            val nameObj = node.objOrNull("name")
            val fullName = nameObj?.strOrNull("full")?.takeIf { it.isNotBlank() } ?: continue
            val nativeName = nameObj.strOrNull("native")?.takeIf { it.isNotBlank() }
            val imageObj = node.objOrNull("image")
            val img = imageObj?.strOrNull("large")?.takeIf { it.isNotBlank() }
                ?: imageObj?.strOrNull("medium")?.takeIf { it.isNotBlank() }

            val entry = AniListStaffEntry(
                id = staffId,
                name = fullName,
                nativeName = nativeName,
                role = role,
                imageUrl = img
            )
            grouped.getOrPut(staffId) { mutableListOf() }.add(entry to role)
        }

        return grouped.values.map { pairs ->
            val first = pairs.first().first
            val combinedRoles = pairs.map { it.second }.distinct().take(3).joinToString(" • ")
            first.copy(role = combinedRoles)
        }
    }

    private fun extractOrderedLanguages(characters: List<AniListCharacterEntry>): List<String> {
        val rawSet = characters.flatMap { it.voiceActors }
            .map { it.language.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (rawSet.isEmpty()) return listOf("Japanese")

        val priority = listOf("Japanese", "English", "Indonesian", "Korean", "Chinese")
        val prioritized = priority.filter { p -> rawSet.any { it.equals(p, ignoreCase = true) } }
            .map { p -> rawSet.first { it.equals(p, ignoreCase = true) } }
        val others = rawSet.filter { lang -> prioritized.none { it.equals(lang, ignoreCase = true) } }
            .sorted()
        return prioritized + others
    }

    private fun JsonObject.strOrNull(key: String): String? {
        val e = get(key) ?: return null
        return if (e.isJsonPrimitive) runCatching { e.asString }.getOrNull() else null
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val e = get(key) ?: return null
        return if (e.isJsonPrimitive && e.asJsonPrimitive.isNumber) runCatching { e.asInt }.getOrNull() else null
    }

    private fun JsonObject.objOrNull(key: String): JsonObject? {
        val e = get(key) ?: return null
        return if (e.isJsonObject) e.asJsonObject else null
    }

    private fun JsonObject.arrOrNull(key: String): JsonArray? {
        val e = get(key) ?: return null
        return if (e.isJsonArray) e.asJsonArray else null
    }
}
