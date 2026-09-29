package io.github.sherifshabans.mushaf.sample.quran

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colours for the sheets that sit **on top of** the mushaf (ayah actions, tafseer).
 *
 * The mushaf page itself stays parchment on purpose — it is meant to look like a
 * printed copy. The sheets are ordinary app UI though, and they were painted with
 * that same fixed parchment palette, so at night they flashed a bright cream panel
 * and the copy/share buttons kept their gold/brown regardless of the theme.
 * These resolve against both switches the app actually has: normal vs cartoon, and
 * light vs dark.
 */
data class QuranSheetColors(
    val surface: Color,
    val ink: Color,
    val inkSoft: Color,
    val divider: Color,
    /** Filled button (copy) — the primary action. */
    val accent: Color,
    val onAccent: Color,
    /** Secondary filled button (share). */
    val accentAlt: Color,
    val onAccentAlt: Color,
    val isDark: Boolean
)

@Composable
fun rememberQuranSheetColors(): QuranSheetColors {
    val dark = isSystemInDarkTheme()

    return when {
        dark -> QuranSheetColors(
            surface = Color(0xFF17130D),          // deep warm brown-black, not blue-black
            ink = Color(0xFFF2E9D8),
            inkSoft = Color(0xFFB6A98F),
            divider = QuranPalette.Gold.copy(alpha = 0.35f),
            accent = Color(0xFFD8C489),           // gold, lifted for contrast on dark
            onAccent = Color(0xFF241B0C),
            accentAlt = Color(0xFF4A382A),
            onAccentAlt = Color(0xFFF2E9D8),
            isDark = true
        )

        else -> QuranSheetColors(
            surface = QuranPalette.Paper,
            ink = QuranPalette.Ink,
            inkSoft = QuranPalette.Brown,
            divider = QuranPalette.Gold.copy(alpha = 0.4f),
            accent = QuranPalette.Gold,
            onAccent = Color.White,
            accentAlt = QuranPalette.Brown,
            onAccentAlt = Color.White,
            isDark = false
        )
    }
}


/** Accent that reads well on the sheet for headings and labels. */
@Composable
fun QuranSheetColors.headingColor(): Color =
    if (isDark) accent else MaterialTheme.colorScheme.primary
