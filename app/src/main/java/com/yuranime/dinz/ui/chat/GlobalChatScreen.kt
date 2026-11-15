package com.yuranime.dinz.ui.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.SentimentSatisfiedAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.GlobalChatMessage
import com.yuranime.dinz.data.firebase.GlobalChatRepository
import com.yuranime.dinz.data.firebase.InlineStickerCodec
import com.yuranime.dinz.ui.components.StyledUsernameText
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Halaman Chat Global Yuranime:
 * - Header real-time dengan panah kembali murni tanpa shadow, titik online, dan maks 4 avatar + `+N`.
 * - Bubble chat berekor modern dengan Avatar Decoration statis, Swipe-to-Reply, dan Menu Konteks iOS.
 * - Composer teks + inline emoji-stiker, Panel Stiker 2 mode dengan sinkronisasi scroll,
 *   upload foto Telegram CDN (maks 10 MB), serta Bottom Sheet preview profil teman beranimasi.
 */
@Composable
fun GlobalChatScreen(
    onBackClick: () -> Unit,
    onOpenLoginProfile: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val currentProfile by FirebaseManager.userProfile.collectAsState()
    val isLoggedIn = FirebaseManager.isUserLoggedIn() && !FirebaseManager.getActiveUid().isNullOrBlank()
    val currentUid = currentProfile?.uid?.ifBlank { null } ?: FirebaseManager.getActiveUid().orEmpty()
    val isModerator = remember(currentProfile?.role) {
        val role = currentProfile?.role.orEmpty()
        role.equals("Admin", ignoreCase = true) || role.equals("Owner", ignoreCase = true)
    }

    val messages by GlobalChatRepository.messages.collectAsState()
    val onlineUsers by GlobalChatRepository.onlineUsers.collectAsState()
    val isLoadingMessages by GlobalChatRepository.isLoadingMessages.collectAsState()
    val messagesError by GlobalChatRepository.messagesError.collectAsState()

    val listState = rememberLazyListState()
    val composerState = remember { InlineStickerComposerState() }
    val inputFocusRequester = remember { FocusRequester() }

    // Single Shared Animation Clock (kontinu ms, membeku saat list scroll/fling, dibaca di fase Draw)
    val sharedClockMs = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var accumulatedMs = 0L
        var lastFrameMs = 0L
        while (isActive) {
            withInfiniteAnimationFrameMillis { frameMs ->
                if (lastFrameMs == 0L) {
                    lastFrameMs = frameMs
                } else {
                    val delta = (frameMs - lastFrameMs).coerceIn(0L, 64L)
                    lastFrameMs = frameMs
                    if (!listState.isScrollInProgress) {
                        accumulatedMs += delta
                        sharedClockMs.longValue = accumulatedMs
                    }
                }
            }
        }
    }
    val sharedTimeProvider = remember { { sharedClockMs.longValue } }

    var showStickerPanel by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var contextMenuTarget by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var previewPeer by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isSendingMessage by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val sender = currentProfile ?: run {
            Toast.makeText(context, "Silakan login Google terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val activeReply = replyingTo
        isUploadingImage = true
        scope.launch {
            val result = GlobalChatRepository.uploadAndSendChatImage(
                context = context,
                sender = sender,
                imageUriString = uri.toString(),
                captionText = composerState.exportEncodedText(),
                replyTo = activeReply
            )
            isUploadingImage = false
            result.onSuccess {
                composerState.clear()
                replyingTo = null
            }.onFailure { err ->
                Toast.makeText(
                    context,
                    err.message ?: "Gagal mengirim gambar ke chat.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Listener pesan + presence RTDB (.onDisconnect() otomatis tanpa heartbeat 20 detik)
    LaunchedEffect(Unit) {
        GlobalChatRepository.startListening()
    }

    LaunchedEffect(currentProfile?.uid, currentProfile?.name, currentProfile?.avatar, isLoggedIn) {
        if (!isLoggedIn) return@LaunchedEffect
        GlobalChatRepository.upsertPresence(currentProfile)
    }

    DisposableEffect(currentUid) {
        onDispose {
            CoroutineScope(Dispatchers.IO).launch {
                if (currentUid.isNotBlank()) {
                    GlobalChatRepository.leavePresence(currentUid)
                }
            }
            GlobalChatRepository.stopListening()
        }
    }

    // Gulir otomatis ke pesan terbaru saat baru masuk atau mengirim/menerima saat di bawah
    var initialScrollDone by remember { mutableStateOf(false) }
    val isNearBottom by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            layout.totalItemsCount == 0 || lastVisible >= layout.totalItemsCount - 3
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (!initialScrollDone) {
                listState.scrollToItem(messages.lastIndex)
                initialScrollDone = true
            } else if (isNearBottom) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    LaunchedEffect(highlightedMessageId) {
        if (highlightedMessageId != null) {
            delay(1500L)
            highlightedMessageId = null
        }
    }

    LaunchedEffect(showStickerPanel) {
        if (showStickerPanel && messages.isNotEmpty() && isNearBottom) {
            delay(120L)
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val hasChatOverlayModal = previewImageUrl != null ||
        previewPeer != null ||
        contextMenuTarget != null ||
        showStickerPanel
    BackHandler(enabled = hasChatOverlayModal) {
        when {
            previewImageUrl != null -> previewImageUrl = null
            previewPeer != null -> previewPeer = null
            contextMenuTarget != null -> contextMenuTarget = null
            showStickerPanel -> showStickerPanel = false
        }
    }

    fun requireLoggedInProfile(): Boolean {
        if (!isLoggedIn || currentProfile == null) {
            Toast.makeText(context, "Login Google dulu untuk ikut mengobrol.", Toast.LENGTH_SHORT).show()
            onOpenLoginProfile()
            return false
        }
        return true
    }

    fun submitCurrentMessage() {
        if (!requireLoggedInProfile()) return
        val sender = currentProfile ?: return
        val encoded = composerState.exportEncodedText()
        if (encoded.isBlank()) return

        val editTarget = editingMessage
        if (editTarget != null) {
            isSendingMessage = true
            scope.launch {
                val res = GlobalChatRepository.editMessage(editTarget.id, encoded)
                isSendingMessage = false
                res.onSuccess {
                    composerState.clear()
                    editingMessage = null
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Gagal memperbarui pesan.", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        val replyTarget = replyingTo
        isSendingMessage = true
        scope.launch {
            val res = GlobalChatRepository.sendMessage(
                sender = sender,
                text = encoded,
                replyTo = replyTarget
            )
            isSendingMessage = false
            res.onSuccess {
                composerState.clear()
                replyingTo = null
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Gagal mengirim pesan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            // 1. TOP HEADER CHAT GLOBAL (Panah murni tanpa shadow + status online + maks 4 avatar + +N)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0B0B))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBackClick
                            )
                            .padding(5.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Chat Global",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${onlineUsers.size} berada dalam chat",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SfProFontFamily,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Pojok Kanan Atas: Maksimal 4 Avatar Online + "+N"
                if (onlineUsers.isNotEmpty()) {
                    val displayed = onlineUsers.take(4)
                    val remaining = (onlineUsers.size - 4).coerceAtLeast(0)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                            displayed.forEachIndexed { idx, user ->
                                Box(
                                    modifier = Modifier
                                        .zIndex((4 - idx).toFloat())
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A1E29))
                                        .border(1.5.dp, Color(0xFF0B0B0B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (user.avatar.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(user.avatar)
                                                .crossfade(false)
                                                .build(),
                                            contentDescription = user.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Text(
                                            text = user.name.take(1).uppercase(Locale.ROOT),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }
                        }

                        if (remaining > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+$remaining",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF181B25))

            // 2. DAFTAR PESAN CHAT GLOBAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoadingMessages && messages.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF5722),
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Memuat Chat Global...",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.5.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                    messagesError != null && messages.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = messagesError ?: "Gagal memuat chat.",
                                color = Color(0xFFFDA4AF),
                                fontSize = 13.5.sp,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { GlobalChatRepository.startListening() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Coba Lagi",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                    messages.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Belum ada pesan di Chat Global.",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mulai obrolan atau kirim stiker pertamamu.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                        ) {
                            items(
                                items = messages,
                                key = { msg -> msg.id },
                                contentType = { msg ->
                                    when {
                                        msg.imageUrl.isNotBlank() -> "image"
                                        msg.stickerUrl.isNotBlank() -> "sticker"
                                        msg.replyToId.isNotBlank() -> "reply"
                                        else -> "text"
                                    }
                                }
                            ) { msg ->
                                val isOwn = currentUid.isNotBlank() && msg.uid == currentUid
                                ChatMessageRow(
                                    message = msg,
                                    isOwn = isOwn,
                                    isHighlighted = highlightedMessageId == msg.id,
                                    sharedTimeMillisProvider = sharedTimeProvider,
                                    onAvatarOrNameClick = { clicked -> previewPeer = clicked },
                                    onReplyTriggered = { target ->
                                        editingMessage = null
                                        replyingTo = target
                                    },
                                    onReplyQuoteClick = { refId ->
                                        val targetIdx = messages.indexOfFirst { it.id == refId }
                                        if (targetIdx >= 0) {
                                            highlightedMessageId = refId
                                            scope.launch {
                                                listState.animateScrollToItem(targetIdx)
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Pesan asli tidak ada di daftar terbaru.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onLongPress = { target -> contextMenuTarget = target },
                                    onImageClick = { url -> previewImageUrl = url }
                                )
                            }
                        }

                        // Tombol cepat kembali ke pesan terbawah bila sedang scroll ke atas
                        if (!isNearBottom && messages.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 14.dp, bottom = 12.dp)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xEE181C26))
                                    .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                    .clickable {
                                        scope.launch {
                                            listState.animateScrollToItem(messages.lastIndex)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "Ke pesan terbaru",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. BAR "MEMBALAS [NAMA]" / "EDIT PESAN" DI ATAS INPUT
            AnimatedVisibility(
                visible = replyingTo != null || editingMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val reply = replyingTo
                val edit = editingMessage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF131620))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF171B26)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFFF5722))
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (edit != null) {
                                Text(
                                    text = "Mengedit pesan",
                                    color = Color(0xFFFF5722),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Membalas ",
                                        color = Color(0xFFFF5722),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                    StyledUsernameText(
                                        name = reply?.name.orEmpty(),
                                        styleId = reply?.equippedNameStyle,
                                        colorHex = reply?.equippedNameColor,
                                        fontSize = 12.5.sp,
                                        defaultColor = Color(0xFFFF5722),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        timeMillisProvider = sharedTimeProvider,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                            InlineStickerText(
                                rawText = if (edit != null) {
                                    InlineStickerCodec.buildReplySnippet(edit)
                                } else {
                                    reply?.let { InlineStickerCodec.buildReplySnippet(it) }.orEmpty()
                                },
                                textColor = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                stickerSizeSp = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                renderLegacyStickerPlaceholder = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Batalkan",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                if (editingMessage != null) {
                                    editingMessage = null
                                    composerState.clear()
                                }
                                replyingTo = null
                            }
                            .padding(4.dp)
                    )
                }
            }

            // 4. BAR INPUT BAWAH FULL-WIDTH DOCKED (Tanpa overlay hitam, tombol menyatu di dalam kapsul)
            if (!isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF151822))
                        .border(0.75.dp, Color(0xFF252938), RoundedCornerShape(24.dp))
                        .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Masuk Google untuk mengirim chat dan stiker.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onOpenLoginProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Masuk",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            } else {
                val canSend = composerState.hasSendableContent() && !isSendingMessage && !isUploadingImage
                val sendBackgroundBrush = remember(canSend) {
                    if (canSend) {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFE11D48), Color(0xFFFF5722))
                        )
                    } else {
                        SolidColor(Color(0xFF1E222E))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .heightIn(min = 50.dp)
                        .background(Color(0xFF14161E), RoundedCornerShape(26.dp))
                        .border(1.dp, Color(0xFF232734), RoundedCornerShape(26.dp))
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Tombol "+" (Upload Gambar) presisi 38.dp
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E222E))
                            .clickable(enabled = !isUploadingImage) {
                                if (requireLoggedInProfile()) {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF5722),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Kirim foto",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 2. Tombol Emoji / Stiker / Keyboard presisi 38.dp
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (showStickerPanel) Color(0xFFE11D48).copy(alpha = 0.18f)
                                else Color(0xFF1E222E)
                            )
                            .clickable {
                                if (showStickerPanel) {
                                    showStickerPanel = false
                                    inputFocusRequester.requestFocus()
                                    keyboardController?.show()
                                } else {
                                    keyboardController?.hide()
                                    showStickerPanel = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showStickerPanel) {
                                Icons.Rounded.Keyboard
                            } else {
                                Icons.Rounded.SentimentSatisfiedAlt
                            },
                            contentDescription = if (showStickerPanel) "Tampilkan keyboard" else "Buka stiker",
                            tint = if (showStickerPanel) Color(0xFFE11D48) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3. Kolom Ketik sejajar tengah vertikal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        InlineStickerInputField(
                            state = composerState,
                            placeholder = if (editingMessage != null) "Edit pesan..." else "Tulis pesan...",
                            enabled = !isSendingMessage,
                            modifier = Modifier.focusRequester(inputFocusRequester),
                            onFocused = { showStickerPanel = false }
                        )
                    }

                    // 4. Tombol Kirim presisi 38.dp dengan gradasi aktif Crimson-Orange
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(sendBackgroundBrush)
                            .clickable(enabled = canSend) { submitCurrentMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSendingMessage) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Kirim pesan",
                                tint = if (canSend) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 5. PANEL STIKER 2 MODE
            AnimatedVisibility(
                visible = showStickerPanel && isLoggedIn,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ChatStickerPanel(
                    onInsertInlineSticker = { sticker ->
                        composerState.insertInlineSticker(sticker.url)
                    },
                    onSendLargeSticker = { sticker ->
                        val sender = currentProfile ?: return@ChatStickerPanel
                        val replyTarget = replyingTo
                        replyingTo = null
                        scope.launch {
                            GlobalChatRepository.sendMessage(
                                sender = sender,
                                stickerUrl = sticker.url,
                                replyTo = replyTarget
                            ).onSuccess {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.lastIndex)
                                }
                            }.onFailure { err ->
                                Toast.makeText(
                                    context,
                                    err.message ?: "Gagal mengirim stiker.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onBackspace = { composerState.backspace() }
                )
            }
        }

        // 6. MENU KONTEKS LONG-PRESS BERGAYA IOS
        ChatContextMenuOverlay(
            targetMessage = contextMenuTarget,
            isOwn = contextMenuTarget?.let { currentUid.isNotBlank() && it.uid == currentUid } == true,
            canModerateDelete = isModerator,
            onDismiss = { contextMenuTarget = null },
            onReply = { target ->
                editingMessage = null
                replyingTo = target
            },
            onCopyText = { target ->
                val clean = InlineStickerCodec.toReadableCopyText(target.text)
                if (clean.isNotBlank()) {
                    clipboardManager.setText(AnnotatedString(clean))
                    Toast.makeText(context, "Teks disalin", Toast.LENGTH_SHORT).show()
                }
            },
            onEdit = { target ->
                replyingTo = null
                editingMessage = target
                composerState.loadEncodedText(target.text)
                showStickerPanel = false
                inputFocusRequester.requestFocus()
                keyboardController?.show()
            },
            onDelete = { target ->
                scope.launch {
                    GlobalChatRepository.deleteMessage(target.id).onFailure { err ->
                        Toast.makeText(
                            context,
                            err.message ?: "Gagal menghapus pesan.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        // 7. BOTTOM SHEET PREVIEW PROFIL TEMAN (Kosmetik animasi penuh, avatar di kanan)
        PeerProfileBottomSheet(
            peerMessage = previewPeer,
            onDismiss = { previewPeer = null }
        )

        // 8. DIALOG PRATINJAU FOTO CHAT FULLSCREEN
        if (previewImageUrl != null) {
            Dialog(
                onDismissRequest = { previewImageUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { previewImageUrl = null }
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(previewImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Pratinjau Foto Chat",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup foto",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(18.dp)
                            .size(30.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { previewImageUrl = null }
                            )
                    )
                }
            }
        }
    }
}
