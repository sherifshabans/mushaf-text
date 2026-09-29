package io.github.sherifshabans.mushaf

import androidx.compose.ui.graphics.Color

/**
 * أحكام التجويد المعروضة على صفحة المصحف: لون لكل حكم، واسمه وشرحه.
 *
 * ## ليه لون لكل حكم لا لون لكل عائلة
 * تجميع أحكام النون كلها في لون واحد بيوفّر ألوان لكن بيضيّع المعلومة: القارئ
 * عايز يعرف إن ده **إخفاء** لا **إدغام**، لأن النطق مختلف. فكل حكم بلونه،
 * والشرح باللمس ([TajweedRule.definition]) بيشيل عن القارئ حفظ المفتاح.
 *
 * ## الألوان
 * المدود ماشية على سلّم **الطول**: أخضر (حركتان) ← زيتوني/أخضر غامق (٤–٥) ←
 * برتقالي (عارض) ← أحمر (٦ حركات). ده الترتيب الطبيعي في المصاحف الملوّنة
 * والقارئ بيتعلّمه بالدرجة مش بالحفظ.
 *
 * أحكام النون والميم متفرّقة على عجلة الألوان عمدًا، **لا** متدرّجة في عائلة
 * واحدة: [IDGHAM_GHUNNA] و [IDGHAM_NO_GHUNNA] بيقعوا جنب بعض في نفس الكلمة
 * كتير، فلو كانوا درجتين من نفس اللون كان الفرق بينهم بيضيع على الشاشة.
 *
 * و[IDGHAM_GHUNNA] تحديدًا اتنقل **مرتين**: كان أزرق زاهي، وده وقع جنب أزرق
 * [HAMZAT_WASL] اللي بيتكرر ١٨ ألف مرة على الصفحة فبقى اللبس وارد. دلوقتي هو
 * **ماجنتا**، على بُعد ١٠٥ درجات من الأزرق — ومعاه فايدة تانية: بقى في نفس
 * عائلة [GHUNNA] اللونية، وده صحيح معنى (الاتنين غنّة) فالقارئ بيربطهم من غير
 * ما يحفظ. والإدغام بغير غنّة فضل **تركوازي** بعيد عن الاتنين.
 *
 * وما لا يُنطق كان رماديًا باهتًا في الأول، والصفحة بانت مغسولة: «ٱل» لوحدها
 * بتتكرر قرابة ١٨ ألف مرة، فأي لون باهت عليها بيحكم على شكل الصفحة كلها. بقى
 * **أزرق رمادي واضح** — ظاهر زي أي حكم تاني، ومطفي بالقدر اللي يفرّقه عن
 * الأحكام المنطوقة.
 */
enum class TajweedRule(
    val color: Color,
    /** اسم الحكم كما في كتب التجويد. */
    val label: String,
    /** العائلة — للترتيب في مفتاح الألوان. */
    val family: TajweedFamily,
    val definition: String,
    /** المقدار أو الكيفية. */
    val amount: String,
    /** حروف الحكم، للعرض في ورقة الشرح. */
    val letters: String
) {
    // ── الغنّة ───────────────────────────────────────────────────────────────
    GHUNNA(
        color = Color(0xFFA81D5D),
        label = "غنّة مشدّدة",
        family = TajweedFamily.GHUNNA,
        definition = "النون والميم المشدّدتان تُغنّان — صوت يخرج من الخيشوم.",
        amount = "حركتان",
        letters = "نّ  مّ"
    ),

    // ── أحكام النون الساكنة والتنوين ─────────────────────────────────────────
    IKHFA(
        color = Color(0xFF7A3DAE),
        label = "إخفاء حقيقي",
        family = TajweedFamily.NOON,
        definition = "نون ساكنة أو تنوين بعدها أحد حروف الإخفاء، " +
            "فتُخفى النون بين الإظهار والإدغام مع الغنّة.",
        amount = "حركتان",
        letters = "ص ذ ث ك ج ش ق س د ط ز ف ت ض ظ"
    ),
    IDGHAM_GHUNNA(
        color = Color(0xFFB5179E),
        label = "إدغام بغنّة",
        family = TajweedFamily.NOON,
        definition = "نون ساكنة أو تنوين بعدها أحد حروف «يَنْمُو»، " +
            "فتُدغم النون في الحرف مع بقاء الغنّة.",
        amount = "حركتان",
        letters = "ي ن م و"
    ),
    IDGHAM_NO_GHUNNA(
        color = Color(0xFF0F766E),
        label = "إدغام بغير غنّة",
        family = TajweedFamily.NOON,
        definition = "نون ساكنة أو تنوين بعدها لام أو راء، فتُدغم النون فيها بلا غنّة.",
        amount = "بلا غنّة",
        letters = "ل ر"
    ),
    IQLAB(
        color = Color(0xFF92400E),
        label = "إقلاب",
        family = TajweedFamily.NOON,
        definition = "نون ساكنة أو تنوين بعدها باء، " +
            "فتُقلب النون ميمًا مخفاة عند الباء مع الغنّة.",
        amount = "حركتان",
        letters = "ب"
    ),

    // ── أحكام الميم الساكنة ──────────────────────────────────────────────────
    IKHFA_SHAFAWI(
        color = Color(0xFFC04A8A),
        label = "إخفاء شفوي",
        family = TajweedFamily.MEEM,
        definition = "ميم ساكنة بعدها باء، فتُخفى الميم عند الباء مع الغنّة — " +
            "والشفتان لا تنطبقان انطباقًا تامًّا.",
        amount = "حركتان",
        letters = "ب"
    ),
    IDGHAM_SHAFAWI(
        color = Color(0xFF5B21B6),
        label = "إدغام شفوي",
        family = TajweedFamily.MEEM,
        definition = "ميم ساكنة بعدها ميم، فتُدغم الأولى في الثانية مع الغنّة " +
            "(إدغام متماثلين صغير).",
        amount = "حركتان",
        letters = "م"
    ),

    // ── القلقلة ──────────────────────────────────────────────────────────────
    QALQALA(
        color = Color(0xFF8A6A12),
        label = "قلقلة",
        family = TajweedFamily.QALQALA,
        definition = "حروف «قُطْبُ جَدٍّ» إذا سكنت، يهتزّ المخرج عند النطق بها " +
            "فيُسمع لها نبرة.",
        amount = "نبرة خفيفة",
        letters = "ق ط ب ج د"
    ),

    // ── المدود ───────────────────────────────────────────────────────────────
    MADD_NATURAL(
        color = Color(0xFF3E8E6E),
        label = "مدّ طبيعي",
        family = TajweedFamily.MADD,
        definition = "حرف مدّ لا يأتي بعده همزة ولا سكون — " +
            "وهو أصل المدّ الذي لا تقوم ذات الحرف دونه.",
        amount = "حركتان",
        letters = "ا و ي"
    ),
    MADD_MUTTASIL(
        color = Color(0xFF1B6E4B),
        label = "مدّ واجب متّصل",
        family = TajweedFamily.MADD,
        definition = "حرف مدّ بعده همزة في الكلمة نفسها، فالمدّ واجب.",
        amount = "٤ أو ٥ حركات",
        letters = "مدّ + ء"
    ),
    MADD_MUNFASIL(
        color = Color(0xFF6E8B18),
        label = "مدّ جائز منفصل",
        family = TajweedFamily.MADD,
        definition = "حرف مدّ في آخر الكلمة وبعده همزة في أول الكلمة التالية.",
        amount = "٤ أو ٥ حركات",
        letters = "مدّ | ء"
    ),
    MADD_LAZIM(
        color = Color(0xFFB3261E),
        label = "مدّ لازم",
        family = TajweedFamily.MADD,
        definition = "حرف مدّ بعده سكون أصلي أو حرف مشدّد، فيلزم إشباع المدّ. " +
            "ويدخل فيه المدّ اللازم الحرفي في فواتح السور.",
        amount = "٦ حركات",
        letters = "مدّ + سكون أو شدّة"
    ),
    MADD_BADAL(
        color = Color(0xFF57A07C),
        label = "مدّ بدل",
        family = TajweedFamily.MADD,
        definition = "همزة بعدها حرف مدّ، فتَقدَّمت الهمزة على المدّ — " +
            "أصله همزتان أُبدلت الثانية مدًّا.",
        amount = "حركتان",
        letters = "ء + مدّ"
    ),
    MADD_SILA(
        color = Color(0xFF2C7F6A),
        label = "مدّ صلة",
        family = TajweedFamily.MADD,
        definition = "هاء الضمير بين متحرّكين تُوصَل بواو أو ياء صغيرة — " +
            "صغرى بحركتين، وكبرى إذا جاءت بعدها همزة.",
        amount = "حركتان، أو ٤–٥ مع الهمزة",
        letters = "ـهُۥ  ـهِۦ"
    ),
    MADD_ARID(
        color = Color(0xFFD97706),
        label = "مدّ عارض للسكون",
        family = TajweedFamily.MADD,
        definition = "حرف مدّ بعده حرف يُسكَّن من أجل الوقف، فالسكون عارض غير أصلي.",
        amount = "٢ أو ٤ أو ٦ حركات",
        letters = "مدّ + حرف موقوف"
    ),

    // ── ما لا يُنطق ──────────────────────────────────────────────────────────
    HAMZAT_WASL(
        color = Color(0xFF2F6FA8),
        label = "همزة وصل",
        family = TajweedFamily.MUTE,
        definition = "همزة تُنطق إذا بدأت بها، وتسقط من اللفظ إذا وصلتها بما قبلها.",
        amount = "تسقط في الوصل",
        letters = "ٱ"
    ),
    LAM_SHAMSIYYA(
        color = Color(0xFF4E8CC0),
        label = "لام شمسية",
        family = TajweedFamily.MUTE,
        definition = "لام «الـ» لا تُنطق، ويُدغم ما بعدها فيُنطق مشدّدًا.",
        amount = "لا تُنطق",
        letters = "ت ث د ذ ر ز س ش ص ض ط ظ ل ن"
    ),
    SILENT(
        color = Color(0xFF2F6FA8),
        label = "حرف لا يُنطق",
        family = TajweedFamily.MUTE,
        definition = "حرف مرسوم في المصحف غير ملفوظ، وعليه الصفر المستدير علامةً على ذلك.",
        amount = "لا يُنطق",
        letters = "ا و ي"
    );

    /**
     * `true` للمدّ الطبيعي وحده.
     *
     * هو أكتر حكم تكرارًا في المصحف بفارق كبير، فتلوينه بيصبغ الصفحة كلها أخضر
     * ويغطّي على باقي الأحكام. فله مفتاح منفصل — شوف
     * `naturalMadd` في [MushafPage].
     */
    val isNaturalMadd: Boolean get() = this == MADD_NATURAL

    /** The rule's name in English, for non-Arabic UIs. */
    val englishName: String
        get() = when (this) {
            GHUNNA -> "Ghunnah"
            IKHFA -> "Ikhfa'"
            IDGHAM_GHUNNA -> "Idgham with ghunnah"
            IDGHAM_NO_GHUNNA -> "Idgham without ghunnah"
            IQLAB -> "Iqlab"
            IKHFA_SHAFAWI -> "Ikhfa' shafawi"
            IDGHAM_SHAFAWI -> "Idgham shafawi"
            QALQALA -> "Qalqalah"
            MADD_NATURAL -> "Natural madd"
            MADD_MUTTASIL -> "Madd muttasil"
            MADD_MUNFASIL -> "Madd munfasil"
            MADD_LAZIM -> "Madd lazim"
            MADD_BADAL -> "Madd badal"
            MADD_SILA -> "Madd silah"
            MADD_ARID -> "Madd 'arid lis-sukun"
            HAMZAT_WASL -> "Hamzat al-wasl"
            LAM_SHAMSIYYA -> "Lam shamsiyyah"
            SILENT -> "Silent letter"
        }
}

/** عائلة الحكم — بترتّب مفتاح الألوان بس، مالهاش أثر على الرسم. */
enum class TajweedFamily(val label: String) {
    GHUNNA("الغنّة"),
    NOON("أحكام النون الساكنة والتنوين"),
    MEEM("أحكام الميم الساكنة"),
    QALQALA("القلقلة"),
    MADD("المدود"),
    MUTE("ما لا يُنطق")
}

/**
 * موضع حكم في نص آية: `[start, end)` بوحدات الحرف (UTF-16) داخل `aya_text`.
 *
 * المدى بيشمل **الحرف وعلاماته** مع بعض: تلوين الحرف وسيب حركته بلون الحبر
 * بيطلّع حرفًا بلونين، وده أسوأ من ما نلوّنش.
 */
data class TajweedSpan(
    val start: Int,
    val end: Int,
    val rule: TajweedRule
)
