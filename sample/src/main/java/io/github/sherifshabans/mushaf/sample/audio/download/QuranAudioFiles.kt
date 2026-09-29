package io.github.sherifshabans.mushaf.sample.audio.download

import android.content.Context
import io.github.sherifshabans.mushaf.sample.audio.AyahReciter
import io.github.sherifshabans.mushaf.sample.audio.Moshaf
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioMeta
import java.io.File

/**
 * أماكن ملفات التلاوة المحمّلة على الجهاز.
 *
 * كله تحت `filesDir` (مش الذاكرة الخارجية): مفيش إذن مطلوب، وبيتمسح مع
 * التطبيق لو المستخدم شاله، وما بيظهرش في معرض الصوتيات للمستخدم.
 *
 * التنظيم مقصود إنه **مرآة للرابط**: مجلد لكل مصحف واسم الملف رقم السورة
 * بثلاث خانات — نفس صيغة الخادم. كده تحويل رابط ← ملف بيبقى حسبة مباشرة،
 * والمشغّل بيقدر يقرّر «محلّي ولا شبكة» من غير قاعدة بيانات تانية تتزامن.
 */
object QuranAudioFiles {

    private const val ROOT = "quran_audio"

    fun root(context: Context): File = File(context.filesDir, ROOT)

    /** ملف سورة كاملة لمصحف بعينه. */
    fun surahFile(context: Context, moshafKey: String, surah: Int): File =
        File(File(root(context), "surah/$moshafKey"), fileName(surah))

    /** ملف آية واحدة لقارئ «آية آية». */
    fun ayahFile(context: Context, folder: String, surah: Int, ayah: Int): File =
        File(
            File(root(context), "ayah/${folder.replace('/', '_')}/${pad3(surah)}"),
            pad3(surah) + pad3(ayah) + ".mp3"
        )

    fun isSurahDownloaded(context: Context, moshafKey: String, surah: Int): Boolean =
        surahFile(context, moshafKey, surah).let { it.exists() && it.length() > 0 }

    /** سورة «آية آية» تُعتبر محمّلة لما تكون كل آياتها موجودة. */
    fun isAyahSurahDownloaded(context: Context, folder: String, surah: Int): Boolean {
        val count = QuranAudioMeta.ayahCount(surah)
        if (count == 0) return false
        val dir = File(root(context), "ayah/${folder.replace('/', '_')}/${pad3(surah)}")
        if (!dir.isDirectory) return false
        // عدّ الملفات أرخص من فحص ٢٨٦ ملف واحدًا واحدًا، والزيادة مستحيلة
        // لأن كل آية اسمها فريد داخل المجلد.
        return (dir.list()?.count { it.endsWith(".mp3") } ?: 0) >= count
    }

    fun deleteSurah(context: Context, moshafKey: String, surah: Int): Boolean =
        surahFile(context, moshafKey, surah).delete()

    fun deleteAyahSurah(context: Context, folder: String, surah: Int): Boolean =
        File(root(context), "ayah/${folder.replace('/', '_')}/${pad3(surah)}").deleteRecursively()

    /** كل السور المحمّلة لمصحف — لعرض «المحمّل» وحذف المصحف كله. */
    fun downloadedSurahs(context: Context, moshafKey: String): Set<Int> =
        File(root(context), "surah/$moshafKey")
            .listFiles { f -> f.isFile && f.name.endsWith(".mp3") }
            ?.mapNotNull { it.name.removeSuffix(".mp3").toIntOrNull() }
            ?.toSet()
            .orEmpty()

    fun downloadedAyahSurahs(context: Context, folder: String): Set<Int> =
        File(root(context), "ayah/${folder.replace('/', '_')}")
            .listFiles { f -> f.isDirectory }
            ?.mapNotNull { it.name.toIntOrNull() }
            ?.filter { isAyahSurahDownloaded(context, folder, it) }
            ?.toSet()
            .orEmpty()

    /** إجمالي المساحة اللي التلاوات واخداها — تظهر في شاشة التنزيلات. */
    fun usedBytes(context: Context): Long = root(context).walkBottomUp()
        .filter { it.isFile }
        .sumOf { it.length() }

    fun deleteAll(context: Context): Boolean = root(context).deleteRecursively()

    // ── بناء أوامر التنزيل ──────────────────────────────────────────────────

    /** أمر تنزيل سورة كاملة: ملف واحد. */
    fun surahRequest(context: Context, moshaf: Moshaf, surah: Int) = DownloadRequest(
        key = surahKey(moshaf.key, surah),
        title = "${QuranAudioMeta.surahName(surah)} — ${moshaf.reciterName}",
        parts = listOf(
            DownloadPart(moshaf.urlFor(surah), surahFile(context, moshaf.key, surah))
        )
    )

    /** أمر تنزيل سورة «آية آية»: ملف لكل آية. */
    fun ayahSurahRequest(context: Context, reciter: AyahReciter, surah: Int) = DownloadRequest(
        key = ayahKey(reciter.folder, surah),
        title = "${QuranAudioMeta.surahName(surah)} (آية آية) — ${reciter.name}",
        parts = (1..QuranAudioMeta.ayahCount(surah)).map { ayah ->
            DownloadPart(
                reciter.urlFor(surah, ayah),
                ayahFile(context, reciter.folder, surah, ayah)
            )
        }
    )

    fun surahKey(moshafKey: String, surah: Int) = "s|$moshafKey|$surah"
    fun ayahKey(folder: String, surah: Int) = "a|$folder|$surah"

    private fun fileName(surah: Int) = pad3(surah) + ".mp3"
    private fun pad3(n: Int) = n.toString().padStart(3, '0')
}
