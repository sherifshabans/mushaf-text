package io.github.sherifshabans.mushaf.sample.quran

import io.github.sherifshabans.mushaf.TajweedRule
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ورقتان لوضع التجويد:
 *
 *  • [TajweedRuleSheet] — بتتفتح لمّا القارئ يدوس على **حرف ملوّن**. بتقول اسم
 *    الحكم وتعريفه ومقداره وحروفه، وأهم حاجة: بتعرض **كلمته هو** والحرف مميّز
 *    فيها. المثال اللي القارئ لقاه بنفسه أوضح من أي مثال جاهز.
 *
 *  • [TajweedLegendSheet] — المفاتيح ومفتاح الألوان. كل حكم صفّ قابل للضغط
 *    بيفتح شرحه، فالقارئ مش مضطر يلاقي مثالًا في الصفحة عشان يسأل عن حكم.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TajweedRuleSheet(
    hit: TajweedHit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rule = hit.rule

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuranPalette.Paper,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(rule.color)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = rule.label,
                        fontFamily = AmiriFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        color = rule.color
                    )
                    Text(
                        text = rule.family.label,
                        fontFamily = AmiriFont,
                        fontSize = 12.5.sp,
                        color = QuranPalette.Brown.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // الكلمة اللي اتلمست، والحرف صاحب الحكم مميّز فيها بلونه.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(QuranPalette.Banner)
                    .border(
                        1.dp,
                        QuranPalette.Gold.copy(alpha = 0.45f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(hit.word)
                        val a = hit.start.coerceIn(0, hit.word.length)
                        val b = hit.end.coerceIn(a, hit.word.length)
                        if (a < b) addStyle(SpanStyle(color = rule.color), a, b)
                    },
                    fontFamily = QuranFont,
                    fontSize = 34.sp,
                    color = QuranPalette.Ink,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(18.dp))
            InfoRow("التعريف", rule.definition)
            InfoRow("المقدار", rule.amount)
            InfoRow("الحروف", rule.letters, mushaf = true)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, mushaf: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            text = label,
            fontFamily = AmiriFont,
            fontSize = 13.sp,
            color = QuranPalette.Brown.copy(alpha = 0.75f),
            modifier = Modifier.width(68.dp)
        )
        Text(
            text = value,
            fontFamily = if (mushaf) QuranFont else AmiriFont,
            fontSize = if (mushaf) 19.sp else 15.sp,
            color = QuranPalette.Ink,
            lineHeight = if (mushaf) 32.sp else 26.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** صفّ في مفتاح الألوان: نقطة اللون، اسم الحكم، ونصّ على اليسار. */
@Composable
internal fun TajweedKeyRow(rule: TajweedRule, trailing: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(rule.color)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = rule.label,
            fontFamily = AmiriFont,
            fontSize = 15.sp,
            color = QuranPalette.Ink,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = trailing,
            fontFamily = AmiriFont,
            fontSize = 12.5.sp,
            color = QuranPalette.Brown.copy(alpha = 0.7f)
        )
    }
}

@Composable
internal fun TajweedSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = AmiriFont,
                fontSize = 15.5.sp,
                color = if (enabled) QuranPalette.Ink else QuranPalette.Ink.copy(alpha = 0.4f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = AmiriFont,
                    fontSize = 12.sp,
                    color = QuranPalette.Brown.copy(alpha = if (enabled) 0.75f else 0.35f),
                    lineHeight = 18.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = QuranPalette.Paper,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

