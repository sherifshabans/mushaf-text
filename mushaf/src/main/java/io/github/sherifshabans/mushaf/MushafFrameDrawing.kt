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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * إطار الصفحة وترويسة السورة — منقولين من MushafFrameDrawable و SuraHeaderSpan
 * في QuranyApp.
 *
 * الأرقام هنا هي **المصدر الواحد** للرسم ولحشوة النص معًا. قبل كده الصفحة كانت
 * بتتخطّط بعرض الـCanvas كله والإطار بيترسم بعده جوّه نفس الـCanvas، فالسطور
 * كانت بتعدّي فوق الإطار وحبل الخرز، والبـanner كان بيخرج من حافتي الإطار.
 * دلوقتي `ornateFrameGeometry` بيحسب الإطار مرة واحدة، والرسم بيرسمه والنص
 * بيتخطّط جوّه `content*` بتاعته — فما ينفعش يفترقوا تاني.
 */

/** Defaults when callers do not pass [MushafDecoration] margins. */
private const val DEFAULT_OUTER_MARGIN = 5f
private const val DEFAULT_INNER_MARGIN = 2f
private const val DEFAULT_TOP_MARGIN = 1.5f
private const val DEFAULT_BOTTOM_MARGIN = 14f
private const val DEFAULT_TEXT_GAP = 8f

/** عرض شريط الإطار: عريض على الجانب الخارجي (وحبل الخرز)، رفيع على الداخلي. */
private const val BAND_OUTER = 12f
private const val BAND_THIN = 6f

/**
 * نصف قطر وردة رقم الصفحة بوحدات الإطار (dp)، ونسبة القرص الداخلي إليها.
 * القرص = نجمة `0.74` من نصف القطر، ثم دائرة `0.92` من النجمة.
 * [pageNumberDiskDiameter] والرسم بيستخدموا نفس الرقمين.
 */
internal const val PAGE_MEDALLION_RADIUS = 16f
internal const val PAGE_MEDALLION_STAR_FACTOR = 0.74f
internal const val PAGE_MEDALLION_DISK_FACTOR = 0.92f

/** نصف قطر وردة علامة القسم في الهامش — أصغر من وردة رقم الصفحة. */
internal const val MARK_MEDALLION_RADIUS = 11f

/** ارتفاع شريط رأس الصفحة المطبوع: السورة يمينًا والجزء شمالًا، جوّه الإطار. */
internal const val RUNNING_HEAD_HEIGHT = 18f

/** أصغر حجم مقروء لتسمية الوردة؛ عندها تتبدّل «ثلاثة» بـ«٣/٤». */
internal const val MARK_LABEL_MIN_SIZE = 7f

/** قطر القرص الداخلي لوردة رقم الصفحة بالبكسل، وهو صندوق الحبر الأقصى للرقم. */
internal fun pageNumberDiskDiameter(unit: Float): Float =
    2f * PAGE_MEDALLION_RADIUS * PAGE_MEDALLION_STAR_FACTOR * PAGE_MEDALLION_DISK_FACTOR * unit

/** قطر القرص الداخلي لوردة علامة القسم بالبكسل — صندوق حبر التسمية. */
internal fun markDiskDiameter(unit: Float): Float =
    2f * MARK_MEDALLION_RADIUS * PAGE_MEDALLION_STAR_FACTOR * PAGE_MEDALLION_DISK_FACTOR * unit

/**
 * A label fitted to a medallion's inner disk, positioned by its **ink**.
 *
 * The sans face used for digits and labels has a tall em (Arabic marks), so the
 * line box is much taller than the drawn ink; centre on the ink, not the box.
 */
class FittedLabel(
    val layout: TextLayoutResult,
    val inkCenterX: Float,
    val inkCenterY: Float,
)

/** One drawn verse line's span in canvas pixels, for frames that underline lines. */
data class LineSpan(val top: Float, val bottom: Float, val left: Float, val right: Float)

/** Everything a [PageFrame] needs to compute its geometry. Pure input. */
data class PageFrameRequest(
    val widthPx: Float,
    val heightPx: Float,
    val pageNumber: Int,
    /** Screen density in px per dp — the `unit` the frame draws in. */
    val density: Float,
    val outerMargin: Float,
    val innerMargin: Float,
    val topMargin: Float,
    val bottomMargin: Float,
    val textGap: Float,
    val divisionMarks: Boolean,
)

/**
 * Frame bounds and the content rect, in canvas pixels.
 *
 * [contentLeft]/[contentTop] are the origin of the page content: everything
 * the layout draws at (0, 0) lands inside the frame. The contract for every
 * [PageFrame]: the content rect ends inside the frame rect and is at least
 * 1 px wide and tall.
 */
data class PageFrameGeometry(
    val unit: Float,
    val rightPage: Boolean,
    val frameLeft: Float,
    val frameTop: Float,
    val frameRight: Float,
    val frameBottom: Float,
    val contentLeft: Float,
    val contentTop: Float,
    val contentWidth: Float,
    val contentHeight: Float,
    /** x of the outer rule, where margin medallions are centred. */
    val markAnchorX: Float,
)

/** A division mark and the y (canvas px) of the centre of the line it labels. */
data class MarkAnchor(
    val mark: MushafMark,
    val centerY: Float,
    /** The mark's label fitted to its medallion, measured by the page. */
    val label: FittedLabel? = null,
)

/**
 * What a [PageFrame] may paint, measured by the page before drawing.
 *
 * @property pageNumberLabel the page number fitted to the medallion disk, or
 *   `null` when there is no number to draw.
 * @property marks division marks with their line anchors (empty unless
 *   `MushafDecoration.divisionMarks`).
 * @property runningSurah running-head surah label (right side), or `null`.
 * @property runningJuzLabel running-head juz label (left side), or `null`.
 * @property lineSpans the verse lines in canvas pixels, top to bottom.
 * @property runningSurahLayout the measured running head, ready to draw —
 *   the built-in frames paint these; hosts may draw [runningSurah] /
 *   [runningJuzLabel] in their own fonts instead.
 */
data class PageFrameContext(
    val colors: MushafColors,
    val frameColors: MushafFrameColors,
    val pageNumber: Int,
    val pageNumberLabel: FittedLabel?,
    val marks: List<MarkAnchor>,
    val runningSurah: String?,
    val runningJuzLabel: String?,
    val lineSpans: List<LineSpan>,
    val runningSurahLayout: TextLayoutResult? = null,
    val runningJuzLayout: TextLayoutResult? = null,
)

/**
 * A page frame: owns the content rect and paints the page's decoration.
 *
 * Implementations must keep `geometry` a pure function of the request, keep
 * the content rect inside the frame, and paint decoration only — never ayah
 * text. [MushafPage] lays the text out in the content rect and translates the
 * canvas by `contentLeft`, `contentTop`.
 */
interface PageFrame {
    fun geometry(request: PageFrameRequest): PageFrameGeometry

    fun DrawScope.draw(geometry: PageFrameGeometry, context: PageFrameContext)
}

/** The frame and header styles shipped with the library. */
object PageFrames {

    /**
     * The bead-rope ornate frame of the Madinah pages — the frame 1.5.0 drew,
     * now behind the [PageFrame] interface.
     */
    object Ornate : PageFrame {
        override fun geometry(request: PageFrameRequest): PageFrameGeometry =
            ornateFrameGeometry(
                width = request.widthPx,
                height = request.heightPx,
                pageNumber = request.pageNumber,
                unit = request.density,
                outerMargin = request.outerMargin,
                innerMargin = request.innerMargin,
                topMargin = request.topMargin,
                bottomMargin = request.bottomMargin,
                textGap = request.textGap,
                runningHead = request.divisionMarks,
            ).toPageFrameGeometry()

        override fun DrawScope.draw(geometry: PageFrameGeometry, context: PageFrameContext) {
            drawOrnatePageFrame(
                geometry = geometry.toMushafFrameGeometry(),
                colors = context.colors,
                frameColors = context.frameColors,
                pageNumber = context.pageNumber,
                pageNumberLabel = context.pageNumberLabel,
            )
            drawDivisionMarks(geometry, context)
            drawRunningHead(geometry, context)
        }
    }

    /** The quiet Cairo-family double rule. See [CairoFrame]. */
    val Cairo: PageFrame get() = CairoFrame

    /** The Diyanet single green rule with floret corners. See [DiyanetFrame]. */
    val Diyanet: PageFrame get() = DiyanetFrame

    /** The Indo-Pak flat red band with verse underlines. See [IndoPakFrame]. */
    val IndoPak: PageFrame get() = IndoPakFrame
}

/**
 * وردات علامات الأقسام في الهامش — على الخط الخارجي للإطار، بلون الزخرفة
 * (مش حبر الآيات) عشان اختبار الشريط يفضل صفر.
 *
 * @param plainCircle دائرة بسيطة (Cairo) بدل وردة الثمانية.
 */
internal fun DrawScope.drawDivisionMarks(
    geometry: PageFrameGeometry,
    context: PageFrameContext,
    frameColors: MushafFrameColors = context.frameColors,
    plainCircle: Boolean = false,
) {
    val unit = geometry.unit
    context.marks.forEach { anchor ->
        if (plainCircle) {
            drawCircle(frameColors.frameBand, MARK_MEDALLION_RADIUS * unit, Offset(geometry.markAnchorX, anchor.centerY))
            drawCircle(
                frameColors.frameLine, MARK_MEDALLION_RADIUS * unit,
                Offset(geometry.markAnchorX, anchor.centerY), style = Stroke(1.3f * unit),
            )
        } else {
            drawRosette(
                cx = geometry.markAnchorX,
                cy = anchor.centerY,
                r = MARK_MEDALLION_RADIUS * unit,
                fill = frameColors.frameBand,
                line = frameColors.frameLine,
                unit = unit,
            )
        }
        anchor.label?.let { label ->
            drawText(
                label.layout,
                topLeft = Offset(
                    geometry.markAnchorX - label.inkCenterX,
                    anchor.centerY - label.inkCenterY,
                ),
            )
        }
    }
}

/** رأس الصفحة المطبوع في شريطه: السورة يمينًا والجزء شمالًا. */
internal fun DrawScope.drawRunningHead(
    geometry: PageFrameGeometry,
    context: PageFrameContext,
) {
    val stripHeight = RUNNING_HEAD_HEIGHT * geometry.unit
    val stripCenterY = geometry.contentTop - stripHeight / 2f
    val contentRight = geometry.contentLeft + geometry.contentWidth
    context.runningSurahLayout?.let { layout ->
        drawText(
            layout,
            topLeft = Offset(contentRight - layout.size.width, stripCenterY - layout.size.height / 2f),
        )
    }
    context.runningJuzLayout?.let { layout ->
        drawText(
            layout,
            topLeft = Offset(geometry.contentLeft, stripCenterY - layout.size.height / 2f),
        )
    }
}

/**
 * مواضع وردات علامات الأقسام — دالة نقية تتنادى من التخطيط وتتكتب بالاختبارات.
 *
 * السطر المستهدف هو سطر الحرف الداخلي (`۞` للربع و`۩` للسجدة)؛ وربع بداية
 * السورة مالوش حرف فيترسّى على أول كلمة من آيته (سطر البانر فوقه). وردتان
 * متداخلتان تُزحزح واحدة بقطرها ناحية أقرب حافة مناسبة جوّه الإطار.
 *
 * `centerY` في إحداثيات المحتوى (قبل إزاحة `contentTop`)، و[frameTop] /
 * [frameBottom] حدود الإزاحة الرأسية المسموح بها.
 */
internal fun markAnchors(
    marks: List<MushafMark>,
    ayahs: List<Ayah>,
    lineTokens: Map<Int, List<MushafToken>>,
    pitch: Float,
    offsetY: Float,
    frameTop: Float,
    frameBottom: Float,
    stackDistance: Float,
): List<MarkAnchor> {
    val idOf = HashMap<Pair<Int, Int>, Int>(ayahs.size)
    ayahs.forEach { idOf[it.surah to it.number] = it.id }
    val lines = lineTokens.toSortedMap()

    val raw = ArrayList<MarkAnchor>(marks.size)
    marks.forEach { mark ->
        val id = idOf[mark.surah to mark.ayah] ?: return@forEach
        val glyph = if (mark.kind == DivisionKind.SAJDAH) '۩' else '۞'
        var glyphLine: Int? = null
        var firstLine: Int? = null
        for ((line, tokens) in lines) {
            for (token in tokens) {
                if (token.ayahId != id) continue
                if (firstLine == null) firstLine = line
                if (glyph in token.text) {
                    glyphLine = line
                    break
                }
            }
            if (glyphLine != null) break
        }
        val line = glyphLine ?: firstLine ?: return@forEach
        raw += MarkAnchor(mark, centerY = offsetY + pitch * (line - 0.5f))
    }

    val placed = ArrayList<MarkAnchor>(raw.size)
    raw.sortedBy { it.centerY }.forEach { anchor ->
        var y = anchor.centerY
        var guard = 0
        while (placed.any { abs(it.centerY - y) < stackDistance } && guard++ < 8) {
            val up = y - stackDistance
            val down = y + stackDistance
            y = when {
                abs(y - frameTop) <= abs(frameBottom - y) && up >= frameTop -> up
                down <= frameBottom -> down
                else -> up
            }.coerceIn(frameTop, frameBottom)
        }
        placed += anchor.copy(centerY = y)
    }
    return placed
}

/** The public geometry of the ornate frame; its medallions sit on the outer rule. */
internal fun MushafFrameGeometry.toPageFrameGeometry(): PageFrameGeometry = PageFrameGeometry(
    unit = unit,
    rightPage = rightPage,
    frameLeft = frameLeft,
    frameTop = frameTop,
    frameRight = frameRight,
    frameBottom = frameBottom,
    contentLeft = contentLeft,
    contentTop = contentTop,
    contentWidth = contentWidth,
    contentHeight = contentHeight,
    markAnchorX = if (rightPage) frameRight else frameLeft,
)

/** The ornate frame's own view of a public geometry (band/inner from the same constants). */
internal fun PageFrameGeometry.toMushafFrameGeometry(): MushafFrameGeometry = MushafFrameGeometry(
    unit = unit,
    rightPage = rightPage,
    frameLeft = frameLeft,
    frameTop = frameTop,
    frameRight = frameRight,
    frameBottom = frameBottom,
    bandLeft = (if (rightPage) BAND_THIN else BAND_OUTER) * unit,
    bandRight = (if (rightPage) BAND_OUTER else BAND_THIN) * unit,
)

/**
 * أبعاد إطار الصفحة بالبكسل، وحدود منطقة النص اللي جوّاه.
 *
 * [contentLeft]/[contentTop] هما أصل الرسم للمحتوى: كل حاجة بتترسم عند 0 بتفضل
 * جوّه الإطار.
 */
internal class MushafFrameGeometry(
    val unit: Float,
    val rightPage: Boolean,
    val frameLeft: Float,
    val frameTop: Float,
    val frameRight: Float,
    val frameBottom: Float,
    val bandLeft: Float,
    val bandRight: Float,
    textGap: Float = DEFAULT_TEXT_GAP,
    /** زيادة فوق منطقة النص (شريط الرأس) — صفر إلا مع علامات الأقسام. */
    topInset: Float = 0f,
) {
    val innerLeft: Float = frameLeft + bandLeft
    val innerTop: Float = frameTop + BAND_THIN * unit
    val innerRight: Float = frameRight - bandRight
    val innerBottom: Float = frameBottom - BAND_THIN * unit

    val contentLeft: Float = innerLeft + textGap * unit
    val contentTop: Float = innerTop + textGap * unit + topInset
    private val contentRight: Float = innerRight - textGap * unit
    private val contentBottom: Float = innerBottom - textGap * unit

    val contentWidth: Float = (contentRight - contentLeft).coerceAtLeast(1f)
    val contentHeight: Float = (contentBottom - contentTop).coerceAtLeast(1f)
}

/** الصفحة الفردية يمينية: الجانب الخارجي (الشريط العريض والحبل) على اليمين. */
internal fun isRightPage(pageNumber: Int): Boolean = pageNumber % 2 == 1

/** يحسب الإطار من أبعاد الـCanvas — نفس الأرقام اللي الرسم بيستخدمها. */
internal fun ornateFrameGeometry(
    width: Float,
    height: Float,
    pageNumber: Int,
    unit: Float,
    outerMargin: Float = DEFAULT_OUTER_MARGIN,
    innerMargin: Float = DEFAULT_INNER_MARGIN,
    topMargin: Float = DEFAULT_TOP_MARGIN,
    bottomMargin: Float = DEFAULT_BOTTOM_MARGIN,
    textGap: Float = DEFAULT_TEXT_GAP,
    runningHead: Boolean = false,
): MushafFrameGeometry {
    val rightPage = isRightPage(pageNumber)
    val leftMargin = (if (rightPage) innerMargin else outerMargin) * unit
    val rightMargin = (if (rightPage) outerMargin else innerMargin) * unit
    return MushafFrameGeometry(
        unit = unit,
        rightPage = rightPage,
        frameLeft = leftMargin,
        frameTop = topMargin * unit,
        frameRight = width - rightMargin,
        frameBottom = height - bottomMargin * unit,
        bandLeft = (if (rightPage) BAND_THIN else BAND_OUTER) * unit,
        bandRight = (if (rightPage) BAND_OUTER else BAND_THIN) * unit,
        textGap = textGap,
        topInset = if (runningHead) RUNNING_HEAD_HEIGHT * unit else 0f,
    )
}

/** قطعة معيّنة (بالمية) — تُرسم بالمسار عشان تشتغل في كل الاتجاهات. */
private fun DrawScope.diamond(
    cx: Float,
    cy: Float,
    r: Float,
    color: Color,
) {
    val d = Path().apply {
        moveTo(cx - r, cy); lineTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); close()
    }
    drawPath(d, color)
}

/**
 * Full port of the ornate page frame from MushafFrameDrawable.
 * Band fill, thin diamonds, rope of beads, double rules, corner marks,
 * and the page-number rosette.
 */
internal fun DrawScope.drawOrnatePageFrame(
    geometry: MushafFrameGeometry,
    colors: MushafColors,
    frameColors: MushafFrameColors,
    pageNumber: Int,
    pageNumberLabel: FittedLabel? = null,
) {
    val unit = geometry.unit
    val rightPage = geometry.rightPage
    val frameLeft = geometry.frameLeft
    val frameTop = geometry.frameTop
    val frameRight = geometry.frameRight
    val frameBottom = geometry.frameBottom
    val innerLeft = geometry.innerLeft
    val innerTop = geometry.innerTop
    val innerRight = geometry.innerRight
    val innerBottom = geometry.innerBottom

    // band fill (frame minus inner) via EVEN_ODD
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(
            RoundRect(frameLeft, frameTop, frameRight, frameBottom, CornerRadius(4 * unit))
        )
        addRect(Rect(innerLeft, innerTop, innerRight, innerBottom))
    }
    drawPath(path, frameColors.frameBand)

    // thin diamonds: along the top/bottom bands, and down the thin side band
    val softLine = frameColors.frameLine.copy(alpha = 120f / 255f)
    var x = innerLeft + 6 * unit
    while (x < innerRight - 3 * unit) {
        diamond(x, frameTop + BAND_THIN * unit / 2, 1.5f * unit, softLine)
        diamond(x, frameBottom - BAND_THIN * unit / 2, 1.5f * unit, softLine)
        x += 9 * unit
    }
    var y = innerTop + 6 * unit
    val thinX = if (rightPage) frameLeft + BAND_THIN * unit / 2
    else frameRight - BAND_THIN * unit / 2
    while (y < innerBottom - 3 * unit) {
        diamond(thinX, y, 1.5f * unit, softLine)
        y += 9 * unit
    }

    // rope of beads down the wide (outer) band
    val ropeX = if (rightPage) innerRight + BAND_OUTER * unit / 2
    else innerLeft - BAND_OUTER * unit / 2
    drawLine(
        color = frameColors.frameLine.copy(alpha = 150f / 255f),
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

    // borders: thick outside line, double rule inside
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
        color = frameColors.frameLine.copy(alpha = 160f / 255f),
        topLeft = Offset(innerLeft + 2.5f * unit, innerTop + 2.5f * unit),
        size = Size(innerRight - innerLeft - 5f * unit, innerBottom - innerTop - 5f * unit),
        style = Stroke(0.6f * unit),
    )

    // corner diamonds
    listOf(
        Offset(innerLeft, innerTop), Offset(innerRight, innerTop),
        Offset(innerLeft, innerBottom), Offset(innerRight, innerBottom),
    ).forEach { c -> diamond(c.x, c.y, 3.2f * unit, frameColors.frameLine) }

    // page-number rosette, on the bottom border at the outer corner.
    // The number is centered on its ink, not the line box: the sans face used
    // for digits has a tall em (Arabic marks) and the glyphs would otherwise
    // sit small, or drift, inside the disk.
    val medallionR = PAGE_MEDALLION_RADIUS * unit
    val pnX = if (rightPage) frameRight - 30 * unit else frameLeft + 30 * unit
    drawRosette(
        cx = pnX,
        cy = frameBottom,
        r = medallionR,
        fill = frameColors.frameBand,
        line = frameColors.frameLine,
        unit = unit,
    )
    pageNumberLabel?.let { label ->
        drawText(
            label.layout,
            topLeft = Offset(pnX - label.inkCenterX, frameBottom - label.inkCenterY),
        )
    }
}

/** نجمة تمانية زي ميداليات المصحف المطبوع. */
private fun DrawScope.drawRosette(
    cx: Float,
    cy: Float,
    r: Float,
    fill: Color,
    line: Color,
    unit: Float,
) {
    val star = r * PAGE_MEDALLION_STAR_FACTOR
    val path = Path().apply {
        fillType = PathFillType.NonZero
        for (i in 0 until 16) {
            val a = Math.PI / 8 * i - Math.PI / 2
            val rr = if (i % 2 == 0) r else star * 1.02f
            val px = cx + (rr * cos(a)).toFloat()
            val py = cy + (rr * sin(a)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(path, fill)
    drawPath(path, line, style = Stroke(1.3f * unit))

    val disk = star * PAGE_MEDALLION_DISK_FACTOR
    drawCircle(fill, disk, Offset(cx, cy))
    drawCircle(line, disk, Offset(cx, cy), style = Stroke(0.9f * unit))
}

/**
 * نصف عرض الكارتوث ونصف ارتفاعه لوحدات التصميم — مشترك بين الرسم وقياس الاسم،
 * عشان الاسم يتقصّ على اللي هيتـرسـم بالظبط.
 */
private class CartoucheSize(val halfWidth: Float, val halfHeight: Float)

private fun cartoucheSize(width: Float, height: Float): CartoucheSize {
    val s = height / 84f
    val scale = (width / (472 * s)).coerceAtMost(1.15f)
    val halfWidth = min(118 * s * scale, width * 0.3f)
    return CartoucheSize(halfWidth, 30 * s)
}

/** أقصى عرض لاسم السورة جوّه الكارتوث. */
internal fun surahNameMaxWidth(width: Float, height: Float): Float =
    (2 * cartoucheSize(width, height).halfWidth - 56 * (height / 84f)).coerceAtLeast(1f)

/** أقصى ارتفاع لاسم السورة جوّه الكارتوث. */
internal fun surahNameMaxHeight(width: Float, height: Float): Float =
    (2 * cartoucheSize(width, height).halfHeight * 0.8f).coerceAtLeast(1f)

/** عرض وارتفاع لوحة عدد الآيات في الترويسة الكلاسيكية. */
internal fun surahCountPanelSize(width: Float, height: Float): Pair<Float, Float> {
    val s = height / 84f
    val cw = cartoucheSize(width, height).halfWidth
    val panelWidth = ((width - 2 * cw - 48 * s) / 2f).coerceAtLeast(0f)
    return panelWidth to (height - 34 * s).coerceAtLeast(1f)
}

/**
 * ترويسة السورة الكلاسيكية — نسخة كاملة من SuraHeaderSpan.drawClassic:
 * شريط ومعيّنات، حقل داخلي، كارتوث مسحوب بمنحنيات، ولوحة عدد الآيات.
 */
internal fun DrawScope.drawClassicSurahHeader(
    banner: DrawnBanner,
    colors: MushafColors,
    frameColors: MushafFrameColors = MushafFrameColors.of(SurahHeaderStyle.CLASSIC, colors),
) {
    val w = banner.width
    val h = banner.boxHeight
    val top = banner.boxTop
    val s = h / 84f
    val cy = top + h / 2f

    // ١) الشريط الخارجي + معيّنات
    drawRoundRect(
        color = frameColors.frameBand,
        topLeft = Offset(s, top + s),
        size = Size(w - 2 * s, h - 2 * s),
        cornerRadius = CornerRadius(5 * s),
    )
    var dx = 12 * s
    while (dx < w - 8 * s) {
        diamond(dx, top + 4.5f * s, 1.6f * s, frameColors.frameLine.copy(alpha = 110f / 255f))
        diamond(dx, top + h - 4.5f * s, 1.6f * s, frameColors.frameLine.copy(alpha = 110f / 255f))
        dx += 10 * s
    }
    drawRoundRect(
        color = frameColors.frameLine,
        topLeft = Offset(s, top + s),
        size = Size(w - 2 * s, h - 2 * s),
        cornerRadius = CornerRadius(5 * s),
        style = Stroke(1.6f * s),
    )

    // ٢) الحقل الداخلي — بلون الورق عشان يشتغل في الوضع الليلي كمان
    drawRoundRect(
        color = colors.paper,
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

    // ٣) الكارتوث
    val cartouche = cartoucheSize(w, h)
    val cw = cartouche.halfWidth
    val ch = cartouche.halfHeight
    val cx = w / 2f

    fun cartouchePath(halfW: Float, halfH: Float) = Path().apply {
        moveTo(cx - halfW, cy)
        cubicTo(
            cx - halfW + 10 * s, cy - halfH * 0.55f,
            cx - halfW + 14 * s, cy - halfH,
            cx - halfW + 34 * s, cy - halfH,
        )
        lineTo(cx + halfW - 34 * s, cy - halfH)
        cubicTo(
            cx + halfW - 14 * s, cy - halfH,
            cx + halfW - 10 * s, cy - halfH * 0.55f,
            cx + halfW, cy,
        )
        cubicTo(
            cx + halfW - 10 * s, cy + halfH * 0.55f,
            cx + halfW - 14 * s, cy + halfH,
            cx + halfW - 34 * s, cy + halfH,
        )
        lineTo(cx - halfW + 34 * s, cy + halfH)
        cubicTo(
            cx - halfW + 14 * s, cy + halfH,
            cx - halfW + 10 * s, cy + halfH * 0.55f,
            cx - halfW, cy,
        )
        close()
    }

    val shadow = cartouchePath(cw, ch)
    drawPath(shadow, frameColors.headerFill)
    drawPath(shadow, frameColors.headerBorder, style = Stroke(2.2f * s))
    drawPath(
        cartouchePath(cw * 0.92f, ch * 0.78f),
        frameColors.headerBorder.copy(alpha = 0.55f),
        style = Stroke(0.9f * s),
    )
    drawCircle(frameColors.headerBorder, 3 * s, Offset(cx - cw, cy))
    drawCircle(frameColors.headerBorder, 3 * s, Offset(cx + cw, cy))

    // ٤) الاسم لوحده جوّه الكارتوث
    drawText(
        banner.layout,
        topLeft = Offset(
            cx - banner.layout.size.width / 2f,
            cy - banner.layout.size.height / 2f,
        ),
    )

    // ٥) لوحتان على الجانبين (زي القارئ الكلاسيكي):
    // شمال = العدد بكلمته (مثل «٧ آيات»)، يمين = مكية/مدنية + رقم السورة.
    val panelHalf = (w - 2 * cw - 48 * s) / 2f
    val panelTop = top + 17 * s
    val panelBottom = top + h - 17 * s
    if (panelHalf > 18 * s) {
        fun panel(left: Float, caption: TextLayoutResult?, value: TextLayoutResult?) {
            drawRoundRect(
                color = frameColors.frameBand,
                topLeft = Offset(left, panelTop),
                size = Size(panelHalf, panelBottom - panelTop),
                cornerRadius = CornerRadius(6 * s),
            )
            drawRoundRect(
                color = frameColors.frameLine,
                topLeft = Offset(left, panelTop),
                size = Size(panelHalf, panelBottom - panelTop),
                cornerRadius = CornerRadius(6 * s),
                style = Stroke(1.2f * s),
            )
            diamond(left + panelHalf / 2, panelTop, 4 * s, frameColors.frameLine)
            diamond(left + panelHalf / 2, panelBottom, 4 * s, frameColors.frameLine)
            val midX = left + panelHalf / 2
            val gap = 3 * s
            val capH = caption?.size?.height?.toFloat() ?: 0f
            val valH = value?.size?.height?.toFloat() ?: 0f
            val block = capH + (if (caption != null && value != null) gap else 0f) + valH
            var y = (panelTop + panelBottom) / 2f - block / 2f
            caption?.let {
                drawText(it, topLeft = Offset(midX - it.size.width / 2f, y))
                y += it.size.height + gap
            }
            value?.let {
                drawText(it, topLeft = Offset(midX - it.size.width / 2f, y))
            }
        }

        val leftPanel = 14 * s
        val rightPanel = w - 14 * s - panelHalf
        val rule = frameColors.frameLine.copy(alpha = 0.45f)
        drawLine(rule, Offset(leftPanel + panelHalf, cy), Offset(cx - cw, cy), 1f * s)
        drawLine(rule, Offset(cx + cw, cy), Offset(rightPanel, cy), 1f * s)

        panel(leftPanel, banner.countCaptionLayout, banner.countLayout)
        panel(rightPanel, banner.revelationLayout, banner.orderLayout)
    }
}

/** الترويسة الحديثة: حبة بعرض عمود النص. */
internal fun DrawScope.drawModernSurahHeader(
    banner: DrawnBanner,
    colors: MushafColors,
    frameColors: MushafFrameColors = MushafFrameColors.of(SurahHeaderStyle.MODERN, colors),
) {
    val w = banner.width
    val h = banner.boxHeight
    val top = banner.boxTop
    val radius = CornerRadius(h / 2f)
    drawRoundRect(
        color = frameColors.headerFill,
        topLeft = Offset(0f, top),
        size = Size(w, h),
        cornerRadius = radius,
    )
    drawText(banner.layout, topLeft = Offset(banner.textLeft, banner.textTop))

    // Side meta when there is room: revelation on the right, ayah count on the left.
    val side = w * 0.22f
    if (side > 40f) {
        fun stack(cx: Float, caption: TextLayoutResult?, value: TextLayoutResult?) {
            val gap = 2f
            val block = (caption?.size?.height ?: 0) + (if (caption != null && value != null) gap else 0f) +
                (value?.size?.height ?: 0)
            var y = top + (h - block) / 2f
            caption?.let {
                drawText(it, topLeft = Offset(cx - it.size.width / 2f, y))
                y += it.size.height + gap
            }
            value?.let {
                drawText(it, topLeft = Offset(cx - it.size.width / 2f, y))
            }
        }
        stack(w - side / 2f, banner.revelationLayout, banner.orderLayout)
        stack(side / 2f, banner.countCaptionLayout, banner.countLayout)
    }
}
