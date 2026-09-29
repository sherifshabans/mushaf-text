package io.github.sherifshabans.mushaf.sample.data

import android.content.Context
import io.github.sherifshabans.mushaf.Ayah
import io.github.sherifshabans.mushaf.Quran
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray

/*
 * The app's core-module types, rebuilt on top of the library's [Quran] so the
 * ported screen reads exactly as it does in the app.
 */

data class DomainAya(
    val id: Int,
    val jozz: Int,
    val sora: Int,
    val soraNameEn: String,
    val soraNameAr: String,
    val page: Int,
    val lineStart: Int,
    val lineEnd: Int,
    val ayaNo: Int,
    val ayaText: String,
    val ayaTextEmlaey: String,
    /** The library's own ayah — what `MushafPage` draws. */
    val source: Ayah
)

data class DomainAyaTafseer(var number: String, var aya: String, var text: String)

data class DomainAyaWithTafseer(val aya: DomainAya, val ayaTafseer: DomainAyaTafseer)

/**
 * Page, tafsir and search, like the app's `QuranDatasourceDatabaseImp`.
 *
 * - Text and layout come from the library.
 * - Tafsir: `assets/quran/tafseer/tafseer.json` (التفسير الميسر), parsed once.
 * - Search matches the imla'i (undiacritised) text, like the app's
 *   `aya_text_emlaey LIKE '%…%'` — `assets/quran/emlaey.tsv`.
 */
object QuranData {

    @Volatile private var tafseer: Map<Pair<Int, Int>, DomainAyaTafseer>? = null
    @Volatile private var emlaey: Map<Int, String>? = null
    @Volatile private var all: List<DomainAya>? = null

    private fun tafseerIndex(context: Context): Map<Pair<Int, Int>, DomainAyaTafseer> {
        tafseer?.let { return it }
        return synchronized(this) {
            tafseer ?: run {
                val json = context.assets.open("quran/tafseer/tafseer.json")
                    .use { it.readBytes().toString(Charsets.UTF_8) }
                val arr = JSONArray(json)
                val out = HashMap<Pair<Int, Int>, DomainAyaTafseer>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val t = DomainAyaTafseer(o.optString("number"), o.optString("aya"), o.optString("text"))
                    val s = t.number.toIntOrNull() ?: continue
                    val a = t.aya.toIntOrNull() ?: continue
                    out[s to a] = t
                }
                out
            }.also { tafseer = it }
        }
    }

    private fun emlaeyIndex(context: Context): Map<Int, String> {
        emlaey?.let { return it }
        return synchronized(this) {
            emlaey ?: context.assets.open("quran/emlaey.tsv").bufferedReader(Charsets.UTF_8)
                .useLines { lines ->
                    lines.filter { it.isNotBlank() }.associate { line ->
                        val tab = line.indexOf('\t')
                        line.substring(0, tab).toInt() to line.substring(tab + 1)
                    }
                }.also { emlaey = it }
        }
    }

    private fun ayat(context: Context): List<DomainAya> {
        all?.let { return it }
        return synchronized(this) {
            all ?: run {
                val plain = emlaeyIndex(context)
                Quran.ayahs(context).map { a ->
                    val surah = Quran.surah(a.surah)
                    DomainAya(
                        id = a.id,
                        jozz = a.juz,
                        sora = a.surah,
                        soraNameEn = surah.nameEnglish,
                        soraNameAr = surah.nameArabic,
                        page = a.page,
                        lineStart = a.lineStart,
                        lineEnd = a.lineEnd,
                        ayaNo = a.number,
                        ayaText = a.text,
                        ayaTextEmlaey = plain[a.id].orEmpty(),
                        source = a
                    )
                }
            }.also { all = it }
        }
    }

    fun getQuranPageAyaWithTafseer(context: Context, page: Int): Flow<List<DomainAyaWithTafseer>> = flow {
        val index = tafseerIndex(context)
        emit(
            ayat(context).filter { it.page == page }.map { aya ->
                DomainAyaWithTafseer(
                    aya,
                    index[aya.sora to aya.ayaNo]
                        ?: DomainAyaTafseer(aya.sora.toString(), aya.ayaNo.toString(), "")
                )
            }
        )
    }.flowOn(Dispatchers.IO)

    fun searchAya(context: Context, keyword: String): Flow<List<DomainAya>> = flow {
        val q = keyword.trim()
        emit(if (q.isEmpty()) emptyList() else ayat(context).filter { it.ayaTextEmlaey.contains(q) })
    }.flowOn(Dispatchers.IO)
}
