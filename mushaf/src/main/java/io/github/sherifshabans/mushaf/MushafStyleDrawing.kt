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

/*
 * الطُرُز المطبوعة الثلاثة — Cairo وDiyanet وIndoPak — كلها **جلود** على نفس
 * صفحات المدينة الستمئة وأربعة: نفس النص ونفس كسور السطور ومواضع النجوم.
 * الجمال هنا في الإطار والترويسة بس.
 *
 * كل إطار بيحسب مساحة النص بنفس القاعدة: محتوى جوّه الخط الداخلي، والرأس
 * بيتزحزح `RUNNING_HEAD_HEIGHT` لما تشتغل علامات الأقسام.
 */

/** فجوة القاعدة المزدوجة في إطار Cairo بين الخط الخارجي والداخلي (dp). */
internal const val CAIRO_RULE_GAP = 3f

/** سُمك القاعدة الواحدة في إطار Diyanet (dp) — المحتوى يبدأ من جوّاه. */
internal const val DIYANET_RULE_GAP = 2f

/** عرض الشريط المسطّح في إطار IndoPak (dp). */
internal const val INDOPAK_BAND = 8f

/** نصف قطر دائرة رقم الصفحة في الإطارات الثلاثة — تتسع لقرص القياس المشترك. */
internal const val PAGE_NUMBER_CIRCLE_RADIUS = 12f

/** تسمية الجزء بترميز شرقية (پارہ + أرقام هندية شرقية U+06F0) لطراز IndoPak. */
internal fun toEasternDigits(n: Int): String {
    val digits = "۰۱۲۳۴۵۶۷۸۹"
    return n.toString().map { if (it.isDigit()) digits[it - '0'] else it }.joinToString("")
}

internal fun indoPakJuzLabel(juz: Int): String = "پارہ " + toEasternDigits(juz)

/**
 * هندسة الإطارات المسطّحة: الخط عند الهوامش، والقاعدة [ruleGapDp] جوّاه،
 * والنص بعد [PageFrameRequest.textGap]. نفس القاعدة لكل الطُرُز الجديدة.
 */
internal fun styledFrameGeometry(
    request: PageFrameRequest,
    outerDp: Float,
    innerDp: Float,
    topDp: Float,
    bottomDp: Float,
    ruleGapDp: Float,
): PageFrameGeometry {
    val unit = request.density
    val rightPage = isRightPage(request.pageNumber)
    val frameLeft = (if (rightPage) innerDp else outerDp) * unit
    val frameTop = topDp * unit
    val frameRight = request.widthPx - (if (rightPage) outerDp else innerDp) * unit
    val frameBottom = request.heightPx - bottomDp * unit
    val innerLeft = frameLeft + ruleGapDp * unit
    val innerTop = frameTop + ruleGapDp * unit
    val innerRight = frameRight - ruleGapDp * unit
    val innerBottom = frameBottom - ruleGapDp * unit
    val textGap = request.textGap * unit
    val topInset = if (request.divisionMarks) RUNNING_HEAD_HEIGHT * unit else 0f
    val contentLeft = innerLeft + textGap
    val contentTop = innerTop + textGap + topInset
    return PageFrameGeometry(
        unit = unit,
        rightPage = rightPage,
        frameLeft = frameLeft,
        frameTop = frameTop,
        frameRight = frameRight,
        frameBottom = frameBottom,
        contentLeft = contentLeft,
        contentTop = contentTop,
        contentWidth = (innerRight - textGap - contentLeft).coerceAtLeast(1f),
        contentHeight = (innerBottom - textGap - contentTop).coerceAtLeast(1f),
        markAnchorX = if (rightPage) frameRight else frameLeft,
    )
}

/** x رقم الصفحة في Cairo: وسط القاعدة السفلية، مش الزاوية. */
internal fun cairoPageNumberX(geometry: PageFrameGeometry): Float =
    (geometry.frameLeft + geometry.frameRight) / 2f

/** x رقم الصفحة في Diyanet: وسط القاعدة السفلية. */
internal fun diyanetPageNumberX(geometry: PageFrameGeometry): Float =
    (geometry.frameLeft + geometry.frameRight) / 2f

/** رقم الصفحة في دائرة صغيرة على وسط القاعدة السفلية. */
private fun DrawScope.drawCenteredPageNumber(
    geometry: PageFrameGeometry,
    context: PageFrameContext,
    frameColors: MushafFrameColors,
    centerX: Float,
) {
    val label = context.pageNumberLabel ?: return
    val r = PAGE_NUMBER_CIRCLE_RADIUS * geometry.unit
    val cy = geometry.frameBottom
    drawCircle(frameColors.frameBand, r, Offset(centerX, cy))
    drawCircle(frameColors.frameLine, r, Offset(centerX, cy), style = Stroke(1.1f * geometry.unit))
    drawText(
        label.layout,
        topLeft = Offset(centerX - label.inkCenterX, cy - label.inkCenterY),
    )
}

/** إطار Cairo: قاعدتان بنيتان هادئتان، وماساة وميداليات الإطار مرسومة بمسارات. */
internal object CairoFrame : PageFrame {

    override fun geometry(request: PageFrameRequest): PageFrameGeometry = styledFrameGeometry(
        request = request,
        outerDp = 16f,
        innerDp = 6f,
        topDp = 8f,
        bottomDp = 20f,
        ruleGapDp = CAIRO_RULE_GAP,
    )

    override fun DrawScope.draw(geometry: PageFrameGeometry, context: PageFrameContext) {
        val colors = MushafFrameColors.cairo(context.colors)
        val unit = geometry.unit
        drawRect(
            color = colors.frameLine,
            topLeft = Offset(geometry.frameLeft, geometry.frameTop),
            size = Size(geometry.frameRight - geometry.frameLeft, geometry.frameBottom - geometry.frameTop),
            style = Stroke(1.2f * unit),
        )
        val gap = CAIRO_RULE_GAP * unit
        drawRect(
            color = colors.frameLine.copy(alpha = 0.8f),
            topLeft = Offset(geometry.frameLeft + gap, geometry.frameTop + gap),
            size = Size(
                geometry.frameRight - geometry.frameLeft - 2 * gap,
                geometry.frameBottom - geometry.frameTop - 2 * gap,
            ),
            style = Stroke(0.6f * unit),
        )
        drawCenteredPageNumber(geometry, context, colors, cairoPageNumberX(geometry))
        drawDivisionMarks(geometry, context, frameColors = colors, plainCircle = true)
        drawRunningHead(geometry, context)
    }
}

/** إطار Diyanet: قاعدة خضراء واحدة وزوايا بزهرات صغيرة مرسومة بالمسار. */
internal object DiyanetFrame : PageFrame {

    override fun geometry(request: PageFrameRequest): PageFrameGeometry = styledFrameGeometry(
        request = request,
        outerDp = 12f,
        innerDp = 6f,
        topDp = 8f,
        bottomDp = 18f,
        ruleGapDp = DIYANET_RULE_GAP,
    )

    override fun DrawScope.draw(geometry: PageFrameGeometry, context: PageFrameContext) {
        val colors = MushafFrameColors.diyanet(context.colors)
        val unit = geometry.unit
        drawRoundRect(
            color = colors.frameLine,
            topLeft = Offset(geometry.frameLeft, geometry.frameTop),
            size = Size(geometry.frameRight - geometry.frameLeft, geometry.frameBottom - geometry.frameTop),
            cornerRadius = CornerRadius(2f * unit),
            style = Stroke(1.5f * unit),
        )
        // زهرة الزاوية: دائرة وورقتان، في كل زاوية ناحية جوّه الإطار.
        floret(geometry.frameLeft, geometry.frameTop, 1f, 1f, unit, colors.frameLine)
        floret(geometry.frameRight, geometry.frameTop, -1f, 1f, unit, colors.frameLine)
        floret(geometry.frameLeft, geometry.frameBottom, 1f, -1f, unit, colors.frameLine)
        floret(geometry.frameRight, geometry.frameBottom, -1f, -1f, unit, colors.frameLine)

        drawCenteredPageNumber(geometry, context, colors, diyanetPageNumberX(geometry))
        drawDivisionMarks(geometry, context, frameColors = colors)
        drawRunningHead(geometry, context)
    }

    /** زهرة زاوية: دائرة صغيرة وورقتان بالاتجاهين المطلوبين. */
    private fun DrawScope.floret(
        cx: Float,
        cy: Float,
        dirX: Float,
        dirY: Float,
        unit: Float,
        color: Color,
    ) {
        drawCircle(color, 2.2f * unit, Offset(cx, cy))
        fun leaf(dx: Float, dy: Float) {
            val len = 6f * unit
            val wid = 2.2f * unit
            val ex = cx + dx * len
            val ey = cy + dy * len
            val path = Path().apply {
                moveTo(cx, cy)
                quadraticBezierTo(
                    cx + dx * len / 2f - dy * wid, cy + dy * len / 2f + dx * wid,
                    ex, ey,
                )
                quadraticBezierTo(
                    cx + dx * len / 2f + dy * wid, cy + dy * len / 2f - dx * wid,
                    cx, cy,
                )
                close()
            }
            drawPath(path, color)
        }
        leaf(dirX, 0f)
        leaf(0f, dirY)
    }
}

/**
 * إطار IndoPak: شريط مسطّح أحمر بدل حبل الخرز، وخط شعرة تحت كل سطر آيات.
 * الأرقام الشرقية (U+06F0) في تسمية الجزء ورقم السورة — **لا** تُمرّ للخط
 * القرآني أبدًا (بيلمّها في وردة).
 */
internal object IndoPakFrame : PageFrame {

    override fun geometry(request: PageFrameRequest): PageFrameGeometry = styledFrameGeometry(
        request = request,
        outerDp = 6f,
        innerDp = 4f,
        topDp = 4f,
        bottomDp = 12f,
        ruleGapDp = INDOPAK_BAND,
    )

    override fun DrawScope.draw(geometry: PageFrameGeometry, context: PageFrameContext) {
        val colors = MushafFrameColors.indoPak(context.colors)
        val unit = geometry.unit
        // الشريط المسطّح: إطار مصمت ٨dp بلا زخرفة.
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRoundRect(
                RoundRect(
                    geometry.frameLeft, geometry.frameTop, geometry.frameRight, geometry.frameBottom,
                    CornerRadius(2f * unit),
                )
            )
            val gap = INDOPAK_BAND * unit
            addRect(
                Rect(
                    geometry.frameLeft + gap, geometry.frameTop + gap,
                    geometry.frameRight - gap, geometry.frameBottom - gap,
                )
            )
        }
        drawPath(path, colors.frameLine)

        // خط شعرة تحت كل سطر آيات، جوّه عمود النص.
        val underline = colors.frameLine.copy(alpha = 0.30f)
        context.lineSpans.forEach { span ->
            drawLine(
                color = underline,
                start = Offset(
                    maxOf(span.left, geometry.contentLeft),
                    span.bottom - 1.5f * unit,
                ),
                end = Offset(
                    minOf(span.right, geometry.contentLeft + geometry.contentWidth),
                    span.bottom - 1.5f * unit,
                ),
                strokeWidth = 1f * unit,
            )
        }

        drawCenteredPageNumber(geometry, context, colors, cairoPageNumberX(geometry))
        drawDivisionMarks(geometry, context, frameColors = colors)
        drawRunningHead(geometry, context)
    }
}

/*
 * ترويسات السور للطُرُز الجديدة — كلها أشرطة تُرسم على الكانفس بنفس قياسات
 * `DrawnBanner` اللي التخطيط حسبها.
 */

/** ترويسة Cairo: مستطيل أحمر غامق، الاسم جوّه، والعدد والنزول في الطرفين. */
internal fun DrawScope.drawCairoSurahHeader(banner: DrawnBanner, colors: MushafColors) {
    val frameColors = MushafFrameColors.cairo(colors)
    val w = banner.width
    val h = banner.boxHeight
    val top = banner.boxTop
    val s = h / 84f
    val cy = top + h / 2f

    drawRoundRect(
        color = frameColors.headerFill,
        topLeft = Offset(0f, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(6f * s),
    )
    drawRoundRect(
        color = frameColors.headerBorder,
        topLeft = Offset(0f, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(6f * s),
        style = Stroke(1.2f * s),
    )

    drawText(banner.layout, topLeft = Offset(banner.textLeft, banner.textTop))

    banner.countLayout?.let {
        drawText(it, topLeft = Offset(14f * s, cy - it.size.height / 2f))
    }
    banner.revelationLayout?.let {
        drawText(it, topLeft = Offset(w - 14f * s - it.size.width, cy - it.size.height / 2f))
    }
}

/**
 * ترويسة Diyanet: كارتوث بيضاوي (مستطيل بزوايا نصف الارتفاع) أخضر، والاسم
 * كريمي، ولوحتا العدد والنزيل على الجانبين زي الكلاسيك.
 */
internal fun DrawScope.drawDiyanetSurahHeader(banner: DrawnBanner, colors: MushafColors) {
    val frameColors = MushafFrameColors.diyanet(colors)
    val w = banner.width
    val h = banner.boxHeight
    val top = banner.boxTop
    val s = h / 84f
    val cy = top + h / 2f

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
        style = Stroke(1.4f * s),
    )

    // الكارتوث البيضاوي: نصف عرضه ونصف ارتفاعه زي الكلاسيك بالظبط.
    val halfW = surahNameMaxWidth(w, h) / 2f + 18f * s
    val halfH = surahNameMaxHeight(w, h) / 2f + 8f * s
    val ovalLeft = w / 2f - halfW
    drawRoundRect(
        color = frameColors.headerFill,
        topLeft = Offset(ovalLeft, cy - halfH),
        size = Size(2 * halfW, 2 * halfH),
        cornerRadius = CornerRadius(halfH),
    )
    drawText(
        banner.layout,
        topLeft = Offset(w / 2f - banner.layout.size.width / 2f, cy - banner.layout.size.height / 2f),
    )

    // لوحتا الجانبين: العدد شمالًا والنزيل يمينًا — على شريط فاتح أخضر.
    val panelHalf = (w - 2 * halfW - 40f * s) / 2f
    if (panelHalf > 18f * s) {
        val panelTop = top + 14f * s
        val panelBottom = top + h - 14f * s
        fun panel(left: Float, value: TextLayoutResult?) {
            drawRoundRect(
                color = frameColors.frameBand,
                topLeft = Offset(left, panelTop),
                size = Size(panelHalf, panelBottom - panelTop),
                cornerRadius = CornerRadius(5f * s),
            )
            drawRoundRect(
                color = frameColors.frameLine,
                topLeft = Offset(left, panelTop),
                size = Size(panelHalf, panelBottom - panelTop),
                cornerRadius = CornerRadius(5f * s),
                style = Stroke(1.1f * s),
            )
            value?.let {
                drawText(it, topLeft = Offset(left + (panelHalf - it.size.width) / 2f, cy - it.size.height / 2f))
            }
        }
        panel(12f * s, banner.countLayout)
        panel(w - 12f * s - panelHalf, banner.revelationLayout)
    }
}

/**
 * ترويسة IndoPak: مستطيل أحمر بسيط، دائرة صغيرة فيها رقم السورة بأرقام
 * شرقية، والاسم بجانبها، والعدد والنزول تحت الاسم لو الارتفاع يسمح.
 */
internal fun DrawScope.drawIndoPakSurahHeader(banner: DrawnBanner, colors: MushafColors) {
    val frameColors = MushafFrameColors.indoPak(colors)
    val w = banner.width
    val h = banner.boxHeight
    val top = banner.boxTop
    val s = h / 84f
    val cy = top + h / 2f

    drawRoundRect(
        color = frameColors.headerFill,
        topLeft = Offset(0f, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(4f * s),
    )

    // دائرة رقم السورة على الطرف الأيمن (أول السطر في RTL).
    val numberRadius = 16f * s
    val circleCx = w - 16f * s - numberRadius
    drawCircle(frameColors.headerText, numberRadius, Offset(circleCx, cy))
    banner.surahNumberLayout?.let {
        drawText(it, topLeft = Offset(circleCx - it.size.width / 2f, cy - it.size.height / 2f))
    }

    // الاسم بجانب الدائرة.
    val nameRight = circleCx - numberRadius - 10f * s
    drawText(
        banner.layout,
        topLeft = Offset(nameRight - banner.layout.size.width, cy - banner.layout.size.height / 2f),
    )

    // العدد والنزول تحت الاسم لو يسمح الارتفاع — وإلا يتخلّفوا.
    val meta = listOfNotNull(banner.revelationLayout, banner.countLayout)
    if (meta.isNotEmpty()) {
        val metaHeight = meta.sumOf { it.size.height }.toFloat() + (meta.size - 1) * 2f * s
        val nameBottom = cy - banner.layout.size.height / 2f + banner.layout.size.height + 2f * s
        if (nameBottom + metaHeight < top + h - 4f * s) {
            var y = nameBottom
            meta.forEach {
                drawText(it, topLeft = Offset(nameRight - it.size.width, y))
                y += it.size.height + 2f * s
            }
        }
    }
}
