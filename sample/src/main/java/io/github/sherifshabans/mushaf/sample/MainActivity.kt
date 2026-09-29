package io.github.sherifshabans.mushaf.sample

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.github.sherifshabans.mushaf.sample.quran.QuranScreen

/**
 * The DailySeventy mushaf screen — tafsir, index, marks, search, share and
 * recitation — drawn with the mushaf-text library.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // إشعار التلاوة والتنزيل محتاج الإذن ده من أندرويد ١٣.
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF8ABEBB))) {
                // الشاشة عربية بالكامل، زي التطبيق.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    QuranScreen(onBackClick = { finish() })
                }
            }
        }
    }
}
