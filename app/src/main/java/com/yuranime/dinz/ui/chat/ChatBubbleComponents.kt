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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.yuranime.dinz.data.firebase.GlobalChatMessage
import com.yuranime.dinz.data.firebase.InlineStickerCodec
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
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
                    val inlineCtx = LocalContext.current
                    var resolvedInlineUrl by remember(seg.url) { mutableStateOf(seg.url) }
                    AsyncImage(
                        model = ImageRequest.Builder(inlineCtx)
                            .data(resolvedInlineUrl)
                            .crossfade(false)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .listener(
                                onError = { _, _ ->
                                    if (resolvedInlineUrl.contains("stickerly.pstatic.net", ignoreCase = true) &&
                                        resolvedInlineUrl.contains(".webp", ignoreCase = true)
                                    ) {
                                        resolvedInlineUrl = resolvedInlineUrl.replace(
                                            Regex("""\.webp(\?.*)?$""", RegexOption.IGNORE_CASE)
                                        ) { ".png${it.groupValues.getOrNull(1).orEmpty()}" }
                                    }
                                }
                            )
                            .build(),
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    val rankUpper = remember(message.rankTitle) { message.rankTitle.uppercase(Locale.ROOT) }
    val edgeGuardPx = with(density) { 26.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightAlpha)
            .padding(horizontal = 10.dp, vertical = 2.5.dp)
    ) {
        // Ikon indikator swipe reply elastis di belakang sisi kiri item
        val dragFraction = (offsetX.value / replyThresholdPx).coerceIn(0f, 1.25f)
        if (dragFraction > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(30.dp)
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
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(message.id) {
                    var thresholdCrossed = false
                    var allowDrag = false
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            // Abaikan sentuhan tepat di tepi layar agar gestur Predictive Back sistem tidak bentrok
                            allowDrag = startOffset.x > edgeGuardPx &&
                                startOffset.x < (size.width - edgeGuardPx)
                            thresholdCrossed = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!allowDrag) return@detectHorizontalDragGestures
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
                            if (!allowDrag) return@detectHorizontalDragGestures
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
                            allowDrag = false
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
                        size = 32.dp,
                        onClick = { onAvatarOrNameClick(message) }
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }

                val bubbleBgModifier = if (isOwn) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF281118), Color(0xFF35131C)),
                            start = Offset.Zero,
                            end = Offset(420f, 360f)
                        ),
                        shape = bubbleShape
                    )
                } else {
                    Modifier.background(
                        color = Color(0xFF151821),
                        shape = bubbleShape
                    )
                }

                Box(
                    modifier = Modifier
                        .widthIn(min = 96.dp, max = 266.dp)
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
                            start = if (isOwn) 10.dp else 17.dp,
                            end = if (isOwn) 17.dp else 10.dp,
                            top = 7.dp,
                            bottom = 6.dp
                        )
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        // 1. Blok Kutipan Reply di PALING ATAS Bubble (sesuai layout Anibi)
                        if (message.replyToId.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.28f))
                                    .clickable { onReplyQuoteClick(message.replyToId) }
                                    .padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .fillMaxHeight()
                                        .background(Color(0xFFFF5722))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = message.replyToName.ifBlank { "Member" },
                                        color = Color(0xFFFF784E),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.replyToSnippet.ifBlank { "Pesan" },
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.5.sp,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DiagonalJumpArrow(
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        // 2. Baris Identitas: Username + #Tag
                        Row(
                            modifier = Modifier.clickable { onAvatarOrNameClick(message) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message.name,
                                color = if (isOwn) Color(0xFFFDA4AF) else Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "#${message.userTag}",
                                color = Color(0xFF64748B),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SfProFontFamily,
                                maxLines = 1
                            )
                        }

                        // 3. Baris Pangkat Kompak: Mini-Title (ALL-CAPS tegak) | Title Besar Beranimasi | Role & Level
                        Spacer(modifier = Modifier.height(2.5.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Diamond,
                                    contentDescription = null,
                                    tint = if (message.level >= 35) Color(0xFFF59E0B) else Color(0xFFE11D48),
                                    modifier = Modifier.size(10.5.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = rankUpper,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 9.8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.35.sp,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (message.equippedTitle.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(9.dp)
                                        .background(Color.White.copy(alpha = 0.22f))
                                )
                                GeometricTitleBadge(
                                    titleName = message.equippedTitle,
                                    tierLevel = message.equippedTitleTier,
                                    styleVariant = message.equippedTitleVariant,
                                    isAnimated = true,
                                    compact = true
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(9.dp)
                                    .background(Color.White.copy(alpha = 0.22f))
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = roleUpper,
                                    color = if (isPrivilegedRole) Color(0xFFFB7185) else Color(0xFF94A3B8),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.35.sp,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1
                                )
                                Text(
                                    text = " • ",
                                    color = Color(0xFF475569),
                                    fontSize = 9.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardDoubleArrowUp,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(1.5.dp))
                                Text(
                                    text = "LV. ${message.level}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1
                                )
                            }
                        }

                        // 4. Foto / Gambar CDN Telegram
                        if (message.imageUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(5.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(message.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto di chat",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(min = 150.dp, max = 225.dp)
                                    .heightIn(min = 100.dp, max = 190.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0E1016))
                                    .clickable { onImageClick(message.imageUrl) }
                            )
                        }

                        // 5. Stiker Besar Mandiri (statis & animasi GIF/WebP bergerak)
                        if (message.stickerUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            var resolvedStickerUrl by remember(message.stickerUrl) {
                                mutableStateOf(message.stickerUrl)
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(resolvedStickerUrl)
                                    .crossfade(false)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .listener(
                                        onError = { _, _ ->
                                            if (resolvedStickerUrl.contains("stickerly.pstatic.net", ignoreCase = true) &&
                                                resolvedStickerUrl.contains(".webp", ignoreCase = true)
                                            ) {
                                                resolvedStickerUrl = resolvedStickerUrl.replace(
                                                    Regex("""\.webp(\?.*)?$""", RegexOption.IGNORE_CASE)
                                                ) { ".png${it.groupValues.getOrNull(1).orEmpty()}" }
                                            }
                                        }
                                    )
                                    .build(),
                                contentDescription = "Stiker chat",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(104.dp)
                                    .aspectRatio(1f)
                            )
                        }

                        // 6. Teks Pesan + Inline Emoji-Stiker
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            InlineStickerText(
                                rawText = message.text,
                                textColor = Color(0xFFF8FAFC),
                                fontSize = 13.8.sp,
                                lineHeight = 18.5.sp
                            )
                        }

                        // 7. Timestamp Kanan Bawah Ramping
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (message.isEdited) {
                                Text(
                                    text = "diedit  •  ",
                                    color = Color(0xFF64748B),
                                    fontSize = 9.5.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            Text(
                                text = formattedTime,
                                color = Color(0xFF94A3B8),
                                fontSize = 9.5.sp,
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
