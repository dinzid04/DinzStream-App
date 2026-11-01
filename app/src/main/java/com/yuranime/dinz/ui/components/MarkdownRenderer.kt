package com.yuranime.dinz.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.ui.theme.SfProFontFamily

sealed class MarkdownNode {
    data class Heading(val level: Int, val text: String) : MarkdownNode()
    data class Bullet(val text: String) : MarkdownNode()
    data class Numbered(val number: String, val text: String) : MarkdownNode()
    data class Blockquote(val text: String) : MarkdownNode()
    data class ImageNode(val url: String, val alt: String) : MarkdownNode()
    data object Divider : MarkdownNode()
    data class Paragraph(val text: String) : MarkdownNode()
}

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFFE5E5EA),
    fontSize: TextUnit = 13.5.sp
) {
    val context = LocalContext.current
    val nodes = remember(markdown) { parseMarkdownNodes(markdown) }

    Column(modifier = modifier.fillMaxWidth()) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownNode.Heading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = node.text,
                            color = Color.White,
                            fontSize = when (node.level) {
                                1 -> 18.sp
                                2 -> 16.sp
                                else -> 14.5.sp
                            },
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = Color(0x330A84FF)
                        )
                    }
                }

                is MarkdownNode.Bullet -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 10.dp)
                                .size(5.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0A84FF))
                        )
                        MarkdownInlineFormattedText(
                            rawText = node.text,
                            textColor = textColor,
                            fontSize = fontSize,
                            onUrlClick = { url ->
                                openBrowserUrl(context, url)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownNode.Numbered -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = node.number,
                            color = Color(0xFF0A84FF),
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        MarkdownInlineFormattedText(
                            rawText = node.text,
                            textColor = textColor,
                            fontSize = fontSize,
                            onUrlClick = { url ->
                                openBrowserUrl(context, url)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownNode.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x331C1C1E))
                            .border(0.5.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFF97316))
                        )
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            MarkdownInlineFormattedText(
                                rawText = node.text,
                                textColor = Color(0xFFF1F5F9),
                                fontSize = 13.sp,
                                onUrlClick = { url ->
                                    openBrowserUrl(context, url)
                                }
                            )
                        }
                    }
                }

                is MarkdownNode.ImageNode -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E2024))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(node.url)
                                .crossfade(true)
                                .placeholder(R.drawable.ic_yuranime_monochrome)
                                .error(R.drawable.ic_yuranime_monochrome)
                                .build(),
                            contentDescription = node.alt.ifBlank { "Gambar README" },
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }

                is MarkdownNode.Divider -> {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color(0x26FFFFFF),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                is MarkdownNode.Paragraph -> {
                    if (node.text.isNotBlank()) {
                        MarkdownInlineFormattedText(
                            rawText = node.text,
                            textColor = textColor,
                            fontSize = fontSize,
                            onUrlClick = { url ->
                                openBrowserUrl(context, url)
                            },
                            modifier = Modifier.padding(vertical = 2.5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Parsing teks baris dengan formatting bold (**teks**) dan URL clickable biru (#0A84FF)
 */
@Composable
fun MarkdownInlineFormattedText(
    rawText: String,
    textColor: Color,
    fontSize: TextUnit,
    onUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(rawText, textColor) {
        buildAnnotatedString {
            // Regex mencari pola link [text](url) atau raw https://... atau **bold**
            val combinedRegex = Regex("""(\*\*([^*]+)\*\*)|(\[([^\]]+)\]\(([^)]+)\))|(https?://[^\s)]+)""")
            var currentIndex = 0

            for (match in combinedRegex.findAll(rawText)) {
                val range = match.range
                if (range.first > currentIndex) {
                    append(rawText.substring(currentIndex, range.first))
                }

                val matchedString = match.value

                when {
                    // Bold **text**
                    matchedString.startsWith("**") && matchedString.endsWith("**") -> {
                        val innerText = matchedString.removeSurrounding("**")
                        pushStyle(
                            SpanStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        append(innerText)
                        pop()
                    }
                    // Markdown Link [title](url)
                    matchedString.startsWith("[") && matchedString.contains("](") -> {
                        val title = match.groupValues[4]
                        val url = match.groupValues[5]
                        pushStringAnnotation(tag = "URL", annotation = url)
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFF0A84FF),
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(title)
                        pop()
                        pop()
                    }
                    // Raw URL https://...
                    matchedString.startsWith("http://") || matchedString.startsWith("https://") -> {
                        pushStringAnnotation(tag = "URL", annotation = matchedString)
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFF0A84FF),
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(matchedString)
                        pop()
                        pop()
                    }
                    else -> {
                        append(matchedString)
                    }
                }
                currentIndex = range.last + 1
            }

            if (currentIndex < rawText.length) {
                append(rawText.substring(currentIndex))
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontFamily = SfProFontFamily,
            lineHeight = 20.sp
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onUrlClick(annotation.item)
                }
        }
    )
}

/**
 * Parser Markdown sederhana yang mengekstrak blok struktur dari teks GitHub README / Release Body
 */
fun parseMarkdownNodes(markdown: String): List<MarkdownNode> {
    val nodes = mutableListOf<MarkdownNode>()
    val lines = markdown.lines()

    val mdImageRegex = Regex("""!\[(.*?)\]\((.*?)\)""")
    val htmlImgRegex = Regex("""<img[^>]+src=["']([^"']+)["'][^>]*>""")

    for (rawLine in lines) {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) continue

        // Deteksi Gambar Markdown ![alt](url)
        val mdImgMatch = mdImageRegex.find(trimmed)
        if (mdImgMatch != null) {
            val alt = mdImgMatch.groupValues[1]
            val url = mdImgMatch.groupValues[2]
            nodes.add(MarkdownNode.ImageNode(url = url, alt = alt))
            continue
        }

        // Deteksi Gambar Tag HTML <img src="url">
        val htmlImgMatch = htmlImgRegex.find(trimmed)
        if (htmlImgMatch != null) {
            val url = htmlImgMatch.groupValues[1]
            nodes.add(MarkdownNode.ImageNode(url = url, alt = "Preview"))
            continue
        }

        // Deteksi Divider --- atau ***
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            nodes.add(MarkdownNode.Divider)
            continue
        }

        // Deteksi Heading #, ##, ###
        if (trimmed.startsWith("### ")) {
            nodes.add(MarkdownNode.Heading(level = 3, text = trimmed.removePrefix("### ").trim()))
            continue
        }
        if (trimmed.startsWith("## ")) {
            nodes.add(MarkdownNode.Heading(level = 2, text = trimmed.removePrefix("## ").trim()))
            continue
        }
        if (trimmed.startsWith("# ")) {
            nodes.add(MarkdownNode.Heading(level = 1, text = trimmed.removePrefix("# ").trim()))
            continue
        }

        // Deteksi Blockquote >
        if (trimmed.startsWith("> ")) {
            nodes.add(MarkdownNode.Blockquote(text = trimmed.removePrefix("> ").trim()))
            continue
        }

        // Deteksi Bullet List * atau - atau +
        if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("+ ")) {
            val bulletText = trimmed.substring(2).trim()
            nodes.add(MarkdownNode.Bullet(text = bulletText))
            continue
        }

        // Deteksi Numbered List 1. , 2. , dsb
        val numberedMatch = Regex("""^(\d+\.)\s+(.*)""").find(trimmed)
        if (numberedMatch != null) {
            val num = numberedMatch.groupValues[1]
            val rest = numberedMatch.groupValues[2]
            nodes.add(MarkdownNode.Numbered(number = num, text = rest))
            continue
        }

        // Paragraph biasa
        nodes.add(MarkdownNode.Paragraph(text = trimmed))
    }

    return nodes
}

private fun openBrowserUrl(context: android.content.Context, url: String) {
    try {
        val parsedUri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
