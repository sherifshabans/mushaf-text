package io.github.sherifshabans.mushaf

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Look of the surah-name banner drawn above a new surah.
 * Matches the styles used in the customized reader of QuranyApp.
 */
enum class SurahHeaderStyle {
    /** Classic gold/teal cartouche look. */
    CLASSIC,
    /** Modern teal pill. */
    MODERN,
    /** No decorative frame — just the name as text. */
    PLAIN
}

/**
 * Decorative options for a mushaf page.
 *
 * @param pageFrame draw an ornamental double-line frame around the page.
 * @param surahHeaderStyle style of the surah name banner.
 * @param ayahFrame draw a light frame around highlighted / selected ayahs.
 */
@Immutable
data class MushafDecoration(
    val pageFrame: Boolean = false,
    val surahHeaderStyle: SurahHeaderStyle = SurahHeaderStyle.CLASSIC,
    val ayahFrame: Boolean = false,
) {
    companion object {
        val None = MushafDecoration()
        val Classic = MushafDecoration(
            pageFrame = true,
            surahHeaderStyle = SurahHeaderStyle.CLASSIC,
            ayahFrame = true,
        )
        val Modern = MushafDecoration(
            pageFrame = true,
            surahHeaderStyle = SurahHeaderStyle.MODERN,
            ayahFrame = true,
        )
    }
}

/**
 * Palette used when drawing frames and headers.
 * Falls back to [MushafColors] when a specific colour is not set.
 */
@Immutable
data class MushafFrameColors(
    val frameLine: Color,
    val frameBand: Color,
    val headerFill: Color,
    val headerBorder: Color,
    val headerText: Color,
) {
    companion object {
        fun classic(colors: MushafColors) = MushafFrameColors(
            frameLine = Color(0xFFB8902F),
            frameBand = Color(0xFFFFF1CC),
            headerFill = Color(0xFF0F5245),
            headerBorder = Color(0xFFB8902F),
            headerText = Color(0xFFF3D98B),
        )
        fun modern(colors: MushafColors) = MushafFrameColors(
            frameLine = Color(0xFF00A388),
            frameBand = Color(0xFFE6F5F1),
            headerFill = Color(0xFF00A388),
            headerBorder = Color(0xFF00A388),
            headerText = Color.White,
        )
        fun of(style: SurahHeaderStyle, colors: MushafColors) = when (style) {
            SurahHeaderStyle.MODERN -> modern(colors)
            else -> classic(colors)
        }
    }
}
