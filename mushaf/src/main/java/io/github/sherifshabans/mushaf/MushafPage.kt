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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.text.font.FontFamily
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

/** حشوة تظليل الآية: أفقية تخلّي النص ما يلمسش الحافة، ورأسية تفصل الآيات. */
private const val AYAH_PAD_X_DP = 2f
private const val AYAH_PAD_Y_DP = 2f

/** نصف قطر زوايا إطار الآية (`MushafDecoration.ayahFrame`). */
private const val AYAH_FRAME_RADIUS_DP = 4f

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
    // الإطار نفسه هو اللي بيحدّد منطقة النص (MushafFrameGeometry)، فمفيش padding
    // إضافي هنا — أي padding زيادة كان بيقلّل الخط من غير داعي.
    CompositionLocalProvider(
        LocalDensity provides Density(platformDensity.density, fontScale = 1f)
    ) {
        MushafPageBody(
            ayahs = ayahs,
            modifier = modifier.padding(contentPadding),
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
 * نمط للأرقام في الترويسة والميدالية.
 *
 * **مش** خط المصحف: خط KFGQPC بيلمّ أي تتابع أرقام في وردة آية، فـ«٢٨٦» كانت
 * بتطلع وردتين ورقم الصفحة وردة. الاتجاه RTL عشان الكلمة اللي مع الرقم
 * («١٧٦ آية») تيجي في مكانها الصح — الأرقام نفسها بتترسم LTR جوّه الفقرة.
 */
private fun labelTextStyle(fontSize: Float, color: Color) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = fontSize.sp,
    color = color,
    textDirection = TextDirection.Rtl
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

/** ألوان التظليل: [fill] اللي بيترسم، و[stroke] إطار `ayahFrame` بنفس اللون. */
private class Wash(val fill: Color, val stroke: Color)

/**
 * امتداد آية واحدة على سطر واحد، بعد الحشوة.
 *
 * [continuesAbove]/[continuesBelow] بيقولوا إن الآية مكمّلة على السطر اللي قبله
 * أو بعده، فالحشوة الرأسية بتتشال من الناحية دي والزوايا بتترسم حادة — كده
 * تظليل الآية الطويلة بيطلع بلوك واحد مش شرايط منفصلة.
 */
private class AyahRun(
    val fill: Color,
    val stroke: Color,
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val continuesAbove: Boolean,
    val continuesBelow: Boolean,
)

/**
 * يجمّع كلمات كل سطر في امتدادات آيات، ويضيف الحشوة ويقصّها على حدود عمود النص.
 */
private fun ayahRuns(
    verses: List<DrawnVerseLine>,
    lineWidth: Float,
    padX: Float,
    padY: Float,
    washOf: (Int) -> Wash?,
): List<AyahRun> {
    class Raw(val line: Int, val ayah: Int, val wash: Wash, val left: Float, val right: Float)
    val raw = ArrayList<Raw>()

    verses.forEachIndexed { index, line ->
        var start: PlacedToken? = null
        var end: PlacedToken? = null
        var ayah = -1
        var wash: Wash? = null
        fun flush() {
            val a = start
            val b = end
            val w = wash
            if (a != null && b != null && w != null) {
                raw += Raw(
                    index, ayah, w,
                    min(a.left, b.left), max(a.left + a.width, b.left + b.width)
                )
            }
            start = null; end = null; ayah = -1; wash = null
        }
        line.tokens.forEach { p ->
            val id = p.token.ayahId
            val next = washOf(id)
            if (next != null && (id != ayah || next.fill != wash?.fill)) {
                flush(); ayah = id; wash = next; start = p
            } else if (next == null && wash != null) {
                flush()
            }
            if (wash != null) end = p
        }
        flush()
    }

    if (raw.isEmpty()) return emptyList()
    val present = raw.mapTo(HashSet()) { it.line to it.ayah }
    return raw.map { r ->
        val above = (r.line - 1) to r.ayah in present
        val below = (r.line + 1) to r.ayah in present
        val band = verses[r.line]
        AyahRun(
            fill = r.wash.fill,
            stroke = r.wash.stroke,
            left = max(0f, r.left - padX),
            right = min(lineWidth, r.right + padX),
            top = if (above) band.bandTop else band.bandTop + padY,
            bottom = if (below) band.bandBottom else band.bandBottom - padY,
            continuesAbove = above,
            continuesBelow = below,
        )
    }
}

internal class DrawnBanner(
    val boxTop: Float,
    val boxHeight: Float,
    /** عرض عمود النص — الشريط بيمتدّ بعرضه بالظبط، مش بعرض الـCanvas. */
    val width: Float,
    val layout: TextLayoutResult,
    val textLeft: Float,
    val textTop: Float,
    val surahNumber: Int,
    val ayahCount: Int,
    val revelationType: RevelationType,
    /** «ترتيبها N» تحت الاسم جوّه الكارتوث (زي القارئ الكلاسيكي). */
    val orderLayout: TextLayoutResult?,
    /** تسمية لوحة النزول: نزولها. */
    val revelationCaptionLayout: TextLayoutResult?,
    /** قيمة لوحة النزول: مكية / مدنية. */
    val revelationLayout: TextLayoutResult?,
    /** تسمية لوحة العدد: آياتها. */
    val countCaptionLayout: TextLayoutResult?,
    /** قيمة لوحة العدد (مثل «١٧٦ آية»). */
    val countLayout: TextLayoutResult?,
)

private class DrawnText(val layout: TextLayoutResult, val left: Float, val top: Float)

private class PageDrawing(
    val verses: List<DrawnVerseLine>,
    val banners: List<DrawnBanner>,
    val basmalas: List<DrawnText>,
    /** رقم الصفحة بأرقام عربية، مقيس لميدالية الإطار. */
    val pageNumberLayout: TextLayoutResult?,
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

        // الإطار بيحسب حدوده مرة واحدة، والنص بيتخطّط جوّه `content*` بتاعته.
        // ده اللي بيمنع السطور إنها تعدّي فوق الإطار وحبل الخرز.
        val frame = remember(decoration.pageFrame, widthPx, heightPx, pageNumber, density.density) {
            if (decoration.pageFrame) {
                ornateFrameGeometry(widthPx, heightPx, pageNumber ?: 1, density.density)
            } else {
                null
            }
        }
        val contentLeft = frame?.contentLeft ?: 0f
        val contentTop = frame?.contentTop ?: 0f
        val contentWidth = frame?.contentWidth ?: widthPx
        val contentHeight = frame?.contentHeight ?: heightPx

        val drawing = remember(
            tokens, contentWidth, contentHeight, maxFontSize, reference, tajweedByAyah,
            colors, decoration, pageNumber
        ) {
            val layout = reference
                ?.let {
                    referencePageLayout(tokens, it, contentWidth, contentHeight, maxFontSize, measurer)
                }
                ?: computedPageLayout(
                    tokens, usedLines, contentWidth, contentHeight, maxFontSize, measurer
                )
            buildPageDrawing(
                slots = buildLineSlots(layout, ayahs, reference),
                layout = layout,
                reference = reference,
                // الفاتحة وأول البقرة: ثمانية سطور متوسّطة بتباعد أوسع.
                centerLines = pageNumber != null && pageNumber <= 2,
                widthPx = contentWidth,
                heightPx = contentHeight,
                measurer = measurer,
                colors = colors,
                tajweedByAyah = tajweedByAyah,
                decoration = decoration,
                pageNumber = pageNumber
            )
        }

        val canvasHeight = with(density) { heightPx.toDp() }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .pointerInput(drawing, contentLeft, contentTop) {
                    detectTapGestures(
                        onLongPress = { pos ->
                            val hit = hitTest(drawing, pos - Offset(contentLeft, contentTop))
                                ?: return@detectTapGestures
                            ayahById[hit.token.token.ayahId]?.let { longClickAyah?.invoke(it) }
                        },
                        onTap = { pos ->
                            val hit = hitTest(drawing, pos - Offset(contentLeft, contentTop))
                                ?: return@detectTapGestures
                            val ayah = ayahById[hit.token.token.ayahId] ?: return@detectTapGestures
                            val onRule = clickTajweed
                            val span = onRule?.let { spanUnder(hit, pos - Offset(contentLeft, contentTop)) }
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
            if (frame != null) {
                drawOrnatePageFrame(
                    geometry = frame,
                    colors = colors,
                    style = decoration.surahHeaderStyle,
                    pageNumber = pageNumber ?: 1,
                    pageNumberLayout = drawing.pageNumberLayout,
                )
            }

            // كل المحتوى بيتـرسـم في إحداثيات الإطار الداخلية.
            translate(left = contentLeft, top = contentTop) {
                // ١) تظليل الآيات — آية واحدة لكل مستطيل، بحشوة تخلّي النص
                // ما يلمسش حافة التظليل، ومتوصل رأسيًا لو الآية مكمّلة على سطر
                // بعده.
                val padX = AYAH_PAD_X_DP.dp.toPx()
                val padY = AYAH_PAD_Y_DP.dp.toPx()
                val runs = ayahRuns(drawing.verses, contentWidth, padX, padY) { id ->
                    // الإطار لونه أغمق من التظليل؛ لو استخدمنا نفس اللون مش هيبان.
                    when {
                        // التلاوة الجارية أولًا: القارئ بيتابع بعينه على السطر.
                        id in playingAyahIds -> Wash(
                            fill = colors.playing,
                            stroke = lerp(colors.playing, colors.ink, 0.45f),
                        )
                        id in highlightedAyahs -> highlightedAyahs.getValue(id).let { raw ->
                            Wash(
                                fill = raw.copy(alpha = 0.30f),
                                stroke = lerp(raw, colors.ink, 0.35f),
                            )
                        }
                        id in selectedAyahIds -> Wash(
                            fill = colors.selection,
                            stroke = lerp(colors.selection, colors.ink, 0.30f),
                        )
                        else -> null
                    }
                }
                runs.forEach { run ->
                    drawRect(
                        color = run.fill,
                        topLeft = Offset(run.left, run.top),
                        size = Size(run.right - run.left, run.bottom - run.top),
                    )
                }

                // ١ب) إطار الآية — نفس المستطيلات بحشوتها، عشان `ayahFrame` يبقى
                // حوالين النص مش لازق فيه.
                if (decoration.ayahFrame) {
                    val radius = AYAH_FRAME_RADIUS_DP.dp.toPx()
                    val stroke = 1.2f.dp.toPx()
                    runs.forEach { run ->
                        val topRadius = if (run.continuesAbove) CornerRadius.Zero else CornerRadius(radius)
                        val bottomRadius = if (run.continuesBelow) CornerRadius.Zero else CornerRadius(radius)
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    run.left, run.top, run.right, run.bottom,
                                    topRadius, topRadius, bottomRadius, bottomRadius
                                )
                            )
                        }
                        drawPath(path, run.stroke.copy(alpha = 0.8f), style = Stroke(stroke))
                    }
                }

                // ١ج) موضع الحكم المطلوب — إطار حوالين **الكلمة** بلون الحكم.
                if (focusRule != null) {
                    val spots = ArrayList<Pair<DrawnVerseLine, PlacedToken>>()
                    drawing.verses.forEach { line ->
                        line.tokens.forEach { p ->
                            if (p.tajweed.any { it.rule == focusRule }) spots += line to p
                        }
                    }
                    if (spots.isNotEmpty()) {
                        val (line, p) =
                            spots[((focusIndex % spots.size) + spots.size) % spots.size]
                        val ruleColor = colors.tajweedColor(focusRule)
                        val pad = 3.dp.toPx()
                        val box = CornerRadius(7.dp.toPx())
                        val topLeft = Offset(p.left - pad, line.bandTop + padY)
                        val boxSize = Size(
                            p.width + pad * 2,
                            line.bandBottom - line.bandTop - padY * 2
                        )
                        drawRoundRect(ruleColor.copy(alpha = 0.16f), topLeft, boxSize, box)
                        drawRoundRect(
                            ruleColor.copy(alpha = 0.70f), topLeft, boxSize, box,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                // ٢) بانرات أسماء السور.
                drawing.banners.forEach { banner ->
                    when (decoration.surahHeaderStyle) {
                        SurahHeaderStyle.CLASSIC ->
                            drawClassicSurahHeader(banner = banner, colors = colors)
                        SurahHeaderStyle.MODERN ->
                            drawModernSurahHeader(banner = banner, colors = colors)
                        SurahHeaderStyle.PLAIN ->
                            drawText(
                                banner.layout,
                                topLeft = Offset(banner.textLeft, banner.textTop)
                            )
                    }
                }

                // ٣) البسملة، ٤) الكلمات والوردة.
                drawing.basmalas.forEach { drawText(it.layout, topLeft = Offset(it.left, it.top)) }
                drawing.verses.forEach { line ->
                    line.tokens.forEach { p ->
                        drawText(p.layout, topLeft = Offset(p.left, line.textTop))
                    }
                }
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
    tajweedByAyah: Map<Int, List<TajweedSpan>>,
    decoration: MushafDecoration,
    pageNumber: Int?
): PageDrawing {
    val style = mushafTextStyle(layout.fontSize, colors.ink)
    val markerStyle = style.copy(color = colors.marker)
    val lineBox = measurer.measure(AnnotatedString("ا"), style).size.height.toFloat()

    // لون اسم السورة لازم يطابق اللي هيترسم عليه: كارتوث غامق في الكلاسيك،
    // فالنص فاتح. والعدد على شريط فاتح، فلونه خط الإطار الذهبي.
    val frameColors = MushafFrameColors.of(decoration.surahHeaderStyle, colors)
    val bannerInk = when (decoration.surahHeaderStyle) {
        SurahHeaderStyle.CLASSIC, SurahHeaderStyle.MODERN -> frameColors.headerText
        SurahHeaderStyle.PLAIN -> colors.bannerText
    }

    val pageNumberLayout = pageNumber?.let {
        measurer.measure(
            AnnotatedString(toArabicDigits(it)),
            labelTextStyle(layout.fontSize * 0.34f, colors.marker)
        )
    }

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
                val surah = Quran.surah(slot.surah)
                val boxTop = bandTop + pitch * BANNER_INSET
                val boxHeight = pitch * BANNER_HEIGHT

                /** يقيس نصًّا ويقصّ حجمه على الصندوق اللي هيتـرسـم جوّاه. */
                fun fitted(
                    text: String,
                    style: (Float) -> TextStyle,
                    size: Float,
                    maxWidth: Float,
                    maxHeight: Float,
                ): TextLayoutResult {
                    var layout = measurer.measure(AnnotatedString(text), style(size))
                    val scale = min(
                        if (layout.size.width > maxWidth) maxWidth / layout.size.width else 1f,
                        if (layout.size.height > maxHeight) maxHeight / layout.size.height else 1f,
                    )
                    if (scale < 1f) layout = measurer.measure(AnnotatedString(text), style(size * scale))
                    return layout
                }

                val override = decoration.surahHeaderOverrides[slot.surah]
                val revelation = override?.revelationType ?: surah.revelationType
                val ayahCount = override?.ayahCount ?: surah.ayahCount
                val displayName = override?.nameArabic?.let { raw ->
                    val clean = raw.trim()
                    if (clean.startsWith("سورة") || clean.startsWith("سُورَةُ")) clean
                    else "سُورَةُ $clean"
                } ?: ("سُورَةُ " + surah.nameArabic)

                // لوحة الشمال: آياتها + العدد. لوحة اليمين: نزولها + مكية/مدنية.
                val nameMaxWidth = surahNameMaxWidth(widthPx, boxHeight)
                val nameText = fitted(
                    displayName,
                    { mushafTextStyle(it, bannerInk) },
                    layout.fontSize * 0.9f,
                    nameMaxWidth,
                    surahNameMaxHeight(widthPx, boxHeight),
                )

                val (panelWidth, panelHeight) = surahCountPanelSize(widthPx, boxHeight)
                // Classic panels sit on the gold band → use frameLine.
                // Modern meta is drawn on the teal pill → use headerText.
                val panelInk = when (decoration.surahHeaderStyle) {
                    SurahHeaderStyle.MODERN -> frameColors.headerText
                    else -> frameColors.frameLine
                }
                val captionStyle = { size: Float -> labelTextStyle(size, panelInk.copy(alpha = 0.75f)) }
                val valueStyle = { size: Float -> labelTextStyle(size, panelInk) }
                val countCaptionLayout = fitted(
                    "آياتها",
                    captionStyle,
                    layout.fontSize * 0.32f,
                    panelWidth * 0.85f,
                    panelHeight * 0.28f,
                )
                val countLayout = fitted(
                    ayahCountLabel(ayahCount),
                    valueStyle,
                    layout.fontSize * 0.42f,
                    panelWidth * 0.85f,
                    panelHeight * 0.42f,
                )
                val revelationCaptionLayout = fitted(
                    "نزولها",
                    captionStyle,
                    layout.fontSize * 0.32f,
                    panelWidth * 0.85f,
                    panelHeight * 0.28f,
                )
                val revelationLayout = fitted(
                    revelation.labelArabic,
                    valueStyle,
                    layout.fontSize * 0.45f,
                    panelWidth * 0.85f,
                    panelHeight * 0.42f,
                )
                val orderLayout = fitted(
                    "ترتيبها " + toArabicDigits(slot.surah),
                    { labelTextStyle(it, frameColors.headerText.copy(alpha = 0.9f)) },
                    layout.fontSize * 0.28f,
                    nameMaxWidth,
                    boxHeight * 0.22f,
                )

                banners += DrawnBanner(
                    boxTop = boxTop,
                    boxHeight = boxHeight,
                    width = widthPx,
                    layout = nameText,
                    textLeft = (widthPx - nameText.size.width) / 2f,
                    textTop = boxTop + (boxHeight - nameText.size.height) / 2f - boxHeight * 0.06f,
                    surahNumber = slot.surah,
                    ayahCount = ayahCount,
                    revelationType = revelation,
                    orderLayout = orderLayout,
                    revelationCaptionLayout = revelationCaptionLayout,
                    revelationLayout = revelationLayout,
                    countCaptionLayout = countCaptionLayout,
                    countLayout = countLayout,
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
    return PageDrawing(verses, banners, basmalas, pageNumberLayout)
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
