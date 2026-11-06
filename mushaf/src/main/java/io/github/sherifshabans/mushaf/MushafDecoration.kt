package io.github.sherifshabans.mushaf

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

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
 * Optional per-surah overrides for the header cartouche panels.
 * Null fields keep the bundled [Surah] table values.
 * Use this when the host app already has Room/json metadata and wants to
 * force a specific revelation type or ayah count without forking the table.
 */
@Immutable
data class SurahHeaderOverride(
    val revelationType: RevelationType? = null,
    val ayahCount: Int? = null,
    val nameArabic: String? = null,
)

/**
 * Decorative options for a mushaf page.
 *
 * @param pageFrame draw an ornamental double-line frame around the page. The frame
 *   geometry also owns the text area, so the ayat are laid out inside it and never
 *   cross the band, the rope of beads, or the page-number medallion.
 * @param surahHeaderStyle style of the surah name banner.
 * @param ayahFrame draw a light frame around highlighted / selected / playing
 *   ayahs. The frame is padded off the glyphs and joins across a multi-line ayah.
 * @param surahHeaderOverrides optional per-surah revelation / count / name overrides.
 */
@Immutable
data class MushafDecoration(
    val pageFrame: Boolean = false,
    val surahHeaderStyle: SurahHeaderStyle = SurahHeaderStyle.CLASSIC,
    val ayahFrame: Boolean = false,
    /**
     * Per-surah header overrides (1‥114). Missing keys use the bundled table.
     * Typical use: pass revelation types from the host app's database.
     */
    val surahHeaderOverrides: Map<Int, SurahHeaderOverride> = emptyMap(),
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
        /**
         * Gold/teal palette. Derived from [colors] so the frame follows the page:
         * a cream page gets the printed gold, a dark page gets a lifted gold that
         * does not sink into the paper.
         */
        fun classic(colors: MushafColors): MushafFrameColors {
            val dark = colors.paper.luminance() < 0.5f
            val gold = if (dark) Color(0xFFD9B45A) else Color(0xFFB8902F)
            return MushafFrameColors(
                frameLine = gold,
                frameBand = if (dark) Color(0xFF2B2418) else Color(0xFFFFF1CC),
                headerFill = if (dark) Color(0xFF123F36) else Color(0xFF0F5245),
                headerBorder = gold,
                headerText = if (dark) Color(0xFFF6E3A8) else Color(0xFFF3D98B),
            )
        }

        /** Teal palette, likewise derived from [colors]. */
        fun modern(colors: MushafColors): MushafFrameColors {
            val dark = colors.paper.luminance() < 0.5f
            val teal = if (dark) Color(0xFF27C4A6) else Color(0xFF00A388)
            return MushafFrameColors(
                frameLine = teal,
                frameBand = if (dark) Color(0xFF1B2B28) else Color(0xFFE6F5F1),
                headerFill = teal,
                headerBorder = teal,
                headerText = if (dark) Color(0xFF07211C) else Color.White,
            )
        }

        fun of(style: SurahHeaderStyle, colors: MushafColors): MushafFrameColors =
            when (style) {
                SurahHeaderStyle.MODERN -> modern(colors)
                else -> classic(colors)
            }
    }
}
