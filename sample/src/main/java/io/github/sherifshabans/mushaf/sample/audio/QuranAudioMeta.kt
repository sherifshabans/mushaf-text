package io.github.sherifshabans.mushaf.sample.audio

import io.github.sherifshabans.mushaf.sample.quran.SURA_AYA_COUNT
import io.github.sherifshabans.mushaf.sample.quran.SuraList
import io.github.sherifshabans.mushaf.sample.quran.toArabicNumerals

/**
 * بيانات السور اللي التشغيل محتاجها: الاسم، عدد الآيات، صفحة البداية.
 *
 * مافيش جدول جديد هنا — كله بيقرأ من [SuraList] و[SURA_AYA_COUNT] الموجودين
 * أصلًا للمصحف. أي جدول تاني كان هيبقى نسخة تانية لازم تتزامن يدويًّا.
 */
object QuranAudioMeta {

    const val SURAH_COUNT = 114

    fun surahName(surah: Int): String =
        SuraList.getOrNull(surah - 1)?.nameAr ?: "سورة $surah"

    fun ayahCount(surah: Int): Int =
        if (surah in 1..SURAH_COUNT) SURA_AYA_COUNT[surah - 1] else 0

    fun startPage(surah: Int): Int =
        SuraList.getOrNull(surah - 1)?.startPage ?: 1

    /** رقم السورة اللي الصفحة دي بتبدأ فيها — للانتقال من المصحف للتشغيل. */
    fun surahOfPage(page: Int): Int {
        var found = 1
        for (s in SuraList) {
            if (s.startPage <= page) found = s.number else break
        }
        return found
    }

    /** الآية اللي بعد دي مباشرة، وبتعدّي للسورة اللي بعدها لو خلصت. */
    fun nextAyah(surah: Int, ayah: Int): Pair<Int, Int>? = when {
        ayah < ayahCount(surah) -> surah to (ayah + 1)
        surah < SURAH_COUNT -> (surah + 1) to 1
        else -> null
    }
}

/**
 * الوقت في شريط التقدّم — «٤:٠٥».
 *
 * الأرقام عربية زي باقي المصحف، وبتعدّي على [toArabicNumerals] الموجودة أصلًا
 * بدل جدول أرقام تاني.
 */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "٠:٠٠"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val mm = if (h > 0) m.toString().padStart(2, '0') else m.toString()
    val ss = s.toString().padStart(2, '0')
    val body = if (h > 0) "$h:$mm:$ss" else "$mm:$ss"
    return body.split(':').joinToString(":") { part ->
        part.map { c -> toArabicNumerals(c - '0') }.joinToString("")
    }
}

/** حجم الملف في قائمة التنزيل — «١٢٫٤ م.ب». */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return ""
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1.0) {
        val text = String.format(java.util.Locale.US, "%.1f", mb)
        text.map { c -> if (c.isDigit()) toArabicNumerals(c - '0').first() else if (c == '.') '٫' else c }
            .joinToString("") + " م.ب"
    } else {
        toArabicNumerals((bytes / 1024).toInt()) + " ك.ب"
    }
}
