package io.github.sherifshabans.mushaf.sample.quran

import io.github.sherifshabans.mushaf.TajweedRule
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * شريط التنقّل بين مواضع حكم على الصفحة.
 *
 * ## ليه شريط عائم لا صفّ في التخطيط
 * الصفحة محسوبة على ١٥ سطرًا وأي حاجة بتاخد من ارتفاعها بتصغّر الخط — وده
 * بالظبط اللي شريط الختمة كان بيعمله قبل ما يتشال. فالشريط ده **فوق** الصفحة
 * بـ`align`، مش جوّه العمود: ما بياخدش ولا بكسل من ارتفاعها.
 *
 * ## ليه أصلًا
 * الضغط على الحكم في الورقة بيوقّف القارئ على أول موضع، والورقة بتتقفل عشان
 * يشوف الصفحة. فلو عايز الموضع اللي بعده كان لازم يفتح الورقة تاني ويدوس تاني
 * — دورة كاملة لكل موضع. الشريط بيخلّي التنقّل في مكانه: سهم للي بعده، سهم
 * للي قبله، والعدّاد بيقول هو فين من كام.
 */
@Composable
fun TajweedFocusBar(
    rule: TajweedRule,
    /** ترتيب الموضع الحالي من الصفر. */
    index: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (total <= 0) return
    val shown = ((index % total) + total) % total + 1

    // الاتجاه متثبّت على RTL، والأيقونات **غير** ذاتية الانعكاس.
    //
    // كانت `Icons.AutoMirrored` — ودي بتتقلب لوحدها مع اتجاه التخطيط، فاللي
    // مكتوب في الكود «يمين» كان بيترسم «شمال» والعكس. يعني السهمين كانوا
    // بيوَدّوا عكس ما بيأشّروا.
    //
    // وترتيب الصفّ نفسه بيعتمد على الاتجاه كمان، فلو الاتجاه اتغيّر (لغة
    // الجهاز مثلًا) كان الترتيب هيتقلب من غير ما الأيقونات تتقلب معاه. تثبيت
    // الاتنين بيخلّي الشكل واحدًا مهما كانت لغة النظام.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(QuranPalette.Paper)
            .border(1.dp, rule.color.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .padding(start = 6.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        RoundButton(
            icon = { tint -> Icon(Icons.Default.Close, "إغلاق", tint = tint, modifier = Modifier.size(17.dp)) },
            tint = QuranPalette.Brown.copy(alpha = 0.7f),
            onClick = onClose
        )

        Box(
            Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(rule.color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = rule.label,
            fontFamily = AmiriFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = rule.color
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "${toArabicNumerals(shown)} من ${toArabicNumerals(total)}",
            fontFamily = AmiriFont,
            fontSize = 12.5.sp,
            color = QuranPalette.Brown.copy(alpha = 0.8f)
        )
        Spacer(Modifier.width(3.dp))

        // القراءة من اليمين للشمال، فـ«السابق» على اليمين و«التالي» على الشمال.
        // المصحف بيتقرا من اليمين للشمال: «السابق» سهم لليمين، و«التالي» سهم
        // للشمال — زي ما العين بتتحرّك على السطر بالظبط.
        RoundButton(
            icon = { tint ->
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    "الموضع السابق", tint = tint, modifier = Modifier.size(22.dp)
                )
            },
            tint = rule.color,
            onClick = onPrevious
        )
        RoundButton(
            icon = { tint ->
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    "الموضع التالي", tint = tint, modifier = Modifier.size(22.dp)
                )
            },
            tint = rule.color,
            onClick = onNext
        )
    }
    }
}

@Composable
private fun RoundButton(
    icon: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon(tint)
    }
}
