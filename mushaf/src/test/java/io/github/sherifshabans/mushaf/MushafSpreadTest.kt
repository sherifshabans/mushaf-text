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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * الازدواجية: الفردية يمينًا والزوجية شمالًا، وأول صفحة شمالها داخل الكتاب.
 * الصفحة المركّزة بتتحدّد في `state.currentPage` زي ما هي (صفحة واحدة).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w820dp-h1280dp-420dpi")
class MushafSpreadTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var pagerState: PagerState
    @Suppress("unused")
    private lateinit var scope: CoroutineScope

    /**
     * أرقام الصفحات الظاهرة فعليًا جوّه صندوق الصفحة (التذييلات الرقمية) —
     * الجيران المركّبين برّة الصندوق مش داخلين.
     */
    private val digitTexts = SemanticsMatcher("pure Arabic-Indic digits") { node ->
        node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }
            ?.takeIf { it.isNotBlank() && it.all { c -> c in '٠'..'٩' } } != null
    }

    private fun visiblePageNumbers(widthDp: Float, heightDp: Float): Set<String> {
        val density = rule.activity.resources.displayMetrics.density
        val maxX = widthDp * density
        val maxY = heightDp * density
        return rule.onAllNodes(digitTexts).fetchSemanticsNodes()
            .mapNotNull { node ->
                // الصفحات المركّبة برّا الشاشة بترجّع مستطيل فاضي — مش ظاهرة.
                if (node.boundsInRoot.width <= 0f || node.boundsInRoot.height <= 0f) {
                    return@mapNotNull null
                }
                val text = node.config.getOrNull(SemanticsProperties.Text)
                    ?.joinToString(" ") { it.text }
                    ?.takeIf { it.isNotBlank() && it.all { c -> c in '٠'..'٩' } } ?: return@mapNotNull null
                val center = node.boundsInRoot.center
                if (center.x in 0f..maxX && center.y in 0f..maxY) text else null
            }
            .toSet()
    }

    private fun content(spread: MushafSpread, widthDp: Float, heightDp: Float = 830f, initialPage: Int = 1) {
        rule.setContent {
            scope = rememberCoroutineScope()
            pagerState = rememberMushafPagerState(initialPage = initialPage)
            Box(Modifier.size(widthDp.dp, heightDp.dp).background(MushafColors.Light.paper)) {
                MushafPager(
                    modifier = Modifier.size(widthDp.dp, heightDp.dp),
                    state = pagerState,
                    colors = MushafColors.Light,
                    spread = spread,
                    pagePadding = PaddingValues(0.dp),
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun narrowScreenShowsOnePage() {
        content(MushafSpread.Auto, widthDp = 400f, initialPage = 50)
        assertEquals(setOf("٥٠"), visiblePageNumbers(400f, 830f))
        assertEquals("state still addresses one page", 49, pagerState.currentPage)
    }

    @Test
    fun wideScreenShowsThePairOddOnTheRight() {
        content(MushafSpread.Auto, widthDp = 800f, initialPage = 50)
        assertEquals(setOf("٤٩", "٥٠"), visiblePageNumbers(800f, 830f))
        assertEquals("state still addresses one page", 49, pagerState.currentPage)

        // الفردية (٤٩) في النصف اليمين والزوجية (٥٠) في النصف الشمال.
        val density = rule.activity.resources.displayMetrics.density
        val nodes = rule.onAllNodes(digitTexts).fetchSemanticsNodes()
            .filter { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
            .mapNotNull { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)
                    ?.joinToString(" ") { it.text }
                    ?.takeIf { it.all { c -> c in '٠'..'٩' } } ?: return@mapNotNull null
                text to node.boundsInRoot.center.x
            }
        val x49 = nodes.first { it.first == "٤٩" && it.second in 0f..800f * density }.second
        val x50 = nodes.first { it.first == "٥٠" && it.second in 0f..800f * density }.second
        assertEquals("٤٩ must sit right of the middle", true, x49 > 400f * density)
        assertEquals("٥٠ must sit left of the middle", true, x50 < 400f * density)
    }

    @Test
    fun pageOneShowsTheInsideCoverOnItsLeft() {
        content(MushafSpread.Auto, widthDp = 800f, initialPage = 1)
        assertEquals(setOf("١"), visiblePageNumbers(800f, 830f))
    }

    @Test
    fun theLastSpreadIsFull() {
        content(MushafSpread.Auto, widthDp = 800f, initialPage = 603)
        assertEquals(setOf("٦٠٣", "٦٠٤"), visiblePageNumbers(800f, 830f))
    }

    @Test
    fun singleModeNeverPairs() {
        content(MushafSpread.Single, widthDp = 800f, initialPage = 50)
        assertEquals(setOf("٥٠"), visiblePageNumbers(800f, 830f))
    }
}
