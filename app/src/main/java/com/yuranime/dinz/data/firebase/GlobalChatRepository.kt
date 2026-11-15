package com.yuranime.dinz.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Representasi satu pesan di halaman Chat Global Yuranime.
 */
data class GlobalChatMessage(
    val id: String = "",
    val uid: String = "",
    val name: String = "Member Yuranime",
    val userTag: String = "1001",
    val avatar: String = "",
    val banner: String = "",
    val bio: String = "",
    val joinDate: String = "",
    val role: String = "Member",
    val level: Int = 1,
    val rankTitle: String = "Wibu Biasa",
    val equippedTitle: String = "",
    val equippedTitleTier: Int = 1,
    val equippedTitleVariant: Int = 0,
    val equippedBorderUrl: String = "",
    val equippedProfileEffectUrl: String = "",
    val text: String = "",
    val stickerUrl: String = "",
    val imageUrl: String = "",
    val replyToId: String = "",
    val replyToUid: String = "",
    val replyToName: String = "",
    val replyToSnippet: String = "",
    val timestamp: Long = 0L,
    val isEdited: Boolean = false
)

/**
 * Kehadiran real-time user di dalam ruang Chat Global (`chat_presence`).
 */
data class ChatPresenceUser(
    val uid: String = "",
    val name: String = "Member Yuranime",
    val avatar: String = "",
    val lastSeenMs: Long = 0L
)

sealed interface ChatTextSegment {
    data class TextChunk(val text: String) : ChatTextSegment
    data class StickerChunk(val url: String) : ChatTextSegment
}

/**
 * Codec bersama untuk menyisipkan, mengurai, dan menyalin token stiker inline `[s:URL]`.
 */
object InlineStickerCodec {
    val TOKEN_REGEX = Regex("\\[s:(https?://[^\\]\\s]+)]")
    const val PLACEHOLDER_CHAR = '\u3000'

    fun parseSegments(rawText: String): List<ChatTextSegment> {
        if (rawText.isEmpty()) return emptyList()
        val out = mutableListOf<ChatTextSegment>()
        var cursor = 0
        for (match in TOKEN_REGEX.findAll(rawText)) {
            if (match.range.first > cursor) {
                out.add(ChatTextSegment.TextChunk(rawText.substring(cursor, match.range.first)))
            }
            val url = match.groupValues.getOrNull(1).orEmpty().trim()
            if (url.isNotEmpty()) {
                out.add(ChatTextSegment.StickerChunk(url))
            }
            cursor = match.range.last + 1
        }
        if (cursor < rawText.length) {
            out.add(ChatTextSegment.TextChunk(rawText.substring(cursor)))
        }
        return out
    }

    /**
     * Memecah teks tersimpan (yang memakai `[s:URL]`) menjadi teks editor memakai `\u3000`
     * beserta daftar URL stiker berurutan sesuai kemunculan `\u3000`.
     */
    fun decodeToEditorState(rawText: String): Pair<String, List<String>> {
        if (rawText.isEmpty()) return "" to emptyList()
        val sb = StringBuilder()
        val urls = mutableListOf<String>()
        for (seg in parseSegments(rawText)) {
            when (seg) {
                is ChatTextSegment.TextChunk -> sb.append(seg.text.replace(PLACEHOLDER_CHAR, ' '))
                is ChatTextSegment.StickerChunk -> {
                    sb.append(PLACEHOLDER_CHAR)
                    urls.add(seg.url)
                }
            }
        }
        return sb.toString() to urls
    }

    /**
     * Mengubah teks editor (`\u3000` + `urls`) menjadi format string `[s:URL]` untuk Firestore.
     */
    fun encodeFromEditorState(editorText: String, stickerUrls: List<String>): String {
        if (editorText.isEmpty()) return ""
        val sb = StringBuilder()
        var stickerIdx = 0
        for (ch in editorText) {
            if (ch == PLACEHOLDER_CHAR) {
                val url = stickerUrls.getOrNull(stickerIdx)
                stickerIdx++
                if (!url.isNullOrBlank()) {
                    sb.append("[s:").append(url).append("]")
                }
            } else {
                sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    fun toReadableCopyText(rawText: String): String {
        return rawText.replace(TOKEN_REGEX, " [stiker] ").replace(Regex("\\s+"), " ").trim()
    }

    fun buildReplySnippet(msg: GlobalChatMessage): String {
        if (msg.text.isNotBlank()) {
            return msg.text
                .replace(TOKEN_REGEX, "[Stiker]")
                .replace('\n', ' ')
                .trim()
                .ifBlank { "[Stiker]" }
        }
        if (msg.stickerUrl.isNotBlank()) return "[Stiker]"
        if (msg.imageUrl.isNotBlank()) return "[Foto]"
        return "Pesan"
    }
}

/**
 * Pengelola data real-time Chat Global (`global_chat_messages` & `chat_presence`).
 */
object GlobalChatRepository {
    private const val TAG = "GlobalChatRepository"
    private const val COLLECTION_MESSAGES = "global_chat_messages"
    private const val COLLECTION_PRESENCE = "chat_presence"
    private const val PRESENCE_TTL_MS = 90_000L
    private const val MESSAGES_LIMIT = 120L

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _rawMessages = MutableStateFlow<List<GlobalChatMessage>>(emptyList())
    private val _liveUsersByUid = MutableStateFlow<Map<String, UserProfile>>(emptyMap())

    private val _messages = MutableStateFlow<List<GlobalChatMessage>>(emptyList())
    val messages: StateFlow<List<GlobalChatMessage>> = _messages.asStateFlow()

    private val _onlineUsers = MutableStateFlow<List<ChatPresenceUser>>(emptyList())
    val onlineUsers: StateFlow<List<ChatPresenceUser>> = _onlineUsers.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(true)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val _messagesError = MutableStateFlow<String?>(null)
    val messagesError: StateFlow<String?> = _messagesError.asStateFlow()

    private var messagesListener: ListenerRegistration? = null
    private var presenceListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null

    init {
        repoScope.launch {
            combine(
                _rawMessages,
                _liveUsersByUid,
                FirebaseManager.userProfile
            ) { rawList, usersByUid, myProfile ->
                rawList.map { msg -> mergeLiveUserOverlay(msg, usersByUid, myProfile) }
            }.collect { merged ->
                _messages.value = merged
            }
        }
    }

    private fun mergeLiveUserOverlay(
        msg: GlobalChatMessage,
        usersByUid: Map<String, UserProfile>,
        myProfile: UserProfile?
    ): GlobalChatMessage {
        val live = when {
            myProfile != null && myProfile.uid.isNotBlank() && myProfile.uid == msg.uid -> myProfile
            msg.uid.isNotBlank() -> usersByUid[msg.uid]
            else -> null
        }
        val replyLive = msg.replyToUid.takeIf { it.isNotBlank() }?.let { rUid ->
            if (myProfile != null && myProfile.uid == rUid) myProfile else usersByUid[rUid]
        }
        val resolvedReplyName = replyLive?.name?.takeIf { it.isNotBlank() } ?: msg.replyToName
        if (live == null) {
            return if (resolvedReplyName != msg.replyToName) {
                msg.copy(replyToName = resolvedReplyName)
            } else msg
        }
        val resolvedLevel = live.level.coerceAtLeast(1)
        return msg.copy(
            name = live.name.ifBlank { msg.name },
            userTag = live.userTag.ifBlank { msg.userTag },
            avatar = live.avatar,
            banner = live.banner,
            bio = live.bio.ifBlank { msg.bio },
            joinDate = live.joinDate.ifBlank { msg.joinDate },
            role = live.role.ifBlank { msg.role },
            level = resolvedLevel,
            rankTitle = live.rankTitle.ifBlank { FirebaseManager.computeRankTitle(resolvedLevel) },
            equippedTitle = live.equippedTitle,
            equippedTitleTier = live.equippedTitleTier,
            equippedTitleVariant = live.equippedTitleVariant,
            equippedBorderUrl = live.equippedBorderUrl,
            equippedProfileEffectUrl = live.equippedProfileEffectUrl,
            replyToName = resolvedReplyName
        )
    }

    fun startListening() {
        if (messagesListener == null) {
            _isLoadingMessages.value = _rawMessages.value.isEmpty()
            messagesListener = firestore.collection(COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(MESSAGES_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listener chat global error: ${error.message}", error)
                        _isLoadingMessages.value = false
                        _messagesError.value = error.localizedMessage ?: "Gagal memuat chat global."
                        return@addSnapshotListener
                    }
                    _messagesError.value = null
                    _isLoadingMessages.value = false
                    if (snapshot == null) return@addSnapshotListener

                    val parsed = snapshot.documents.mapNotNull { doc ->
                        runCatching {
                            val uid = doc.getString("uid").orEmpty()
                            val level = (doc.getLong("level") ?: 1L).toInt().coerceAtLeast(1)
                            val rawTs = when (val tsAny = doc.get("timestamp")) {
                                is Number -> tsAny.toLong()
                                is Timestamp -> tsAny.toDate().time
                                else -> 0L
                            }
                            GlobalChatMessage(
                                id = doc.id,
                                uid = uid,
                                name = doc.getString("name")?.ifBlank { null } ?: "Member Yuranime",
                                userTag = doc.getString("userTag")?.ifBlank { null }
                                    ?: FirebaseManager.computeUserTag(uid),
                                avatar = doc.getString("avatar").orEmpty(),
                                banner = doc.getString("banner").orEmpty(),
                                bio = doc.getString("bio").orEmpty(),
                                joinDate = doc.getString("joinDate").orEmpty(),
                                role = doc.getString("role")?.ifBlank { null } ?: "Member",
                                level = level,
                                rankTitle = doc.getString("rankTitle")?.ifBlank { null }
                                    ?: FirebaseManager.computeRankTitle(level),
                                equippedTitle = doc.getString("equippedTitle").orEmpty(),
                                equippedTitleTier = (doc.getLong("equippedTitleTier") ?: level.toLong()).toInt(),
                                equippedTitleVariant = (doc.getLong("equippedTitleVariant") ?: 0L).toInt(),
                                equippedBorderUrl = doc.getString("equippedBorderUrl").orEmpty(),
                                equippedProfileEffectUrl = doc.getString("equippedProfileEffectUrl").orEmpty(),
                                text = doc.getString("text").orEmpty(),
                                stickerUrl = doc.getString("stickerUrl").orEmpty(),
                                imageUrl = doc.getString("imageUrl").orEmpty(),
                                replyToId = doc.getString("replyToId").orEmpty(),
                                replyToUid = doc.getString("replyToUid").orEmpty(),
                                replyToName = doc.getString("replyToName").orEmpty(),
                                replyToSnippet = doc.getString("replyToSnippet").orEmpty(),
                                timestamp = rawTs,
                                isEdited = doc.getBoolean("isEdited") ?: false
                            )
                        }.getOrNull()
                    }
                    _rawMessages.value = parsed
                }
        }

        if (usersListener == null) {
            usersListener = firestore.collection("users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listener live profile chat error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val updated = HashMap<String, UserProfile>(snapshot.documents.size * 2)
                    snapshot.documents.forEach { doc ->
                        val profile = runCatching {
                            FirebaseManager.parsePublicProfileSnapshot(doc)
                        }.getOrNull() ?: return@forEach
                        updated[doc.id] = profile
                        if (profile.uid.isNotBlank()) {
                            updated[profile.uid] = profile
                        }
                    }
                    _liveUsersByUid.value = updated
                }
        }

        if (presenceListener == null) {
            presenceListener = firestore.collection(COLLECTION_PRESENCE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listener presence chat error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val cutoff = System.currentTimeMillis() - PRESENCE_TTL_MS
                    val active = snapshot.documents.mapNotNull { doc ->
                        val isOnline = doc.getBoolean("isOnline") ?: true
                        val lastSeenMs = when (val v = doc.get("lastSeenMs")) {
                            is Number -> v.toLong()
                            is Timestamp -> v.toDate().time
                            else -> 0L
                        }
                        if (!isOnline || lastSeenMs < cutoff) return@mapNotNull null
                        val uid = doc.getString("uid")?.ifBlank { null } ?: doc.id
                        ChatPresenceUser(
                            uid = uid,
                            name = doc.getString("name")?.ifBlank { null } ?: "Member",
                            avatar = doc.getString("avatar").orEmpty(),
                            lastSeenMs = lastSeenMs
                        )
                    }.distinctBy { it.uid }
                        .sortedWith(
                            compareByDescending<ChatPresenceUser> { it.avatar.isNotBlank() }
                                .thenByDescending { it.lastSeenMs }
                        )
                    _onlineUsers.value = active
                }
        }
    }

    fun stopListening() {
        messagesListener?.remove()
        messagesListener = null
        presenceListener?.remove()
        presenceListener = null
        usersListener?.remove()
        usersListener = null
    }

    suspend fun upsertPresence(profile: UserProfile?) = withContext(Dispatchers.IO) {
        val uid = profile?.uid?.takeIf { it.isNotBlank() }
            ?: FirebaseManager.getActiveUid()?.takeIf { it.isNotBlank() }
            ?: return@withContext
        val name = profile?.name?.ifBlank { null }
            ?: FirebaseManager.googleSession.value?.name
            ?: "Member Yuranime"
        val avatar = profile?.avatar?.ifBlank { null }
            ?: FirebaseManager.googleSession.value?.photoUrl
            ?: ""
        runCatching {
            firestore.collection(COLLECTION_PRESENCE).document(uid)
                .set(
                    mapOf(
                        "uid" to uid,
                        "name" to name,
                        "avatar" to avatar,
                        "isOnline" to true,
                        "lastSeenMs" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
        }.onFailure {
            Log.w(TAG, "Gagal perbarui presence chat: ${it.message}")
        }
    }

    suspend fun leavePresence(uid: String?) = withContext(Dispatchers.IO) {
        val resolvedUid = uid?.takeIf { it.isNotBlank() } ?: return@withContext
        runCatching {
            firestore.collection(COLLECTION_PRESENCE).document(resolvedUid)
                .set(
                    mapOf(
                        "isOnline" to false,
                        "lastSeenMs" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
        }
    }

    suspend fun sendMessage(
        sender: UserProfile,
        text: String = "",
        stickerUrl: String = "",
        imageUrl: String = "",
        replyTo: GlobalChatMessage? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (text.isBlank() && stickerUrl.isBlank() && imageUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Pesan kosong."))
        }
        val uid = sender.uid.ifBlank { FirebaseManager.getActiveUid().orEmpty() }
        if (uid.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Silakan login Google terlebih dahulu."))
        }
        val payload = hashMapOf<String, Any>(
            "uid" to uid,
            "name" to sender.name.ifBlank { "Member Yuranime" },
            "userTag" to sender.userTag.ifBlank { FirebaseManager.computeUserTag(uid) },
            "avatar" to sender.avatar,
            "banner" to sender.banner,
            "bio" to sender.bio,
            "joinDate" to sender.joinDate,
            "role" to sender.role.ifBlank { "Member" },
            "level" to sender.level.coerceAtLeast(1),
            "rankTitle" to sender.rankTitle.ifBlank { FirebaseManager.computeRankTitle(sender.level) },
            "equippedTitle" to sender.equippedTitle,
            "equippedTitleTier" to sender.equippedTitleTier,
            "equippedTitleVariant" to sender.equippedTitleVariant,
            "equippedBorderUrl" to sender.equippedBorderUrl,
            "equippedProfileEffectUrl" to sender.equippedProfileEffectUrl,
            "text" to text,
            "stickerUrl" to stickerUrl,
            "imageUrl" to imageUrl,
            "replyToId" to (replyTo?.id ?: ""),
            "replyToUid" to (replyTo?.uid ?: ""),
            "replyToName" to (replyTo?.name ?: ""),
            "replyToSnippet" to (replyTo?.let { InlineStickerCodec.buildReplySnippet(it) } ?: ""),
            "timestamp" to System.currentTimeMillis(),
            "isEdited" to false
        )
        runCatching {
            firestore.collection(COLLECTION_MESSAGES).add(payload).await()
            Unit
        }.onFailure {
            Log.e(TAG, "Gagal mengirim pesan: ${it.message}", it)
        }
    }

    suspend fun uploadAndSendChatImage(
        context: Context,
        sender: UserProfile,
        imageUriString: String,
        captionText: String = "",
        replyTo: GlobalChatMessage? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = sender.uid.ifBlank { FirebaseManager.getActiveUid().orEmpty() }
        if (uid.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Silakan login Google terlebih dahulu."))
        }
        val uploadResult = R2CdnUploader.compressAndUploadChatImage(
            context = context,
            uriString = imageUriString,
            uid = uid
        )
        val uploadedUrl = uploadResult.getOrElse { err ->
            return@withContext Result.failure(err)
        }
        sendMessage(
            sender = sender,
            text = captionText,
            imageUrl = uploadedUrl,
            replyTo = replyTo
        )
    }

    suspend fun editMessage(messageId: String, newText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = newText.trim()
        if (messageId.isBlank() || trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Teks pesan tidak boleh kosong."))
        }
        runCatching {
            firestore.collection(COLLECTION_MESSAGES).document(messageId)
                .update(
                    mapOf(
                        "text" to trimmed,
                        "isEdited" to true,
                        "editedAt" to System.currentTimeMillis()
                    )
                ).await()
            Unit
        }.onFailure {
            Log.e(TAG, "Gagal edit pesan $messageId: ${it.message}", it)
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("ID pesan tidak valid."))
        }
        runCatching {
            firestore.collection(COLLECTION_MESSAGES).document(messageId).delete().await()
            Unit
        }.onFailure {
            Log.e(TAG, "Gagal hapus pesan $messageId: ${it.message}", it)
        }
    }
}
