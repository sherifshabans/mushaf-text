package io.github.sherifshabans.mushaf.sample.quran

// ════════════════════════════════════════════════════════════════════════════
//  TajweedPrefs.kt
//
//  «وضع التجويد»: كل حكم بلونه على صفحة المصحف.
//
//  مفتاحان لا واحد:
//   • [isEnabled] — الوضع نفسه، مقفول افتراضيًا. القارئ اللي بيحفظ عايز حبرًا
//     واحدًا هادئًا، والتلوين تدخّل لو مجاش بطلبه.
//   • [showNaturalMadd] — المدّ الطبيعي وحده. هو أكتر حكم تكرارًا في المصحف
//     بفارق كبير، فتلوينه بيصبغ الصفحة كلها ويغطّي على باقي الأحكام. فمقفول
//     افتراضيًا كمان حتى والوضع مفتوح.
// ════════════════════════════════════════════════════════════════════════════

import android.content.Context

object TajweedPrefs {

    private const val PREFS = "tajweed_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_NATURAL_MADD = "natural_madd"

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun showNaturalMadd(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NATURAL_MADD, false)

    fun setShowNaturalMadd(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_NATURAL_MADD, show).apply()
    }
}
