package io.github.sherifshabans.mushaf

import android.content.Context
import java.io.Reader

/*
 * تخطيط سطور مصحف المدينة.
 *
 * المصدر الأساسي هو `assets/mushaf/layout.txt` — عدد الكلمات على كل سطر في كل
 * صفحة، مبني من رقم السطر لكل كلمة في بيانات quran.com (نفس البيانات اللي صور
 * صفحات المصحف مرسومة منها). فالتوزيع **مطابق** للمطبوع، مش مخمَّن.
 *
 * `line_start`/`line_end` في النص مجرّد تلميح للمسار الاحتياطي [breakIntoLines]:
 * بيثبّتوا سطر علامة الآية بس، وغلط في آيات على حدود الصفحات (المعارج ٤٠ مكتوب
 * لها سطر ١٥ وهي على سطر ١ من صفحة ٥٧٠).
 *
 * الصيغة: `page:lineCount:tokensPerLine:reservedRoles:lineFill`
 *  - tokensPerLine مفصولة بفاصلة، الصفر = سطر محجوز (بانر/بسملة)
 *  - reservedRoles مفصولة بـ`;`: `line,b|m,sura` (b = بانر، m = بسملة)
 *  - lineFill حرف لكل سطر: `j` ممدود، `c` متوسّط، `-` محجوز — **مقيس من صور
 *    المصحف**: الممدود ≥ ٠٫٩٦ من عمود النص والمتوسّط ≤ ٠٫٨٣، ومفيش بينهم.
 */

/** كلمة واحدة (أو علامة نهاية آية) على سطر، مع الآية اللي هي تابعة لها. */
internal data class MushafToken(
    val text: String,
    val ayahId: Int,
    /** `true` لو ده علامة نهاية الآية (الوردة والرقم) مش كلمة. */
    val isEndMark: Boolean,
    /**
     * موضع أول حرف في الكلمة داخل نصّ آيتها، أو `-1` لعلامة نهاية الآية.
     * أحكام التجويد بتتحسب على الآية كاملة (فيه أحكام بتعبر حدود الكلمات)،
     * والرقم ده هو اللي بيترجمها لمواضع جوّه الكلمة.
     */
    val charStart: Int = -1,
    val minLine: Int,
    val maxLine: Int
)

/** اللي بيتحط على سطر محجوز. */
internal sealed interface ReservedRole {
    val surah: Int

    data class Banner(override val surah: Int) : ReservedRole
    data class Basmala(override val surah: Int) : ReservedRole
}

internal class MushafPageLines(
    /** ١٥، وثمانية في الفاتحة وأول البقرة. */
    val lineCount: Int,
    val tokensPerLine: IntArray,
    /**
     * المصحف بيحطّ البانر في السطر اللي قبل أول آية مباشرةً **حتى لو ده في
     * الصفحة اللي قبلها**: بانر النساء على صفحة ٧٦ سطر ١٥ وبسملتها على ٧٧ سطر ١.
     */
    val reservedRoles: Map<Int, ReservedRole>,
    val centredLines: Set<Int>
) {
    val reservedLines: List<Int> =
        (1..lineCount).filter { tokensPerLine.getOrElse(it - 1) { 0 } == 0 }

    val totalTokens: Int get() = tokensPerLine.sum()

    /**
     * بيقسّم توكِنز الصفحة على سطورها، أو `null` لو العدد مش مطابق — وساعتها
     * بنرجع للكسر التقديري.
     */
    fun sliceTokens(tokens: List<MushafToken>): Map<Int, List<MushafToken>>? {
        if (tokens.size != totalTokens) return null
        val out = LinkedHashMap<Int, List<MushafToken>>()
        var at = 0
        tokensPerLine.forEachIndexed { index, n ->
            if (n > 0) {
                out[index + 1] = tokens.subList(at, at + n)
                at += n
            }
        }
        return out
    }
}

internal object MushafLineIndex {

    private const val ASSET = "mushaf/layout.txt"

    @Volatile
    private var cache: Map<Int, MushafPageLines>? = null

    fun forPage(context: Context, page: Int): MushafPageLines? = load(context)[page]

    private fun load(context: Context): Map<Int, MushafPageLines> {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: runCatching {
                context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use(::parse)
            }.getOrElse { emptyMap() }.also { cache = it }
        }
    }

    fun parse(reader: Reader): Map<Int, MushafPageLines> {
        val out = HashMap<Int, MushafPageLines>(Quran.PAGE_COUNT)
        reader.buffered().useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach
                val parts = line.split(':')
                if (parts.size != 5) return@forEach
                val page = parts[0].toIntOrNull() ?: return@forEach
                val count = parts[1].toIntOrNull() ?: return@forEach
                val counts = parts[2].split(',').map { it.trim().toIntOrNull() ?: 0 }
                if (counts.size != count) return@forEach
                val roles = HashMap<Int, ReservedRole>()
                parts[3].split(';').forEach { entry ->
                    if (entry.isBlank()) return@forEach
                    val f = entry.split(',')
                    if (f.size != 3) return@forEach
                    val at = f[0].toIntOrNull() ?: return@forEach
                    val surah = f[2].toIntOrNull() ?: return@forEach
                    roles[at] = when (f[1]) {
                        "b" -> ReservedRole.Banner(surah)
                        "m" -> ReservedRole.Basmala(surah)
                        else -> return@forEach
                    }
                }
                val centred = parts[4].withIndex()
                    .filter { (_, c) -> c == 'c' }
                    .map { (i, _) -> i + 1 }
                    .toSet()
                out[page] = MushafPageLines(count, counts.toIntArray(), roles, centred)
            }
        }
        return out
    }
}

/**
 * آيات الصفحة → توكِنز مرتّبة، والسطور اللي عليها آيات.
 *
 * بنمشي على النص بالمواضع لا بـ`split` عشان كل كلمة تعرف مكانها في آيتها
 * (`charStart`). والكلمة بعلاماتها كما هي — علامات الوقف والتشكيل ملزوقة
 * بكلمتها والخط هو اللي بيرصّها. تفصيلها بيدّي مسافة كلمة كاملة قبلها.
 */
internal fun buildPageTokens(
    ayahs: List<Ayah>,
    markerTextOf: (number: Int) -> String
): Pair<List<MushafToken>, List<Int>> {
    val tokens = ArrayList<MushafToken>()
    val covered = HashSet<Int>()

    ayahs.forEach { ayah ->
        (ayah.lineStart..ayah.lineEnd).forEach(covered::add)
        val text = ayah.text
        var at = 0
        while (at < text.length) {
            if (text[at] == ' ') { at++; continue }
            var end = at
            while (end < text.length && text[end] != ' ') end++
            tokens += MushafToken(
                text = text.substring(at, end),
                ayahId = ayah.id,
                isEndMark = false,
                charStart = at,
                minLine = ayah.lineStart,
                maxLine = ayah.lineEnd
            )
            at = end
        }
        tokens += MushafToken(
            text = markerTextOf(ayah.number),
            ayahId = ayah.id,
            isEndMark = true,
            minLine = ayah.lineEnd,
            maxLine = ayah.lineEnd
        )
    }
    return tokens to covered.sorted()
}

/**
 * المسار الاحتياطي: توزيع التوكِنز على السطور ببرمجة ديناميكية بتقلّل مجموع
 * مربّعات الفراغ المتبقّي (زي Knuth–Plass)، مع قيد إن كل توكِن يقع جوّه سطور
 * آيته. بيشتغل بس لو [MushafLineIndex] ناقص أو عدد التوكِنز مش مطابق له.
 *
 * @return محتوى كل سطر، أو `null` لو مفيش توزيع ممكن بالعرض ده.
 */
internal fun breakIntoLines(
    tokens: List<MushafToken>,
    availableLines: List<Int>,
    lineWidth: Float,
    spaceWidth: Float,
    widthOf: (MushafToken) -> Float,
    respectAyahLines: Boolean = true
): Map<Int, List<MushafToken>>? {
    val n = tokens.size
    val m = availableLines.size
    if (n == 0 || m == 0) return null

    val inf = Float.MAX_VALUE
    val cost = Array(m + 1) { FloatArray(n + 1) { inf } }
    val from = Array(m + 1) { IntArray(n + 1) { -1 } }
    cost[0][0] = 0f

    for (k in 0 until m) {
        val line = availableLines[k]
        for (i in 0..n) {
            val soFar = cost[k][i]
            if (soFar == inf) continue
            var width = 0f
            for (j in i until n) {
                val token = tokens[j]
                width += widthOf(token) + if (j > i) spaceWidth else 0f
                if (width > lineWidth) break
                if (respectAyahLines && (line < token.minLine || line > token.maxLine)) break
                val slack = lineWidth - width
                val candidate = soFar + slack * slack
                if (candidate < cost[k + 1][j + 1]) {
                    cost[k + 1][j + 1] = candidate
                    from[k + 1][j + 1] = i
                }
            }
        }
    }

    if (cost[m][n] == inf) return null

    val result = LinkedHashMap<Int, List<MushafToken>>()
    var end = n
    for (k in m downTo 1) {
        val start = from[k][end]
        result[availableLines[k - 1]] = tokens.subList(start, end).toList()
        end = start
    }
    return result.toSortedMap()
}
