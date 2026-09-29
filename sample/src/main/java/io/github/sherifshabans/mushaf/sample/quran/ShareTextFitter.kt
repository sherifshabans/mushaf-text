package io.github.sherifshabans.mushaf.sample.quran

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint

/**
 * Picks a card width and a text size that actually fit the text being shared.
 *
 * The share cards used to pick one of five font sizes from the **character count**
 * and keep a fixed 360dp width. Character count is a poor stand-in for how text
 * wraps (harakat take no width at all, and Arabic word lengths vary a lot), and a
 * fixed width turns anything long — a full surah, say — into an unreadably tall
 * ribbon of an image.
 *
 * Here the text is measured for real with [StaticLayout] and the size is found by
 * binary search, widening the card first when a comfortable size would not fit.
 * Nothing is ever truncated: if even the smallest size overflows the tallest
 * allowance, the card simply grows.
 */
internal object ShareTextFitter {

    data class Fit(
        val cardWidthPx: Int,
        val textSizePx: Float,
        val layout: StaticLayout
    )

    /**
     * @param widthTiersPx candidate card widths, narrowest first.
     * @param sidePaddingPx horizontal padding inside the card, per side.
     * @param minPx        never go below this — text stops being readable.
     * @param maxPx        never go above this.
     * @param comfortablePx a tier is only accepted if it can hold at least this size;
     *                      otherwise the next (wider) tier is tried.
     * @param maxHeightRatio cap on text-block height as a multiple of the card width,
     *                      which is what keeps the final image from becoming a ribbon.
     */
    fun fit(
        text: CharSequence,
        widthTiersPx: IntArray,
        sidePaddingPx: Float,
        minPx: Float,
        maxPx: Float,
        comfortablePx: Float,
        maxHeightRatio: Float,
        lineSpacingMult: Float = 1.3f,
        justify: Boolean = false,
        spacingAddRatio: Float = 0.18f,
        paintFactory: (Float) -> TextPaint
    ): Fit {
        var fallback: Fit? = null

        for ((index, cardWidth) in widthTiersPx.withIndex()) {
            val textWidth = (cardWidth - sidePaddingPx * 2).toInt().coerceAtLeast(1)
            val budget = cardWidth * maxHeightRatio

            var low = minPx
            var high = maxPx
            var best: Fit? = null

            // 8 halvings gets us within ~0.05px of the largest size that fits.
            repeat(8) {
                val mid = (low + high) / 2f
                val layout = rtlLayout(
                    text, paintFactory(mid), textWidth, lineSpacingMult,
                    justify = justify, spacingAddRatio = spacingAddRatio
                )
                if (layout.height <= budget) {
                    best = Fit(cardWidth, mid, layout)
                    low = mid
                } else {
                    high = mid
                }
            }

            val fitted = best
            if (fitted != null) {
                // Good enough to stop widening?
                if (fitted.textSizePx >= comfortablePx || index == widthTiersPx.lastIndex) {
                    return fitted
                }
                fallback = fitted
            }
        }

        // Nothing fit the height budget at any tier: use the widest card at the
        // smallest readable size and let the card grow rather than cut the text.
        fallback?.let { return it }
        val widest = widthTiersPx.last()
        val textWidth = (widest - sidePaddingPx * 2).toInt().coerceAtLeast(1)
        return Fit(
            widest, minPx,
            rtlLayout(
                text, paintFactory(minPx), textWidth, lineSpacingMult,
                justify = justify, spacingAddRatio = spacingAddRatio
            )
        )
    }

    data class MultiFit(
        val cardWidthPx: Int,
        val textSizePx: Float,
        val layouts: List<StaticLayout>
    )

    /**
     * Same as [fit], but for a text that comes in several parts which must share
     * one card width and **one** size.
     *
     * Fitting each part on its own would give a du'a whose first passage is set in
     * 23px and whose second is set in 14px — the parts are one text, so they are
     * measured as one: the budget is checked against their combined height plus
     * [extraPerPartPx] for whatever is drawn between them (divider, part number).
     */
    fun fitAll(
        texts: List<CharSequence>,
        widthTiersPx: IntArray,
        sidePaddingPx: Float,
        minPx: Float,
        maxPx: Float,
        comfortablePx: Float,
        maxHeightRatio: Float,
        extraPerPartPx: Float = 0f,
        lineSpacingMult: Float = 1.3f,
        paintFactory: (Float) -> TextPaint
    ): MultiFit {
        var fallback: MultiFit? = null

        for ((index, cardWidth) in widthTiersPx.withIndex()) {
            val textWidth = (cardWidth - sidePaddingPx * 2).toInt().coerceAtLeast(1)
            val budget = cardWidth * maxHeightRatio - extraPerPartPx * (texts.size - 1)

            var low = minPx
            var high = maxPx
            var best: MultiFit? = null

            repeat(8) {
                val mid = (low + high) / 2f
                val paint = paintFactory(mid)
                val layouts = texts.map { rtlLayout(it, paint, textWidth, lineSpacingMult) }
                if (layouts.sumOf { it.height } <= budget) {
                    best = MultiFit(cardWidth, mid, layouts)
                    low = mid
                } else {
                    high = mid
                }
            }

            val fitted = best
            if (fitted != null) {
                if (fitted.textSizePx >= comfortablePx || index == widthTiersPx.lastIndex) {
                    return fitted
                }
                fallback = fitted
            }
        }

        fallback?.let { return it }
        val widest = widthTiersPx.last()
        val textWidth = (widest - sidePaddingPx * 2).toInt().coerceAtLeast(1)
        val paint = paintFactory(minPx)
        return MultiFit(
            widest, minPx,
            texts.map { rtlLayout(it, paint, textWidth, lineSpacingMult) }
        )
    }

    /**
     * RTL paragraph layout. The extra leading is proportional to the text size —
     * it used to be a flat 8px, which is airy under a 12px font and cramped under
     * a 46px one.
     *
     * The alignment is [Layout.Alignment.ALIGN_NORMAL], i.e. flush with the start
     * of the paragraph — the **right** edge, since the direction is forced RTL.
     * It used to be `ALIGN_OPPOSITE`, which in an RTL paragraph means flush left:
     * every Arabic line hugged the left margin and left a ragged gap down the
     * right side of the card. Paints must leave `textAlign` at its LEFT default —
     * setting it to RIGHT shifts each line again on top of this and the two
     * cancel out into something that only looks centred by accident.
     *
     * [justify] spreads the spaces so both margins are flush, the way a printed
     * mushaf sets a page. Android only does this from API 26; below it the text
     * simply stays right-aligned.
     */
    @Suppress("DEPRECATION")
    fun rtlLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        lineSpacingMult: Float = 1.3f,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        justify: Boolean = false,
        spacingAddRatio: Float = 0.18f
    ): StaticLayout {
        val spacingAdd = paint.textSize * spacingAddRatio
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(alignment)
                .setLineSpacing(spacingAdd, lineSpacingMult)
                .setIncludePad(false)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .apply {
                    if (justify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                    }
                }
                .build()
        } else {
            StaticLayout(text, paint, width, alignment, lineSpacingMult, spacingAdd, false)
        }
    }
}
