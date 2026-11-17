package com.yuranime.dinz.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.yuranime.dinz.data.model.EpisodeCommentItem

class LocalAuthDatabase(context: Context) : SQLiteOpenHelper(context, "animein_local.db", null, 6) {

    private val prefs = context.getSharedPreferences("animein_auth_prefs", Context.MODE_PRIVATE)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS comment_reactions (
                comment_id TEXT PRIMARY KEY,
                reaction_type TEXT,
                timestamp INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_comments (
                id TEXT PRIMARY KEY,
                episode_id TEXT,
                user_name TEXT,
                user_pokemon TEXT,
                avatar_url TEXT DEFAULT '',
                text TEXT,
                time TEXT,
                parent_id TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS watch_history (
                movie_id TEXT PRIMARY KEY,
                anime_title TEXT,
                episode_id TEXT,
                episode_number TEXT DEFAULT '',
                episode_title TEXT,
                poster_url TEXT,
                position_ms INTEGER,
                duration_ms INTEGER,
                is_completed INTEGER DEFAULT 0,
                updated_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS watched_episodes (
                movie_id TEXT NOT NULL,
                episode_id TEXT NOT NULL,
                episode_number TEXT DEFAULT '',
                position_ms INTEGER DEFAULT 0,
                duration_ms INTEGER DEFAULT 0,
                is_completed INTEGER DEFAULT 0,
                exp_awarded INTEGER DEFAULT 0,
                updated_at INTEGER DEFAULT 0,
                PRIMARY KEY (movie_id, episode_id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS comic_read_history (
                comic_id TEXT PRIMARY KEY,
                comic_title TEXT,
                cover_url TEXT,
                chapter_id TEXT,
                chapter_number TEXT,
                chapter_title TEXT,
                updated_at INTEGER,
                page_index INTEGER DEFAULT 0,
                scroll_offset INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS offline_comic_chapters (
                chapter_id TEXT PRIMARY KEY,
                comic_id TEXT,
                comic_title TEXT,
                chapter_number TEXT,
                chapter_title TEXT,
                cover_url TEXT,
                page_count INTEGER,
                local_dir_path TEXT,
                total_bytes INTEGER,
                downloaded_at INTEGER
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS watch_history (
                    movie_id TEXT PRIMARY KEY,
                    anime_title TEXT,
                    episode_id TEXT,
                    episode_title TEXT,
                    poster_url TEXT,
                    position_ms INTEGER,
                    duration_ms INTEGER,
                    updated_at INTEGER
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 3) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS comic_read_history (
                    comic_id TEXT PRIMARY KEY,
                    comic_title TEXT,
                    cover_url TEXT,
                    chapter_id TEXT,
                    chapter_number TEXT,
                    chapter_title TEXT,
                    updated_at INTEGER
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 4) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS offline_comic_chapters (
                    chapter_id TEXT PRIMARY KEY,
                    comic_id TEXT,
                    comic_title TEXT,
                    chapter_number TEXT,
                    chapter_title TEXT,
                    cover_url TEXT,
                    page_count INTEGER,
                    local_dir_path TEXT,
                    total_bytes INTEGER,
                    downloaded_at INTEGER
                )
                """.trimIndent()
            )
            try {
                db.execSQL("ALTER TABLE comic_read_history ADD COLUMN page_index INTEGER DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE comic_read_history ADD COLUMN scroll_offset INTEGER DEFAULT 0")
            } catch (_: Exception) {}
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE watch_history ADD COLUMN episode_number TEXT DEFAULT ''")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE watch_history ADD COLUMN is_completed INTEGER DEFAULT 0")
            } catch (_: Exception) {}
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS watched_episodes (
                    movie_id TEXT NOT NULL,
                    episode_id TEXT NOT NULL,
                    episode_number TEXT DEFAULT '',
                    position_ms INTEGER DEFAULT 0,
                    duration_ms INTEGER DEFAULT 0,
                    is_completed INTEGER DEFAULT 0,
                    exp_awarded INTEGER DEFAULT 0,
                    updated_at INTEGER DEFAULT 0,
                    PRIMARY KEY (movie_id, episode_id)
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE local_comments ADD COLUMN avatar_url TEXT DEFAULT ''")
            } catch (_: Exception) {}
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun setLoggedIn(loggedIn: Boolean, username: String = "Gading Kencana") {
        prefs.edit()
            .putBoolean("is_logged_in", loggedIn)
            .putString("username", username)
            .apply()
    }

    fun getUsername(): String {
        return prefs.getString("username", "Gading Kencana") ?: "Gading Kencana"
    }

    fun setUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    fun getAvatarUrl(): String {
        return prefs.getString("avatar_url", "") ?: ""
    }

    fun setAvatarUrl(url: String) {
        prefs.edit().putString("avatar_url", url).apply()
    }

    fun getCoverUrl(): String {
        return prefs.getString("cover_url", "") ?: ""
    }

    fun setCoverUrl(url: String) {
        prefs.edit().putString("cover_url", url).apply()
    }

    fun getUserLevel(): Int {
        return prefs.getInt("user_level", 1)
    }

    fun getUserCoins(): Int {
        return prefs.getInt("user_coins", 60)
    }

    fun addCoins(amount: Int) {
        val current = getUserCoins()
        prefs.edit().putInt("user_coins", current + amount).apply()
    }

    fun isDailyCheckedIn(): Boolean {
        val lastCheckIn = prefs.getLong("last_check_in_date", 0L)
        val now = System.currentTimeMillis()
        // Cek dalam 24 jam terakhir
        return (now - lastCheckIn) < (24L * 60 * 60 * 1000)
    }

    fun performDailyCheckIn(): Boolean {
        if (isDailyCheckedIn()) return false
        prefs.edit()
            .putLong("last_check_in_date", System.currentTimeMillis())
            .apply()
        addCoins(10)
        return true
    }

    fun saveReaction(commentId: String, reaction: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("comment_id", commentId)
            put("reaction_type", reaction)
            put("timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict("comment_reactions", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        com.yuranime.dinz.data.firebase.FirebaseManager.addExpAndPoints(5L, 2L)
    }

    fun getReaction(commentId: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT reaction_type FROM comment_reactions WHERE comment_id = ?", arrayOf(commentId))
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun addLocalComment(
        episodeId: String,
        text: String,
        parentId: String? = null,
        customUserName: String? = null,
        customAvatarUrl: String? = null
    ): EpisodeCommentItem {
        val db = writableDatabase
        val newId = "local_" + System.currentTimeMillis()
        val firebaseProfile = com.yuranime.dinz.data.firebase.FirebaseManager.userProfile.value
        val username = customUserName?.takeIf { it.isNotBlank() }
            ?: firebaseProfile?.name?.takeIf { it.isNotBlank() }
            ?: getUsername()
        val avatar = customAvatarUrl?.takeIf { it.isNotBlank() }
            ?: firebaseProfile?.avatar?.takeIf { it.isNotBlank() }
            ?: getAvatarUrl()
        val cv = ContentValues().apply {
            put("id", newId)
            put("episode_id", episodeId)
            put("user_name", username)
            put("user_pokemon", "025")
            put("avatar_url", avatar)
            put("text", text)
            put("time", "Baru saja")
            put("parent_id", parentId)
        }
        db.insert("local_comments", null, cv)
        com.yuranime.dinz.data.firebase.FirebaseManager.addExpAndPoints(10L, 5L)

        return EpisodeCommentItem(
            id = newId,
            userName = username,
            userPokemon = "025",
            avatarUrl = avatar,
            text = text,
            time = "Baru saja",
            like = "0",
            dislike = "0",
            replay = "0",
            isLocal = true
        )
    }

    fun getLocalComments(episodeId: String): List<EpisodeCommentItem> {
        val db = readableDatabase
        val list = mutableListOf<EpisodeCommentItem>()
        val cursor = db.rawQuery(
            "SELECT id, user_name, user_pokemon, avatar_url, text, time FROM local_comments WHERE episode_id = ? AND parent_id IS NULL ORDER BY id DESC",
            arrayOf(episodeId)
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    EpisodeCommentItem(
                        id = it.getString(0),
                        userName = it.getString(1),
                        userPokemon = it.getString(2),
                        avatarUrl = it.getString(3),
                        text = it.getString(4),
                        time = it.getString(5),
                        like = "0",
                        dislike = "0",
                        replay = "0",
                        isLocal = true
                    )
                )
            }
        }
        return list
    }

    fun getLocalReplies(commentId: String): List<EpisodeCommentItem> {
        val db = readableDatabase
        val list = mutableListOf<EpisodeCommentItem>()
        val cursor = db.rawQuery(
            "SELECT id, user_name, user_pokemon, avatar_url, text, time FROM local_comments WHERE parent_id = ? ORDER BY id DESC",
            arrayOf(commentId)
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    EpisodeCommentItem(
                        id = it.getString(0),
                        userName = it.getString(1),
                        userPokemon = it.getString(2),
                        avatarUrl = it.getString(3),
                        text = it.getString(4),
                        time = it.getString(5),
                        like = "0",
                        dislike = "0",
                        replay = "0",
                        isLocal = true
                    )
                )
            }
        }
        return list
    }

    fun deleteLocalComment(commentId: String) {
        if (commentId.isBlank()) return
        val db = writableDatabase
        db.delete("local_comments", "id = ? OR parent_id = ?", arrayOf(commentId, commentId))
    }

    fun saveWatchProgress(
        movieId: String,
        animeTitle: String,
        episodeId: String,
        episodeTitle: String,
        posterUrl: String,
        positionMs: Long,
        durationMs: Long,
        episodeNumber: String = "",
        isCompleted: Boolean = false
    ): WatchHistoryItem? {
        if (movieId.isBlank()) return null
        val now = System.currentTimeMillis()
        val safePos = positionMs.coerceAtLeast(0L)
        val safeDur = durationMs.coerceAtLeast(0L)
        val resolvedEpNum = episodeNumber.ifBlank {
            Regex("\\d+").find(episodeTitle)?.value.orEmpty()
        }
        val computedCompleted = isCompleted || (
            safeDur > 0L &&
                (safePos.toFloat() / safeDur.toFloat() >= 0.85f || (safeDur - safePos) <= 30_000L)
            )

        val db = writableDatabase
        var expAlreadyAwarded = false
        var previousCompleted = false

        if (episodeId.isNotBlank()) {
            runCatching {
                db.rawQuery(
                    "SELECT is_completed, exp_awarded FROM watched_episodes WHERE movie_id = ? AND episode_id = ?",
                    arrayOf(movieId, episodeId)
                ).use { c ->
                    if (c.moveToFirst()) {
                        previousCompleted = c.getInt(0) == 1
                        expAlreadyAwarded = c.getInt(1) == 1
                    }
                }
            }
        }

        val finalCompleted = computedCompleted || previousCompleted
        val cv = ContentValues().apply {
            put("movie_id", movieId)
            put("anime_title", animeTitle)
            put("episode_id", episodeId)
            put("episode_number", resolvedEpNum)
            put("episode_title", episodeTitle)
            put("poster_url", posterUrl)
            put("position_ms", safePos)
            put("duration_ms", safeDur)
            put("is_completed", if (finalCompleted) 1 else 0)
            put("updated_at", now)
        }
        db.insertWithOnConflict("watch_history", null, cv, SQLiteDatabase.CONFLICT_REPLACE)

        val shouldAwardExp = safePos > 5_000L && !expAlreadyAwarded && episodeId.isNotBlank()
        if (episodeId.isNotBlank()) {
            val epCv = ContentValues().apply {
                put("movie_id", movieId)
                put("episode_id", episodeId)
                put("episode_number", resolvedEpNum)
                put("position_ms", safePos)
                put("duration_ms", safeDur)
                put("is_completed", if (finalCompleted) 1 else 0)
                put("exp_awarded", if (shouldAwardExp || expAlreadyAwarded) 1 else 0)
                put("updated_at", now)
            }
            db.insertWithOnConflict("watched_episodes", null, epCv, SQLiteDatabase.CONFLICT_REPLACE)
        }

        if (shouldAwardExp) {
            com.yuranime.dinz.data.firebase.FirebaseManager.addExpAndPoints(20L, 10L)
        }

        return WatchHistoryItem(
            movieId = movieId,
            animeTitle = animeTitle,
            episodeId = episodeId,
            episodeNumber = resolvedEpNum,
            episodeTitle = episodeTitle,
            posterUrl = posterUrl,
            positionMs = safePos,
            durationMs = safeDur,
            isCompleted = finalCompleted,
            updatedAt = now
        )
    }

    fun getWatchHistory(): List<WatchHistoryItem> {
        val db = readableDatabase
        val list = mutableListOf<WatchHistoryItem>()
        val cursor = db.rawQuery(
            "SELECT movie_id, anime_title, episode_id, episode_number, episode_title, poster_url, position_ms, duration_ms, is_completed, updated_at FROM watch_history ORDER BY updated_at DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    WatchHistoryItem(
                        movieId = it.getString(0) ?: "",
                        animeTitle = it.getString(1) ?: "",
                        episodeId = it.getString(2) ?: "",
                        episodeNumber = it.getString(3) ?: "",
                        episodeTitle = it.getString(4) ?: "",
                        posterUrl = it.getString(5) ?: "",
                        positionMs = it.getLong(6),
                        durationMs = it.getLong(7),
                        isCompleted = it.getInt(8) == 1,
                        updatedAt = it.getLong(9)
                    )
                )
            }
        }
        return list
    }

    fun getWatchedEpisodeIds(movieId: String): Set<String> {
        if (movieId.isBlank()) return emptySet()
        val db = readableDatabase
        val set = HashSet<String>()
        runCatching {
            db.rawQuery(
                "SELECT episode_id FROM watched_episodes WHERE movie_id = ?",
                arrayOf(movieId)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    if (!id.isNullOrBlank()) set.add(id)
                }
            }
        }
        return set
    }

    fun getEpisodeResumePositionMs(movieId: String, episodeId: String): Long {
        if (movieId.isBlank() || episodeId.isBlank()) return 0L
        val db = readableDatabase
        var resumePos = 0L
        runCatching {
            db.rawQuery(
                "SELECT position_ms, duration_ms, is_completed FROM watched_episodes WHERE movie_id = ? AND episode_id = ?",
                arrayOf(movieId, episodeId)
            ).use { c ->
                if (c.moveToFirst()) {
                    val pos = c.getLong(0)
                    val dur = c.getLong(1)
                    val completed = c.getInt(2) == 1
                    if (!completed && pos > 5_000L && (dur <= 0L || pos.toFloat() / dur.toFloat() < 0.92f)) {
                        resumePos = pos
                    }
                }
            }
        }
        return resumePos
    }

    fun getWatchedEpisodesForAnime(movieId: String): List<WatchedEpisodeRecord> {
        if (movieId.isBlank()) return emptyList()
        val db = readableDatabase
        val out = mutableListOf<WatchedEpisodeRecord>()
        runCatching {
            db.rawQuery(
                "SELECT episode_id, episode_number, position_ms, duration_ms, is_completed, updated_at FROM watched_episodes WHERE movie_id = ?",
                arrayOf(movieId)
            ).use { c ->
                while (c.moveToNext()) {
                    out.add(
                        WatchedEpisodeRecord(
                            movieId = movieId,
                            episodeId = c.getString(0) ?: "",
                            episodeNumber = c.getString(1) ?: "",
                            positionMs = c.getLong(2),
                            durationMs = c.getLong(3),
                            isCompleted = c.getInt(4) == 1,
                            updatedAt = c.getLong(5)
                        )
                    )
                }
            }
        }
        return out
    }

    fun mergeRemoteWatchHistory(remoteRecords: List<RemoteWatchHistoryBundle>) {
        if (remoteRecords.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            remoteRecords.forEach { bundle ->
                val item = bundle.item
                if (item.movieId.isBlank()) return@forEach

                var localUpdatedAt = -1L
                db.rawQuery(
                    "SELECT updated_at FROM watch_history WHERE movie_id = ?",
                    arrayOf(item.movieId)
                ).use { c ->
                    if (c.moveToFirst()) localUpdatedAt = c.getLong(0)
                }

                if (item.updatedAt >= localUpdatedAt) {
                    val cv = ContentValues().apply {
                        put("movie_id", item.movieId)
                        put("anime_title", item.animeTitle)
                        put("episode_id", item.episodeId)
                        put("episode_number", item.episodeNumber)
                        put("episode_title", item.episodeTitle)
                        put("poster_url", item.posterUrl)
                        put("position_ms", item.positionMs)
                        put("duration_ms", item.durationMs)
                        put("is_completed", if (item.isCompleted) 1 else 0)
                        put("updated_at", item.updatedAt)
                    }
                    db.insertWithOnConflict("watch_history", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }

                bundle.watchedEpisodes.forEach { ep ->
                    if (ep.episodeId.isBlank()) return@forEach
                    var localEpUpdated = -1L
                    var localExpAwarded = 0
                    db.rawQuery(
                        "SELECT updated_at, exp_awarded FROM watched_episodes WHERE movie_id = ? AND episode_id = ?",
                        arrayOf(item.movieId, ep.episodeId)
                    ).use { c ->
                        if (c.moveToFirst()) {
                            localEpUpdated = c.getLong(0)
                            localExpAwarded = c.getInt(1)
                        }
                    }
                    if (ep.updatedAt >= localEpUpdated) {
                        val epCv = ContentValues().apply {
                            put("movie_id", item.movieId)
                            put("episode_id", ep.episodeId)
                            put("episode_number", ep.episodeNumber)
                            put("position_ms", ep.positionMs)
                            put("duration_ms", ep.durationMs)
                            put("is_completed", if (ep.isCompleted) 1 else 0)
                            put("exp_awarded", maxOf(localExpAwarded, 1))
                            put("updated_at", ep.updatedAt)
                        }
                        db.insertWithOnConflict("watched_episodes", null, epCv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteWatchHistory(movieId: String) {
        val db = writableDatabase
        db.delete("watch_history", "movie_id = ?", arrayOf(movieId))
        db.delete("watched_episodes", "movie_id = ?", arrayOf(movieId))
    }

    fun clearWatchHistory() {
        val db = writableDatabase
        db.delete("watch_history", null, null)
        db.delete("watched_episodes", null, null)
    }

    fun saveComicReadHistory(
        comicId: String,
        comicTitle: String,
        coverUrl: String,
        chapterId: String,
        chapterNumber: String,
        chapterTitle: String = "",
        pageIndex: Int = 0,
        scrollOffset: Int = 0
    ) {
        val db = writableDatabase
        // Proteksi: jangan pernah menimpa coverUrl atau comicTitle yang sudah valid dengan string kosong
        val existing = getComicReadHistoryItem(comicId)
        val finalCover = if (coverUrl.isNotBlank()) coverUrl else (existing?.coverUrl.orEmpty())
        val finalTitle = if (comicTitle.isNotBlank()) comicTitle else (existing?.comicTitle.orEmpty())

        val cv = ContentValues().apply {
            put("comic_id", comicId)
            put("comic_title", finalTitle.ifBlank { "Komik" })
            put("cover_url", finalCover)
            put("chapter_id", chapterId)
            put("chapter_number", chapterNumber)
            put("chapter_title", chapterTitle)
            put("page_index", pageIndex)
            put("scroll_offset", scrollOffset)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("comic_read_history", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        if (existing?.chapterId != chapterId) {
            com.yuranime.dinz.data.firebase.FirebaseManager.addExpAndPoints(15L, 5L)
        }
    }

    fun getTotalCommentCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM local_comments", null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getComicReadHistoryItem(comicId: String): com.yuranime.dinz.data.model.ComicReadHistoryItem? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT comic_id, comic_title, cover_url, chapter_id, chapter_number, chapter_title, updated_at, page_index, scroll_offset FROM comic_read_history WHERE comic_id = ? LIMIT 1",
            arrayOf(comicId)
        )
        cursor.use {
            if (it.moveToFirst()) {
                return com.yuranime.dinz.data.model.ComicReadHistoryItem(
                    comicId = it.getString(0) ?: "",
                    comicTitle = it.getString(1) ?: "",
                    coverUrl = it.getString(2) ?: "",
                    chapterId = it.getString(3) ?: "",
                    chapterNumber = it.getString(4) ?: "",
                    chapterTitle = it.getString(5) ?: "",
                    updatedAt = it.getLong(6),
                    pageIndex = if (it.columnCount > 7) it.getInt(7) else 0,
                    scrollOffset = if (it.columnCount > 8) it.getInt(8) else 0
                )
            }
        }
        return null
    }

    fun getComicReadHistory(): List<com.yuranime.dinz.data.model.ComicReadHistoryItem> {
        val db = readableDatabase
        val list = mutableListOf<com.yuranime.dinz.data.model.ComicReadHistoryItem>()
        val cursor = db.rawQuery(
            "SELECT comic_id, comic_title, cover_url, chapter_id, chapter_number, chapter_title, updated_at, page_index, scroll_offset FROM comic_read_history ORDER BY updated_at DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    com.yuranime.dinz.data.model.ComicReadHistoryItem(
                        comicId = it.getString(0) ?: "",
                        comicTitle = it.getString(1) ?: "",
                        coverUrl = it.getString(2) ?: "",
                        chapterId = it.getString(3) ?: "",
                        chapterNumber = it.getString(4) ?: "",
                        chapterTitle = it.getString(5) ?: "",
                        updatedAt = it.getLong(6),
                        pageIndex = if (it.columnCount > 7) it.getInt(7) else 0,
                        scrollOffset = if (it.columnCount > 8) it.getInt(8) else 0
                    )
                )
            }
        }
        return list
    }

    fun deleteComicReadHistory(comicId: String) {
        val db = writableDatabase
        db.delete("comic_read_history", "comic_id = ?", arrayOf(comicId))
    }

    fun clearComicReadHistory() {
        val db = writableDatabase
        db.delete("comic_read_history", null, null)
    }

    // ==================== OFFLINE COMIC CHAPTERS ====================

    fun saveDownloadedComicChapter(item: com.yuranime.dinz.data.model.DownloadedComicChapter) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("chapter_id", item.chapterId)
            put("comic_id", item.comicId)
            put("comic_title", item.comicTitle)
            put("chapter_number", item.chapterNumber)
            put("chapter_title", item.chapterTitle)
            put("cover_url", item.coverUrl)
            put("page_count", item.pageCount)
            put("local_dir_path", item.localDirPath)
            put("total_bytes", item.totalBytes)
            put("downloaded_at", item.downloadedAt)
        }
        db.insertWithOnConflict("offline_comic_chapters", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getDownloadedComicChapters(): List<com.yuranime.dinz.data.model.DownloadedComicChapter> {
        val db = readableDatabase
        val list = mutableListOf<com.yuranime.dinz.data.model.DownloadedComicChapter>()
        val cursor = db.rawQuery(
            "SELECT chapter_id, comic_id, comic_title, chapter_number, chapter_title, cover_url, page_count, local_dir_path, total_bytes, downloaded_at FROM offline_comic_chapters ORDER BY downloaded_at DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    com.yuranime.dinz.data.model.DownloadedComicChapter(
                        chapterId = it.getString(0) ?: "",
                        comicId = it.getString(1) ?: "",
                        comicTitle = it.getString(2) ?: "",
                        chapterNumber = it.getString(3) ?: "",
                        chapterTitle = it.getString(4) ?: "",
                        coverUrl = it.getString(5) ?: "",
                        pageCount = it.getInt(6),
                        localDirPath = it.getString(7) ?: "",
                        totalBytes = it.getLong(8),
                        downloadedAt = it.getLong(9)
                    )
                )
            }
        }
        return list
    }

    fun isChapterDownloaded(chapterId: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM offline_comic_chapters WHERE chapter_id = ? LIMIT 1",
            arrayOf(chapterId)
        )
        cursor.use {
            return it.count > 0
        }
    }

    fun deleteDownloadedComicChapter(chapterId: String) {
        val db = writableDatabase
        db.delete("offline_comic_chapters", "chapter_id = ?", arrayOf(chapterId))
    }
}

data class WatchHistoryItem(
    val movieId: String,
    val animeTitle: String,
    val episodeId: String,
    val episodeTitle: String,
    val posterUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val episodeNumber: String = "",
    val isCompleted: Boolean = false
)

data class WatchedEpisodeRecord(
    val movieId: String,
    val episodeId: String,
    val episodeNumber: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isCompleted: Boolean = false,
    val updatedAt: Long = 0L
)

data class RemoteWatchHistoryBundle(
    val item: WatchHistoryItem,
    val watchedEpisodes: List<WatchedEpisodeRecord>
)
