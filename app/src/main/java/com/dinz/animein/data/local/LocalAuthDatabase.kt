package com.dinz.animein.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dinz.animein.data.model.EpisodeCommentItem

class LocalAuthDatabase(context: Context) : SQLiteOpenHelper(context, "animein_local.db", null, 1) {

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS comment_reactions")
        db.execSQL("DROP TABLE IF EXISTS local_comments")
        onCreate(db)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun setLoggedIn(loggedIn: Boolean, username: String = "UserAnimeIn") {
        prefs.edit()
            .putBoolean("is_logged_in", loggedIn)
            .putString("username", username)
            .apply()
    }

    fun getUsername(): String {
        return prefs.getString("username", "UserAnimeIn") ?: "UserAnimeIn"
    }

    fun saveReaction(commentId: String, reaction: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("comment_id", commentId)
            put("reaction_type", reaction)
            put("timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict("comment_reactions", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
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
}
