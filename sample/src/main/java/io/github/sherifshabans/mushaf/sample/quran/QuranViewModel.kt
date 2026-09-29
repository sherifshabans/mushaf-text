package io.github.sherifshabans.mushaf.sample.quran

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.sherifshabans.mushaf.Quran
import io.github.sherifshabans.mushaf.sample.data.DomainAyaWithTafseer
import io.github.sherifshabans.mushaf.sample.data.QuranData
import io.github.sherifshabans.mushaf.sample.dataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * The app's `QuranViewModel`, minus Hilt and `AppPreferences`: the last page and
 * the ayah marks live in the sample's DataStore, the marks as a JSON array.
 */
class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val store = application.dataStore

    /** آخر صفحة محفوظة، و`null` = لسه ما اتقرتش من القرص (شوف QuranScreen). */
    val savedPage: StateFlow<Int?> = store.data
        .map { it[KEY_PAGE] ?: 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** حجم خط المصحف — الشاشة بتقراه زي التطبيق؛ الصفحة نفسها بتحسب حجمها. */
    val fontSize: StateFlow<Float> = kotlinx.coroutines.flow.MutableStateFlow(20f)

    suspend fun ayatOfPage(page: Int): List<DomainAyaWithTafseer> =
        if (page in 1..Quran.PAGE_COUNT) {
            runCatching { QuranData.getQuranPageAyaWithTafseer(getApplication(), page).first() }
                .getOrDefault(emptyList())
        } else emptyList()

    /** صفحة آية بعينها — للمتابعة مع التلاوة. */
    fun pageOfAyah(sura: Int, ayaNo: Int, hintPage: Int? = null): Int? =
        Quran.pageOf(getApplication(), sura, ayaNo)

    fun setPage(page: Int) {
        if (page in 1..Quran.PAGE_COUNT) viewModelScope.launch { store.edit { it[KEY_PAGE] = page } }
    }

    // ─── Ayah marks ───────────────────────────────────────────────────────────
    val ayaMarks: StateFlow<List<AyaMark>> = store.data
        .map { decode(it[KEY_MARKS]).usable() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Adds a mark for an ayah, replacing any existing mark on the same ayah. */
    fun addMark(mark: AyaMark) = editMarks { current -> current.filterNot { it.ayaId == mark.ayaId } + mark }

    fun removeMark(ayaId: Int) = editMarks { current -> current.filterNot { it.ayaId == ayaId } }

    private fun editMarks(change: (List<AyaMark>) -> List<AyaMark>) {
        viewModelScope.launch {
            store.edit { prefs -> prefs[KEY_MARKS] = encode(change(decode(prefs[KEY_MARKS]))) }
        }
    }

    private fun encode(marks: List<AyaMark>): String = JSONArray().apply {
        marks.forEach { m ->
            put(
                JSONObject()
                    .put("ayaId", m.ayaId).put("page", m.page).put("soraNameAr", m.soraNameAr)
                    .put("ayaNo", m.ayaNo).put("label", m.label).put("colorArgb", m.colorArgb)
                    .put("timestamp", m.timestamp)
            )
        }
    }.toString()

    private fun decode(json: String?): List<AyaMark> = runCatching {
        val arr = JSONArray(json ?: return emptyList())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AyaMark(
                ayaId = o.getInt("ayaId"),
                page = o.getInt("page"),
                soraNameAr = o.getString("soraNameAr"),
                ayaNo = o.getInt("ayaNo"),
                label = o.getString("label"),
                colorArgb = o.getLong("colorArgb"),
                timestamp = o.getLong("timestamp")
            )
        }
    }.getOrDefault(emptyList())

    private companion object {
        val KEY_PAGE = intPreferencesKey("quran_page")
        val KEY_MARKS = stringPreferencesKey("aya_marks")
    }
}
