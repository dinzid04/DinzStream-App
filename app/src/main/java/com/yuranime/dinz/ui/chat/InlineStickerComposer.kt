package com.yuranime.dinz.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.yuranime.dinz.data.firebase.InlineStickerCodec
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * State editor untuk kolom ketik yang mendukung teks sekaligus stiker kecil sebaris (`[s:URL]`).
 */
@Stable
class InlineStickerComposerState {
    var textFieldValue by mutableStateOf(TextFieldValue(""))
        private set
    var stickerUrls by mutableStateOf<List<String>>(emptyList())
        private set

    val isEmpty: Boolean
        get() = textFieldValue.text.isBlank() && stickerUrls.isEmpty()

    fun hasSendableContent(): Boolean = textFieldValue.text.trim().isNotEmpty() || stickerUrls.isNotEmpty()

    fun updateFromTextField(incoming: TextFieldValue) {
        val oldText = textFieldValue.text
        val oldUrls = stickerUrls

        // Tolak penyisipan karakter placeholder ilegal dari keyboard/clipboard
        val oldCount = oldText.count { it == InlineStickerCodec.PLACEHOLDER_CHAR }
        val newCount = incoming.text.count { it == InlineStickerCodec.PLACEHOLDER_CHAR }

        if (newCount > oldCount) {
            val cleaned = incoming.copy(
                text = incoming.text.replace(InlineStickerCodec.PLACEHOLDER_CHAR, ' ')
            )
            // Hitung ulang setelah menormalkan placeholder asing
            updateFromTextField(cleaned)
            return
        }

        if (newCount == oldCount) {
            textFieldValue = incoming
            return
        }

        // Sinkronisasi tepat saat satu atau lebih placeholder dihapus (backspace / cut / seleksi)
        val newText = incoming.text
        var prefix = 0
        val minLen = min(oldText.length, newText.length)
        while (prefix < minLen && oldText[prefix] == newText[prefix]) {
            prefix++
        }
        var suffix = 0
        while (suffix < (minLen - prefix) &&
            oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) {
            suffix++
        }

        val delStart = prefix
        val delEndExclusive = (oldText.length - suffix).coerceAtLeast(delStart)

        val surviving = mutableListOf<String>()
        var urlCursor = 0
        for (i in oldText.indices) {
            if (oldText[i] == InlineStickerCodec.PLACEHOLDER_CHAR) {
                if (i < delStart || i >= delEndExclusive) {
                    oldUrls.getOrNull(urlCursor)?.let { surviving.add(it) }
                }
                urlCursor++
            }
        }

        stickerUrls = surviving
        textFieldValue = incoming
    }

    fun insertInlineSticker(url: String) {
        if (url.isBlank()) return
        val cur = textFieldValue
        val selStart = cur.selection.min.coerceIn(0, cur.text.length)
        val selEnd = cur.selection.max.coerceIn(selStart, cur.text.length)

        // Jika ada teks yang sedang diblok/diseleksi, hapus rentang URL pada seleksi dulu
        if (selEnd > selStart) {
            val before = cur.text.substring(0, selStart)
            val after = cur.text.substring(selEnd)
            updateFromTextField(TextFieldValue(before + after, TextRange(selStart)))
        }

        val latest = textFieldValue
        val insertPos = latest.selection.min.coerceIn(0, latest.text.length)
        val insertUrlIndex = latest.text
            .substring(0, insertPos)
            .count { it == InlineStickerCodec.PLACEHOLDER_CHAR }

        val updatedUrls = stickerUrls.toMutableList().apply {
            add(insertUrlIndex.coerceIn(0, size), url)
        }
        val updatedText = latest.text.substring(0, insertPos) +
            InlineStickerCodec.PLACEHOLDER_CHAR +
            latest.text.substring(insertPos)

        stickerUrls = updatedUrls
        textFieldValue = TextFieldValue(
            text = updatedText,
            selection = TextRange(insertPos + 1)
        )
    }

    fun backspace() {
        val cur = textFieldValue
        if (cur.text.isEmpty()) return
        val start = cur.selection.min.coerceIn(0, cur.text.length)
        val end = cur.selection.max.coerceIn(start, cur.text.length)
        val next = if (end > start) {
            cur.text.removeRange(start, end) to start
        } else if (start > 0) {
            cur.text.removeRange(start - 1, start) to (start - 1)
        } else {
            return
        }
        updateFromTextField(TextFieldValue(next.first, TextRange(next.second)))
    }

    fun loadEncodedText(rawText: String) {
        val (editorStr, urls) = InlineStickerCodec.decodeToEditorState(rawText)
        stickerUrls = urls
        textFieldValue = TextFieldValue(
            text = editorStr,
            selection = TextRange(editorStr.length)
        )
    }

    fun exportEncodedText(): String {
        return InlineStickerCodec.encodeFromEditorState(textFieldValue.text, stickerUrls)
    }

    fun clear() {
        stickerUrls = emptyList()
        textFieldValue = TextFieldValue("")
    }
}

private val InlineStickerSlotVisualTransformation = VisualTransformation { original ->
    val transformed = buildAnnotatedString {
        append(original.text)
        original.text.forEachIndexed { idx, ch ->
            if (ch == InlineStickerCodec.PLACEHOLDER_CHAR) {
                addStyle(
                    style = SpanStyle(
                        color = Color.Transparent,
                        fontSize = 23.sp
                    ),
                    start = idx,
                    end = idx + 1
                )
            }
        }
    }
    TransformedText(transformed, OffsetMapping.Identity)
}

/**
 * Input teks Compose yang melapis stiker inline bergerak/statis tepat di koordinat kursor pengguna.
 */
@Composable
fun InlineStickerInputField(
    state: InlineStickerComposerState,
    placeholder: String = "Tulis pesan...",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onFocused: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var textLayoutResult by androidx.compose.runtime.remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    BasicTextField(
        value = state.textFieldValue,
        onValueChange = { next -> state.updateFromTextField(next) },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp, max = 96.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocused()
            },
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontFamily = SfProFontFamily
        ),
        cursorBrush = SolidColor(Color(0xFFFF5722)),
        maxLines = 4,
        visualTransformation = InlineStickerSlotVisualTransformation,
        onTextLayout = { layout -> textLayoutResult = layout },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (state.isEmpty) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF64748B),
                        fontSize = 15.sp,
                        fontFamily = SfProFontFamily
                    )
                }

                innerTextField()

                // Render setiap stiker inline tepat di atas slot placeholder \u3000
                val layout = textLayoutResult
                val currentText = state.textFieldValue.text
                if (layout != null && state.stickerUrls.isNotEmpty() && layout.layoutInput.text.text == currentText) {
                    var stickerIdx = 0
                    for (charIdx in currentText.indices) {
                        if (currentText[charIdx] == InlineStickerCodec.PLACEHOLDER_CHAR) {
                            val url = state.stickerUrls.getOrNull(stickerIdx)
                            stickerIdx++
                            if (!url.isNullOrBlank() && charIdx < layout.layoutInput.text.length) {
                                val box = layout.getBoundingBox(charIdx)
                                val wDp = with(density) { (box.right - box.left).coerceAtLeast(18f).toDp() }
                                val hDp = with(density) { (box.bottom - box.top).coerceAtLeast(18f).toDp() }
                                val iconSize = minOf(wDp, hDp).coerceIn(20.dp, 24.dp)
                                val offsetX = (box.left + ((box.right - box.left) - with(density) { iconSize.toPx() }) / 2f).roundToInt()
                                val offsetY = (box.top + ((box.bottom - box.top) - with(density) { iconSize.toPx() }) / 2f).roundToInt()

                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(url)
                                        .crossfade(false)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = "Stiker inline",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .offset { IntOffset(offsetX, offsetY) }
                                        .size(iconSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
