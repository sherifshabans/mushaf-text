package io.github.sherifshabans.mushaf

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/**
 * **KFGQPC HAFS Uthmanic Script** (v0.18) — the King Fahd Complex font the
 * Madinah Mushaf is set in, bundled unmodified.
 *
 * The text uses the KFGQPC encoding (`U+06E1` as sukun, `U+0657`/`U+065E` as
 * stacked tanween, digits ligating into the ayah rosette), so it must be drawn
 * in this font: a generic Arabic font shows the tanween and markers wrong.
 *
 * The font's licence permits free use and redistribution but forbids modifying
 * or selling it — see `licenses/` in the repository.
 */
val MushafFont: FontFamily = FontFamily(Font(R.font.uthmanic_hafs))

/**
 * Colours of the page. The page background is **not** drawn by [MushafPage] —
 * put [paper] (or your own) behind it — so it composes with any container.
 *
 * @param tajweed overrides for individual rules; anything missing uses
 *   [TajweedRule.color].
 */
@Immutable
data class MushafColors(
    val paper: Color,
    val ink: Color,
    val marker: Color,
    val bannerFill: Color,
    val bannerBorder: Color,
    val bannerText: Color,
    val selection: Color,
    val playing: Color,
    val tajweed: Map<TajweedRule, Color> = emptyMap()
) {
    fun tajweedColor(rule: TajweedRule): Color = tajweed[rule] ?: rule.color

    companion object {
        /** Cream paper, near-black ink — the printed look. */
        val Light = MushafColors(
            paper = Color(0xFFFAF6EB),
            ink = Color(0xFF1A1208),
            marker = Color(0xFF6B4F3A),
            bannerFill = Color(0xFFF4EDE0),
            bannerBorder = Color(0xFFC4B275),
            bannerText = Color(0xFF6B4F3A),
            selection = Color(0xFFEADDC9),
            playing = Color(0xFFCFE3CC)
        )

        /**
         * Night reading. The tajweed colours are lifted towards white, since the
         * defaults are tuned for cream paper and sink into a dark page.
         */
        val Dark = MushafColors(
            paper = Color(0xFF15130F),
            ink = Color(0xFFEDE6D6),
            marker = Color(0xFFC9A77C),
            bannerFill = Color(0xFF221E17),
            bannerBorder = Color(0xFF8C7A45),
            bannerText = Color(0xFFD9C29A),
            selection = Color(0xFF3A3122),
            playing = Color(0xFF23402A),
            tajweed = TajweedRule.entries.associateWith { lerp(it.color, Color.White, 0.42f) }
        )
    }
}
