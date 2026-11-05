package io.github.sherifshabans.mushaf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * راندرر صفحة المصحف — بيرسم الصفحة زي المطبوع: عدد سطور ثابت، كل سطر ممدود
 * لحافتي الصفحة، وكسر السطور مطابق لمصحف المدينة.
 *
 * ليه رسم يدوي مش `Text`: كل سطر فقرة مستقلة، و`TextAlign.Justify` بيرفض يمدّ
 * آخر سطر في أي فقرة — يعني ولا سطر كان هيتمدّ. فبنقيس كل كلمة ونوزّع الفراغ
 * بنفسنا. وده كمان بيدّينا مستطيل كل كلمة للّمس والتظليل.
 *
 * وليه سطح رسم واحد للصفحة: علامات الضبط بتطلع فوق صندوق السطر، وأي تقسيم
 * للصفحة لخانة لكل سطر بيقصّها عند حدود الخانات.
 */

/** Default upper bound for the page's font size, in sp (the page usually fits by width first). */
const val DEFAULT_MAX_FONT_SIZE = 34f

/** عرض النص بيتناسب خطيًا مع حجم الخط، فبنقيس مرة عند الحجم ده ونضرب. */
private const val REFERENCE_FONT_SIZE = 100f
private const val MIN_FONT_SIZE = 9f

/** المصحف بيرصّ الكلمات أقرب من مسافة الخط الكاملة. */
private const val WORD_SPACE_FACTOR = 0.62f

/** تباعد السطور في صفحتي الفاتحة وأول البقرة (ثمانية سطور متوسّطة). */
private const val CENTERED_LINE_SPACING = 1.9f

private const val BANNER_INSET = 0.08f
private const val BANNER_HEIGHT = 0.84f

/** نسبة ارتفاع الصفحة لعرضها لو الأب مابيحدّدش ارتفاع (جوّه scroll مثلًا). */
private const val FALLBACK_ASPECT = 1.62f

/**
 * A tap on a tajweed-coloured letter.
 *
 * @property word the whole word that was tapped.
 * @property start/end the coloured letter's range inside [word].
 */
data class TajweedHit(
    val rule: TajweedRule,
    val ayah: Ayah,
    val word: String,
    val start: Int,
    val end: Int
)

/**
 * One Madinah Mushaf page (1‥604), laid out line-for-line like the printed copy.
 *
 * The text is loaded off the main thread on first use; the page draws nothing
 * until it's ready (a few tens of ms, once per process).
 *
 * @param tajweed colour every tajweed rule. Colouring never moves a single glyph:
 *   colour-only spans are not metric-affecting, so the layout is identical.
 * @param naturalMadd also colour natural madd (2 counts). Off by default — it is
 *   the most frequent rule by far and turns the page green.
 * @param selectedAyahIds ayat to highlight with [MushafColors.selection].
 * @param highlightedAyahs per-ayah highlight colours (bookmarks, notes…). Drawn at 30% alpha.
 * @param playingAyahIds the ayah being recited; wins over every other highlight.
 * @param onTajweedClick when set, a tap on a coloured letter reports the rule
 *   instead of calling [onAyahClick].
 * @param focusRule outlines one occurrence of this rule on the page;
 *   [focusIndex] picks which one (wraps around).
 * @param maxFontSize upper bound in sp. The page picks the largest size at which
 *   its widest line fits, so on phones the width decides, not this.
 */
@Composable
fun MushafPage(
    page: Int,
    modifier: Modifier = Modifier,
    tajweed: Boolean = false,
    naturalMadd: Boolean = false,
    colors: MushafColors = MushafColors.Light,
    decoration: MushafDecoration = MushafDecoration.None,
    selectedAyahIds: Set<Int> = emptySet(),
    highlightedAyahs: Map<Int, Color> = emptyMap(),
    playingAyahIds: Set<Int> = emptySet(),
    onAyahClick: ((Ayah) -> Unit)? = null,
    onAyahLongClick: ((Ayah) -> Unit)? = null,
    onTajweedClick: ((TajweedHit) -> Unit)? = null,
    focusRule: TajweedRule? = null,
    focusIndex: Int = 0,
    maxFontSize: Float = DEFAULT_MAX_FONT_SIZE,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
) {
    val context = LocalContext.current
    val ayahs by produceState<List<Ayah>?>(null, page) {
        value = Quran.load(context).filter { it.page == page }
    }
    MushafPage(
        ayahs = ayahs ?: emptyList(),
        modifier = modifier,
        tajweed = tajweed,
        naturalMadd = naturalMadd,
        colors = colors,
        decoration = decoration,
        selectedAyahIds = selectedAyahIds,
        highlightedAyahs = highlightedAyahs,
        playingAyahIds = playingAyahIds,
        onAyahClick = onAyahClick,
        onAyahLongClick = onAyahLongClick,
        onTajweedClick = onTajweedClick,
        focusRule = focusRule,
        focusIndex = focusIndex,
        maxFontSize = maxFontSize,
        contentPadding = contentPadding
    )
}

/**
 * Same as the `page` overload, for callers that already hold the page's ayat
 * (from [Quran.page]). All [ayahs] must be on the same page.
 */
@Composable
fun MushafPage(
    ayahs: List<Ayah>,
    modifier: Modifier = Modifier,
    tajweed: Boolean = false,
    naturalMadd: Boolean = false,
    colors: MushafColors = MushafColors.Light,
    decoration: MushafDecoration = MushafDecoration.None,
    selectedAyahIds: Set<Int> = emptySet(),
    highlightedAyahs: Map<Int, Color> = emptyMap(),
    playingAyahIds: Set<Int> = emptySet(),
    onAyahClick: ((Ayah) -> Unit)? = null,
    onAyahLongClick: ((Ayah) -> Unit)? = null,
    onTajweedClick: ((TajweedHit) -> Unit)? = null,
    focusRule: TajweedRule? = null,
    focusIndex: Int = 0,
    maxFontSize: Float = DEFAULT_MAX_FONT_SIZE,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
) {
    // مقياس خط النظام متثبّت على ١ جوّه الصفحة: الصفحة نسخة من المطبوع
    // (١٥ سطرًا وكسر محدّد) وحجمها محسوب من أبعادها. الحجم بيتحط بالـsp،
    // فبدون التثبيت السقف `maxFontSize` بيتضرب في مقياس الجهاز والصفحة تخرج من
    // حافتيها — ومن أندرويد ١٤ التحويل sp→px مش خطّي أصلًا فالحسبة تبوظ.
    val platformDensity = LocalDensity.current
    // لو فيه إطار، نزود الـ padding عشان النص يفضل جوه الإطار (زي contentPadding في الإطار القديم).
    val framePad = if (decoration.pageFrame) PaddingValues(
        start = 28.dp, end = 28.dp, top = 18.dp, bottom = 32.dp
    ) else PaddingValues(0.dp)
    CompositionLocalProvider(
        LocalDensity provides Density(platformDensity.density, fontScale = 1f)
    ) {
        MushafPageBody(
            ayahs = ayahs,
            modifier = modifier.padding(contentPadding).padding(framePad),
            tajweed = tajweed,
            naturalMadd = naturalMadd,
            colors = colors,
            decoration = decoration,
            selectedAyahIds = selectedAyahIds,
            highlightedAyahs = highlightedAyahs,
            playingAyahIds = playingAyahIds,
            onAyahClick = onAyahClick,
            onAyahLongClick = onAyahLongClick,
            onTajweedClick = onTajweedClick,
            focusRule = focusRule,
            focusIndex = focusIndex,
            maxFontSize = maxFontSize
        )
    }
}

private fun mushafTextStyle(fontSize: Float, color: Color) = TextStyle(
    fontFamily = MushafFont,
    fontSize = fontSize.sp,
    color = color
)

/**
 * علامة نهاية الآية بتتقاس بفقرة LTR إجباري: أرقامها تصنيفها AN، وفي سياق عربي
 * بتوصل للمشكِّل بترتيب مقلوب فرابطة الخط بتتكوّن غلط.
 */
private fun TextMeasurer.measureToken(
    token: MushafToken,
    style: TextStyle,
    spans: List<TajweedSpan> = emptyList(),
    tajweedColor: (TajweedRule) -> Color = { it.color }
): TextLayoutResult = measure(
    text = colouredText(token.text, spans, tajweedColor),
    style = if (token.isEndMark) style.copy(textDirection = TextDirection.Ltr) else style
)

/**
 * هل لازم نعكس أرقام الآية عشان رابطة الوردة تتكوّن صح؟
 *
 * الخط بيلمّ أرقام الآية في حرف واحد (الوردة والرقم جوّاها)، وأندرويد بيوصّل رَنّ
 * الأرقام للمشكِّل مقلوبًا — «٢٥٣» بتطلع وردتين. الترتيب الصح بيدّي حرفًا واحدًا
 * بنصّ العرض تقريبًا، فبنقيس الاتجاهين ونختار الأضيق. كده بيتصحّح لوحده لو
 * منصّة وقّفت العكس.
 */
internal fun markerDigitsReversed(
    measurer: TextMeasurer,
    style: TextStyle,
    direction: TextDirection = TextDirection.Ltr
): Boolean {
    val probe = toArabicDigits(253)
    fun widthOf(s: String) =
        measurer.measure(AnnotatedString(s), style.copy(textDirection = direction)).size.width
    return widthOf(probe.reversed()) < widthOf(probe)
}

internal fun ayahMarkerText(number: Int, reversed: Boolean): String =
    toArabicDigits(number).let { if (reversed) it.reversed() else it }

/**
 * النص بألوان أحكامه. `SpanStyle` بلون بس بيتحوّل لـ`ForegroundColorSpan` — مش
 * metric-affecting — فعرض الكلمة مابيتغيّرش والتخطيط كله بيفضل مطابق.
 * الحدّ الوحيد: الرابطة (زي «لا») بتاخد لونًا واحدًا — قيد في OpenType نفسه.
 */
internal fun colouredText(
    text: String,
    spans: List<TajweedSpan>,
    tajweedColor: (TajweedRule) -> Color
): AnnotatedString {
    if (spans.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        spans.forEach { s -> addStyle(SpanStyle(color = tajweedColor(s.rule)), s.start, s.end) }
    }
}

private enum class LineFill { Justify, Center }

private class PlacedToken(
    val token: MushafToken,
    val layout: TextLayoutResult,
    val left: Float,
    val width: Float,
    /** أحكام التجويد بمواضع **جوّه الكلمة**. */
    val tajweed: List<TajweedSpan>
)

private class DrawnVerseLine(
    val bandTop: Float,
    val bandBottom: Float,
    val textTop: Float,
    val tokens: List<PlacedToken>
)

private class DrawnBanner(
    val boxTop: Float,
    val boxHeight: Float,
    val layout: TextLayoutResult,
    val textLeft: Float,
    val textTop: Float,
    val surahNumber: Int,
    val ayahCount: Int,
)

private class DrawnText(val layout: TextLayoutResult, val left: Float, val top: Float)

private class PageDrawing(
    val verses: List<DrawnVerseLine>,
    val banners: List<DrawnBanner>,
    val basmalas: List<DrawnText>
)

private class PageLayout(
    val fontSize: Float,
    val lines: Map<Int, List<MushafToken>>,
    val reservedLines: List<Int>,
    val totalLines: Int
)

private sealed interface LineSlot {
    val lineNumber: Int

    class Banner(override val lineNumber: Int, val surah: Int) : LineSlot
    class Basmala(override val lineNumber: Int) : LineSlot
    class Blank(override val lineNumber: Int) : LineSlot
    class Verses(override val lineNumber: Int, val tokens: List<MushafToken>) : LineSlot
}

/** أحكام الآية مترجمة لمواضع جوّه الكلمة، ومقصوصة على حدودها. */
private fun tokenSpans(token: MushafToken, ayahSpans: List<TajweedSpan>?): List<TajweedSpan> {
    if (ayahSpans.isNullOrEmpty() || token.isEndMark || token.charStart < 0) return emptyList()
    val from = token.charStart
    val to = from + token.text.length
    val out = ArrayList<TajweedSpan>(4)
    ayahSpans.forEach { s ->
        val a = max(s.start, from)
        val b = min(s.end, to)
        if (a < b) out += TajweedSpan(a - from, b - from, s.rule)
    }
    return out
}

/** التوزيع المرجعي: التقسيم جاهز، فالمطلوب بس أكبر حجم بيدخّل أوسع سطر. */
private fun referencePageLayout(
    tokens: List<MushafToken>,
    reference: MushafPageLines,
    widthPx: Float,
    heightPx: Float,
    maxFontSize: Float,
    measurer: TextMeasurer
): PageLayout? {
    val lines = reference.sliceTokens(tokens) ?: return null
    val refStyle = mushafTextStyle(REFERENCE_FONT_SIZE, Color.Black)
    val widthCache = HashMap<String, Float>()
    fun refWidth(t: MushafToken): Float = widthCache.getOrPut(t.text) {
        measurer.measureToken(t, refStyle).size.width.toFloat()
    }
    val spaceRef = measurer.measure(AnnotatedString(" "), refStyle)
        .size.width.toFloat() * WORD_SPACE_FACTOR
    val lineBoxRef = measurer.measure(AnnotatedString("ا"), refStyle).size.height.toFloat()

    val widest = lines.values.maxOfOrNull { line ->
        line.sumOf { refWidth(it).toDouble() }.toFloat() + spaceRef * (line.size - 1)
    } ?: return null
    if (widest <= 0f) return null

    val fontFromWidth = widthPx * REFERENCE_FONT_SIZE / widest
    val fontFromHeight = (heightPx / reference.lineCount) * REFERENCE_FONT_SIZE / lineBoxRef
    val fontSize = min(min(fontFromWidth, fontFromHeight), maxFontSize)
    return PageLayout(max(fontSize, MIN_FONT_SIZE), lines, reference.reservedLines, reference.lineCount)
}

/** المسار الاحتياطي: بحث ثنائي على أصغر عرض سطر لسه ليه توزيع صالح. */
private fun computedPageLayout(
    tokens: List<MushafToken>,
    usedLines: List<Int>,
    widthPx: Float,
    heightPx: Float,
    maxFontSize: Float,
    measurer: TextMeasurer
): PageLayout {
    val refStyle = mushafTextStyle(REFERENCE_FONT_SIZE, Color.Black)
    val widthCache = HashMap<String, Float>()
    fun refWidth(t: MushafToken): Float = widthCache.getOrPut(t.text) {
        measurer.measureToken(t, refStyle).size.width.toFloat()
    }
    val spaceRef = measurer.measure(AnnotatedString(" "), refStyle)
        .size.width.toFloat() * WORD_SPACE_FACTOR
    val lineBoxRef = measurer.measure(AnnotatedString("ا"), refStyle).size.height.toFloat()

    val totalLines = usedLines.maxOrNull() ?: 1
    val reserved = (1..totalLines).filter { it !in usedLines }

    var low = 0f
    var high = tokens.sumOf { refWidth(it).toDouble() }.toFloat() + spaceRef * tokens.size
    var respectAyahLines = true
    var feasible = breakIntoLines(tokens, usedLines, high, spaceRef, ::refWidth)
    if (feasible == null) {
        respectAyahLines = false
        feasible = breakIntoLines(tokens, usedLines, high, spaceRef, ::refWidth, false)
    }
    if (feasible == null) feasible = mapOf(usedLines.first() to tokens)
    repeat(22) {
        val mid = (low + high) / 2f
        val attempt = breakIntoLines(tokens, usedLines, mid, spaceRef, ::refWidth, respectAyahLines)
        if (attempt != null) { high = mid; feasible = attempt } else low = mid
    }

    val fontFromWidth = widthPx * REFERENCE_FONT_SIZE / high
    val fontFromHeight = (heightPx / totalLines) * REFERENCE_FONT_SIZE / lineBoxRef
    val fontSize = min(min(fontFromWidth, fontFromHeight), maxFontSize)
    val finalRefWidth = widthPx * REFERENCE_FONT_SIZE / fontSize
    val finalLines =
        breakIntoLines(tokens, usedLines, finalRefWidth, spaceRef, ::refWidth, respectAyahLines)
            ?: feasible!!
    return PageLayout(max(fontSize, MIN_FONT_SIZE), finalLines, reserved, totalLines)
}

@Composable
private fun MushafPageBody(
    ayahs: List<Ayah>,
    modifier: Modifier,
    tajweed: Boolean,
    naturalMadd: Boolean,
    colors: MushafColors,
    decoration: MushafDecoration,
    selectedAyahIds: Set<Int>,
    highlightedAyahs: Map<Int, Color>,
    playingAyahIds: Set<Int>,
    onAyahClick: ((Ayah) -> Unit)?,
    onAyahLongClick: ((Ayah) -> Unit)?,
    onTajweedClick: ((TajweedHit) -> Unit)?,
    focusRule: TajweedRule?,
    focusIndex: Int,
    maxFontSize: Float
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val context = LocalContext.current

    val reverseDigits = remember(measurer) {
        markerDigitsReversed(measurer, mushafTextStyle(REFERENCE_FONT_SIZE, Color.Black))
    }
    val (tokens, usedLines) = remember(ayahs, reverseDigits) {
        buildPageTokens(ayahs) { ayahMarkerText(it, reverseDigits) }
    }
    val ayahById = remember(ayahs) { ayahs.associateBy { it.id } }
    val pageNumber = ayahs.firstOrNull()?.page
    val reference = remember(pageNumber) {
        pageNumber?.let { MushafLineIndex.forPage(context, it) }
    }
    // الأحكام بتتحسب مرة لكل صفحة ولكل آية لوحدها — علامة نهاية الآية وقف،
    // فمفيش حكم بيعبر من آية للّي بعدها.
    val tajweedByAyah = remember(ayahs, tajweed, naturalMadd) {
        if (!tajweed) emptyMap()
        else ayahs.associate {
            it.id to TajweedAnnotator.annotate(it.text, naturalMadd)
        }
    }

    val clickAyah by rememberUpdatedState(onAyahClick)
    val longClickAyah by rememberUpdatedState(onAyahLongClick)
    val clickTajweed by rememberUpdatedState(onTajweedClick)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx =
            if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat()
            else widthPx * FALLBACK_ASPECT
        if (ayahs.isEmpty() || widthPx <= 0f || heightPx <= 0f) return@BoxWithConstraints

        val drawing = remember(tokens, widthPx, heightPx, maxFontSize, reference, tajweedByAyah, colors) {
            val layout = reference
                ?.let { referencePageLayout(tokens, it, widthPx, heightPx, maxFontSize, measurer) }
                ?: computedPageLayout(tokens, usedLines, widthPx, heightPx, maxFontSize, measurer)
            buildPageDrawing(
                slots = buildLineSlots(layout, ayahs, reference),
                layout = layout,
                reference = reference,
                // الفاتحة وأول البقرة: ثمانية سطور متوسّطة بتباعد أوسع.
                centerLines = pageNumber != null && pageNumber <= 2,
                widthPx = widthPx,
                heightPx = heightPx,
                measurer = measurer,
                colors = colors,
                tajweedByAyah = tajweedByAyah
            )
        }

        val canvasHeight = with(density) { heightPx.toDp() }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .pointerInput(drawing) {
                    detectTapGestures(
                        onLongPress = { pos ->
                            val hit = hitTest(drawing, pos) ?: return@detectTapGestures
                            ayahById[hit.token.token.ayahId]?.let { longClickAyah?.invoke(it) }
                        },
                        onTap = { pos ->
                            val hit = hitTest(drawing, pos) ?: return@detectTapGestures
                            val ayah = ayahById[hit.token.token.ayahId] ?: return@detectTapGestures
                            val onRule = clickTajweed
                            val span = onRule?.let { spanUnder(hit, pos) }
                            if (onRule != null && span != null) {
                                onRule(
                                    TajweedHit(span.rule, ayah, hit.token.token.text, span.start, span.end)
                                )
                            } else clickAyah?.invoke(ayah)
                        }
                    )
                }
        ) {
            // ٠) إطار الصفحة إن وُجد — نسخة من الإطار الزخرفي الحالي في التطبيق
            // (حبل من الخرز على الجانب الخارجي + ماسات + خطين + ميدالية رقم الصفحة).
            if (decoration.pageFrame) {
                drawOrnatePageFrame(
                    colors = colors,
                    style = decoration.surahHeaderStyle,
                    pageNumber = pageNumber ?: 1,
                )
            }

            // ١) تظليل الآيات — محصور في شريحة سطره عشان مايزحفش على اللي فوق وتحت.
            drawing.verses.forEach { line ->
                var runStart: PlacedToken? = null
                var runEnd: PlacedToken? = null
                var runColor = Color.Unspecified
                fun flush() {
                    val a = runStart
                    val b = runEnd
                    if (a != null && b != null && runColor != Color.Unspecified) {
                        val left = min(a.left, b.left)
                        val right = max(a.left + a.width, b.left + b.width)
                        drawRect(
                            color = runColor,
                            topLeft = Offset(left, line.bandTop),
                            size = Size(right - left, line.bandBottom - line.bandTop)
                        )
                    }
                    runStart = null; runEnd = null; runColor = Color.Unspecified
                }
                line.tokens.forEach { p ->
                    val id = p.token.ayahId
                    val bg = when {
                        // التلاوة الجارية أولًا: القارئ بيتابع بعينه على السطر.
                        id in playingAyahIds -> colors.playing
                        id in highlightedAyahs -> highlightedAyahs.getValue(id).copy(alpha = 0.30f)
                        id in selectedAyahIds -> colors.selection
                        else -> Color.Unspecified
                    }
                    if (bg != runColor) { flush(); runColor = bg; runStart = p }
                    runEnd = p
                }
                flush()
            }

            // ١ب) موضع الحكم المطلوب — إطار حوالين **الكلمة** بلون الحكم.
            if (focusRule != null) {
                val spots = ArrayList<Pair<DrawnVerseLine, PlacedToken>>()
                drawing.verses.forEach { line ->
                    line.tokens.forEach { p ->
                        if (p.tajweed.any { it.rule == focusRule }) spots += line to p
                    }
                }
                if (spots.isNotEmpty()) {
                    val (line, p) = spots[((focusIndex % spots.size) + spots.size) % spots.size]
                    val ruleColor = colors.tajweedColor(focusRule)
                    val pad = 3.dp.toPx()
                    val box = CornerRadius(7.dp.toPx())
                    val topLeft = Offset(p.left - pad, line.bandTop)
                    val boxSize = Size(p.width + pad * 2, line.bandBottom - line.bandTop)
                    drawRoundRect(ruleColor.copy(alpha = 0.16f), topLeft, boxSize, box)
                    drawRoundRect(
                        ruleColor.copy(alpha = 0.70f), topLeft, boxSize, box,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // ٢) بانرات أسماء السور.
            drawing.banners.forEach { banner ->
                if (decoration.surahHeaderStyle != SurahHeaderStyle.PLAIN) {
                    drawSurahHeader(
                        banner = banner,
                        style = decoration.surahHeaderStyle,
                        colors = colors,
                    )
                } else {
                    drawText(banner.layout, topLeft = Offset(banner.textLeft, banner.textTop))
                }
            }

            // ٣) البسملة، ٤) الكلمات والوردة.
            drawing.basmalas.forEach { drawText(it.layout, topLeft = Offset(it.left, it.top)) }
            drawing.verses.forEach { line ->
                line.tokens.forEach { p -> drawText(p.layout, topLeft = Offset(p.left, line.textTop)) }
            }
        }
    }
}

private class TokenHit(val line: DrawnVerseLine, val token: PlacedToken)

private fun hitTest(drawing: PageDrawing, pos: Offset): TokenHit? {
    val line = drawing.verses.firstOrNull { pos.y >= it.bandTop && pos.y <= it.bandBottom }
        ?: return null
    line.tokens.firstOrNull { pos.x >= it.left && pos.x <= it.left + it.width }
        ?.let { return TokenHit(line, it) }
    // لمسة في فراغ بين كلمتين — أقرب كلمة على نفس السطر.
    return line.tokens.minByOrNull { abs(pos.x - (it.left + it.width / 2f)) }
        ?.let { TokenHit(line, it) }
}

/**
 * الحكم تحت اللمسة بالظبط. `getOffsetForPosition` هي نفس حسبة مؤشّر الكتابة،
 * وبنسأل بالمواضع لا بمستطيلات مرسومة لأن الرابطات بتقلّل عدد الحروف المرسومة.
 */
private fun spanUnder(hit: TokenHit, pos: Offset): TajweedSpan? {
    val placed = hit.token
    if (placed.tajweed.isEmpty()) return null
    val local = Offset(pos.x - placed.left, pos.y - hit.line.textTop)
    val offset = runCatching { placed.layout.getOffsetForPosition(local) }.getOrNull()
        ?: return null
    return placed.tajweed.firstOrNull { offset >= it.start && offset < it.end }
}

/**
 * خانة **لكل سطر** من ١ لـ`totalLines` حتى لو فاضي — الرسم بيوزّع بالفهرس،
 * فأي سطر ناقص بيزحزح كل اللي بعده.
 */
private fun buildLineSlots(
    layout: PageLayout,
    ayahs: List<Ayah>,
    reference: MushafPageLines?
): List<LineSlot> {
    val reserved = layout.reservedLines.toSet()
    val roles = HashMap<Int, LineSlot>()

    if (reference != null) {
        reference.reservedRoles.forEach { (line, role) ->
            roles[line] = when (role) {
                is ReservedRole.Banner -> LineSlot.Banner(line, role.surah)
                is ReservedRole.Basmala -> LineSlot.Basmala(line)
            }
        }
    } else {
        ayahs.filter { it.number == 1 }.forEach { first ->
            val block = generateSequence(first.lineStart - 1) { it - 1 }
                .takeWhile { it >= 1 && it in reserved && it !in roles }
                .toList()
                .reversed()
            if (block.isEmpty()) return@forEach
            roles[block[0]] = LineSlot.Banner(block[0], first.surah)
            // البسملة لكل السور ما عدا الفاتحة (آيتها الأولى) والتوبة.
            if (first.surah != 1 && first.surah != 9 && block.size > 1) {
                roles[block[1]] = LineSlot.Basmala(block[1])
            }
        }
    }

    return (1..layout.totalLines).map { line ->
        layout.lines[line]?.let { LineSlot.Verses(line, it) } ?: roles[line] ?: LineSlot.Blank(line)
    }
}

private fun buildPageDrawing(
    slots: List<LineSlot>,
    layout: PageLayout,
    reference: MushafPageLines?,
    centerLines: Boolean,
    widthPx: Float,
    heightPx: Float,
    measurer: TextMeasurer,
    colors: MushafColors,
    tajweedByAyah: Map<Int, List<TajweedSpan>>
): PageDrawing {
    val style = mushafTextStyle(layout.fontSize, colors.ink)
    val markerStyle = style.copy(color = colors.marker)
    val lineBox = measurer.measure(AnnotatedString("ا"), style).size.height.toFloat()

    val pitch: Float
    val offsetY: Float
    if (centerLines) {
        pitch = min(heightPx / layout.totalLines, lineBox * CENTERED_LINE_SPACING)
        offsetY = (heightPx - pitch * layout.totalLines) / 2f
    } else {
        pitch = heightPx / layout.totalLines
        offsetY = 0f
    }

    val verses = ArrayList<DrawnVerseLine>()
    val banners = ArrayList<DrawnBanner>()
    val basmalas = ArrayList<DrawnText>()

    slots.forEach { slot ->
        val bandTop = offsetY + pitch * (slot.lineNumber - 1)
        when (slot) {
            is LineSlot.Blank -> Unit

            is LineSlot.Banner -> {
                val text = measurer.measure(
                    AnnotatedString("سُورَةُ " + Quran.surah(slot.surah).nameArabic),
                    mushafTextStyle(layout.fontSize * 0.9f, colors.bannerText)
                )
                val boxTop = bandTop + pitch * BANNER_INSET
                val boxHeight = pitch * BANNER_HEIGHT
                banners += DrawnBanner(
                    boxTop = boxTop,
                    boxHeight = boxHeight,
                    layout = text,
                    textLeft = (widthPx - text.size.width) / 2f,
                    textTop = boxTop + (boxHeight - text.size.height) / 2f,
                    surahNumber = slot.surah,
                    ayahCount = Quran.surah(slot.surah).ayahCount,
                )
            }

            is LineSlot.Basmala -> {
                val text = measurer.measure(
                    AnnotatedString(BASMALA),
                    mushafTextStyle(layout.fontSize * 1.05f, colors.ink)
                )
                basmalas += DrawnText(
                    layout = text,
                    left = (widthPx - text.size.width) / 2f,
                    top = bandTop + (pitch - text.size.height) / 2f
                )
            }

            is LineSlot.Verses -> {
                // ممدود ولا متوسّط؟ مقيس من صور المصحف ومحفوظ في الأصل. قاعدة
                // «آخر سطر في السورة يتوسّط» غلط: آخر سطر في الإخلاص ممدود
                // وآخر سطر في الفلق متوسّط.
                val fill = when {
                    centerLines -> LineFill.Center
                    reference != null && slot.lineNumber in reference.centredLines -> LineFill.Center
                    else -> LineFill.Justify
                }
                val textTop = bandTop + (pitch - lineBox) / 2f
                verses += DrawnVerseLine(
                    bandTop = bandTop,
                    bandBottom = bandTop + pitch,
                    textTop = textTop,
                    tokens = placeTokens(
                        slot.tokens, widthPx, fill, style, markerStyle, measurer,
                        tajweedByAyah, colors
                    )
                )
            }
        }
    }
    return PageDrawing(verses, banners, basmalas)
}

/** كلمات السطر من اليمين لليسار، والفراغ الزايد بيتوزّع بالتساوي على المسافات. */
private fun placeTokens(
    tokens: List<MushafToken>,
    lineWidth: Float,
    fill: LineFill,
    style: TextStyle,
    markerStyle: TextStyle,
    measurer: TextMeasurer,
    tajweedByAyah: Map<Int, List<TajweedSpan>>,
    colors: MushafColors
): List<PlacedToken> {
    if (tokens.isEmpty()) return emptyList()
    val spaceWidth = measurer.measure(AnnotatedString(" "), style).size.width.toFloat() *
        WORD_SPACE_FACTOR

    val spans = tokens.map { tokenSpans(it, tajweedByAyah[it.ayahId]) }
    val layouts = tokens.mapIndexed { i, t ->
        measurer.measureToken(t, if (t.isEndMark) markerStyle else style, spans[i], colors::tajweedColor)
    }
    val widths = layouts.map { it.size.width.toFloat() }

    val natural = widths.sum() + spaceWidth * (tokens.size - 1)
    val gaps = (tokens.size - 1).coerceAtLeast(1)
    val slack = lineWidth - natural
    val extraPerGap =
        if (fill == LineFill.Justify && tokens.size > 1 && slack > 0f) slack / gaps else 0f
    var x = if (fill == LineFill.Center && slack > 0f) lineWidth - slack / 2f else lineWidth

    val placed = ArrayList<PlacedToken>(tokens.size)
    tokens.forEachIndexed { i, token ->
        val w = widths[i]
        x -= w
        placed += PlacedToken(token, layouts[i], x, w, spans[i])
        x -= spaceWidth + extraPerGap
    }
    return placed
}

/**
 * Ornamental page frame matching the current MushafFrameDrawable style:
 * band fill, thin diamonds, rope of beads on the outer side, double rules,
 * corner marks, and a simple page-number medallion.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrnatePageFrame(
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

    // band fill (frame minus inner) via EVEN_ODD path
    val path = androidx.compose.ui.graphics.Path().apply {
        fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                frameLeft, frameTop, frameRight, frameBottom,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4 * unit)
            )
        )
        addRect(androidx.compose.ui.geometry.Rect(innerLeft, innerTop, innerRight, innerBottom))
    }
    drawPath(path, frameColors.frameBand)

    // thin diamonds on the thin bands
    fun diamond(cx: Float, cy: Float, r: Float) {
        val d = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - r, cy)
            lineTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            close()
        }
        drawPath(d, frameColors.frameLine.copy(alpha = 0.5f))
    }
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

    // rope of beads on the thick outer side
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
    // double rule inside
    drawRect(
        color = frameColors.frameLine.copy(alpha = 0.6f),
        topLeft = Offset(innerLeft + 2.5f * unit, innerTop + 2.5f * unit),
        size = Size(innerRight - innerLeft - 5f * unit, innerBottom - innerTop - 5f * unit),
        style = Stroke(0.6f * unit),
    )

    // corner diamonds
    listOf(
        Offset(innerLeft, innerTop),
        Offset(innerRight, innerTop),
        Offset(innerLeft, innerBottom),
        Offset(innerRight, innerBottom),
    ).forEach { c -> diamond(c.x, c.y, 3.2f * unit) }

    // simple page-number medallion at bottom outer corner
    val pnX = if (rightPage) frameRight - 30 * unit else frameLeft + 30 * unit
    val medR = 12f * unit
    drawCircle(frameColors.frameBand, medR, Offset(pnX, frameBottom))
    drawCircle(frameColors.frameLine, medR, Offset(pnX, frameBottom), style = Stroke(1.2f * unit))
}

/**
 * Surah header matching the classic/modern styles from SuraHeaderSpan.
 * Classic: ornamental band + cartouche with name/order + side panels (ayah count).
 * Modern: teal pill.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurahHeader(
    banner: DrawnBanner,
    style: SurahHeaderStyle,
    colors: MushafColors,
) {
    val frameColors = MushafFrameColors.of(style, colors)
    val w = size.width
    val h = banner.boxHeight
    val top = banner.boxTop
    val s = h / if (style == SurahHeaderStyle.MODERN) 64f else 84f
    val cx = w / 2f
    val cy = top + h / 2f

    if (style == SurahHeaderStyle.MODERN) {
        // simple modern pill
        drawRoundRect(
            color = frameColors.headerFill,
            topLeft = Offset(s, top + s),
            size = Size(w - 2 * s, h - 2 * s),
            cornerRadius = CornerRadius(16 * s),
        )
        drawRoundRect(
            color = frameColors.headerBorder,
            topLeft = Offset(s, top + s),
            size = Size(w - 2 * s, h - 2 * s),
            cornerRadius = CornerRadius(16 * s),
            style = Stroke(1.2f * s),
        )
        val pw2 = (w * 0.26f).coerceAtMost(92 * s)
        drawRoundRect(
            color = frameColors.headerFill,
            topLeft = Offset(cx - pw2, top + 11 * s),
            size = Size(2 * pw2, h - 22 * s),
            cornerRadius = CornerRadius(h / 2),
        )
        drawText(banner.layout, topLeft = Offset(banner.textLeft, banner.textTop))
        return
    }

    // CLASSIC
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
    // cartouche (simplified rounded rect)
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
    // side panel: ayah count (left in RTL)
    val pw = ((w - 2 * cw - 48 * s) / 2f).coerceAtLeast(0f)
    if (pw > 18 * s) {
        val px = 14 * s
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(px, top + 17 * s),
            size = Size(pw, h - 34 * s),
            cornerRadius = CornerRadius(6 * s),
            style = Stroke(frameColors.frameLine, 1f * s),
        )
        // small diamonds top/bottom of panel
        fun diamond(cx: Float, cy: Float, r: Float) {
            val d = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - r, cy); lineTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); close()
            }
            drawPath(d, frameColors.frameLine)
        }
        diamond(px + pw / 2, top + 17 * s, 4 * s)
        diamond(px + pw / 2, top + h - 17 * s, 4 * s)
    }
    // name text (already measured)
    drawText(banner.layout, topLeft = Offset(banner.textLeft, banner.textTop))
}
