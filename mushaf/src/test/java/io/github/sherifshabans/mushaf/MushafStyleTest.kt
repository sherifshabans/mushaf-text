package io.github.sherifshabans.mushaf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** هندسة الطُرُز الثلاثة: المحتوى جوّه القاعدة الداخلية لكل إطار، وكل الصفحات. */
class MushafStyleGeometryTest {

    private val sizeGrid = listOf(1080f to 1920f, 1080f to 2340f, 1440f to 2560f)
    private val unitGrid = listOf(1f, 2f, 2.625f, 3f, 4f)

    private fun request(width: Float, height: Float, page: Int, unit: Float) = PageFrameRequest(
        widthPx = width, heightPx = height, pageNumber = page, density = unit,
        outerMargin = 5f, innerMargin = 2f, topMargin = 1.5f, bottomMargin = 14f,
        textGap = 8f, divisionMarks = false,
    )

    private fun assertInsideInnerRule(g: PageFrameGeometry, gap: Float, where: String) {
        assertTrue(where, g.contentLeft >= g.frameLeft + gap)
        assertTrue(where, g.contentTop >= g.frameTop + gap)
        assertTrue(where, g.contentLeft + g.contentWidth <= g.frameRight - gap)
        assertTrue(where, g.contentTop + g.contentHeight <= g.frameBottom - gap)
        assertTrue(where, g.contentWidth > 0f)
        assertTrue(where, g.contentHeight > 0f)
    }

    @Test
    fun contentRectAlwaysSitsInsideEachFramesOwnInnerRule() {
        for (page in 1..604) {
            for ((width, height) in sizeGrid) {
                for (unit in unitGrid) {
                    val where = "page $page, ${width}x$height @$unit"
                    val req = request(width, height, page, unit)
                    assertInsideInnerRule(
                        PageFrames.Cairo.geometry(req), CAIRO_RULE_GAP * unit, "cairo $where",
                    )
                    assertInsideInnerRule(
                        PageFrames.Diyanet.geometry(req), DIYANET_RULE_GAP * unit, "diyanet $where",
                    )
                    assertInsideInnerRule(
                        PageFrames.IndoPak.geometry(req), INDOPAK_BAND * unit, "indopak $where",
                    )
                }
            }
        }
    }

    @Test
    fun divisionMarksGrowContentTopOnEveryFrame() {
        for (frame in listOf(PageFrames.Cairo, PageFrames.Diyanet, PageFrames.IndoPak)) {
            val plain = frame.geometry(request(1080f, 1920f, 5, 2.625f))
            val withMarks = frame.geometry(
                request(1080f, 1920f, 5, 2.625f).copy(divisionMarks = true)
            )
            assertEquals(
                "contentTop strip",
                plain.contentTop + RUNNING_HEAD_HEIGHT * 2.625f,
                withMarks.contentTop,
                0.01f,
            )
        }
    }

    @Test
    fun cairoAndDiyanetPageNumbersAreHorizontallyCentered() {
        val req = request(1080f, 1920f, 5, 2.625f)
        // within 1 dp of the frame's mid-x
        val tolerance = 1f * 2.625f

        val cairo = PageFrames.Cairo.geometry(req)
        assertEquals(
            "cairo page number x",
            (cairo.frameLeft + cairo.frameRight) / 2f,
            cairoPageNumberX(cairo),
            tolerance,
        )

        val diyanet = PageFrames.Diyanet.geometry(req)
        assertEquals(
            "diyanet page number x",
            (diyanet.frameLeft + diyanet.frameRight) / 2f,
            diyanetPageNumberX(diyanet),
            tolerance,
        )
    }

    @Test
    fun indoPakJuzLabelUsesEasternDigits() {
        val label = indoPakJuzLabel(27)
        assertEquals("پارہ ۲۷", label)
        label.substringAfter("پارہ ").forEach { c ->
            assertTrue("digit $c must be U+06F0..U+06F9", c in '\u06F0'..'\u06F9')
        }
    }
}

/** بكسل الطُرُز الجديدة: كل ستايل يغيّر الصفحة، والشعار بلا حبر جوّه الشريط. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp-420dpi")
class MushafStyleRenderTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val pageWidth = 411.dp
    private val pageHeight = 830.dp

    private val shownPage = mutableStateOf(2)
    private val decoration = mutableStateOf(MushafDecoration.Classic)
    private val colors = mutableStateOf(MushafColors.Light)
    private var contentSet = false

    private fun content() {
        if (contentSet) return
        contentSet = true
        rule.setContent {
            Box(
                Modifier.size(pageWidth, pageHeight)
                    .background(colors.value.paper)
            ) {
                MushafPage(
                    ayahs = TestData.ayahs.filter { it.page == shownPage.value },
                    modifier = Modifier.size(pageWidth, pageHeight),
                    colors = colors.value,
                    decoration = decoration.value,
                    contentPadding = PaddingValues(0.dp),
                )
            }
        }
    }

    private fun shot(
        decoration: MushafDecoration,
        page: Int = 2,
        colors: MushafColors = MushafColors.Light,
    ): Bitmap {
        content()
        rule.runOnUiThread {
            shownPage.value = page
            this.decoration.value = decoration
            this.colors.value = colors
        }
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bitmap)) }
        return bitmap
    }

    private fun diff(a: Bitmap, b: Bitmap): Int {
        assertEquals(a.width, b.width)
        assertEquals(a.height, b.height)
        val pa = IntArray(a.width * a.height)
        val pb = IntArray(b.width * b.height)
        a.getPixels(pa, 0, a.width, 0, 0, a.width, a.height)
        b.getPixels(pb, 0, b.width, 0, 0, b.width, b.height)
        var changed = 0
        for (i in pa.indices) if (pa[i] != pb[i]) changed++
        return changed
    }

    private fun Bitmap.pixels(): IntArray {
        val px = IntArray(width * height)
        getPixels(px, 0, width, 0, 0, width, height)
        return px
    }

    private fun isInk(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return r < 90 && g < 90 && b < 90
    }

    /** قريب من لون معيّن — لفحص حبر الآيات على الورق الغامق. */
    private fun closeTo(pixel: Int, color: Color): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return kotlin.math.abs(r - (color.red * 255).toInt()) < 28 &&
            kotlin.math.abs(g - (color.green * 255).toInt()) < 28 &&
            kotlin.math.abs(b - (color.blue * 255).toInt()) < 28
    }

    @Test
    fun eachStyleChangesThePage() {
        val ornate = shot(MushafDecoration(pageFrame = true), page = 2)
        for ((name, preset) in listOf(
            "cairo" to MushafDecoration.Cairo,
            "diyanet" to MushafDecoration.Diyanet,
            "indopak" to MushafDecoration.IndoPak,
        )) {
            assertTrue("$name looks like Ornate", diff(ornate, shot(preset, page = 2)) > 500)
        }
    }

    @Test
    fun cairoDrawsNoBeadRope() {
        val page = 2
        val ornate = shot(MushafDecoration(pageFrame = true), page = page)
        val cairo = shot(MushafDecoration.Cairo, page = page)
        val density = rule.activity.resources.displayMetrics.density
        val g = ornateFrameGeometry(
            ornate.width.toFloat(), ornate.height.toFloat(), page, density
        )
        // عمود وسط الشريط الخارجي — مكان حبل الخرز في الإطار الأورنات.
        val x = if (g.rightPage) {
            ((g.innerRight + g.frameRight) / 2f).toInt()
        } else {
            ((g.frameLeft + g.innerLeft) / 2f).toInt()
        }
        val y0 = (g.innerTop + 10).toInt()
        val y1 = (g.innerBottom - 10).toInt()

        fun nonPaperInColumn(bitmap: Bitmap): Int {
            val px = bitmap.pixels()
            var hits = 0
            for (y in y0..y1) {
                val pixel = px[y * bitmap.width + x]
                if (!closeTo(pixel, MushafColors.Light.paper)) hits++
            }
            return hits
        }

        val ornateCount = nonPaperInColumn(ornate)
        val cairoCount = nonPaperInColumn(cairo)
        assertTrue("probe missed the rope ($ornateCount hits)", ornateCount > 200)
        assertTrue("cairo drew rope beads ($cairoCount hits)", cairoCount < 20)
    }

    /**
     * خط الشعرة تحت سطر الآيات الأول في صفحة ٣، وعدم وجوده تحت خانة البانر
     * (سطر ١٥ في صفحة ٧٦ — بانر النساء).
     */
    @Test
    fun indoPakUnderlinesVerseLinesButNotTheBannerSlot() {
        fun underlineHits(page: Int, line: Int): Int {
            val bitmap = shot(MushafDecoration.IndoPak, page = page)
            val density = rule.activity.resources.displayMetrics.density
            // الارتفاع الحقيقي للصفحة هو صندوق 830dp — مش ارتفاع النافذة/الـbitmap.
            val g = PageFrames.IndoPak.geometry(
                PageFrameRequest(
                    widthPx = bitmap.width.toFloat(), heightPx = pageHeight.value * density,
                    pageNumber = page, density = density,
                    outerMargin = 5f, innerMargin = 2f, topMargin = 1.5f, bottomMargin = 14f,
                    textGap = 8f, divisionMarks = true,
                )
            )
            val pitch = g.contentHeight / 15f
            val y = (g.contentTop + pitch * line - 1.5f * density).toInt()
            val px = bitmap.pixels()
            var hits = 0
            for (x in (g.contentLeft + 2f).toInt() until (g.contentLeft + g.contentWidth - 2f).toInt()) {
                val pixel = px[y * bitmap.width + x]
                val r = (pixel shr 16) and 0xFF
                val gr = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                if (r > 140 && r - gr > 25 && r - b > 25) hits++
            }
            return hits
        }

        assertTrue("no underline under verse line 1 of page 3", underlineHits(3, 1) > 50)
        assertTrue(
            "underline drawn in the banner slot (page 76 line 15)",
            underlineHits(76, 15) == 0,
        )
        assertTrue("no underline under verse line 14 of page 76", underlineHits(76, 14) > 50)
    }

    @Test
    fun bodyInkNeverAppearsInTheFrameBandOfAnyStyle() {
        for ((name, preset) in listOf(
            "cairo" to MushafDecoration.Cairo,
            "diyanet" to MushafDecoration.Diyanet,
            "indopak" to MushafDecoration.IndoPak,
        )) {
            for (page in listOf(2, 76, 604)) {
                for (dark in listOf(false, true)) {
                    val palette = if (dark) MushafColors.Dark else MushafColors.Light
                    val bitmap = shot(preset, page = page, colors = palette)
                    val density = rule.activity.resources.displayMetrics.density
                    val g = preset.frame.geometry(
                        PageFrameRequest(
                            widthPx = bitmap.width.toFloat(), heightPx = pageHeight.value * density,
                            pageNumber = page, density = density,
                            outerMargin = 5f, innerMargin = 2f, topMargin = 1.5f, bottomMargin = 14f,
                            textGap = 8f, divisionMarks = true,
                        )
                    )
                    val px = bitmap.pixels()
                    val where = "$name page $page dark=$dark"
                    // فحص الشريط نفسه (بين القاعدة الخارجية والداخلية) — فجوة النص
                    // فيها سَوَابق الحروف الطبيعية من الخط القرآني.
                    val gap = when (preset.frame) {
                        PageFrames.Cairo -> CAIRO_RULE_GAP
                        PageFrames.Diyanet -> DIYANET_RULE_GAP
                        else -> INDOPAK_BAND
                    } * density
                    val innerLeft = g.frameLeft + gap
                    val innerRight = g.frameRight - gap
                    for (y in (g.frameTop + 2f).toInt()..(g.frameBottom - 2f).toInt()) {
                        for (side in listOf(
                            (g.frameLeft + 2f).toInt()..(innerLeft - 2f).toInt(),
                            (innerRight + 2f).toInt()..(g.frameRight - 2f).toInt(),
                        )) {
                            for (x in side) {
                                if (x < 0 || x >= bitmap.width) continue
                                val pixel = px[y * bitmap.width + x]
                                if (if (dark) closeTo(pixel, palette.ink) else isInk(pixel)) {
                                    throw AssertionError("body ink in the band at ($x, $y) — $where")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
