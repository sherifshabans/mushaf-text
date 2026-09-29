package io.github.sherifshabans.mushaf.sample.quran

import io.github.sherifshabans.mushaf.TajweedRule

/**
 * لمسة على حرف ملوّن، بالشكل اللي ورقة الشرح في التطبيق مستنياه.
 *
 * نوع المكتبة (`io.github.sherifshabans.mushaf.TajweedHit`) بيحمل الآية كمان؛
 * ده من غيرها، لأن مفتاح الألوان بيفتح نفس الورقة لحكم من غير موضع.
 */
data class TajweedHit(
    val rule: TajweedRule,
    val word: String,
    val start: Int,
    val end: Int
)

/** النص بيجي من المكتبة نضيف أصلًا — مفيش رقم آية في آخره. */
internal fun String.cleanAyaText(): String = trim()
