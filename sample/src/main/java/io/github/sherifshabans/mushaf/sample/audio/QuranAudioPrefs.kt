package io.github.sherifshabans.mushaf.sample.audio

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.sherifshabans.mushaf.sample.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * تفضيلات الاستماع — كلها في نفس الـDataStore بتاع التطبيق.
 *
 * الهدف إن المستخدم ما يعيدش نفس الاختيارات كل مرة: القارئ اللي بيسمع له،
 * الرواية، السرعة، والتكرار كلها بترجع زي ما سابها.
 */
class QuranAudioPrefs constructor(
    private val context: Context
) {

    /** مفتاح آخر مصحف (سور كاملة) — `m123`. */
    val lastMoshafKey: Flow<String?> =
        context.dataStore.data.map { it[KEY_MOSHAF] }

    suspend fun setLastMoshafKey(key: String) =
        context.dataStore.edit { it[KEY_MOSHAF] = key }.let { }

    /** معرّف آخر قارئ «آية آية». */
    val lastAyahReciterId: Flow<String?> =
        context.dataStore.data.map { it[KEY_AYAH_RECITER] }

    suspend fun setLastAyahReciterId(id: String) =
        context.dataStore.edit { it[KEY_AYAH_RECITER] = id }.let { }

    val playbackSpeed: Flow<Float> =
        context.dataStore.data.map { it[KEY_SPEED] ?: 1f }

    suspend fun setPlaybackSpeed(speed: Float) =
        context.dataStore.edit { it[KEY_SPEED] = speed.coerceIn(0.5f, 2f) }.let { }

    val repeatMode: Flow<RepeatMode> = context.dataStore.data.map { prefs ->
        RepeatMode.entries.firstOrNull { it.name == prefs[KEY_REPEAT] } ?: RepeatMode.OFF
    }

    suspend fun setRepeatMode(mode: RepeatMode) =
        context.dataStore.edit { it[KEY_REPEAT] = mode.name }.let { }

    /** هل المصحف يقلب صفحاته مع التلاوة؟ افتراضيًّا نعم. */
    val followPlayback: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_FOLLOW] ?: true }

    suspend fun setFollowPlayback(enabled: Boolean) =
        context.dataStore.edit { it[KEY_FOLLOW] = enabled }.let { }

    /**
     * القرّاء المفضّلون — مفاتيح مفصولة بفواصل.
     *
     * قائمة نصّية بسيطة لأن العدد صغير (وحدات) والترتيب مهم: آخر مفضّل يظهر
     * الأول. مجموعة `Set` في DataStore ما بتحفظش ترتيبًا.
     */
    val favorites: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITES].orEmpty().split(',').filter { it.isNotBlank() }
    }

    suspend fun toggleFavorite(key: String) {
        val current = favorites.first()
        val updated = if (key in current) current - key else listOf(key) + current
        context.dataStore.edit { it[KEY_FAVORITES] = updated.joinToString(",") }
    }

    /** آخر ما سمعه المستخدم — يظهر في «تابع الاستماع». */
    val lastPlayed: Flow<LastPlayed?> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_LAST_PLAYED] ?: return@map null
        val parts = raw.split('|')
        if (parts.size < 3) return@map null
        LastPlayed(
            sourceKey = parts[0],
            surah = parts[1].toIntOrNull() ?: return@map null,
            ayah = parts[2].toIntOrNull()?.takeIf { it > 0 }
        )
    }

    suspend fun setLastPlayed(sourceKey: String, surah: Int, ayah: Int?) =
        context.dataStore.edit {
            it[KEY_LAST_PLAYED] = "$sourceKey|$surah|${ayah ?: 0}"
        }.let { }

    data class LastPlayed(val sourceKey: String, val surah: Int, val ayah: Int?)

    /**
     * إيه اللي يحصل للتلاوة لمّا المستخدم يخرج من المصحف.
     *
     * `null` معناها **اسأله**. المشغّل خدمة بتعيش أطول من الشاشة عن قصد
     * (التلاوة المفروض تكمّل والمستخدم بيعمل حاجة تانية)، بس ده مش واضح: ناس
     * بتخرج وتفتكر إنها وقّفت التلاوة وتلاقيها شغّالة، وناس تانية عايزاها
     * تكمّل. فبنسأل مرة، ولو حبّ يفتكر اختياره ما نسألوش تاني.
     */
    val exitPlaybackChoice: Flow<ExitChoice?> = context.dataStore.data.map { prefs ->
        ExitChoice.entries.firstOrNull { it.name == prefs[KEY_EXIT_CHOICE] }
    }

    suspend fun setExitPlaybackChoice(choice: ExitChoice?) =
        context.dataStore.edit { prefs ->
            if (choice == null) prefs.remove(KEY_EXIT_CHOICE)
            else prefs[KEY_EXIT_CHOICE] = choice.name
        }.let { }

    /** اختيار المستخدم وقت الخروج من المصحف والتلاوة شغّالة. */
    enum class ExitChoice { KEEP_PLAYING, STOP }

    private companion object {
        val KEY_MOSHAF = stringPreferencesKey("QURAN_AUDIO_MOSHAF")
        val KEY_AYAH_RECITER = stringPreferencesKey("QURAN_AUDIO_AYAH_RECITER")
        val KEY_SPEED = floatPreferencesKey("QURAN_AUDIO_SPEED")
        val KEY_REPEAT = stringPreferencesKey("QURAN_AUDIO_REPEAT")
        val KEY_FOLLOW = booleanPreferencesKey("QURAN_AUDIO_FOLLOW")
        val KEY_FAVORITES = stringPreferencesKey("QURAN_AUDIO_FAVORITES")
        val KEY_LAST_PLAYED = stringPreferencesKey("QURAN_AUDIO_LAST_PLAYED")
        val KEY_EXIT_CHOICE = stringPreferencesKey("QURAN_AUDIO_EXIT_CHOICE")
    }
}
