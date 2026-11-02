package com.yuranime.dinz.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.yuranime.dinz.data.model.EpisodeCommentItem

class LocalAuthDatabase(context: Context) : SQLiteOpenHelper(context, "animein_local.db", null, 2) {

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
                episode_title TEXT,
                poster_url TEXT,
                position_ms INTEGER,
                duration_ms INTEGER,
                updated_at INTEGER
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

    fun addLocalComment(episodeId: String, text: String, parentId: String? = null): EpisodeCommentItem {
        val db = writableDatabase
        val newId = "local_" + System.currentTimeMillis()
        val username = getUsername()
        val cv = ContentValues().apply {
            put("id", newId)
            put("episode_id", episodeId)
            put("user_name", username)
            put("user_pokemon", "025") // Pikachu avatar
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
            text = text,
            time = "Baru saja",
            like = "0",
            dislike = "0",
            replay = "0"
        )
    }

    fun getLocalComments(episodeId: String): List<EpisodeCommentItem> {
        val db = readableDatabase
        val list = mutableListOf<EpisodeCommentItem>()
        val cursor = db.rawQuery("SELECT id, user_name, user_pokemon, text, time FROM local_comments WHERE episode_id = ? AND parent_id IS NULL ORDER BY id DESC", arrayOf(episodeId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    EpisodeCommentItem(
                        id = it.getString(0),
                        userName = it.getString(1),
                        userPokemon = it.getString(2),
                        text = it.getString(3),
                        time = it.getString(4),
                        like = "0",
                        dislike = "0",
                        replay = "0"
                    )
                )
            }
        }
        return list
    }

    fun getLocalReplies(commentId: String): List<EpisodeCommentItem> {
        val db = readableDatabase
        val list = mutableListOf<EpisodeCommentItem>()
        val cursor = db.rawQuery("SELECT id, user_name, user_pokemon, text, time FROM local_comments WHERE parent_id = ? ORDER BY id DESC", arrayOf(commentId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    EpisodeCommentItem(
                        id = it.getString(0),
                        userName = it.getString(1),
                        userPokemon = it.getString(2),
                        text = it.getString(3),
                        time = it.getString(4),
                        like = "0",
                        dislike = "0",
                        replay = "0"
                    )
                )
            }
        }
        return list
    }

    fun saveWatchProgress(
        movieId: String,
        animeTitle: String,
        episodeId: String,
        episodeTitle: String,
        posterUrl: String,
        positionMs: Long,
        durationMs: Long
    ) {
        if (movieId.isBlank()) return
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("movie_id", movieId)
            put("anime_title", animeTitle)
            put("episode_id", episodeId)
            put("episode_title", episodeTitle)
            put("poster_url", posterUrl)
            put("position_ms", positionMs)
            put("duration_ms", durationMs)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("watch_history", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        com.yuranime.dinz.data.firebase.FirebaseManager.addExpAndPoints(20L, 10L)
    }

    fun getWatchHistory(): List<WatchHistoryItem> {
        val db = readableDatabase
        val list = mutableListOf<WatchHistoryItem>()
        val cursor = db.rawQuery(
            "SELECT movie_id, anime_title, episode_id, episode_title, poster_url, position_ms, duration_ms, updated_at FROM watch_history ORDER BY updated_at DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    WatchHistoryItem(
                        movieId = it.getString(0) ?: "",
                        animeTitle = it.getString(1) ?: "",
                        episodeId = it.getString(2) ?: "",
                        episodeTitle = it.getString(3) ?: "",
                        posterUrl = it.getString(4) ?: "",
                        positionMs = it.getLong(5),
                        durationMs = it.getLong(6),
                        updatedAt = it.getLong(7)
                    )
                )
            }
        }
        return list
    }

    fun deleteWatchHistory(movieId: String) {
        val db = writableDatabase
        db.delete("watch_history", "movie_id = ?", arrayOf(movieId))
    }

    fun clearWatchHistory() {
        val db = writableDatabase
        db.delete("watch_history", null, null)
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
    val updatedAt: Long
)
