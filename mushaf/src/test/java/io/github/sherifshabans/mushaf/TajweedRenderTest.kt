package io.github.sherifshabans.mushaf

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The invariant tajweed mode rests on: **colouring a word never changes its size.**
 * If it did, line breaks, justification and the font size of the whole page would
 * shift the moment tajweed is switched on. Measured with real Skia and the real
 * font, not assumed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class TajweedRenderTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun colouringDoesNotChangeWordSize() {
        var measurer: TextMeasurer? = null
        rule.setContent { measurer = rememberTextMeasurer() }
        rule.waitForIdle()
        val m = requireNotNull(measurer)
        val style = TextStyle(fontFamily = MushafFont, fontSize = 34.sp)

        var coloured = 0
        TestData.ayahs.filter { it.page in listOf(1, 2, 3, 42, 604) }.forEach { ayah ->
            val spans = TajweedAnnotator.annotate(ayah.text, includeNaturalMadd = true)
            val (tokens, _) = buildPageTokens(listOf(ayah)) { "" }
            tokens.filterNot { it.isEndMark }.forEach { t ->
                val inWord = spans.mapNotNull { s ->
                    val a = maxOf(s.start, t.charStart)
                    val b = minOf(s.end, t.charStart + t.text.length)
                    if (a < b) TajweedSpan(a - t.charStart, b - t.charStart, s.rule) else null
                }
                if (inWord.isEmpty()) return@forEach
                coloured++
                val plain = m.measure(AnnotatedString(t.text), style)
                val painted = m.measure(colouredText(t.text, inWord) { it.color }, style)
                assertEquals("width of «${t.text}»", plain.size, painted.size)
            }
        }
        assertTrue("too few coloured words: $coloured", coloured > 200)
    }
}
