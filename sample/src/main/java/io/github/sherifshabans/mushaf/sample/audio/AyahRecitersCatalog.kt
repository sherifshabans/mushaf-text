package io.github.sherifshabans.mushaf.sample.audio

/**
 * قرّاء «آية آية».
 *
 * ## ليه مكتوبين هنا بدل ما يتقروا من ملف
 * فهرس `reciters.json` (mp3quran) بيدّي **ملف لكل سورة** بس — مافيهوش أي رابط
 * لآية مفردة. تشغيل الآية الواحدة مصدره تاني خالص: everyayah.com، وهو مالوش
 * API فهرس، مجرد مجلدات ثابتة اسمها `{القارئ}_{البِتريت}` وجوّه كل مجلد ٦٢٣٦
 * ملف باسم `SSSAAA.mp3`.
 *
 * فالقايمة دي **متأكَّد منها واحدة واحدة** بطلب HTTP فعلي على المجلد
 * (`001001.mp3`)، ومكتوب معاها البِتريت زي ما هو في اسم المجلد. أي إضافة
 * جديدة لازم تتأكّد بنفس الطريقة قبل ما تتحط، لأن المجلد الغلط بيدّي 404
 * لكل آية فيقف التشغيل من غير سبب ظاهر للمستخدم.
 */
object AyahRecitersCatalog {

    val all: List<AyahReciter> = listOf(
        // ── حفص عن عاصم — مرتل ────────────────────────────────────────────
        r("alafasy", "مشاري راشد العفاسي", "Alafasy_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("husary", "محمود خليل الحصري", "Husary_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("abdulbasit", "عبد الباسط عبد الصمد", "Abdul_Basit_Murattal_192kbps", Riwaya.HAFS, "مرتل", 192),
        r("minshawi", "محمد صديق المنشاوي", "Minshawy_Murattal_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("sudais", "عبد الرحمن السديس", "Abdurrahmaan_As-Sudais_192kbps", Riwaya.HAFS, "مرتل", 192),
        r("shuraim", "سعود الشريم", "Saood_ash-Shuraym_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("maher", "ماهر المعيقلي", "MaherAlMuaiqly128kbps", Riwaya.HAFS, "مرتل", 128),
        r("ghamdi", "سعد الغامدي", "Ghamadi_40kbps", Riwaya.HAFS, "مرتل", 40),
        r("shatri", "أبو بكر الشاطري", "Abu_Bakr_Ash-Shaatree_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("juhany", "عبد الله عواد الجهني", "Abdullaah_3awwaad_Al-Juhaynee_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("dossari_y", "ياسر الدوسري", "Yasser_Ad-Dussary_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("qatami", "ناصر القطامي", "Nasser_Alqatami_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("hudhaify", "علي بن عبد الرحمن الحذيفي", "Hudhaify_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("ajamy", "أحمد بن علي العجمي", "Ahmed_ibn_Ali_al_Ajamy_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("basfar", "عبد الله بصفر", "Abdullah_Basfar_192kbps", Riwaya.HAFS, "مرتل", 192),
        r("rifai", "هاني الرفاعي", "Hani_Rifai_192kbps", Riwaya.HAFS, "مرتل", 192),
        r("ayyub", "محمد أيوب", "Muhammad_Ayyoub_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("jibreel", "محمد جبريل", "Muhammad_Jibreel_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("tablawi", "محمد الطبلاوي", "Mohammad_al_Tablaway_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("banna", "محمود علي البنا", "Mahmoud_Ali_Al_Banna_32kbps", Riwaya.HAFS, "مرتل", 32),
        r("alijaber", "علي جابر", "Ali_Jaber_64kbps", Riwaya.HAFS, "مرتل", 64),
        r("abbad", "فارس عباد", "Fares_Abbad_64kbps", Riwaya.HAFS, "مرتل", 64),
        r("bukhatir", "صلاح بو خاطر", "Salaah_AbdulRahman_Bukhatir_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("qahtani", "خالد عبد الله القحطاني", "Khaalid_Abdullaah_al-Qahtaanee_192kbps", Riwaya.HAFS, "مرتل", 192),
        r("qasim", "محسن القاسم", "Muhsin_Al_Qasim_192kbps", Riwaya.HAFS, "مرتل", 192),
        r("budair", "صلاح البدير", "Salah_Al_Budair_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("akhdar", "إبراهيم الأخضر", "Ibrahim_Akhdar_32kbps", Riwaya.HAFS, "مرتل", 32),
        r("alaqimy", "أكرم العلاقمي", "Akram_AlAlaqimy_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("salamah", "ياسر سلامة", "Yaser_Salamah_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("abdulkareem", "محمد عبد الكريم", "Muhammad_AbdulKareem_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("alili", "عزيز عليلي", "aziz_alili_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("sahl", "سهل ياسين", "Sahl_Yassin_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("matroud", "عبد الله المطرود", "Abdullah_Matroud_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("neana", "أحمد نعينع", "Ahmed_Neana_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("suesy", "علي حجاج السويسي", "Ali_Hajjaj_AlSuesy_128kbps", Riwaya.HAFS, "مرتل", 128),
        r("tunaiji", "خليفة الطنيجي", "Khalefa_Al_Tunaiji_64kbps", Riwaya.HAFS, "مرتل", 64),
        r("nabil", "نبيل الرفاعي", "Nabil_Rifa3i_48kbps", Riwaya.HAFS, "مرتل", 48),
        r("sowaid", "أيمن سويد", "Ayman_Sowaid_64kbps", Riwaya.HAFS, "مرتل مع بيان الأحكام", 64),

        // ── المصحف المجوّد ────────────────────────────────────────────────
        r("abdulbasit_moj", "عبد الباسط عبد الصمد", "Abdul_Basit_Mujawwad_128kbps", Riwaya.MOJAWWAD, "مجوّد", 128),
        r("minshawi_moj", "محمد صديق المنشاوي", "Minshawy_Mujawwad_192kbps", Riwaya.MOJAWWAD, "مجوّد", 192),
        r("husary_moj", "محمود خليل الحصري", "Husary_128kbps_Mujawwad", Riwaya.MOJAWWAD, "مجوّد", 128),
        r("abdulsamad_qe", "عبد الباسط عبد الصمد", "AbdulSamad_64kbps_QuranExplorer.Com", Riwaya.MOJAWWAD, "مجوّد", 64),

        // ── المصحف المعلّم ────────────────────────────────────────────────
        r("husary_mo", "محمود خليل الحصري", "Husary_Muallim_128kbps", Riwaya.MOALLIM, "المصحف المعلّم", 128),

        // ── ورش عن نافع ──────────────────────────────────────────────────
        r("warsh_dosary", "إبراهيم الدوسري", "warsh/warsh_ibrahim_aldosary_128kbps", Riwaya.WARSH, "مرتل", 128),
        r("warsh_jazaery", "ياسين الجزائري", "warsh/warsh_yassin_al_jazaery_64kbps", Riwaya.WARSH, "مرتل", 64)
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String?): AyahReciter? = id?.let { byId[it] }

    /** الافتراضي أول ما يفتح المستخدم التشغيل بالآية. */
    val default: AyahReciter get() = byId["husary"] ?: all.first()

    /** الروايات الموجودة فعلًا في القائمة — للفلترة في الواجهة. */
    val riwayat: List<Riwaya> get() = all.map { it.riwaya }.distinct()

    private fun r(
        id: String,
        name: String,
        folder: String,
        riwaya: Riwaya,
        style: String,
        bitrate: Int
    ) = AyahReciter(id, name, folder, riwaya, style, bitrate)
}
