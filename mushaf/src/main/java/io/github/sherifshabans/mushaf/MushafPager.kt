package io.github.sherifshabans.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A pager state over the 604 pages. [initialPage] is the **mushaf page number** (1‥604);
 * `state.currentPage + 1` is the page on screen.
 */
@Composable
fun rememberMushafPagerState(initialPage: Int = 1): PagerState =
    rememberPagerState(initialPage = (initialPage - 1).coerceIn(0, Quran.PAGE_COUNT - 1)) {
        Quran.PAGE_COUNT
    }

/**
 * The whole mushaf, one page per screen, turning right-to-left like a printed copy
 * (the next page comes in from the left) whatever the app's locale.
 *
 * Each page gets a small header (surah · juz) and footer (page number) unless
 * [showHeaders] is false. Every other parameter is passed through to [MushafPage].
 */
@Composable
fun MushafPager(
    modifier: Modifier = Modifier,
    state: PagerState = rememberMushafPagerState(),
    tajweed: Boolean = false,
    naturalMadd: Boolean = false,
    colors: MushafColors = MushafColors.Light,
    showHeaders: Boolean = true,
    selectedAyahIds: Set<Int> = emptySet(),
    highlightedAyahs: Map<Int, Color> = emptyMap(),
    playingAyahIds: Set<Int> = emptySet(),
    onAyahClick: ((Ayah) -> Unit)? = null,
    onAyahLongClick: ((Ayah) -> Unit)? = null,
    onTajweedClick: ((TajweedHit) -> Unit)? = null,
    maxFontSize: Float = DEFAULT_MAX_FONT_SIZE,
    pagePadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
) {
    val outerDirection = LocalLayoutDirection.current
    // الصفحة الجاية تيجي من الشمال زي المصحف المطبوع، مهما كانت لغة التطبيق.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HorizontalPager(
            state = state,
            modifier = modifier.background(colors.paper),
            beyondViewportPageCount = 1,
            key = { it }
        ) { index ->
            CompositionLocalProvider(LocalLayoutDirection provides outerDirection) {
                val page = index + 1
                val context = LocalContext.current
                val ayahs by produceState<List<Ayah>?>(null, page) {
                    value = Quran.load(context).filter { it.page == page }
                }
                Column(Modifier.fillMaxSize().padding(pagePadding)) {
                    if (showHeaders) PageHeader(ayahs, colors)
                    MushafPage(
                        ayahs = ayahs ?: emptyList(),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        tajweed = tajweed,
                        naturalMadd = naturalMadd,
                        colors = colors,
                        selectedAyahIds = selectedAyahIds,
                        highlightedAyahs = highlightedAyahs,
                        playingAyahIds = playingAyahIds,
                        onAyahClick = onAyahClick,
                        onAyahLongClick = onAyahLongClick,
                        onTajweedClick = onTajweedClick,
                        maxFontSize = maxFontSize
                    )
                    if (showHeaders) PageFooter(page, colors)
                }
            }
        }
    }
}

private fun chromeStyle(colors: MushafColors) =
    TextStyle(fontFamily = MushafFont, fontSize = 15.sp, color = colors.marker)

@Composable
private fun PageHeader(ayahs: List<Ayah>?, colors: MushafColors) {
    val first = ayahs?.firstOrNull()
    // السورة يمين والجزء شمال — زي رأس الصفحة المطبوعة.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                text = first?.let { "سُورَةُ " + Quran.surah(it.surah).nameArabic } ?: "",
                style = chromeStyle(colors)
            )
            BasicText(
                text = first?.let { "الجزء " + toArabicDigits(it.juz) } ?: "",
                style = chromeStyle(colors)
            )
        }
    }
}

@Composable
private fun PageFooter(page: Int, colors: MushafColors) {
    Box(Modifier.fillMaxWidth().padding(top = 2.dp), contentAlignment = Alignment.Center) {
        BasicText(text = toArabicDigits(page), style = chromeStyle(colors))
    }
}

/**
 * The colour key for the tajweed rules — a wrapping row of swatch + name.
 *
 * @param english show [TajweedRule.englishName] instead of the Arabic name.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TajweedLegend(
    modifier: Modifier = Modifier,
    colors: MushafColors = MushafColors.Light,
    includeNaturalMadd: Boolean = false,
    english: Boolean = false,
    textColor: Color = colors.ink
) {
    val rules = TajweedRule.entries.filter { includeNaturalMadd || !it.isNaturalMadd }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (english) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rules.forEach { rule ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape).background(colors.tajweedColor(rule))
                    )
                    BasicText(
                        text = if (english) rule.englishName else rule.label,
                        modifier = Modifier.padding(horizontal = 5.dp),
                        style = TextStyle(fontSize = 13.sp, color = textColor)
                    )
                }
            }
        }
    }
}
