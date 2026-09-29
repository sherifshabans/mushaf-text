package io.github.sherifshabans.mushaf.sample.quran.audio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sherifshabans.mushaf.sample.audio.AyahReciter
import io.github.sherifshabans.mushaf.sample.audio.AyahRecitersCatalog
import io.github.sherifshabans.mushaf.sample.audio.Moshaf
import io.github.sherifshabans.mushaf.sample.audio.PlaybackMode
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioMeta
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioPrefs
import io.github.sherifshabans.mushaf.sample.audio.Reciter
import io.github.sherifshabans.mushaf.sample.audio.RecitersRepository
import io.github.sherifshabans.mushaf.sample.audio.RepeatMode
import io.github.sherifshabans.mushaf.sample.audio.Riwaya
import io.github.sherifshabans.mushaf.sample.audio.download.DownloadProgress
import io.github.sherifshabans.mushaf.sample.audio.download.QuranAudioDownloader
import io.github.sherifshabans.mushaf.sample.audio.download.QuranAudioFiles
import io.github.sherifshabans.mushaf.sample.audio.player.QuranPlaybackState
import io.github.sherifshabans.mushaf.sample.audio.player.QuranPlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** حالة اختيار القارئ اللي الشاشة بتبني عليها القوائم. */
data class ReciterSelection(
    val moshaf: Moshaf? = null,
    val ayahReciter: AyahReciter = AyahRecitersCatalog.default,
    val mode: PlaybackMode = PlaybackMode.SURAH
) {
    /** اسم القارئ المعروض دلوقتي حسب الوضع. */
    val displayName: String
        get() = if (mode == PlaybackMode.AYAH) ayahReciter.name else moshaf?.reciterName.orEmpty()

    val displayRiwaya: String
        get() = if (mode == PlaybackMode.AYAH) {
            "${ayahReciter.riwaya.label} — ${ayahReciter.styleLabel}"
        } else {
            moshaf?.let { "${it.riwaya.label} — ${it.styleLabel}" }.orEmpty()
        }

    /** مفتاح المصدر الحالي — بيدخل في مفاتيح التنزيل. */
    val sourceKey: String
        get() = if (mode == PlaybackMode.AYAH) ayahReciter.folder else moshaf?.key.orEmpty()
}

/**
 * عقل شاشة الاستماع.
 *
 * بيجمع تلات مصادر مستقلة: فهرس القرّاء، حالة التنزيل، وحالة المشغّل — وكل
 * واحد فيهم بيعيش أطول من الشاشة (المشغّل والتنزيل مفردات على مستوى التطبيق)،
 * فالـViewModel هنا **منسّق** مش مالك حالة.
 */
class QuranAudioViewModel constructor(
    private val context: Context,
    private val recitersRepository: RecitersRepository,
    private val downloader: QuranAudioDownloader,
    private val prefs: QuranAudioPrefs,
    val player: QuranPlayerController
) : ViewModel() {

    private val _reciters = MutableStateFlow<List<Reciter>>(emptyList())
    val reciters: StateFlow<List<Reciter>> = _reciters.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selection = MutableStateFlow(ReciterSelection())
    val selection: StateFlow<ReciterSelection> = _selection.asStateFlow()

    val playback: StateFlow<QuranPlaybackState> = player.state

    val downloads: StateFlow<Map<String, DownloadProgress>> = downloader.progress

    val favorites: StateFlow<List<String>> = prefs.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val followPlayback: StateFlow<Boolean> = prefs.followPlayback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /**
     * اختيار المستخدم المحفوظ للخروج من المصحف والتلاوة شغّالة، أو `null`
     * لو لسه ما اختارش — ساعتها الشاشة بتسأله.
     */
    val exitPlaybackChoice: StateFlow<QuranAudioPrefs.ExitChoice?> = prefs.exitPlaybackChoice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** يسجّل الاختيار عشان ما نسألش تاني. */
    fun rememberExitChoice(choice: QuranAudioPrefs.ExitChoice) {
        viewModelScope.launch { prefs.setExitPlaybackChoice(choice) }
    }

    /**
     * السور المحمّلة للمصدر الحالي.
     *
     * القرص بيتقرا مرة واحدة عند تغيير القارئ وبعد كل تنزيل خالص، مش مع كل
     * رسم للقائمة: `File.exists()` لـ١١٤ سورة جوّه `LazyColumn` كان بيعمل
     * وقفات محسوسة أثناء التمرير.
     */
    private val _downloadedSurahs = MutableStateFlow<Set<Int>>(emptySet())
    val downloadedSurahs: StateFlow<Set<Int>> = _downloadedSurahs.asStateFlow()

    private val _storageBytes = MutableStateFlow(0L)
    val storageBytes: StateFlow<Long> = _storageBytes.asStateFlow()

    init {
        player.connect()
        viewModelScope.launch {
            val list = recitersRepository.reciters()
            _reciters.value = list
            restoreSelection()
            _loading.value = false
            // التحديث بعد ما القائمة بانت، عشان الشاشة ما تستناش الشبكة.
            recitersRepository.refreshIfStale()
            _reciters.value = recitersRepository.reciters()
            restoreSelection()
        }
        viewModelScope.launch {
            downloader.progress.collect { map ->
                if (map.values.any { it.state == DownloadProgress.State.DONE }) {
                    refreshDownloaded()
                }
            }
        }
    }

    private suspend fun restoreSelection() {
        val moshaf = recitersRepository.moshafByKey(prefs.lastMoshafKey.first())
            ?: recitersRepository.defaultMoshaf()
        val ayahReciter = AyahRecitersCatalog.byId(prefs.lastAyahReciterId.first())
            ?: AyahRecitersCatalog.default
        _selection.value = _selection.value.copy(moshaf = moshaf, ayahReciter = ayahReciter)
        refreshDownloaded()
    }

    // ── الاختيار ───────────────────────────────────────────────────────────

    fun selectMoshaf(moshaf: Moshaf) {
        _selection.value = _selection.value.copy(moshaf = moshaf, mode = PlaybackMode.SURAH)
        // التلاوة الشغّالة بتتحوّل للقارئ الجديد في نفس اللحظة. من غير السطر ده
        // كان الاختيار بيتسجّل في التفضيلات وبس، فالمستخدم يختار شيخ والصوت
        // يفضل بصوت الشيخ القديم — وده اللي كان مبيّن إن الاختيار مش شغّال.
        if (playback.value.hasTrack) player.switchMoshaf(moshaf)
        viewModelScope.launch {
            prefs.setLastMoshafKey(moshaf.key)
            refreshDownloaded()
        }
    }

    fun selectAyahReciter(reciter: AyahReciter) {
        _selection.value = _selection.value.copy(
            ayahReciter = reciter,
            mode = PlaybackMode.AYAH
        )
        if (playback.value.hasTrack) player.switchAyahReciter(reciter)
        viewModelScope.launch {
            prefs.setLastAyahReciterId(reciter.id)
            refreshDownloaded()
        }
    }

    fun setMode(mode: PlaybackMode) {
        _selection.value = _selection.value.copy(mode = mode)
        viewModelScope.launch { refreshDownloaded() }
    }

    fun toggleFavorite(key: String) {
        viewModelScope.launch { prefs.toggleFavorite(key) }
    }

    fun setFollowPlayback(enabled: Boolean) {
        viewModelScope.launch { prefs.setFollowPlayback(enabled) }
    }

    // ── التشغيل ────────────────────────────────────────────────────────────

    /** سورة كاملة بالمصحف المختار، والقائمة بتكمّل لكل اللي بعدها. */
    fun playSurah(surah: Int) {
        val moshaf = _selection.value.moshaf ?: return
        if (!moshaf.hasSurah(surah)) return
        player.playSurah(moshaf, surah)
        viewModelScope.launch { prefs.setLastPlayed(moshaf.key, surah, null) }
    }

    /** آية بعينها، ثم اللي بعدها بلا توقف. */
    fun playAyah(surah: Int, ayah: Int) {
        val reciter = _selection.value.ayahReciter
        _selection.value = _selection.value.copy(mode = PlaybackMode.AYAH)
        player.playAyah(reciter, surah, ayah)
        viewModelScope.launch {
            prefs.setLastAyahReciterId(reciter.id)
            prefs.setLastPlayed(reciter.folder, surah, ayah)
        }
    }

    /** تشغيل الصفحة اللي المستخدم واقف عليها من أول سورتها. */
    fun playFromPage(page: Int) = playSurah(QuranAudioMeta.surahOfPage(page))

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun seekBy(ms: Long) = player.seekBy(ms)
    fun stop() = player.stop()

    fun setSpeed(speed: Float) {
        player.setSpeed(speed)
        viewModelScope.launch { prefs.setPlaybackSpeed(speed) }
    }

    fun cycleRepeatMode() {
        val next = when (playback.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        player.setRepeatMode(next)
        viewModelScope.launch { prefs.setRepeatMode(next) }
    }

    fun startSleepTimer(minutes: Int) = player.startSleepTimer(minutes)
    fun cancelSleepTimer() = player.cancelSleepTimer()
    fun clearError() = player.clearError()

    // ── التنزيل ────────────────────────────────────────────────────────────

    /** مفتاح التنزيل للسورة دي في المصدر الحالي. */
    fun downloadKey(surah: Int): String {
        val sel = _selection.value
        return if (sel.mode == PlaybackMode.AYAH) {
            QuranAudioFiles.ayahKey(sel.ayahReciter.folder, surah)
        } else {
            QuranAudioFiles.surahKey(sel.moshaf?.key.orEmpty(), surah)
        }
    }

    fun download(surah: Int) {
        val sel = _selection.value
        val request = if (sel.mode == PlaybackMode.AYAH) {
            QuranAudioFiles.ayahSurahRequest(context, sel.ayahReciter, surah)
        } else {
            QuranAudioFiles.surahRequest(context, sel.moshaf ?: return, surah)
        }
        downloader.enqueue(request)
    }

    /** تنزيل المصحف كله — كل السور المتاحة عند القارئ ده. */
    fun downloadAll() {
        val sel = _selection.value
        val surahs = if (sel.mode == PlaybackMode.AYAH) {
            (1..QuranAudioMeta.SURAH_COUNT).toList()
        } else {
            sel.moshaf?.surahList ?: return
        }
        val pending = surahs.filter { it !in _downloadedSurahs.value }
        downloader.enqueueAll(
            pending.map { surah ->
                if (sel.mode == PlaybackMode.AYAH) {
                    QuranAudioFiles.ayahSurahRequest(context, sel.ayahReciter, surah)
                } else {
                    QuranAudioFiles.surahRequest(context, sel.moshaf ?: return, surah)
                }
            }
        )
    }

    fun cancelDownload(surah: Int) = downloader.cancel(downloadKey(surah))

    fun cancelAllDownloads() = downloader.cancelAll()

    fun deleteDownload(surah: Int) {
        val sel = _selection.value
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (sel.mode == PlaybackMode.AYAH) {
                    QuranAudioFiles.deleteAyahSurah(context, sel.ayahReciter.folder, surah)
                } else {
                    QuranAudioFiles.deleteSurah(context, sel.moshaf?.key.orEmpty(), surah)
                }
            }
            refreshDownloaded()
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { QuranAudioFiles.deleteAll(context) }
            refreshDownloaded()
        }
    }

    fun refreshDownloaded() {
        viewModelScope.launch {
            val sel = _selection.value
            val (surahs, size) = withContext(Dispatchers.IO) {
                val set = if (sel.mode == PlaybackMode.AYAH) {
                    QuranAudioFiles.downloadedAyahSurahs(context, sel.ayahReciter.folder)
                } else {
                    QuranAudioFiles.downloadedSurahs(context, sel.moshaf?.key.orEmpty())
                }
                set to QuranAudioFiles.usedBytes(context)
            }
            _downloadedSurahs.value = surahs
            _storageBytes.value = size
        }
    }

    // ── فلترة القائمة ──────────────────────────────────────────────────────

    /**
     * القرّاء بعد البحث والفلترة.
     *
     * الفلترة على مستوى **المصحف** لا القارئ: القارئ ممكن يكون مسجّل حفصًا
     * وورشًا، ولو المستخدم فلتر بورش المفروض يشوف مصحف ورش بتاعه بس.
     */
    fun filtered(query: String, riwaya: Riwaya?, onlyComplete: Boolean): List<Reciter> {
        val q = query.trim()
        return _reciters.value.mapNotNull { reciter ->
            val moshafs = reciter.moshafs.filter { moshaf ->
                (riwaya == null || moshaf.riwaya == riwaya) &&
                    (!onlyComplete || moshaf.isComplete)
            }
            if (moshafs.isEmpty()) return@mapNotNull null
            if (q.isNotEmpty() && !reciter.name.contains(q) &&
                !moshafs.any { it.rawName.contains(q) }
            ) {
                return@mapNotNull null
            }
            reciter.copy(moshafs = moshafs)
        }
    }

    /** قرّاء «آية آية» بعد نفس الفلترة. */
    fun filteredAyahReciters(query: String, riwaya: Riwaya?): List<AyahReciter> {
        val q = query.trim()
        return AyahRecitersCatalog.all.filter { reciter ->
            (riwaya == null || reciter.riwaya == riwaya) &&
                (q.isEmpty() || reciter.name.contains(q))
        }
    }

    override fun onCleared() {
        // المشغّل مفرد بيعيش أطول من الشاشة عن قصد — التلاوة المفروض تكمّل
        // والمستخدم خارج من المصحف. فمفيش release هنا.
        super.onCleared()
    }
}
