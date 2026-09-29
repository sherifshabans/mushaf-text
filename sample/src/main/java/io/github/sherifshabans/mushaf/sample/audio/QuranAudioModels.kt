package io.github.sherifshabans.mushaf.sample.audio

/**
 * نماذج الصوت للمصحف.
 *
 * فيه مصدرين مختلفين تمامًا، وكل واحد بيخدم طلب مختلف من المستخدم:
 *
 * 1. **سورة كاملة** — `reciters.json` (نسخة من فهرس mp3quran.net، ٢٢٧ قارئ /
 *    ٢٧١ مصحف). كل مصحف ملفاته `{server}/001.mp3 … 114.mp3`. ده اللي فيه كل
 *    الشيوخ وكل الروايات (حفص، ورش، قالون، الدوري، البزي، المجود، المعلّم…).
 *
 * 2. **آية آية** — everyayah.com، ملف لكل آية `{folder}/SSSAAA.mp3`. الفهرس ده
 *    **مش موجود في `reciters.json`** أصلًا، فمكتوب هنا يدويًّا في
 *    [AyahRecitersCatalog] بعد التأكّد من كل مجلد.
 *
 * الاتنين بيتحوّلوا لنفس النوع [AudioTrack] عشان المشغّل ما يفرّقش بينهم.
 */

/** رواية/نوع الأداء — مستخرجة من اسم المصحف عشان نفلتر بيها. */
enum class Riwaya(val id: String, val label: String) {
    HAFS("hafs", "حفص عن عاصم"),
    WARSH("warsh", "ورش عن نافع"),
    QALOON("qaloon", "قالون عن نافع"),
    SHUBA("shuba", "شعبة عن عاصم"),
    DOURI_KISAI("douri_kisai", "الدوري عن الكسائي"),
    DOURI_AMR("douri_amr", "الدوري عن أبي عمرو"),
    SOUSI("sousi", "السوسي عن أبي عمرو"),
    IBN_THAKWAN("ibn_thakwan", "ابن ذكوان عن ابن عامر"),
    HISHAM("hisham", "هشام عن ابن عامر"),
    BAZZI("bazzi", "البزي عن ابن كثير"),
    QUNBUL("qunbul", "قنبل عن ابن كثير"),
    KHALAF("khalaf", "خلف عن حمزة"),
    YAQOUB("yaqoub", "يعقوب الحضرمي"),
    IBN_JAMMAZ("ibn_jammaz", "ابن جماز عن أبي جعفر"),
    MOJAWWAD("mojawwad", "المصحف المجوّد"),
    MOALLIM("moallim", "المصحف المعلّم"),
    OTHER("other", "روايات أخرى");

    companion object {
        /**
         * الرواية من اسم المصحف كما يكتبه mp3quran.
         *
         * التطابق بالاحتواء لا بالتساوي: الأسماء في الملف مش موحّدة — فيه
         * «شعبة  عن عاصم» بمسافتين، و«هشام عن ابي عامر» بغير همزة، و«ورش عن
         * نافع من طريق الأزرق»، وكلها لازم تقع في نفس الخانة.
         */
        fun from(moshafName: String): Riwaya {
            val n = moshafName.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            return when {
                n.contains("المجود") || n.contains("المجوّد") -> MOJAWWAD
                n.contains("المعلم") || n.contains("المعلّم") -> MOALLIM
                n.contains("ورش") -> WARSH
                n.contains("قالون") -> QALOON
                n.contains("شعبة") -> SHUBA
                n.contains("الدوري") && n.contains("الكسائي") -> DOURI_KISAI
                n.contains("الدوري") -> DOURI_AMR
                n.contains("السوسي") -> SOUSI
                n.contains("ابن ذكوان") -> IBN_THAKWAN
                n.contains("هشام") -> HISHAM
                n.contains("البزي") -> BAZZI
                n.contains("قنبل") -> QUNBUL
                n.contains("خلف") -> KHALAF
                n.contains("يعقوب") || n.contains("رويس") || n.contains("روح") -> YAQOUB
                n.contains("جماز") -> IBN_JAMMAZ
                n.contains("حفص") -> HAFS
                else -> OTHER
            }
        }
    }
}

/**
 * مصحف مسجّل لقارئ: رواية واحدة + خادم واحد + قائمة السور المتاحة فيه.
 *
 * `surahList` مش دايمًا ١١٤: فيه قرّاء مسجّلين جزء عمّ بس (٣٧ سورة)، ولو
 * عرضنا لهم السور الناقصة هيرجع الخادم 404 ويقف التشغيل.
 */
data class Moshaf(
    val id: Int,
    val reciterId: Int,
    val reciterName: String,
    val rawName: String,
    val server: String,
    val surahList: List<Int>,
    val surahTotal: Int
) {
    val riwaya: Riwaya = Riwaya.from(rawName)

    /** اسم مختصر للعرض: «حفص عن عاصم - مرتل» ← «مرتل». */
    val styleLabel: String = rawName.substringAfter(" - ", "").trim().ifBlank { rawName.trim() }

    /** مفتاح ثابت للتخزين (آخر قارئ، مجلد التنزيل). */
    val key: String get() = "m$id"

    val isComplete: Boolean get() = surahList.size >= 114

    fun hasSurah(surah: Int): Boolean = surah in surahList

    /** رابط سورة كاملة: الخادم + الرقم بثلاث خانات. */
    fun urlFor(surah: Int): String =
        server.trimEnd('/') + "/" + surah.toString().padStart(3, '0') + ".mp3"
}

/** قارئ واحد ومعه كل مصاحفه (رواياته). */
data class Reciter(
    val id: Int,
    val name: String,
    val letter: String,
    val moshafs: List<Moshaf>
) {
    val riwayat: List<Riwaya> = moshafs.map { it.riwaya }.distinct()
}

/** قارئ «آية آية» من everyayah — مجلد واحد ثابت لكل قارئ. */
data class AyahReciter(
    val id: String,
    val name: String,
    val folder: String,
    val riwaya: Riwaya,
    val styleLabel: String,
    val bitrateKbps: Int
) {
    val key: String get() = "a$id"

    /** رابط آية بعينها: `SSSAAA.mp3` — ٣ خانات للسورة و٣ للآية. */
    fun urlFor(surah: Int, ayah: Int): String =
        "https://everyayah.com/data/$folder/" +
            surah.toString().padStart(3, '0') + ayah.toString().padStart(3, '0') + ".mp3"
}

/** طريقة التشغيل اللي المستخدم اختارها. */
enum class PlaybackMode { SURAH, AYAH }

/** التكرار: مرة واحدة، أو إعادة المقطع الحالي، أو إعادة القائمة كلها. */
enum class RepeatMode { OFF, ONE, ALL }

/**
 * مقطع واحد في قائمة التشغيل — سورة كاملة أو آية.
 *
 * `id` هو `MediaItem.mediaId`، وهو اللي بيرجع من المشغّل فنعرف منه إيه اللي
 * شغّال دلوقتي من غير ما نمسك حالة موازية ممكن تختلف مع الجلسة.
 */
data class AudioTrack(
    val id: String,
    val url: String,
    val surah: Int,
    val ayah: Int?,
    val reciterName: String,
    val reciterKey: String,
    val riwayaLabel: String,
    val title: String
) {
    companion object {
        fun surahTrack(moshaf: Moshaf, surah: Int): AudioTrack = AudioTrack(
            id = "s|${moshaf.key}|$surah",
            url = moshaf.urlFor(surah),
            surah = surah,
            ayah = null,
            reciterName = moshaf.reciterName,
            reciterKey = moshaf.key,
            riwayaLabel = moshaf.riwaya.label,
            title = QuranAudioMeta.surahName(surah)
        )

        fun ayahTrack(reciter: AyahReciter, surah: Int, ayah: Int): AudioTrack = AudioTrack(
            id = "a|${reciter.folder}|$surah|$ayah",
            url = reciter.urlFor(surah, ayah),
            surah = surah,
            ayah = ayah,
            reciterName = reciter.name,
            reciterKey = reciter.key,
            riwayaLabel = reciter.riwaya.label,
            title = "${QuranAudioMeta.surahName(surah)} — آية $ayah"
        )

        /** يفكّ الـ mediaId الراجع من الجلسة. */
        fun parse(mediaId: String): ParsedId? {
            val parts = mediaId.split('|')
            return when {
                parts.size == 3 && parts[0] == "s" ->
                    ParsedId(parts[1], parts[2].toIntOrNull() ?: return null, null)

                parts.size == 4 && parts[0] == "a" ->
                    ParsedId(
                        parts[1],
                        parts[2].toIntOrNull() ?: return null,
                        parts[3].toIntOrNull() ?: return null
                    )

                else -> null
            }
        }
    }

    data class ParsedId(val source: String, val surah: Int, val ayah: Int?)
}
