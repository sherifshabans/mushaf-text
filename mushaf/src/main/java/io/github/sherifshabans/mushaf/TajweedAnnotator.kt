package io.github.sherifshabans.mushaf

/**
 * بيستخرج أحكام التجويد من نصّ الآية نفسه.
 *
 * ## ليه استخراج لا مجموعة بيانات جاهزة
 * المجموعات المنشورة (زي `cpfair/quran-tajweed`) مواضعها محسوبة على نصّ
 * **Tanzil العثماني**، ونصّنا **KFGQPC Hafs** — والاتنين بيختلفوا في ترميز
 * العلامات نفسها (السكون U+06E1 عندنا مقابل U+0652، والتنوين المركّب U+0657
 * و U+065E و U+0656). فتطبيق مواضعهم كما هي بيزحزح الألوان على حروف غلط.
 *
 * والأهم: **نصّ KFGQPC بيرمّز الأحكام في كتابته أصلًا**. اتحقّقنا من ده على
 * الـ٦٢٣٦ آية:
 *
 * | العلامة | معناها | العدد |
 * |---|---|---|
 * | نون بسكون (U+06E1/U+0652) أو تنوين جانبي (U+064B/C/D) | **إظهار** | ١٧١٦ / ١٩١١ |
 * | نون عريانة أو تنوين مركّب (U+0656/U+0657/U+065E) | إخفاء أو إدغام | ٥١٣٩ / ٦٦٤٣ |
 * | ميم صغيرة عالية أو منخفضة (U+06E2/U+06ED) | **إقلاب** | ٦٠٩ |
 * | مدّة (U+0653) | مدّ أطول من حركتين | ٥٦٥٢ |
 * | صفر مستطيل (U+06E0) | حرف لا يُنطق | ٦٦ |
 * | واو/ياء صغيرة (U+06E5/U+06E6) | **مدّ صلة** | ٢٢١٣ |
 *
 * الحرف اللي بعد النون/التنوين هو اللي بيفصل الباقي: **مشدّد** ← إدغام كامل
 * (لام وراء بغير غنّة، نون وميم بغنّة)، **ياء أو واو غير مشدّدة** ← إدغام ناقص
 * بغنّة (وده رسم حفص: الإدغام في «ي و» ناقص فما بيتشدّدش)، **أي حرف من
 * الخمستاشر** ← إخفاء. اتحقَّق: بعد تخطّي ألف مقعد التنوين، الحروف اللي بتظهر
 * هي بالظبط حروف الإخفاء زائد «و ي» للإدغام الناقص، ولا حرف غيرهم.
 *
 * فالاستخراج من نصّنا **أدقّ** من مواءمة نصّ تاني، ومعاه صفر مخاطرة إزاحة.
 *
 * ## الوقف
 * كل آية بتتحلّل لوحدها: علامة نهاية الآية وقف، فمفيش حكم بيعبر من آية للّي
 * بعدها. وآخر الآية هو اللي بيولّد **مدّ عارض للسكون**.
 */
object TajweedAnnotator {

    // ── العلامات ─────────────────────────────────────────────────────────────
    private const val SHADDA = 'ّ'
    private const val MADDAH = 'ٓ'
    private const val SUP_ALEF = 'ٰ'          // ألف خنجرية
    private const val SUBSCRIPT_ALEF = 'ٖ'    // كسرتان مركّبة
    private const val INVERTED_DAMMA = 'ٗ'    // فتحتان مركّبة
    private const val FATHA_TWO_DOTS = 'ٞ'    // ضمّتان مركّبة
    private const val HAMZA_ABOVE = 'ٔ'
    private const val HAMZA_BELOW = 'ٕ'
    private const val RECT_ZERO = '۠'         // حرف لا يُنطق
    private const val SMALL_WAW = 'ۥ'
    private const val SMALL_YEH = 'ۦ'
    private const val ALEF_WASLA = 'ٱ'
    private const val DAMMA = 'ُ'
    private const val KASRA = 'ِ'

    private val SUKUNS = charArrayOf('ْ', 'ۡ')

    /** التنوين المركّب — علامة الإخفاء والإدغام والإقلاب. */
    private val STACKED_TANWEEN = charArrayOf(SUBSCRIPT_ALEF, INVERTED_DAMMA, FATHA_TWO_DOTS)

    /** التنوين الجانبي — علامة الإظهار. */
    private val OPEN_TANWEEN = charArrayOf('ً', 'ٌ', 'ٍ')

    /** الميم الصغيرة: علامة الإقلاب. */
    private val IQLAB_MARKS = charArrayOf('ۢ', 'ۭ')

    private const val IKHFA_LETTERS = "صذثكجشقسدطزفتضظ"
    private const val SHAMSI_LETTERS = "تثدذرزسشصضطظلن"
    private const val QALQALA_LETTERS = "قطبجد"
    /**
     * همزة **القطع** وحدها.
     *
     * ألف الوصل (`ٱ`) مقصودة إنها **برّه** المجموعة: هي مش همزة قطع، فما
     * بتسبّبش مدًّا. «ٱهۡدِنَا ٱلصِّرَٰطَ» ما فيهاش مدّ منفصل — الألف بتسقط في
     * الوصل أصلًا. لمّا كانت جوّه المجموعة كان الاختبار بيطلّعها مدًّا منفصلًا
     * (٤٥٣٣ موضعًا، أولها الفاتحة ٦) وده غلط.
     *
     * والهمزة ممكن تيجي كعلامة فوق التطويل (U+0654/U+0655) — دي بتتفحص لوحدها
     * في [isHamzaAt].
     */
    private const val HAMZA_LETTERS = "ءأإؤئ"

    /** كل ما هو علامة ضبط لا حرف. */
    private fun isMark(c: Char): Boolean =
        c in 'ً'..'ٟ' || c == SUP_ALEF || c in 'ۖ'..'ۭ' || c == 'ـ'

    private fun isSpace(c: Char): Boolean = c == ' ' || c == ' '
    private fun isDigit(c: Char): Boolean = c in '٠'..'٩'

    // ── مساعدات المشي على النص ───────────────────────────────────────────────

    /** العلامات الملزوقة بالحرف اللي في [i]. */
    private fun marksAfter(t: String, i: Int): String {
        val sb = StringBuilder()
        var j = i + 1
        while (j < t.length && isMark(t[j])) { sb.append(t[j]); j++ }
        return sb.toString()
    }

    /**
     * علامات الوقف والسجدة والرُّبع — رموز قائمة بذاتها، مش حركات على الحرف.
     *
     * بتفضل بلون الحبر: هي مش جزءًا من الحكم، وتلوينها بلون الحكم بيخلّي علامة
     * الوقف تبان كأنها تابعة للحرف اللي قبلها. اتشافت في أول رسم: «غِشَٰوَةٌ»
     * طلعت ومعاها علامة الوقف اللي بعدها بلون الإدغام.
     */
    private fun isPauseSign(c: Char): Boolean =
        c in 'ۖ'..'ۜ' || c == '۞' || c in '۩'..'۬'

    /** آخر موضع في عنقود الحرف [i] — الحرف وحركاته، بدون علامات الوقف. */
    private fun clusterEnd(t: String, i: Int): Int {
        var j = i + 1
        while (j < t.length && isMark(t[j]) && !isPauseSign(t[j])) j++
        return j
    }

    /** أول حرف حقيقي بعد [i]، أو `-1`. المسافات والأرقام بتتخطّى. */
    private fun nextLetter(t: String, i: Int): Int {
        var j = i + 1
        while (j < t.length && (isMark(t[j]) || isSpace(t[j]) || isDigit(t[j]))) j++
        return if (j < t.length) j else -1
    }

    private fun prevLetter(t: String, i: Int): Int {
        var j = i - 1
        while (j >= 0 && (isMark(t[j]) || isSpace(t[j]) || isDigit(t[j]))) j--
        return j
    }

    private fun hasShadda(t: String, i: Int) = marksAfter(t, i).contains(SHADDA)
    private fun hasSukun(t: String, i: Int) = marksAfter(t, i).any { it in SUKUNS }
    private fun isBare(t: String, i: Int) = marksAfter(t, i).isEmpty()

    /** هل فيه مسافة بين [i] و [j]؟ يعني هما في كلمتين مختلفتين. */
    private fun crossesWord(t: String, i: Int, j: Int): Boolean {
        for (k in i + 1 until j) if (isSpace(t[k])) return true
        return false
    }

    /**
     * هل الحرف في [i] حرف مدّ؟
     *
     * ألف أو ألف خنجرية دائمًا، وواو بعد ضمّة، وياء بعد كسرة — والاتنين لازم
     * يكونوا بدون حركة عليهم.
     */
    private fun isMaddLetter(t: String, i: Int): Boolean {
        val c = t[i]
        if (c == SUP_ALEF) return true
        if (c == 'ا' && isBare(t, i)) return true
        if (!isBare(t, i)) return false
        return when (c) {
            'ى' -> true
            'و', 'ي' -> {
                val p = prevLetter(t, i)
                if (p < 0) false else {
                    val pm = marksAfter(t, p)
                    if (c == 'و') pm.contains(DAMMA) else pm.contains(KASRA)
                }
            }
            else -> false
        }
    }

    /** الهمزة ممكن تيجي حرفًا أو علامة فوق التطويل — الاتنين همزة. */
    private fun isHamzaAt(t: String, i: Int): Boolean =
        t[i] in HAMZA_LETTERS || marksAfter(t, i).any { it == HAMZA_ABOVE || it == HAMZA_BELOW }

    // ── الاستخراج ────────────────────────────────────────────────────────────

    /**
     * أحكام آية واحدة.
     *
     * @param text نصّ الآية ([Ayah.text]) — بدون رقم الآية في آخره.
     * @param includeNaturalMadd المدّ الطبيعي أكتر حكم تكرارًا، فله مفتاح منفصل.
     * @return مواضع مرتّبة بالبداية، ومفيش تداخل بينها.
     */
    fun annotate(text: String, includeNaturalMadd: Boolean = false): List<TajweedSpan> {
        if (text.isEmpty()) return emptyList()
        val rules = arrayOfNulls<TajweedRule>(text.length)

        /** بيحطّ الحكم على الحرف وعلاماته، ومابيدوسش على حكم موجود. */
        fun mark(i: Int, rule: TajweedRule) {
            if (i < 0 || i >= text.length || rules[i] != null) return
            val end = clusterEnd(text, i)
            for (k in i until end) if (rules[k] == null) rules[k] = rule
        }

        // ١) ما لا يُنطق — الأول، عشان ما يتلوّنش بحكم تاني
        for (i in text.indices) {
            when (text[i]) {
                RECT_ZERO -> {
                    rules[i] = TajweedRule.SILENT
                    val p = prevLetter(text, i)
                    if (p >= 0) {
                        for (k in p until i) if (rules[k] == null) rules[k] = TajweedRule.SILENT
                    }
                }

                ALEF_WASLA -> {
                    mark(i, TajweedRule.HAMZAT_WASL)
                    // لام شمسية: ٱ + ل + حرف شمسي مشدّد ← اللام ساكتة
                    val lam = i + 1
                    if (lam < text.length && text[lam] == 'ل') {
                        val k = nextLetter(text, lam)
                        if (k > 0 && text[k] in SHAMSI_LETTERS && hasShadda(text, k)) {
                            mark(lam, TajweedRule.LAM_SHAMSIYYA)
                        }
                    }
                }
            }
        }

        // ٢) مدّ الصلة — الواو والياء الصغيرتان
        for (i in text.indices) {
            if (text[i] == SMALL_WAW || text[i] == SMALL_YEH) {
                rules[i] = TajweedRule.MADD_SILA
                val p = prevLetter(text, i)
                if (p >= 0 && rules[p] == null) mark(p, TajweedRule.MADD_SILA)
            }
        }

        // ٣) أحكام النون والتنوين والميم، والغنّة، والقلقلة
        for (i in text.indices) {
            if (isMark(text[i]) || isSpace(text[i]) || isDigit(text[i])) continue
            if (rules[i] != null) continue
            val c = text[i]
            val ms = marksAfter(text, i)

            // الإقلاب معلَّم صريحًا بالميم الصغيرة
            if (ms.any { it in IQLAB_MARKS }) { mark(i, TajweedRule.IQLAB); continue }

            // غنّة مشدّدة
            if ((c == 'ن' || c == 'م') && ms.contains(SHADDA)) {
                mark(i, TajweedRule.GHUNNA); continue
            }

            val openTanween = ms.any { it in OPEN_TANWEEN }
            val stacked = ms.any { it in STACKED_TANWEEN }
            val bareNoon = c == 'ن' && ms.isEmpty()
            if (stacked || bareNoon) {
                var k = nextLetter(text, i)
                // التنوين المركّب مرسوم قبل ألف مقعده — نتخطّاها
                if (stacked && k > 0 && (text[k] == 'ا' || text[k] == 'ى') && isBare(text, k)) {
                    k = nextLetter(text, k)
                }
                if (k > 0) {
                    val f = text[k]
                    val rule = when {
                        f == 'ب' -> TajweedRule.IQLAB
                        hasShadda(text, k) && (f == 'ل' || f == 'ر') -> TajweedRule.IDGHAM_NO_GHUNNA
                        hasShadda(text, k) && (f == 'ن' || f == 'م') -> TajweedRule.IDGHAM_GHUNNA
                        // إدغام ناقص: الياء والواو ما بيتشدّدوش في رسم حفص
                        f == 'ي' || f == 'و' -> TajweedRule.IDGHAM_GHUNNA
                        f in IKHFA_LETTERS -> TajweedRule.IKHFA
                        else -> null
                    }
                    if (rule != null) { mark(i, rule); continue }
                }
            }
            if (openTanween) continue   // إظهار — لا لون

            // ميم ساكنة: إخفاء شفوي / إدغام شفوي.
            //
            // الميم الساكنة **عريانة** في رسم KFGQPC زي النون بالظبط — السكون
            // ما بيترسمش عليها إلا في الإظهار الشفوي. اتحقَّق: الميم اللي بعدها
            // باء عريانة في ٤٨١ موضعًا، واللي بعدها ميم مشدّدة عريانة في ٨٢٢ —
            // ولا واحدة فيهم عليها سكون. فلو اشترطنا السكون، الحكمين ما
            // بيطلعوش ولا مرة (وده اللي حصل فعلًا في أول تشغيل).
            if (c == 'م' && ms.isEmpty()) {
                val k = nextLetter(text, i)
                if (k > 0) {
                    if (text[k] == 'ب') { mark(i, TajweedRule.IKHFA_SHAFAWI); continue }
                    if (text[k] == 'م' && hasShadda(text, k)) {
                        mark(i, TajweedRule.IDGHAM_SHAFAWI); continue
                    }
                }
            }

            // قلقلة
            if (c in QALQALA_LETTERS && hasSukun(text, i)) {
                mark(i, TajweedRule.QALQALA); continue
            }
        }

        // ٤) المدود المعلَّمة بالمدّة
        for (i in text.indices) {
            if (isMark(text[i]) || rules[i] != null) continue
            if (!marksAfter(text, i).contains(MADDAH)) continue
            // الهمزة حاملة المدّة ← مدّ بدل (حركتان)، زي «ٱلۡأٓخِرَة»
            if (text[i] in HAMZA_LETTERS) { mark(i, TajweedRule.MADD_BADAL); continue }
            val k = nextLetter(text, i)
            val rule = when {
                k < 0 -> TajweedRule.MADD_LAZIM
                hasShadda(text, k) || hasSukun(text, k) -> TajweedRule.MADD_LAZIM
                isHamzaAt(text, k) ->
                    if (crossesWord(text, i, k)) TajweedRule.MADD_MUNFASIL
                    else TajweedRule.MADD_MUTTASIL
                // فواتح السور: المدّة على حرف مقطّع ← مدّ لازم حرفي
                else -> TajweedRule.MADD_LAZIM
            }
            mark(i, rule)
        }

        // ٥) مدّ منفصل بغير مدّة: حرف مدّ في آخر الكلمة وبعده همزة
        for (i in text.indices) {
            if (rules[i] != null || isMark(text[i]) || isSpace(text[i])) continue
            if (!isMaddLetter(text, i)) continue
            val k = nextLetter(text, i)
            if (k > 0 && isHamzaAt(text, k) && crossesWord(text, i, k)) {
                mark(i, TajweedRule.MADD_MUNFASIL)
            }
        }

        // ٦) مدّ عارض للسكون — آخر الآية وقف، فالحرف الأخير يُسكَّن
        val last = prevLetter(text, text.length)
        if (last >= 0) {
            val p = prevLetter(text, last)
            if (p >= 0 && rules[p] == null && isMaddLetter(text, p)) {
                mark(p, TajweedRule.MADD_ARID)
            }
            // قلقلة الوقف: آخر حرف من «قطب جد» يُقلقَل عند الوقف
            if (rules[last] == null && text[last] in QALQALA_LETTERS && isBare(text, last)) {
                mark(last, TajweedRule.QALQALA)
            }
        }

        // ٧) المدّ الطبيعي — كل ما تبقّى من حروف المدّ
        if (includeNaturalMadd) {
            for (i in text.indices) {
                if (rules[i] != null || isMark(text[i]) || isSpace(text[i])) continue
                if (isMaddLetter(text, i)) mark(i, TajweedRule.MADD_NATURAL)
            }
        }

        return toSpans(rules)
    }

    /** بيضمّ الخانات المتجاورة اللي ليها نفس الحكم في مدى واحد. */
    private fun toSpans(rules: Array<TajweedRule?>): List<TajweedSpan> {
        val out = ArrayList<TajweedSpan>()
        var i = 0
        while (i < rules.size) {
            val r = rules[i]
            if (r == null) { i++; continue }
            var j = i
            while (j < rules.size && rules[j] == r) j++
            out += TajweedSpan(i, j, r)
            i = j
        }
        return out
    }
}
