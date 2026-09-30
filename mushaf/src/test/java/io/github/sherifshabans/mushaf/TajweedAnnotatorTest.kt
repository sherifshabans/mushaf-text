package io.github.sherifshabans.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * يتحقّق من مستخرج أحكام التجويد على الـ٦٢٣٦ آية.
 *
 * ١. **سلامة المواضع**: مرتّبة، بلا تداخل، جوّه النص، بتبدأ على حرف لا علامة،
 *    ومابتغطّيش مسافة — وإلا التلوين يقع على الحرف الغلط.
 * ٢. **الإظهار يفضل أسود**: النون بسكون والتنوين الجانبي علامتهم إظهار في رسم
 *    KFGQPC.
 * ٣. **أمثلة مُتحقَّقة بالعين** لكل حكم.
 */
class TajweedAnnotatorTest {

    private data class Aya(val sora: Int, val ayaNo: Int, val text: String)

    private fun allAyat(): List<Aya> = TestData.ayahs.map { Aya(it.surah, it.number, it.text) }

    private fun textOf(sora: Int, ayaNo: Int): String = TestData.ayah(sora, ayaNo).text

    private fun isMark(c: Char): Boolean =
        c in 'ً'..'ٟ' || c == 'ٰ' || c in 'ۖ'..'ۭ' || c == 'ـ'

    /** الكلمة اللي الموضع واقع فيها — للرسائل. */
    private fun wordAt(text: String, at: Int): String {
        var s = at
        while (s > 0 && text[s - 1] != ' ' && text[s - 1] != ' ') s--
        var e = at
        while (e < text.length && text[e] != ' ' && text[e] != ' ') e++
        return text.substring(s, e)
    }

    // ── ١) سلامة المواضع على المصحف كله ──────────────────────────────────────

    @Test
    fun spansAreWellFormedEverywhere() {
        var total = 0
        allAyat().forEach { aya ->
            val spans = TajweedAnnotator.annotate(aya.text, includeNaturalMadd = true)
            total += spans.size
            var previousEnd = 0
            spans.forEach { s ->
                val where = "الآية ${aya.sora}:${aya.ayaNo} [${s.start},${s.end}) ${s.rule}"
                assertTrue("$where — بداية سالبة", s.start >= 0)
                assertTrue("$where — نهاية برّه النص", s.end <= aya.text.length)
                assertTrue("$where — مدى فاضي", s.start < s.end)
                assertTrue("$where — تداخل مع اللي قبله", s.start >= previousEnd)
                assertFalse(
                    "$where — بيبدأ على علامة ضبط لا على حرف: «${wordAt(aya.text, s.start)}»",
                    isMark(aya.text[s.start])
                )
                for (k in s.start until s.end) {
                    assertFalse(
                        "$where — بيغطّي مسافة: «${wordAt(aya.text, s.start)}»",
                        aya.text[k] == ' ' || aya.text[k] == ' '
                    )
                }
                previousEnd = s.end
            }
        }
        // رقم حقيقي، بيتغيّر لو القواعد اتغيّرت — والاختبار بيطبعه عشان يتقارن.
        println("إجمالي مواضع الأحكام على المصحف كله: $total")
        assertTrue("مفيش أحكام اتّطلعت خالص", total > 50_000)
    }

    // ── ٢) الإظهار لازم يفضل بلا لون ─────────────────────────────────────────

    /**
     * النون بسكون صريح، والتنوين الجانبي — علامتهم **إظهار** في رسم KFGQPC.
     * أي حكم نون عليهم غلط.
     */
    @Test
    fun izhaarStaysUncoloured() {
        val noonRules = setOf(
            TajweedRule.IKHFA,
            TajweedRule.IDGHAM_GHUNNA,
            TajweedRule.IDGHAM_NO_GHUNNA,
            TajweedRule.IQLAB
        )
        var checked = 0
        allAyat().forEach { aya ->
            val t = aya.text
            val byIndex = HashMap<Int, TajweedRule>()
            TajweedAnnotator.annotate(t, includeNaturalMadd = true).forEach { s ->
                for (k in s.start until s.end) byIndex[k] = s.rule
            }
            for (i in t.indices) {
                if (isMark(t[i])) continue
                val marks = buildString {
                    var j = i + 1
                    while (j < t.length && isMark(t[j])) { append(t[j]); j++ }
                }
                val sukooned = t[i] == 'ن' && marks.any { it == 'ْ' || it == 'ۡ' }
                val openTanween = marks.any { it == 'ً' || it == 'ٌ' || it == 'ٍ' }
                if (!sukooned && !openTanween) continue
                checked++
                val rule = byIndex[i] ?: continue
                assertFalse(
                    "إظهار اتلوّن بحكم نون — الآية ${aya.sora}:${aya.ayaNo} " +
                        "«${wordAt(t, i)}» ← $rule",
                    rule in noonRules
                )
            }
        }
        println("مواضع إظهار اتفحصت: $checked")
        assertTrue(checked > 3_000)
    }

    // ── ٣) أمثلة مُتحقَّقة ───────────────────────────────────────────────────

    /**
     * الحكم الواقع على الموضع [at].
     *
     * المواضع بتتلاقى **بالكود** لا بكتابة كلمة عربية في الاختبار: ترميز
     * KFGQPC بيحطّ الحركة بعد الشدّة (`نَّ` = ن + شدّة + فتحة)، فأي حرف ناقص في
     * ما بنكتبه بيخلّي `indexOf` مايلاقيش حاجة — وده اللي وقّع الاختبار أول مرة.
     */
    private fun ruleAt(text: String, at: Int): TajweedRule? =
        TajweedAnnotator.annotate(text, includeNaturalMadd = true)
            .firstOrNull { at >= it.start && at < it.end }?.rule

    @Test
    fun ghunnaOnShaddedNoon() {
        // البقرة ٦ بتبدأ بـ«إِنَّ» — أول نون في الآية مشدّدة
        val t = textOf(2, 6)
        val noon = t.indexOf('ن')
        assertTrue(noon > 0)
        assertEquals('ّ', t[noon + 1])          // شدّة
        assertEquals(TajweedRule.GHUNNA, ruleAt(t, noon))
    }

    /** ميم عريانة (بلا أي علامة) بعدها مباشرةً الحرف [after]. */
    private fun bareMeemBefore(text: String, after: Char): Int =
        text.indices.first { i ->
            text[i] == 'م' &&
                (i + 1 >= text.length || !isMark(text[i + 1])) &&
                run {
                    var j = i + 1
                    while (j < text.length && (isMark(text[j]) || text[j] == ' ')) j++
                    j < text.length && text[j] == after
                }
        }

    @Test
    fun meemRulesOccur() {
        // «وَمَا هُم بِمُؤۡمِنِينَ» (البقرة ٨) — ميم عريانة قبل باء ← إخفاء شفوي
        val t8 = textOf(2, 8)
        val meem = bareMeemBefore(t8, 'ب')
        assertEquals(TajweedRule.IKHFA_SHAFAWI, ruleAt(t8, meem))

        // «قُلُوبِهِم مَّرَضٌ» (البقرة ١٠) — ميم عريانة قبل ميم مشدّدة ← إدغام شفوي
        val t10 = textOf(2, 10)
        val meem2 = bareMeemBefore(t10, 'م')
        assertEquals(TajweedRule.IDGHAM_SHAFAWI, ruleAt(t10, meem2))
    }

    @Test
    fun hamzatWaslAndShamsiLam() {
        // «ٱلرَّحۡمَٰنِ» — همزة وصل، واللام شمسية لأن الراء مشدّدة
        val t = textOf(1, 1)
        val spans = TajweedAnnotator.annotate(t)
        val wasl = t.indexOf('ٱ', t.indexOf("ٱلرَّ"))
        assertEquals(
            TajweedRule.HAMZAT_WASL,
            spans.first { wasl >= it.start && wasl < it.end }.rule
        )
        val lam = wasl + 1
        assertEquals('ل', t[lam])
        assertEquals(
            TajweedRule.LAM_SHAMSIYYA,
            spans.first { lam >= it.start && lam < it.end }.rule
        )
    }

    @Test
    fun maddLazimInFatiha() {
        // «ٱلضَّآلِّينَ» — مدّة بعدها لام مشدّدة ← ٦ حركات
        val t = textOf(1, 7)
        val at = t.indexOf('ٓ') - 1
        assertTrue(at > 0)
        assertEquals(
            TajweedRule.MADD_LAZIM,
            TajweedAnnotator.annotate(t).first { at >= it.start && at < it.end }.rule
        )
    }

    /**
     * كل حكم لازم يظهر في المصحف، وبعدد معقول.
     *
     * الغرض إن حكمًا ما يبقى ميتًا في الكود من غير ما حد ياخد باله: لو تعديل
     * صغير في القواعد صفّر حكمًا، الاختبار ده بيسقط.
     */
    @Test
    fun everyRuleOccurs() {
        val counts = HashMap<TajweedRule, Int>()
        val samples = HashMap<TajweedRule, String>()
        allAyat().forEach { aya ->
            TajweedAnnotator.annotate(
                aya.text,
                includeNaturalMadd = true,
                includeTafkhim = true
            ).forEach { s ->
                counts[s.rule] = (counts[s.rule] ?: 0) + 1
                if (s.rule !in samples) {
                    samples[s.rule] = "${aya.sora}:${aya.ayaNo} «${wordAt(aya.text, s.start)}»"
                }
            }
        }
        println("─── توزيع الأحكام على المصحف كله ───")
        TajweedRule.entries.forEach { r ->
            println("%-18s %7d   %s".format(r.name, counts[r] ?: 0, samples[r] ?: "—"))
        }
        TajweedRule.entries.forEach { r ->
            assertTrue("الحكم $r ما ظهرش ولا مرة", (counts[r] ?: 0) > 0)
        }
    }

    /** المدّ الطبيعي مقفول افتراضيًا — والفرق بينه وبين المفتوح كبير. */
    @Test
    fun naturalMaddIsOptional() {
        val ayat = allAyat()
        fun count(natural: Boolean) = ayat.sumOf { a ->
            TajweedAnnotator.annotate(a.text, includeNaturalMadd = natural)
                .count { it.rule == TajweedRule.MADD_NATURAL }
        }
        assertEquals(0, count(false))
        val on = count(true)
        println("مواضع المدّ الطبيعي: $on")
        assertTrue("المدّ الطبيعي المفروض يكون أكتر حكم", on > 10_000)
    }

    /** المواضع المتجاورة لنفس الحكم لازم تكون متضمّة في مدى واحد. */
    @Test
    fun adjacentSpansOfSameRuleAreMerged() {
        allAyat().forEach { aya ->
            val spans: List<TajweedSpan> =
                TajweedAnnotator.annotate(aya.text, includeNaturalMadd = true)
            spans.zipWithNext { a, b ->
                assertFalse(
                    "موضعان متلاصقان لنفس الحكم في ${aya.sora}:${aya.ayaNo} — ${a.rule}",
                    a.end == b.start && a.rule == b.rule
                )
            }
        }
    }
}
