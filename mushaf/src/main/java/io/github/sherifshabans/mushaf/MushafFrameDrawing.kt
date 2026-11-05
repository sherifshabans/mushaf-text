package io.github.sherifshabans.mushaf

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawText
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full port of the ornate page frame from MushafFrameDrawable.
 * Band fill (EVEN_ODD), thin diamonds, rope of beads, double rules,
 * corner marks, and a simple page-number medallion.
 */
internal fun DrawScope.drawOrnatePageFrame(
    colors: MushafColors,
    style: SurahHeaderStyle,
    pageNumber: Int,
) {
    val frameColors = MushafFrameColors.of(style, colors)
    val unit = 1.dp.toPx()
    val rightPage = pageNumber % 2 == 1
    val outerMargin = 9f * unit
    val innerMargin = 3f * unit
    val topMargin = 2f * unit
    val bottomMargin = 19f * unit
    val bandOuter = 12f * unit
    val bandThin = 6f * unit
    val leftM = if (rightPage) innerMargin else outerMargin
    val rightM = if (rightPage) outerMargin else innerMargin
    val frameLeft = leftM
    val frameTop = topMargin
    val frameRight = size.width - rightM
    val frameBottom = size.height - bottomMargin
    val bl = if (rightPage) bandThin else bandOuter
    val br = if (rightPage) bandOuter else bandThin
    val innerLeft = frameLeft + bl
    val innerTop = frameTop + bandThin
    val innerRight = frameRight - br
    val innerBottom = frameBottom - bandThin

    // band fill (frame minus inner) via EVEN_ODD
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(RoundRect(frameLeft, frameTop, frameRight, frameBottom, CornerRadius(4 * unit)))
        addRect(Rect(innerLeft, innerTop, innerRight, innerBottom))
    }
    drawPath(path, frameColors.frameBand)

    fun diamond(cx: Float, cy: Float, r: Float, alpha: Float = 0.5f) {
        val d = Path().apply {
            moveTo(cx - r, cy); lineTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); close()
        }
        drawPath(d, frameColors.frameLine.copy(alpha = alpha))
    }

    // thin diamonds
    var x = innerLeft + 6 * unit
    while (x < innerRight - 3 * unit) {
        diamond(x, frameTop + bandThin / 2, 1.5f * unit)
        diamond(x, frameBottom - bandThin / 2, 1.5f * unit)
        x += 9 * unit
    }
    val thinX = if (rightPage) frameLeft + bandThin / 2 else frameRight - bandThin / 2
    var y = innerTop + 6 * unit
    while (y < innerBottom - 3 * unit) {
        diamond(thinX, y, 1.5f * unit)
        y += 9 * unit
    }

    // rope of beads
    val ropeX = if (rightPage) innerRight + bandOuter / 2 else innerLeft - bandOuter / 2
    drawLine(
        color = frameColors.frameLine.copy(alpha = 0.6f),
        start = Offset(ropeX, innerTop),
        end = Offset(ropeX, innerBottom),
        strokeWidth = 0.8f * unit,
    )
    y = innerTop + 7 * unit
    while (y < innerBottom - 4 * unit) {
        drawCircle(frameColors.frameBand, 2.8f * unit, Offset(ropeX, y))
        drawCircle(frameColors.frameLine, 2.8f * unit, Offset(ropeX, y), style = Stroke(0.9f * unit))
        drawCircle(frameColors.frameLine, 1f * unit, Offset(ropeX, y))
        y += 11 * unit
    }

    // outer border + inner double rule
    drawRoundRect(
        color = frameColors.frameLine,
        topLeft = Offset(frameLeft, frameTop),
        size = Size(frameRight - frameLeft, frameBottom - frameTop),
        cornerRadius = CornerRadius(4 * unit),
        style = Stroke(2f * unit),
    )
    drawRect(
        color = frameColors.frameLine,
        topLeft = Offset(innerLeft, innerTop),
        size = Size(innerRight - innerLeft, innerBottom - innerTop),
        style = Stroke(1.2f * unit),
    )
    drawRect(
        color = frameColors.frameLine.copy(alpha = 0.6f),
        topLeft = Offset(innerLeft + 2.5f * unit, innerTop + 2.5f * unit),
        size = Size(innerRight - innerLeft - 5f * unit, innerBottom - innerTop - 5f * unit),
        style = Stroke(0.6f * unit),
    )

    // corner diamonds
    listOf(
        Offset(innerLeft, innerTop), Offset(innerRight, innerTop),
        Offset(innerLeft, innerBottom), Offset(innerRight, innerBottom),
    ).forEach { c -> diamond(c.x, c.y, 3.2f * unit, 1f) }

    // page-number medallion (simple circle; text drawn by caller if needed)
    val pnX = if (rightPage) frameRight - 30 * unit else frameLeft + 30 * unit
    val medR = 12f * unit
    drawCircle(frameColors.frameBand, medR, Offset(pnX, frameBottom))
    drawCircle(frameColors.frameLine, medR, Offset(pnX, frameBottom), style = Stroke(1.2f * unit))
}

/**
 * Full port of the classic surah header from SuraHeaderSpan.drawClassic.
 * Ornamental band, cartouche, side panels for ayah count.
 */
internal fun DrawScope.drawClassicSurahHeader(
    banner: DrawnBanner,
    colors: MushafColors,
) {
    val frameColors = MushafFrameColors.of(SurahHeaderStyle.CLASSIC, colors)
    val w = size.width
    val h = banner.boxHeight
    val top = banner.boxTop
    val s = h / 84f
    val cx = w / 2f
    val cy = top + h / 2f

    // outer band
    drawRoundRect(
        color = frameColors.frameBand,
        topLeft = Offset(s, top + s),
        size = Size(w - 2 * s, h - 2 * s),
        cornerRadius = CornerRadius(5 * s),
    )
    drawRoundRect(
        color = frameColors.frameLine,
        topLeft = Offset(s, top + s),
        size = Size(w - 2 * s, h - 2 * s),
        cornerRadius = CornerRadius(5 * s),
        style = Stroke(1.6f * s),
    )

    // inner field
    drawRoundRect(
        color = Color(0xFFFFF8E7),
        topLeft = Offset(8 * s, top + 8 * s),
        size = Size(w - 16 * s, h - 16 * s),
        cornerRadius = CornerRadius(3 * s),
    )
    drawRoundRect(
        color = frameColors.frameLine,
        topLeft = Offset(8 * s, top + 8 * s),
        size = Size(w - 16 * s, h - 16 * s),
        cornerRadius = CornerRadius(3 * s),
        style = Stroke(1.2f * s),
    )

    // cartouche (rounded for now; full cubic path can be added)
    val cw = (w * 0.28f).coerceAtMost(118 * s)
    val ch = 22 * s
    drawRoundRect(
        color = frameColors.headerFill,
        topLeft = Offset(cx - cw, cy - ch),
        size = Size(2 * cw, 2 * ch),
        cornerRadius = CornerRadius(8 * s),
    )
    drawRoundRect(
        color = frameColors.frameLine,
        topLeft = Offset(cx - cw, cy - ch),
        size = Size(2 * cw, 2 * ch),
        cornerRadius = CornerRadius(8 * s),
        style = Stroke(2f * s),
    )
    drawCircle(frameColors.frameLine, 3 * s, Offset(cx - cw, cy))
    drawCircle(frameColors.frameLine, 3 * s, Offset(cx + cw, cy))

    // side panel for ayah count
    val pw = ((w - 2 * cw - 48 * s) / 2f).coerceAtLeast(0f)
    if (pw > 18 * s) {
        val px = 14 * s
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(px, top + 17 * s),
            size = Size(pw, h - 34 * s),
            cornerRadius = CornerRadius(6 * s),
            style = Stroke(width = 1f * s),
        )
        fun diamond(cx: Float, cy: Float, r: Float) {
            val d = Path().apply {
                moveTo(cx - r, cy); lineTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); close()
            }
            drawPath(d, frameColors.frameLine)
        }
        diamond(px + pw / 2, top + 17 * s, 4 * s)
        diamond(px + pw / 2, top + h - 17 * s, 4 * s)
    }

    // name text
    drawText(banner.layout, topLeft = Offset(banner.textLeft, banner.textTop))
}
