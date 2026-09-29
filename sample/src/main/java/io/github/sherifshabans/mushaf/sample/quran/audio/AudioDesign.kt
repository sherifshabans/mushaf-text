package io.github.sherifshabans.mushaf.sample.quran.audio

import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sherifshabans.mushaf.sample.quran.AmiriFont
import io.github.sherifshabans.mushaf.sample.quran.QuranSheetColors

/**
 * القطع الصغيرة المشتركة بين شاشات الاستماع.
 *
 * كل الألوان بتيجي من [QuranSheetColors] — نفس المصدر اللي شيتات المصحف
 * بتلوّن بيه نفسها، فالاستماع بيتبع تبديل الوضع الليلي والكرتوني زي باقي
 * التطبيق بدل ما يبقى جزيرة بلون ثابت.
 */

/** شريحة اختيار (فلتر) بحدّ ذهبي، ممتلئة لما تتحدّد. */
@Composable
fun AudioChip(
    label: String,
    selected: Boolean,
    sheet: QuranSheetColors,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val bg = if (selected) sheet.accent else sheet.accent.copy(alpha = 0.10f)
    val fg = if (selected) sheet.onAccent else sheet.ink
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) sheet.accent else sheet.divider,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontFamily = AmiriFont,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = fg,
            maxLines = 1
        )
    }
}

/** زرار دائري — التشغيل والتالي والسابق. */
@Composable
fun AudioCircleButton(
    icon: ImageVector,
    contentDescription: String,
    sheet: QuranSheetColors,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (filled) sheet.accent.copy(alpha = alpha)
                else sheet.accent.copy(alpha = 0.10f * alpha)
            )
            .then(
                if (filled) Modifier
                else Modifier.border(1.dp, sheet.divider.copy(alpha = alpha), CircleShape)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = (if (filled) sheet.onAccent else sheet.accent).copy(alpha = alpha),
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

/** عنوان قسم داخل شيت. */
@Composable
fun AudioSectionTitle(text: String, sheet: QuranSheetColors, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = AmiriFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = sheet.inkSoft,
        modifier = modifier.padding(vertical = 6.dp)
    )
}

/**
 * مؤشّر «بتحمّل دلوقتي» — تلات نقط بتنبض.
 *
 * مؤشّر دائري عادي كان بيقول «الشاشة واقفة»، والتلاوة هنا بتفضل شغّالة وهي
 * بتحمّل. النبض الخفيف بيقول «مستني الشبكة» من غير ما يوقف العين.
 */
@Composable
fun BufferingDots(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "buffering")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 180),
                    repeatMode = AnimRepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

/** شريط تقدّم رفيع بلا حواف — بيتحط على حافة كارت المشغّل الصغير. */
@Composable
fun ThinProgress(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 2.dp
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(color)
        )
    }
}
