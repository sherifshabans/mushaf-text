package io.github.sherifshabans.mushaf

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Ayat as flowing, wrapping text — for tafsir screens, search results, cards,
 * a verse of the day… anywhere the fixed 15-line page is not wanted.
 *
 * Uses the same font, ayah rosettes and tajweed colouring as [MushafPage], and
 * unlike the page it follows the reader's font size.
 *
 * @param onAyahClick tap anywhere on an ayah (including its rosette).
 */
@Composable
fun QuranText(
    ayahs: List<Ayah>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    lineHeight: TextUnit = 2.1.em,
    tajweed: Boolean = false,
    naturalMadd: Boolean = false,
    colors: MushafColors = MushafColors.Light,
    selectedAyahIds: Set<Int> = emptySet(),
    highlightedAyahs: Map<Int, Color> = emptyMap(),
    playingAyahIds: Set<Int> = emptySet(),
    textAlign: TextAlign = TextAlign.Justify,
    onAyahClick: ((Ayah) -> Unit)? = null
) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(
        fontFamily = MushafFont,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = colors.ink,
        textAlign = textAlign,
        textDirection = TextDirection.Rtl
    )

    // نفس فخّ أرقام الوردة اللي في الصفحة، بس هنا في سياق فقرة عربية: بنقيس
    // الترتيبين **جوّه جملة** ونختار اللي بيدّي وردة واحدة.
    val reverseDigits = remember(measurer, fontSize) {
        val probe = toArabicDigits(253)
        fun widthOf(s: String) = measurer.measure(AnnotatedString("ٱللَّهِ $s"), style).size.width
        widthOf("ٱللَّهِ " + probe.reversed()) < widthOf("ٱللَّهِ $probe")
    }

    val built = remember(
        ayahs, reverseDigits, tajweed, naturalMadd, colors,
        selectedAyahIds, highlightedAyahs, playingAyahIds
    ) {
        val ranges = ArrayList<Pair<IntRange, Ayah>>(ayahs.size)
        val text = buildAnnotatedString {
            ayahs.forEachIndexed { i, ayah ->
                val start = length
                append(ayah.text)
                if (tajweed) {
                    TajweedAnnotator.annotate(ayah.text, naturalMadd).forEach { s ->
                        addStyle(SpanStyle(color = colors.tajweedColor(s.rule)), start + s.start, start + s.end)
                    }
                }
                append(' ')
                val markerStart = length
                append(ayahMarkerText(ayah.number, reverseDigits))
                addStyle(SpanStyle(color = colors.marker), markerStart, length)
                val bg = when (ayah.id) {
                    in playingAyahIds -> colors.playing
                    in highlightedAyahs -> highlightedAyahs.getValue(ayah.id).copy(alpha = 0.30f)
                    in selectedAyahIds -> colors.selection
                    else -> null
                }
                if (bg != null) addStyle(SpanStyle(background = bg), start, length)
                ranges += (start until length) to ayah
                if (i != ayahs.lastIndex) append(' ')
            }
        }
        text to ranges
    }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val onClick by rememberUpdatedState(onAyahClick)
    val tapModifier = if (onAyahClick == null) Modifier else Modifier.pointerInput(built) {
        detectTapGestures { pos ->
            val offset = layout?.getOffsetForPosition(pos) ?: return@detectTapGestures
            built.second.firstOrNull { offset in it.first }?.second?.let { onClick?.invoke(it) }
        }
    }

    BasicText(
        text = built.first,
        modifier = modifier.then(tapModifier),
        style = style,
        onTextLayout = { layout = it }
    )
}

/** A single ayah as flowing text. */
@Composable
fun QuranText(
    ayah: Ayah,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    tajweed: Boolean = false,
    colors: MushafColors = MushafColors.Light,
    textAlign: TextAlign = TextAlign.Center
) = QuranText(
    ayahs = listOf(ayah),
    modifier = modifier,
    fontSize = fontSize,
    tajweed = tajweed,
    colors = colors,
    textAlign = textAlign
)
