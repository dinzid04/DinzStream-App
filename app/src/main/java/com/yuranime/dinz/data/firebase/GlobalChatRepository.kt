package com.yuranime.dinz.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query as RtdbQuery
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
 * Pengelola data real-time Chat Global berbasis Firebase Realtime Database (RTDB):
 * - `global_chat_messages` (`orderByChild("timestamp").limitToLast(120)`) + ServerValue.TIMESTAMP.
 * - `chat_presence` dengan `.onDisconnect().removeValue()` otomatis tanpa heartbeat spam.
 * - Sinkronisasi profil `users` Firestore hanya untuk UID unik yang tampil di 120 pesan aktif (anti-bengkak read).
 */
object GlobalChatRepository {
    private const val TAG = "GlobalChatRepository"
    private const val RTDB_URL = "https://dinz-62119-default-rtdb.asia-southeast1.firebasedatabase.app"
    private const val NODE_MESSAGES = "global_chat_messages"
    private const val NODE_PRESENCE = "chat_presence"
    private const val PRESENCE_TTL_MS = 300_000L
    private const val MESSAGES_LIMIT = 120
    private const val FIRESTORE_IN_BATCH_SIZE = 30

    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance(RTDB_URL) }
    private val messagesRef by lazy { database.getReference(NODE_MESSAGES) }
    private val presenceRef by lazy { database.getReference(NODE_PRESENCE) }
    private val connectedRef by lazy { database.getReference(".info/connected") }
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

    private var messagesQuery: RtdbQuery? = null
    private var messagesValueListener: ValueEventListener? = null
    private var presenceValueListener: ValueEventListener? = null
    private var connectedValueListener: ValueEventListener? = null

    private var subscribedUserUids: Set<String> = emptySet()
    private val userChunkListeners = mutableListOf<ListenerRegistration>()
    private val chunkUserMaps = LinkedHashMap<Int, Map<String, UserProfile>>()

    @Volatile
    private var lastPresenceUid: String? = null

    @Volatile
    private var lastPresenceName: String? = null

    @Volatile
    private var lastPresenceAvatar: String? = null

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

    private fun DataSnapshot.readString(key: String): String =
        (child(key).value as? String).orEmpty()

    private fun DataSnapshot.readLong(key: String, default: Long = 0L): Long =
        (child(key).value as? Number)?.toLong() ?: default

    private fun DataSnapshot.readBool(key: String, default: Boolean = false): Boolean =
        (child(key).value as? Boolean) ?: default

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

    private fun syncActiveUserListeners(messages: List<GlobalChatMessage>) {
        val activeUids = buildSet {
            messages.forEach { msg ->
                if (msg.uid.isNotBlank()) add(msg.uid)
                if (msg.replyToUid.isNotBlank()) add(msg.replyToUid)
            }
        }
        if (activeUids == subscribedUserUids) return

        subscribedUserUids = activeUids
        userChunkListeners.forEach { runCatching { it.remove() } }
        userChunkListeners.clear()
        chunkUserMaps.clear()

        if (activeUids.isEmpty()) {
            _liveUsersByUid.value = emptyMap()
            return
        }

        val sortedChunks = activeUids.toList().sorted().chunked(FIRESTORE_IN_BATCH_SIZE)
        sortedChunks.forEachIndexed { chunkIdx, uidChunk ->
            val reg = firestore.collection("users")
                .whereIn(FieldPath.documentId(), uidChunk)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listener profil aktif chat error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val localMap = HashMap<String, UserProfile>(snapshot.documents.size * 2)
                    snapshot.documents.forEach { doc ->
                        val profile = runCatching {
                            FirebaseManager.parsePublicProfileSnapshot(doc)
                        }.getOrNull() ?: return@forEach
                        localMap[doc.id] = profile
                        if (profile.uid.isNotBlank()) {
                            localMap[profile.uid] = profile
                        }
                    }
                    synchronized(chunkUserMaps) {
                        chunkUserMaps[chunkIdx] = localMap
                        val merged = HashMap<String, UserProfile>()
                        chunkUserMaps.values.forEach { merged.putAll(it) }
                        _liveUsersByUid.value = merged
                    }
                }
            userChunkListeners.add(reg)
        }
    }

    fun startListening() {
        if (messagesValueListener == null) {
            _isLoadingMessages.value = _rawMessages.value.isEmpty()
            val query = messagesRef.orderByChild("timestamp").limitToLast(MESSAGES_LIMIT)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _messagesError.value = null
                    _isLoadingMessages.value = false
                    val parsed = snapshot.children.mapNotNull { child ->
                        runCatching {
                            val id = child.key.orEmpty()
                            if (id.isBlank()) return@mapNotNull null
                            val uid = child.readString("uid")
                            val level = child.readLong("level", 1L).toInt().coerceAtLeast(1)
                            val rawTs = child.readLong("timestamp", 0L)
                            GlobalChatMessage(
                                id = id,
                                uid = uid,
                                name = child.readString("name").ifBlank { "Member Yuranime" },
                                userTag = child.readString("userTag").ifBlank {
                                    FirebaseManager.computeUserTag(uid)
                                },
                                avatar = child.readString("avatar"),
                                banner = child.readString("banner"),
                                bio = child.readString("bio"),
                                joinDate = child.readString("joinDate"),
                                role = child.readString("role").ifBlank { "Member" },
                                level = level,
                                rankTitle = child.readString("rankTitle").ifBlank {
                                    FirebaseManager.computeRankTitle(level)
                                },
                                equippedTitle = child.readString("equippedTitle"),
                                equippedTitleTier = child.readLong("equippedTitleTier", level.toLong()).toInt(),
                                equippedTitleVariant = child.readLong("equippedTitleVariant", 0L).toInt(),
                                equippedBorderUrl = child.readString("equippedBorderUrl"),
                                equippedProfileEffectUrl = child.readString("equippedProfileEffectUrl"),
                                text = child.readString("text"),
                                stickerUrl = child.readString("stickerUrl"),
                                imageUrl = child.readString("imageUrl"),
                                replyToId = child.readString("replyToId"),
                                replyToUid = child.readString("replyToUid"),
                                replyToName = child.readString("replyToName"),
                                replyToSnippet = child.readString("replyToSnippet"),
                                timestamp = rawTs,
                                isEdited = child.readBool("isEdited", false)
                            )
                        }.getOrNull()
                    }.sortedBy { it.timestamp }
                    _rawMessages.value = parsed
                    syncActiveUserListeners(parsed)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Listener RTDB chat global error: ${error.message}", error.toException())
                    _isLoadingMessages.value = false
                    _messagesError.value = error.message.ifBlank { "Gagal memuat chat global." }
                }
            }
            messagesQuery = query
            messagesValueListener = listener
            query.addValueEventListener(listener)
        }

        if (presenceValueListener == null) {
            val pListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = System.currentTimeMillis()
                    val cutoff = now - PRESENCE_TTL_MS
                    val active = snapshot.children.mapNotNull { child ->
                        val isOnline = child.readBool("isOnline", false)
                        val lastSeenMs = child.readLong("lastSeenMs", now)
                        if (!isOnline || (lastSeenMs > 0L && lastSeenMs < cutoff)) return@mapNotNull null
                        val uid = child.readString("uid").ifBlank { child.key.orEmpty() }
                        if (uid.isBlank()) return@mapNotNull null
                        ChatPresenceUser(
                            uid = uid,
                            name = child.readString("name").ifBlank { "Member" },
                            avatar = child.readString("avatar"),
                            lastSeenMs = lastSeenMs
                        )
                    }.distinctBy { it.uid }
                        .sortedWith(
                            compareByDescending<ChatPresenceUser> { it.avatar.isNotBlank() }
                                .thenByDescending { it.lastSeenMs }
                        )
                    _onlineUsers.value = active
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Listener RTDB presence chat error: ${error.message}")
                }
            }
            presenceValueListener = pListener
            presenceRef.addValueEventListener(pListener)
        }

        if (connectedValueListener == null) {
            val cListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isConnected = snapshot.getValue(Boolean::class.java) == true
                    val uid = lastPresenceUid?.takeIf { it.isNotBlank() } ?: return
                    if (isConnected) {
                        val pName = lastPresenceName ?: "Member Yuranime"
                        val pAvatar = lastPresenceAvatar.orEmpty()
                        repoScope.launch(Dispatchers.IO) {
                            publishPresenceNode(uid, pName, pAvatar)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) = Unit
            }
            connectedValueListener = cListener
            connectedRef.addValueEventListener(cListener)
        }
    }

    fun stopListening() {
        val mQuery = messagesQuery
        val mListener = messagesValueListener
        if (mQuery != null && mListener != null) {
            runCatching { mQuery.removeEventListener(mListener) }
        }
        messagesQuery = null
        messagesValueListener = null

        presenceValueListener?.let { listener ->
            runCatching { presenceRef.removeEventListener(listener) }
        }
        presenceValueListener = null

        connectedValueListener?.let { listener ->
            runCatching { connectedRef.removeEventListener(listener) }
        }
        connectedValueListener = null

        userChunkListeners.forEach { runCatching { it.remove() } }
        userChunkListeners.clear()
        subscribedUserUids = emptySet()
        synchronized(chunkUserMaps) {
            chunkUserMaps.clear()
        }
    }

    private suspend fun publishPresenceNode(uid: String, name: String, avatar: String) {
        if (FirebaseAuth.getInstance().currentUser == null) return
        runCatching {
            val nodeRef = presenceRef.child(uid)
            nodeRef.onDisconnect().removeValue().await()
            nodeRef.setValue(
                mapOf(
                    "uid" to uid,
                    "name" to name,
                    "avatar" to avatar,
                    "isOnline" to true,
                    "lastSeenMs" to ServerValue.TIMESTAMP
                )
            ).await()
        }.onFailure {
            Log.w(TAG, "Gagal perbarui presence RTDB: ${it.message}")
        }
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
        lastPresenceUid = uid
        lastPresenceName = name
        lastPresenceAvatar = avatar
        publishPresenceNode(uid, name, avatar)
    }

    suspend fun leavePresence(uid: String?) = withContext(Dispatchers.IO) {
        val resolvedUid = uid?.takeIf { it.isNotBlank() } ?: return@withContext
        if (lastPresenceUid == resolvedUid) {
            lastPresenceUid = null
            lastPresenceName = null
            lastPresenceAvatar = null
        }
        runCatching {
            val nodeRef = presenceRef.child(resolvedUid)
            nodeRef.onDisconnect().cancel().await()
            nodeRef.removeValue().await()
        }
    }

    private fun mapRtdbWriteError(err: Throwable): Throwable {
        val msg = err.message.orEmpty()
        return if (msg.contains("Permission denied", ignoreCase = true)) {
            IllegalStateException("Sesi Firebase belum aktif. Silakan logout lalu login ulang Google untuk mengirim pesan.")
        } else {
            err
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
            "timestamp" to ServerValue.TIMESTAMP,
            "isEdited" to false
        )
        runCatching {
            val pushRef = messagesRef.push()
            pushRef.setValue(payload).await()
            Unit
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { err ->
                Log.e(TAG, "Gagal mengirim pesan RTDB: ${err.message}", err)
                Result.failure(mapRtdbWriteError(err))
            }
        )
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
            messagesRef.child(messageId)
                .updateChildren(
                    mapOf(
                        "text" to trimmed,
                        "isEdited" to true,
                        "editedAt" to ServerValue.TIMESTAMP
                    )
                ).await()
            Unit
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { err ->
                Log.e(TAG, "Gagal edit pesan RTDB $messageId: ${err.message}", err)
                Result.failure(mapRtdbWriteError(err))
            }
        )
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("ID pesan tidak valid."))
        }
        runCatching {
            messagesRef.child(messageId).removeValue().await()
            Unit
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { err ->
                Log.e(TAG, "Gagal hapus pesan RTDB $messageId: ${err.message}", err)
                Result.failure(mapRtdbWriteError(err))
            }
        )
    }
}
