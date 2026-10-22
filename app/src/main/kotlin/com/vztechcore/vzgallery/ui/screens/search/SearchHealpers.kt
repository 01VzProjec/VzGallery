package com.vztechcore.vzgallery.ui.screens.search

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import java.text.Normalizer

/**
 * Removes accents and lowercases the text.
 * "Vídeo" → "video"
 * "VÍDEO" → "video"
 */
internal fun normalizeText(text: String): String {
    val noAccent = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
    return noAccent.lowercase()
}

/**
 * Checks whether `target` contains `query`, ignoring accents and case.
 */
internal fun containsText(target: String, query: String): Boolean {
    if (query.isBlank()) return false
    return normalizeText(target).contains(normalizeText(query))
}

/**
 * Returns the index of `query` inside the normalized `target`
 * (used for highlighting), or -1 if not found.
 */
internal fun indexOfText(target: String, query: String): Int {
    if (query.isBlank()) return -1
    return normalizeText(target).indexOf(normalizeText(query))
}

/**
 * Text with a colored highlight on the matched query substring.
 */
@Composable
internal fun HighlightedText(
    text: String,
    query: String,
    normalColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    highlightColor: Color = VzSecondary,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val idx = indexOfText(text, query)

    if (idx < 0 || query.isBlank()) {
        Text(
            text = text,
            color = normalColor,
            style = style,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
        return
    }

    val end = (idx + query.length).coerceAtMost(text.length)

    val annotated = buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
            append(text.substring(idx, end))
        }
        append(text.substring(end))
    }

    Text(
        text = annotated,
        color = normalColor,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}