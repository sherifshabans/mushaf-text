package io.github.sherifshabans.mushaf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One ayah of the Madinah Mushaf (Hafs ʿan ʿĀṣim), in the KFGQPC Uthmanic encoding.
 *
 * @property id 1‥6236, in mushaf order.
 * @property text the verse **without** its number — the renderer draws the
 *   end-of-ayah rosette itself.
 * @property lineStart first line of the page the ayah occupies (1‥15). Only a hint:
 *   the exact word-per-line layout comes from the bundled layout asset.
 */
data class Ayah(
    val id: Int,
    val surah: Int,
    val number: Int,
    val juz: Int,
    val page: Int,
    val lineStart: Int,
    val lineEnd: Int,
    val text: String
)

/** A surah with its Arabic and transliterated names and its page range. */
data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val ayahCount: Int,
    val startPage: Int,
    val endPage: Int
)

/**
 * Entry point to the bundled Quran text.
 *
 * The text is loaded from the library's assets once (≈1.4 MB, a few tens of ms)
 * and cached for the life of the process. Every call is safe from any thread;
 * the `suspend` variants move the first load off the main thread.
 */
object Quran {

    const val PAGE_COUNT = 604
    const val AYAH_COUNT = 6236

    private const val TEXT_ASSET = "mushaf/hafs.tsv"

    @Volatile
    private var cache: List<Ayah>? = null

    /** The 114 surahs, in order. Needs no loading. */
    val surahs: List<Surah> get() = SURAHS

    fun surah(number: Int): Surah = SURAHS[number - 1]

    /** Every ayah, in mushaf order. Blocks on first use — prefer [load] from a coroutine. */
    fun ayahs(context: Context): List<Ayah> {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: parse(context.applicationContext).also { cache = it }
        }
    }

    /** Loads (or returns the cached) text on [Dispatchers.IO]. */
    suspend fun load(context: Context): List<Ayah> =
        cache ?: withContext(Dispatchers.IO) { ayahs(context) }

    /** The ayat printed on [page] (1‥604). */
    fun page(context: Context, page: Int): List<Ayah> {
        require(page in 1..PAGE_COUNT) { "page must be in 1..$PAGE_COUNT, was $page" }
        return ayahs(context).filter { it.page == page }
    }

    fun ayah(context: Context, surah: Int, number: Int): Ayah? =
        ayahs(context).firstOrNull { it.surah == surah && it.number == number }

    fun ayahsOfSurah(context: Context, surah: Int): List<Ayah> =
        ayahs(context).filter { it.surah == surah }

    /** The page a given ayah is printed on. */
    fun pageOf(context: Context, surah: Int, number: Int): Int? =
        ayah(context, surah, number)?.page

    private fun parse(context: Context): List<Ayah> {
        val out = ArrayList<Ayah>(AYAH_COUNT)
        context.assets.open(TEXT_ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                if (line.isBlank()) return@forEach
                out += parseAyahLine(line)
            }
        }
        check(out.size == AYAH_COUNT) { "Corrupt $TEXT_ASSET: ${out.size} ayat" }
        return out
    }
}

/** `id sura ayah juz page line_start line_end text`, tab separated. */
internal fun parseAyahLine(line: String): Ayah {
    val f = line.split('\t', limit = 8)
    return Ayah(
        id = f[0].toInt(),
        surah = f[1].toInt(),
        number = f[2].toInt(),
        juz = f[3].toInt(),
        page = f[4].toInt(),
        lineStart = f[5].toInt(),
        lineEnd = f[6].toInt(),
        text = f[7]
    )
}

/** Converts `253` to `٢٥٣`. */
fun toArabicDigits(n: Int): String {
    val digits = "٠١٢٣٤٥٦٧٨٩"
    return n.toString().map { if (it.isDigit()) digits[it - '0'] else it }.joinToString("")
}

/** The basmala in the same Uthmanic spelling as the text. */
const val BASMALA = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
