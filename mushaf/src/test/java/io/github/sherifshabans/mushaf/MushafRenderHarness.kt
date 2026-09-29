package io.github.sherifshabans.mushaf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders pages to `mushaf/build/mushaf-shots/` with real Skia and the real font —
 * the way to *see* a change without an emulator.
 *
 *     ./gradlew :mushaf:testDebugUnitTest --tests '*MushafRenderHarness*' -Pmushaf.pages=1,3,604
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp-420dpi")
class MushafRenderHarness {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private data class Shot(val page: Int, val tajweed: Boolean, val dark: Boolean)

    @Test
    fun renderPages() {
        val pages = (System.getProperty("mushaf.pages") ?: "1,2,3,77,604")
            .split(',').mapNotNull { it.trim().toIntOrNull() }
        val shots = pages.flatMap { listOf(Shot(it, false, false), Shot(it, true, false)) } +
            Shot(pages.last(), true, true)
        val outDir = File("build/mushaf-shots").apply { mkdirs() }
        val shown = mutableStateOf(shots.first())
        val showText = mutableStateOf(false)

        rule.setContent {
            if (showText.value) {
                // النص المتدفّق: آية الكرسي وما حولها.
                Column(Modifier.width(411.dp).background(MushafColors.Light.paper).padding(12.dp)) {
                    QuranText(
                        ayahs = TestData.ayahs.filter { it.surah == 2 && it.number in 253..256 },
                        fontSize = 22.sp,
                        tajweed = true,
                        selectedAyahIds = setOf(TestData.ayah(2, 255).id)
                    )
                }
                return@setContent
            }
            val s = shown.value
            val colors = if (s.dark) MushafColors.Dark else MushafColors.Light
            Box(Modifier.size(411.dp, 830.dp).background(colors.paper)) {
                MushafPage(
                    ayahs = TestData.ayahs.filter { it.page == s.page },
                    modifier = Modifier.padding(8.dp),
                    tajweed = s.tajweed,
                    colors = colors
                )
            }
        }

        shots.forEach { s ->
            rule.runOnUiThread { shown.value = s }
            rule.waitForIdle()
            val name = "page_%03d%s%s.png".format(
                s.page, if (s.tajweed) "_tajweed" else "", if (s.dark) "_dark" else ""
            )
            save(File(outDir, name))
        }

        rule.runOnUiThread { showText.value = true }
        rule.waitForIdle()
        save(File(outDir, "quran_text.png"))
    }

    private fun save(file: File) {
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bitmap)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("[mushaf] ${file.absolutePath}")
    }
}
