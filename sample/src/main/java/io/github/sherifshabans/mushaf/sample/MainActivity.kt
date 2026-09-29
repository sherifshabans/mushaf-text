package io.github.sherifshabans.mushaf.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sherifshabans.mushaf.MushafColors
import io.github.sherifshabans.mushaf.MushafPager
import io.github.sherifshabans.mushaf.TajweedHit
import io.github.sherifshabans.mushaf.TajweedLegend
import io.github.sherifshabans.mushaf.rememberMushafPagerState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var tajweed by remember { mutableStateOf(true) }
            var dark by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf(emptySet<Int>()) }
            var hit by remember { mutableStateOf<TajweedHit?>(null) }
            val colors = if (dark) MushafColors.Dark else MushafColors.Light
            val pager = rememberMushafPagerState(initialPage = 1)

            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = colors.paper) {
                    Column(Modifier.systemBarsPadding()) {
                        Row(Modifier.padding(horizontal = 12.dp)) {
                            FilterChip(
                                selected = tajweed,
                                onClick = { tajweed = !tajweed },
                                label = { Text("Tajweed") }
                            )
                            FilterChip(
                                selected = dark,
                                onClick = { dark = !dark },
                                label = { Text("Night") },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        MushafPager(
                            state = pager,
                            modifier = Modifier.weight(1f),
                            tajweed = tajweed,
                            colors = colors,
                            selectedAyahIds = selected,
                            onAyahClick = { ayah ->
                                hit = null
                                selected = if (ayah.id in selected) emptySet() else setOf(ayah.id)
                            },
                            onTajweedClick = { hit = it }
                        )
                        val h = hit
                        if (h != null) {
                            Text(
                                text = "${h.rule.label} — ${h.rule.definition} (${h.rule.amount})",
                                color = colors.tajweedColor(h.rule),
                                modifier = Modifier.fillMaxWidth().padding(12.dp)
                            )
                        } else if (tajweed) {
                            TajweedLegend(
                                colors = colors,
                                modifier = Modifier.fillMaxWidth().padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
