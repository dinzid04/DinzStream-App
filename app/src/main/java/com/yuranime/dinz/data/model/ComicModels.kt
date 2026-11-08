package com.yuranime.dinz.data.model

data class ComicItem(
    val id: String,
    val title: String,
    val alternativeTitle: String? = null,
    val description: String? = null,
    val format: String = "Manga",
    val countryCode: String = "JP",
    val coverPortrait: String = "",
    val coverLandscape: String? = null,
    val rating: Float? = null,
    val status: String? = null,
    val releaseYear: String? = null,
    val chapters: List<ComicChapterItem> = emptyList()
) {
    val flagUrl: String
        get() = when (format.trim().lowercase()) {
            "manhwa" -> "https://flagpedia.net/data/flags/w1160/kr.webp"
            "manhua" -> "https://flagpedia.net/data/flags/w1160/cn.webp"
            else -> "https://flagpedia.net/data/flags/w1160/jp.webp"
        }
}

data class ComicChapterItem(
    val id: String,
    val comicId: String = "",
    val number: String,
    val title: String = "",
    val releaseTime: String = "",
    val releaseDate: String? = null,
    val formattedDate: String = "",
    val thumbnailUrl: String = "",
    val viewCount: Long = 0L
)

data class ComicDetail(
    val id: String,
    val title: String,
    val alternativeTitle: String? = null,
    val description: String? = null,
    val format: String = "Manga",
    val countryCode: String = "JP",
    val coverPortrait: String = "",
    val coverLandscape: String? = null,
    val rating: Float? = null,
    val viewCount: Long = 0L,
    val bookmarkCount: Long = 0L,
    val status: String? = null,
    val releaseYear: String? = null,
    val airedStart: String = "-",
    val airedEnd: String = "-",
    val genres: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val chapters: List<ComicChapterItem> = emptyList()
) {
    val flagUrl: String
        get() = when (format.trim().lowercase()) {
            "manhwa" -> "https://flagpedia.net/data/flags/w1160/kr.webp"
            "manhua" -> "https://flagpedia.net/data/flags/w1160/cn.webp"
            else -> "https://flagpedia.net/data/flags/w1160/jp.webp"
        }
}

data class ComicChapterDetail(
    val chapterId: String,
    val comicId: String,
    val chapterNumber: String,
    val chapterTitle: String? = null,
    val prevChapterId: String? = null,
    val nextChapterId: String? = null,
    val pages: List<String> = emptyList()
)
