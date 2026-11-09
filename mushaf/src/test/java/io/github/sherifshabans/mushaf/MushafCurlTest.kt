package io.github.sherifshabans.mushaf

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** هندسة التكشيرة النقية — الطرفان بالضبط، والشريط جنب الحد. */
class MushafCurlSliceTest {

    private val w = 1000f

    @Test
    fun settledEndsHaveNoStripAndTheBoundaryOnTheCorrectEdge() {
        for ((progress, forward, expectedBoundary) in listOf(
            Triple(0f, true, 0f),
            Triple(1f, true, w),
            Triple(0f, false, w),
            Triple(1f, false, 0f),
        )) {
            val s = curlSlice(w, progress, forward, backWidthPx = 18f, shadowWidthPx = 24f)
            val where = "progress=$progress forward=$forward"
            assertEquals("$where boundary", expectedBoundary, s.boundaryX, 0f)
            // لا شريط ظهر عند الطرفين — ولا ورقة تخلّف ورقة صفحة واحدة.
            assertEquals("$where backLeft", s.boundaryX, s.backLeft, 0f)
            assertEquals("$where backRight", s.boundaryX, s.backRight, 0f)
        }
    }

    @Test
    fun halfForwardPutsTheBackStripLeftOfTheBoundary() {
        val s = curlSlice(w, 0.5f, forward = true, backWidthPx = 18f, shadowWidthPx = 24f)
        assertEquals(500f, s.boundaryX, 0f)
        assertEquals("back strip right edge on the boundary", 500f, s.backRight, 0f)
        assertEquals("back strip on the left of it", 482f, s.backLeft, 0f)
        assertEquals(500f, s.shadowRight, 0f)
        assertEquals(476f, s.shadowLeft, 0f)
    }

    @Test
    fun backwardPeelMirrorsForward() {
        val s = curlSlice(w, 0.5f, forward = false, backWidthPx = 18f, shadowWidthPx = 24f)
        assertEquals(500f, s.boundaryX, 0f)
        assertEquals("back strip right of the boundary", 518f, s.backRight, 0f)
        assertEquals(500f, s.backLeft, 0f)
        assertEquals(524f, s.shadowRight, 0f)
        assertEquals(500f, s.shadowLeft, 0f)
    }

    @Test
    fun progressIsClamped() {
        val over = curlSlice(w, 1.5f, forward = true, backWidthPx = 18f, shadowWidthPx = 24f)
        val at = curlSlice(w, 1f, forward = true, backWidthPx = 18f, shadowWidthPx = 24f)
        assertEquals(at.boundaryX, over.boundaryX, 0f)
        assertEquals(at.backLeft, over.backLeft, 0f)
    }
}

/**
 * تقليب الورقة داخل الـpager: السحب يودّي للصفحة الجاية، ولمسة الهامش تقلّب،
 * والقفزة الخارجية تستبدل الوجهة، والصفحات الغريبة مش بتتراكّب.
 *
 * ملاحظة اتجاه: الـpager RTL — السحب **يمينًا** هو التقدّم للصفحة الجاية
 * (اللي بتيجي من الشمال)، زي ما سحب الـHorizontalPager بيتصرّف.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp-420dpi")
class MushafCurlPagerTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var pagerState: PagerState
    private lateinit var scope: CoroutineScope

    private var tappedAyah: Ayah? = null

    private fun content(pageTurn: PageTurn, decoration: MushafDecoration = MushafDecoration.None) {
        rule.setContent {
            scope = rememberCoroutineScope()
            pagerState = rememberMushafPagerState(initialPage = 1)
            Box(Modifier.size(411.dp, 830.dp).background(MushafColors.Light.paper)) {
                MushafPager(
                    modifier = Modifier.size(411.dp, 830.dp),
                    state = pagerState,
                    colors = MushafColors.Light,
                    decoration = decoration,
                    pageTurn = pageTurn,
                    onAyahClick = { tappedAyah = it },
                    pagePadding = PaddingValues(0.dp),
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun slideModeNeverComposesCurlPages() {
        MushafCurlProbe.reset()
        content(PageTurn.Slide)
        assertTrue(
            "curl content composed in Slide mode",
            MushafCurlProbe.composedPages.isEmpty(),
        )
    }

    @Test
    fun draggingRightSettlesOnTheNextPage() {
        content(PageTurn.Curl)
        assertEquals(0, pagerState.currentPage)
        rule.onRoot().performTouchInput { swipeRight() }
        rule.waitForIdle()
        assertEquals("RTL forward drag must land on page 2", 1, pagerState.currentPage)
    }

    @Test
    fun tapOnAWordStillInvokesOnAyahClick() {
        content(PageTurn.Curl)
        rule.onRoot().performTouchInput {
            down(Offset(center.x, center.y))
            up()
        }
        rule.waitForIdle()
        assertNotNull("tap inside the text column was stolen by the curl", tappedAyah)
    }

    @Test
    fun aMarginTapTurnsTheLeaf() {
        // الهامش موجود مع الإطار بس — برّة عمود النص.
        content(PageTurn.Curl, decoration = MushafDecoration.Classic)
        // يسار الإطار (برّة عمود النص) = تقليب للأمام.
        rule.onRoot().performTouchInput {
            down(Offset(4f, center.y))
            up()
        }
        rule.waitForIdle()
        assertEquals("left margin tap must turn forward", 1, pagerState.currentPage)
    }

    @Test
    fun aScrollJumpDuringAPeelReplacesTheTarget() {
        MushafCurlProbe.reset()
        content(PageTurn.Curl)
        rule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(400f, 0f))
            // قفزة خارجية أثناء التكشيرة — لازم تحلّ محلّ الوجهة الحالية.
            scope.launch { pagerState.scrollToPage(9) }
            up()
        }
        rule.waitForIdle()
        assertEquals("settled page must be the jump target", 9, pagerState.currentPage)
        val composed = MushafCurlProbe.composedPages
        assertTrue("target page never composed", 9 in composed)
        for (unrelated in listOf(4, 5, 6, 7)) {
            assertTrue("unrelated page $unrelated was composed", unrelated !in composed)
        }
    }
}
