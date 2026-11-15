package com.yuranime.dinz.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.yuranime.dinz.data.firebase.ChatTextSegment
import com.yuranime.dinz.data.firebase.DiscordCosmeticsRepository
import com.yuranime.dinz.data.firebase.GlobalChatMessage
import com.yuranime.dinz.data.firebase.InlineStickerCodec
import com.yuranime.dinz.ui.components.AnimatedCosmeticImage
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Bentuk Modern Tail Chat Bubble dengan sudut lembut 18.dp dan ekor melengkung di sudut bawah.
 */
class ModernTailChatBubbleShape(
    private val isOwn: Boolean,
    private val cornerRadius: Dp = 18.dp,
    private val tailWidth: Dp = 7.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val r = with(density) { cornerRadius.toPx() }.coerceAtMost(minOf(w, h) * 0.42f)
        val tw = with(density) { tailWidth.toPx() }.coerceAtMost(w * 0.12f)
        val th = (tw * 1.15f).coerceAtMost(h * 0.35f)

        val path = Path().apply {
            if (!isOwn) {
                val left = tw
                moveTo(left + r, 0f)
                lineTo(w - r, 0f)
                arcTo(Rect(w - 2 * r, 0f, w, 2 * r), -90f, 90f, false)
                lineTo(w, h - r)
                arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
                lineTo(left + r * 0.45f, h)
                quadraticTo(left * 0.45f, h, 0f, h)
                quadraticTo(left * 0.95f, h - th * 0.72f, left, h - r.coerceAtLeast(th))
                lineTo(left, r)
                arcTo(Rect(left, 0f, left + 2 * r, 2 * r), 180f, 90f, false)
                close()
            } else {
                val right = w - tw
                moveTo(r, 0f)
                lineTo(right - r, 0f)
                arcTo(Rect(right - 2 * r, 0f, right, 2 * r), -90f, 90f, false)
                lineTo(right, h - r.coerceAtLeast(th))
                quadraticTo(right + tw * 0.05f, h - th * 0.72f, w, h)
                quadraticTo(right + tw * 0.55f, h, right - r * 0.45f, h)
                lineTo(r, h)
                arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
                lineTo(0f, r)
                arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
                close()
            }
        }
        return Outline.Generic(path)
    }
}

/**
 * Avatar bulat pengirim beserta Avatar Decoration statis agar scroll list chat tetap ringan.
 */
@Composable
fun StaticChatAvatarWithBorder(
    avatarUrl: String,
    name: String,
    borderUrl: String,
    size: Dp = 36.dp,
    onClick: () -> Unit
) {
    val frameSize = size * 1.24f
    val staticBorderUrl = remember(borderUrl) {
        if (borderUrl.isNotBlank()) {
            DiscordCosmeticsRepository.getOptimizedUrl(borderUrl, size = 128, animated = false)
        } else ""
    }
    Box(
        modifier = Modifier
            .size(frameSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF1A1D27)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Avatar $name",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = name.take(1).uppercase(Locale.ROOT),
                    color = Color.White,
                    fontSize = (size.value * 0.42f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }
        if (staticBorderUrl.isNotBlank()) {
            AnimatedCosmeticImage(
                url = staticBorderUrl,
                isAnimated = false,
                contentDescription = null,
                modifier = Modifier.size(frameSize)
            )
        }
    }
}

/**
 * Merender teks chat yang dapat mengandung token stiker inline `[s:URL]` secara sebaris.
 */
@Composable
fun InlineStickerText(
    rawText: String,
    textColor: Color = Color(0xFFF8FAFC),
    fontSize: TextUnit = 14.5.sp,
    lineHeight: TextUnit = 21.sp,
    stickerSizeSp: TextUnit = 24.sp
) {
    val segments = remember(rawText) { InlineStickerCodec.parseSegments(rawText) }
    val annotated = remember(segments) {
        buildAnnotatedString {
            segments.forEachIndexed { idx, seg ->
                when (seg) {
                    is ChatTextSegment.TextChunk -> append(seg.text)
                    is ChatTextSegment.StickerChunk -> appendInlineContent("stk_$idx", "[stiker]")
                }
            }
        }
    }
    val inlineMap = remember(segments, stickerSizeSp) {
        val map = mutableMapOf<String, InlineTextContent>()
        segments.forEachIndexed { idx, seg ->
            if (seg is ChatTextSegment.StickerChunk) {
                map["stk_$idx"] = InlineTextContent(
                    Placeholder(
                        width = stickerSizeSp,
                        height = stickerSizeSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    AsyncImage(
                        model = seg.url,
                        contentDescription = "Stiker inline",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 1.dp)
                    )
                }
            }
        }
        map
    }
    Text(
        text = annotated,
        inlineContent = inlineMap,
        color = textColor,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = SfProFontFamily
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageRow(
    message: GlobalChatMessage,
    isOwn: Boolean,
    isHighlighted: Boolean,
    onAvatarOrNameClick: (GlobalChatMessage) -> Unit,
    onReplyTriggered: (GlobalChatMessage) -> Unit,
    onReplyQuoteClick: (String) -> Unit,
    onLongPress: (GlobalChatMessage) -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val replyThresholdPx = with(density) { 56.dp.toPx() }
    val maxDragPx = with(density) { 82.dp.toPx() }
    val offsetX = remember(message.id) { Animatable(0f) }

    val highlightAlpha by animateColorAsState(
        targetValue = if (isHighlighted) Color(0x28FF5722) else Color.Transparent,
        animationSpec = tween(durationMillis = 360),
        label = "replyJumpHighlight"
    )

    val formattedTime = remember(message.timestamp) {
        if (message.timestamp > 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
        } else {
            "--:--"
        }
    }

    val bubbleShape = remember(isOwn) { ModernTailChatBubbleShape(isOwn = isOwn) }
    val roleUpper = remember(message.role) { message.role.ifBlank { "Member" }.uppercase(Locale.ROOT) }
    val isPrivilegedRole = remember(roleUpper) { roleUpper == "ADMIN" || roleUpper == "OWNER" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightAlpha)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Ikon indikator swipe reply elastis di belakang sisi kiri item
        val dragFraction = (offsetX.value / replyThresholdPx).coerceIn(0f, 1.25f)
        if (dragFraction > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(32.dp)
                    .graphicsLayer {
                        val s = (0.45f + dragFraction * 0.55f).coerceAtMost(1.1f)
                        scaleX = s
                        scaleY = s
                        alpha = dragFraction.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF1E222E))
                    .border(
                        width = 1.dp,
                        color = if (dragFraction >= 1f) Color(0xFFFF5722) else Color(0xFF33394A),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Reply,
                    contentDescription = "Balas",
                    tint = if (dragFraction >= 1f) Color(0xFFFF5722) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(message.id) {
                    var thresholdCrossed = false
                    detectHorizontalDragGestures(
                        onDragStart = { thresholdCrossed = false },
                        onHorizontalDrag = { change, dragAmount ->
                            val next = (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                            if (dragAmount != 0f) {
                                change.consume()
                            }
                            scope.launch { offsetX.snapTo(next) }
                            if (!thresholdCrossed && next >= replyThresholdPx) {
                                thresholdCrossed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDragEnd = {
                            if (offsetX.value >= replyThresholdPx) {
                                onReplyTriggered(message)
                            }
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                    )
                },
            contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
            ) {
                if (!isOwn) {
                    StaticChatAvatarWithBorder(
                        avatarUrl = message.avatar,
                        name = message.name,
                        borderUrl = message.equippedBorderUrl,
                        size = 36.dp,
                        onClick = { onAvatarOrNameClick(message) }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }

                val bubbleBgModifier = if (isOwn) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2A1219), Color(0xFF38141E)),
                            start = Offset.Zero,
                            end = Offset(420f, 360f)
                        ),
                        shape = bubbleShape
                    )
                } else {
                    Modifier.background(
                        color = Color(0xFF161922),
                        shape = bubbleShape
                    )
                }

                Box(
                    modifier = Modifier
                        .widthIn(min = 148.dp, max = 282.dp)
                        .clip(bubbleShape)
                        .then(bubbleBgModifier)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress(message)
                            }
                        )
                        .padding(
                            start = if (isOwn) 12.dp else 19.dp,
                            end = if (isOwn) 19.dp else 12.dp,
                            top = 9.dp,
                            bottom = 8.dp
                        )
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        // 1. Baris Header Bubble: Username + #Tag di kiri, LV. X tanpa kotak di kanan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable { onAvatarOrNameClick(message) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = message.name,
                                    color = if (isOwn) Color(0xFFFDA4AF) else Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "#${message.userTag}",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardDoubleArrowUp,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "LV. ${message.level}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }

                        // 2. Baris Title Aktif: Mini-Title bersih tanpa kapsul + Custom Title kompak jika terpasang
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Diamond,
                                    contentDescription = null,
                                    tint = if (message.level >= 35) Color(0xFFF59E0B) else Color(0xFFE11D48),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = message.rankTitle,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (message.equippedTitle.isNotBlank()) {
                                GeometricTitleBadge(
                                    titleName = message.equippedTitle,
                                    tierLevel = message.equippedTitleTier,
                                    styleVariant = message.equippedTitleVariant,
                                    isAnimated = false,
                                    compact = true
                                )
                            }
                        }

                        // 3. Blok Kutipan Reply di Dalam Bubble (klik untuk lompat ke pesan asli)
                        if (message.replyToId.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.25f))
                                    .clickable { onReplyQuoteClick(message.replyToId) }
                                    .padding(end = 8.dp),
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
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = message.replyToName.ifBlank { "Member" },
                                        color = Color(0xFFFF784E),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.replyToSnippet.ifBlank { "Pesan" },
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.5.sp,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DiagonalJumpArrow(
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // 4. Foto / Gambar CDN Telegram
                        if (message.imageUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(message.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto di chat",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(min = 170.dp, max = 235.dp)
                                    .heightIn(min = 110.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0E1016))
                                    .clickable { onImageClick(message.imageUrl) }
                            )
                        }

                        // 5. Stiker Besar Mandiri (statis & animasi GIF/WebP bergerak)
                        if (message.stickerUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(message.stickerUrl)
                                    .crossfade(false)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "Stiker chat",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(124.dp)
                                    .aspectRatio(1f)
                            )
                        }

                        // 6. Teks Pesan + Inline Emoji-Stiker
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            InlineStickerText(
                                rawText = message.text,
                                textColor = Color(0xFFF8FAFC),
                                fontSize = 14.5.sp,
                                lineHeight = 20.sp
                            )
                        }

                        // 7. Footer Kanan Bawah: Role + diedit + HH:mm
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = roleUpper,
                                color = if (isPrivilegedRole) Color(0xFFFB7185) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            if (message.isEdited) {
                                Text(
                                    text = "  •  diedit",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formattedTime,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }

                if (isOwn) {
                    Spacer(modifier = Modifier.width(3.dp))
                    StaticChatAvatarWithBorder(
                        avatarUrl = message.avatar,
                        name = message.name,
                        borderUrl = message.equippedBorderUrl,
                        size = 36.dp,
                        onClick = { onAvatarOrNameClick(message) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagonalJumpArrow(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.4.dp.toPx()
        drawLine(color, Offset(w * 0.18f, h * 0.82f), Offset(w * 0.84f, h * 0.16f), strokeWidth = stroke, cap = StrokeCap.Round)
        val head = Path().apply {
            moveTo(w * 0.36f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.64f)
        }
        drawPath(head, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Popup Context Menu bergaya iOS Frosted Popover saat bubble chat ditekan lama.
 */
@Composable
fun ChatContextMenuOverlay(
    targetMessage: GlobalChatMessage?,
    isOwn: Boolean,
    canModerateDelete: Boolean,
    onDismiss: () -> Unit,
    onReply: (GlobalChatMessage) -> Unit,
    onCopyText: (GlobalChatMessage) -> Unit,
    onEdit: (GlobalChatMessage) -> Unit,
    onDelete: (GlobalChatMessage) -> Unit
) {
    val visible = targetMessage != null
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(140))
    ) {
        val msg = targetMessage ?: return@AnimatedVisibility
        val canCopy = msg.text.isNotBlank()
        val canEdit = isOwn && msg.text.isNotBlank() && msg.stickerUrl.isBlank() && msg.imageUrl.isBlank()
        val canDelete = isOwn || canModerateDelete

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(140)),
                exit = scaleOut(targetScale = 0.9f, animationSpec = tween(120)) + fadeOut(tween(120))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 310.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    // Pratinjau singkat pesan di atas menu ala iOS
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xEE181B24))
                            .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg.name,
                            color = Color(0xFFFF784E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = InlineStickerCodec.buildReplySnippet(msg),
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xF21A1D27))
                            .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    ) {
                        ContextMenuActionItem(
                            icon = Icons.AutoMirrored.Outlined.Reply,
                            label = "Balas",
                            tint = Color.White,
                            onClick = {
                                onReply(msg)
                                onDismiss()
                            }
                        )
                        if (canCopy) {
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            ContextMenuActionItem(
                                icon = Icons.Outlined.ContentCopy,
                                label = "Salin Teks",
                                tint = Color.White,
                                onClick = {
                                    onCopyText(msg)
                                    onDismiss()
                                }
                            )
                        }
                        if (canEdit) {
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            ContextMenuActionItem(
                                icon = Icons.Outlined.Edit,
                                label = "Edit Pesan",
                                tint = Color.White,
                                onClick = {
                                    onEdit(msg)
                                    onDismiss()
                                }
                            )
                        }
                        if (canDelete) {
                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                            ContextMenuActionItem(
                                icon = Icons.Outlined.DeleteOutline,
                                label = "Hapus Pesan",
                                tint = Color(0xFFF43F5E),
                                onClick = {
                                    onDelete(msg)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = tint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}
