package io.github.sherifshabans.mushaf.sample.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.sherifshabans.mushaf.MushafDecoration
import io.github.sherifshabans.mushaf.MushafPager
import io.github.sherifshabans.mushaf.MushafSpread
import io.github.sherifshabans.mushaf.PageTurn
import io.github.sherifshabans.mushaf.rememberMushafPagerState

/**
 * معاينة طُرُز المصحف وطرق التقليب — **خارج** مسار القراءة الأساسي.
 *
 * الشاشة الأولى تفضل القارئ الحالي زي ما هو؛ الورقة دي معاينة حيّة
 * للمكتبة: الطُرُز الجاهزة، تقليب الورقة (انزلاق/تكشيرة)، والصفحة
 * المزدوجة. القفزات والعلامات كلها من المكتبة نفسها.
 */
@Composable
fun ReaderStylePreviewSheet(onClose: () -> Unit) {
    var preset by remember { mutableIntStateOf(3) }
    var curl by remember { mutableStateOf(false) }
    var spread by remember { mutableStateOf(false) }

    val presets = listOf(
        "بدون" to MushafDecoration.None,
        "كلاسيك" to MushafDecoration.Classic,
        "عصري" to MushafDecoration.Modern,
        "المدينة" to MushafDecoration.Madinah,
        "القاهرة" to MushafDecoration.Cairo,
        "ديانت" to MushafDecoration.Diyanet,
        "هندي" to MushafDecoration.IndoPak,
    )

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = QuranPalette.Paper) {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                ) {
                    presets.forEachIndexed { index, (label, _) ->
                        PreviewChip(label = label, selected = index == preset) { preset = index }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    PreviewChip(label = "انزلاق", selected = !curl) { curl = false }
                    PreviewChip(label = "تقليب الورقة", selected = curl) { curl = true }
                    PreviewChip(label = "صفحتين", selected = spread) { spread = !spread }
                }
                MushafPager(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = rememberMushafPagerState(initialPage = 22),
                    decoration = presets[preset].second,
                    pageTurn = if (curl) PageTurn.Curl else PageTurn.Slide,
                    spread = if (spread) MushafSpread.Auto else MushafSpread.Single,
                )
                PreviewChip(label = "إغلاق", selected = false, onClick = onClose)
            }
        }
    }
}

@Composable
private fun PreviewChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) QuranPalette.Gold.copy(alpha = 0.35f) else Color(0x14000000))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = QuranPalette.Ink,
        )
    }
}
