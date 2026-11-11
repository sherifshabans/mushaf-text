package io.github.sherifshabans.mushaf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders decorated pages to `mushaf/build/decoration-shots/` with real Skia and the
 * real font — the way to *see* the decoration without an emulator.
 *
 *     ./gradlew :mushaf:testDebugUnitTest --tests '*MushafDecorationHarness*'
 *     ./gradlew :mushaf:testDebugUnitTest --tests '*MushafDecorationHarness*' \
 *         -Pmushaf.pages=1,2,76,604
 *
 * The default page list is the milestone gallery: 5 (rubʿ at 2:26), 22 (juz 2),
 * 77 (surah opening, no star), 176 (sajdah), 264 (the corrected 15:49), 604 —
 * for None, Classic, Modern, Plain, and Madinah / Cairo / Diyanet / IndoPak in
 * light and dark. Note on labels: the `ثلاثة` medallion falls back to `٣/٤`
 * when the word cannot fit the 11 dp disk at the readable floor size — check
 * the `three-quarters` shots to see which one a screen density picks.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp-420dpi")
class MushafDecorationHarness {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private data class Shot(
        val page: Int,
        val style: String,
        val decoration: MushafDecoration,
        val highlighted: Boolean = false,
        val dark: Boolean = false,
    )

    @Test
    fun renderDecoratedPages() {
        val pages = (System.getProperty("mushaf.pages") ?: "5,22,77,176,264,604")
            .split(',').mapNotNull { it.trim().toIntOrNull() }
        val shots = pages.flatMap { page ->
            listOf(
                Shot(page, "none", MushafDecoration.None),
                Shot(page, "classic", MushafDecoration.Classic),
                Shot(page, "modern", MushafDecoration.Modern),
                Shot(
                    page, "plain",
                    MushafDecoration(pageFrame = true, surahHeaderStyle = SurahHeaderStyle.PLAIN),
                ),
                Shot(page, "madinah", MushafDecoration.Madinah),
                Shot(page, "madinah_dark", MushafDecoration.Madinah, dark = true),
                Shot(page, "cairo", MushafDecoration.Cairo),
                Shot(page, "cairo_dark", MushafDecoration.Cairo, dark = true),
                Shot(page, "diyanet", MushafDecoration.Diyanet),
                Shot(page, "diyanet_dark", MushafDecoration.Diyanet, dark = true),
                Shot(page, "indopak", MushafDecoration.IndoPak),
                Shot(page, "indopak_dark", MushafDecoration.IndoPak, dark = true),
                Shot(page, "classic_hl", MushafDecoration.Classic, highlighted = true),
                Shot(page, "classic_dark", MushafDecoration.Classic, highlighted = true, dark = true),
            )
        }
        val outDir = File("build/decoration-shots").apply { mkdirs() }
        val shown = mutableStateOf(shots.first())

        rule.setContent {
            val s = shown.value
            val colors = if (s.dark) MushafColors.Dark else MushafColors.Light
            val first = TestData.ayahs.first { it.page == s.page }
            Box(Modifier.size(411.dp, 830.dp).background(colors.paper)) {
                MushafPage(
                    ayahs = TestData.ayahs.filter { it.page == s.page },
                    modifier = Modifier.size(411.dp, 830.dp),
                    colors = colors,
                    decoration = s.decoration,
                    highlightedAyahs = if (s.highlighted) {
                        mapOf(first.id to Color(0xFFFFC107))
                    } else emptyMap(),
                )
            }
        }

        shots.forEach { s ->
            rule.runOnUiThread { shown.value = s }
            rule.waitForIdle()
            val name = "page_%03d_%s.png".format(s.page, s.style)
            save(File(outDir, name))
        }
    }

    private fun save(file: File) {
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bitmap)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("[mushaf] ${file.absolutePath}")
    }
}
