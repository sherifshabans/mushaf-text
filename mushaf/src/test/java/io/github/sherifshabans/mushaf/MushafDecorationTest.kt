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

/** الإطار بيحدّد منطقة النص — أي خطأ في الحسبة بيرجّع النص فوق الإطار تاني. */
class MushafFrameGeometryTest {

    @Test
    fun ayahCountLabelPluralisesLikeArabic() {
        assertEquals("آية واحدة", ayahCountLabel(1))
        assertEquals("آيتان", ayahCountLabel(2))
        assertEquals("٣ آيات", ayahCountLabel(3))
        assertEquals("٧ آيات", ayahCountLabel(7))
        assertEquals("١٠ آيات", ayahCountLabel(10))
        assertEquals("١١ آية", ayahCountLabel(11))
        assertEquals("١٠٠ آية", ayahCountLabel(100))
        assertEquals("٢٨٦ آية", ayahCountLabel(286))
    }

    @Test
    fun everySurahHasALabel() {
        for (surah in Quran.surahs) {
            assertTrue("surah ${surah.number}", ayahCountLabel(surah.ayahCount).contains("آي"))
        }
    }

    @Test
    fun contentRectAlwaysSitsInsideTheInnerRule() {
        for (page in 1..604) {
            for ((width, height) in listOf(1080f to 1920f, 1080f to 2340f, 1440f to 2560f)) {
                for (unit in listOf(1f, 2f, 2.625f, 3f, 4f)) {
                    val g = ornateFrameGeometry(width, height, page, unit)
                    val where = "page $page, ${width}x$height @$unit"
                    assertTrue(where, g.contentLeft >= g.innerLeft)
                    assertTrue(where, g.contentTop >= g.innerTop)
                    assertTrue(where, g.contentLeft + g.contentWidth <= g.innerRight)
                    assertTrue(where, g.contentTop + g.contentHeight <= g.innerBottom)
                    assertTrue(where, g.contentWidth > 0f)
                    assertTrue(where, g.contentHeight > 0f)
                }
            }
        }
    }

    @Test
    fun pageNumberFillsTheMedallionInsteadOfTrackingTheAyahFont() {
        // Inner disk of the rosette: 2 * 16 * 0.74 * 0.92 ≈ 21.8dp.
        val disk = pageNumberDiskDiameter(1f)
        assertEquals(2f * 16f * 0.74f * 0.92f, disk, 0.001f)
        assertEquals(0.72f, DEFAULT_PAGE_NUMBER_SCALE, 0.0001f)
        assertEquals(DEFAULT_PAGE_NUMBER_SCALE, MushafDecoration().pageNumberScale, 0.0001f)

        val maxInk = disk * DEFAULT_PAGE_NUMBER_SCALE
        // Tight ink of Arabic-Indic digits in Noto Sans Arabic at a 100px face
        // (the usual Android sans fallback): "٢" and "٦٠٤".
        val oneDigit = fittedPageNumberFontSize(40.6f, 68.5f, maxInk)
        val threeDigit = fittedPageNumberFontSize(124.7f, 68.7f, maxInk)
        // Old size was 0.34 × the ayah font: ~6.8sp on a 20sp page, ~8.8sp on a 26sp page.
        val oldOnFullPage = 20f * 0.34f
        val oldOnOpeningPage = 26f * 0.34f
        assertTrue(
            "one digit $oneDigit sp should be clearly larger than $oldOnOpeningPage",
            oneDigit > oldOnOpeningPage * 2f,
        )
        assertTrue(
            "three digits $threeDigit sp should be larger than $oldOnFullPage",
            threeDigit > oldOnFullPage * 1.5f,
        )
        // The binding edge of the ink lands on the disk, not past it.
        assertEquals(maxInk, 68.5f * oneDigit / 100f, 0.05f)
        assertEquals(maxInk, 124.7f * threeDigit / 100f, 0.05f)
        assertTrue(40.6f * oneDigit / 100f < maxInk)
    }

    @Test
    fun theWideBandAndTheRopeSwapSidesWithPageParity() {
        val odd = ornateFrameGeometry(1000f, 2000f, 77, 1f)
        val even = ornateFrameGeometry(1000f, 2000f, 78, 1f)
        assertTrue(odd.rightPage)
        assertTrue("odd page: wide band on the right", odd.bandRight > odd.bandLeft)
        assertTrue("even page: wide band on the left", even.bandLeft > even.bandRight)
    }
}

/**
 * يرسم صفحات مزخرفة وينظر في البكسل: الحبر ما ينفعش يوصل لشريط الإطار.
 * ده اللي كان بيحصل قبل ما النص يتخطّط جوّه `MushafFrameGeometry`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp-420dpi")
class MushafDecorationRenderTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val pageWidth = 411.dp
    private val pageHeight = 830.dp

    private val shownPage = mutableStateOf(2)
    private val decoration = mutableStateOf(MushafDecoration.Classic)
    private val selected = mutableStateOf(false)
    private var contentSet = false

    /** `setContent` بيتنادى مرة واحدة بس في اختبار Compose، فبنغيّر الحالة. */
    private fun content() {
        if (contentSet) return
        contentSet = true
        rule.setContent {
            Box(Modifier.size(pageWidth, pageHeight).background(MushafColors.Light.paper)) {
                MushafPage(
                    ayahs = TestData.ayahs.filter { it.page == shownPage.value },
                    modifier = Modifier.size(pageWidth, pageHeight),
                    colors = MushafColors.Light,
                    decoration = decoration.value,
                    selectedAyahIds = if (selected.value) {
                        setOf(TestData.ayahs.first { it.page == shownPage.value }.id)
                    } else emptySet(),
                    contentPadding = PaddingValues(0.dp),
                )
            }
        }
    }

    private fun shot(
        decoration: MushafDecoration,
        page: Int = 2,
        selected: Boolean = false,
    ): Bitmap {
        content()
        rule.runOnUiThread {
            shownPage.value = page
            this.selected.value = selected
            this.decoration.value = decoration
        }
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bitmap)) }
        return bitmap
    }

    private fun isInk(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return r < 90 && g < 90 && b < 90
    }

    @Test
    fun textAndBannerNeverInvadeTheFrameBand() {
        // 2: banner + basmala at the top. 76: banner at the bottom. 604: three
        // banners with the longest names, all inside a 15-line page.
        for (page in listOf(2, 76, 604)) {
            val bitmap = shot(MushafDecoration.Classic, page = page)
            val density = rule.activity.resources.displayMetrics.density
            val g = ornateFrameGeometry(
                bitmap.width.toFloat(), bitmap.height.toFloat(), page, density
            )
            val px = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            fun inkBetween(x0: Int, x1: Int, y0: Int, y1: Int): Int {
                var hits = 0
                for (y in y0.coerceAtLeast(0)..y1.coerceAtMost(bitmap.height - 1)) {
                    for (x in x0.coerceAtLeast(0)..x1.coerceAtMost(bitmap.width - 1)) {
                        if (isInk(px[y * bitmap.width + x])) hits++
                    }
                }
                return hits
            }

            val top = (g.frameTop + 2).toInt()
            val bottom = (g.frameBottom - 2).toInt()
            val left = (g.frameLeft + 2).toInt()
            val innerLeft = (g.innerLeft - 2).toInt()
            val innerRight = (g.innerRight + 2).toInt()
            val right = (g.frameRight - 2).toInt()

            assertEquals("page $page: ink left of the inner rule", 0, inkBetween(left, innerLeft, top, bottom))
            assertEquals("page $page: ink right of the inner rule", 0, inkBetween(innerRight, right, top, bottom))

            // النص موجود فعلًا جوّه الإطار — الاختبار مش بيمرّ على صفحة فاضية.
            val inside = inkBetween(
                (g.contentLeft + 4).toInt(), (g.contentLeft + g.contentWidth - 4).toInt(),
                (g.contentTop + 4).toInt(), (g.contentTop + g.contentHeight - 4).toInt(),
            )
            assertTrue("page $page: expected text inside the frame, found none", inside > 500)
        }
    }

    @Test
    fun ayahFrameActuallyDrawsAroundTheHighlightedAyah() {
        val without = shot(MushafDecoration.Classic.copy(ayahFrame = false), selected = true)
        val with = shot(MushafDecoration.Classic.copy(ayahFrame = true), selected = true)
        assertEquals(without.width, with.width)
        assertEquals(without.height, with.height)

        assertTrue("ayahFrame changed nothing — it is still a no-op", diff(without, with) > 200)
    }

    @Test
    fun everyDecorationStyleIsApplied() {
        // كل مقارنة بتغيّر **حاجة واحدة** بس، عشان الفشل يقول إيه اللي مابيرسمش.
        fun decorated(style: SurahHeaderStyle, frame: Boolean = true) =
            MushafDecoration(pageFrame = frame, surahHeaderStyle = style, ayahFrame = false)

        val noFrame = shot(decorated(SurahHeaderStyle.CLASSIC, frame = false))
        val framed = shot(decorated(SurahHeaderStyle.CLASSIC))
        val plainHeader = shot(decorated(SurahHeaderStyle.PLAIN))
        val classic = shot(decorated(SurahHeaderStyle.CLASSIC))
        val modern = shot(decorated(SurahHeaderStyle.MODERN))

        assertTrue("pageFrame drew nothing", diff(noFrame, framed) > 500)
        assertTrue("CLASSIC header drew nothing", diff(plainHeader, classic) > 500)
        assertTrue("MODERN header drew nothing", diff(plainHeader, modern) > 500)
        assertTrue("CLASSIC and MODERN must not look alike", diff(classic, modern) > 500)
    }

    /** عدد البكسلات المختلفة بين صورتين بنفس المقاس. */
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
}
