package io.github.sherifshabans.mushaf.sample.audio.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.sherifshabans.mushaf.sample.audio.AudioTrack
import io.github.sherifshabans.mushaf.sample.audio.AyahReciter
import io.github.sherifshabans.mushaf.sample.audio.Moshaf
import io.github.sherifshabans.mushaf.sample.audio.PlaybackMode
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioMeta
import io.github.sherifshabans.mushaf.sample.audio.RepeatMode
import io.github.sherifshabans.mushaf.sample.audio.player.QuranAudioTracks.toMediaItem
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** كل اللي الواجهة محتاجة تعرفه عن التشغيل دلوقتي. */
data class QuranPlaybackState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasTrack: Boolean = false,
    val mode: PlaybackMode = PlaybackMode.SURAH,
    val surah: Int = 0,
    val ayah: Int? = null,
    val title: String = "",
    val reciterName: String = "",
    val riwayaLabel: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    /** دقائق باقية على مؤقّت النوم، أو `null` لو مفيش مؤقّت. */
    val sleepTimerMinutesLeft: Int? = null,
    val error: String? = null
) {
    val surahName: String get() = if (surah > 0) QuranAudioMeta.surahName(surah) else ""
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * الواجهة الوحيدة بين الشاشات و[QuranAudioService].
 *
 * ## ليه مفرد (Singleton) مش داخل الـViewModel
 * التلاوة بتفضل شغّالة والمستخدم بيتنقّل بين الشاشات ويقفل الشاشة. لو الاتصال
 * بالجلسة كان مربوط بعمر ViewModel كان هيتقفل ويتفتح مع كل تنقّل، وكل مرة
 * الحالة بترجع للواجهة متأخّرة فيلمح المستخدم شريط تشغيل فاضي.
 *
 * ## الحالة من الجلسة لا من نُسخة موازية
 * كل قيمة في [state] مقروءة من `MediaController` نفسه بعد كل حدث. الإشعار
 * وشاشة القفل والسمّاعة بيغيّروا التشغيل من برّه التطبيق، وأي نسخة حالة
 * محلّية كانت هتفضل تتعارض معاهم.
 */
class QuranPlayerController constructor(
    private val context: Context
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(QuranPlaybackState())
    val state: StateFlow<QuranPlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connecting = false
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    private var clampJob: Job? = null

    /** أوامر متأخّرة لحد ما الاتصال يتم — أول ضغطة تشغيل بتيجي قبل الاتصال. */
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                error = friendlyError(error),
                isBuffering = false,
                isPlaying = false
            )
        }
    }

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, QuranAudioService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                connecting = false
                runCatching { future.get() }
                    .onSuccess { c ->
                        controller = c
                        c.addListener(listener)
                        pending.forEach { it(c) }
                        pending.clear()
                        publish()
                    }
                    .onFailure { Log.e(TAG, "media session connect failed", it) }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun release() {
        ticker?.cancel()
        sleepJob?.cancel()
        clampJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    // ── أوامر التشغيل ──────────────────────────────────────────────────────

    /**
     * سورة كاملة بصوت مصحف مختار — والقائمة بتكمّل لكل السور اللي بعدها.
     */
    fun playSurah(moshaf: Moshaf, surah: Int, startPositionMs: Long = 0L) {
        val (tracks, index) = QuranAudioTracks.surahPlaylist(moshaf, surah)
        if (tracks.isEmpty()) return
        withController { c ->
            c.setMediaItems(tracks.map { it.toMediaItem() }, index, startPositionMs)
            c.prepare()
            c.play()
        }
    }

    /** آية بعينها، والقائمة بتكمّل لآخر السورة ثم اللي بعدها. */
    fun playAyah(reciter: AyahReciter, surah: Int, ayah: Int) {
        val (tracks, index) = QuranAudioTracks.ayahPlaylist(reciter, surah, ayah)
        if (tracks.isEmpty()) return
        withController { c ->
            c.setMediaItems(tracks.map { it.toMediaItem() }, index, 0L)
            c.prepare()
            c.play()
        }
    }

    /**
     * تبديل المصحف والتلاوة شغّالة — نفس السورة، نفس الموضع، ونفس حالة التشغيل.
     *
     * الموضع بينتقل بالمللي ثانية: القرّاء بيختلفوا في السرعة، لكن القفزة لنفس
     * اللحظة أقرب لمكان المستمع بكتير من الرجوع لأول السورة — والبقرة رجوعها
     * للأول معناه ضياع ساعتين. ولإن تلاوة القارئ الجديد ممكن تكون أقصر من
     * القديمة، [clampToDuration] بيقصّ الموضع أول ما المدة تبان.
     */
    fun switchMoshaf(moshaf: Moshaf) = withController { c ->
        val current = c.currentMediaItem?.mediaId?.let { AudioTrack.parse(it) }
            ?: return@withController
        if (!moshaf.hasSurah(current.surah)) {
            _state.value = _state.value.copy(error = "هذه السورة غير متاحة عند هذا القارئ")
            return@withController
        }
        val (tracks, index) = QuranAudioTracks.surahPlaylist(moshaf, current.surah)
        if (tracks.isEmpty()) return@withController
        val position = c.currentPosition.coerceAtLeast(0L)
        swapPlaylist(c, tracks, index, position)
        clampToDuration(tracks[index].id, position)
    }

    /**
     * تبديل قارئ «آية آية» والتلاوة شغّالة.
     *
     * هنا الآية بتبدأ من أولها مش من نفس الموضع: الآية تلات أربع ثوانٍ، فنصّها
     * بصوت قارئ ونصّها التاني بصوت غيره كان هيبقى أوحش من إعادتها كاملة.
     */
    fun switchAyahReciter(reciter: AyahReciter) = withController { c ->
        val current = c.currentMediaItem?.mediaId?.let { AudioTrack.parse(it) }
            ?: return@withController
        val (tracks, index) =
            QuranAudioTracks.ayahPlaylist(reciter, current.surah, current.ayah ?: 1)
        if (tracks.isEmpty()) return@withController
        swapPlaylist(c, tracks, index, 0L)
    }

    /** يستبدل القائمة كلها ويحافظ على كون التلاوة كانت شغّالة ولا واقفة. */
    private fun swapPlaylist(
        c: MediaController,
        tracks: List<AudioTrack>,
        index: Int,
        positionMs: Long
    ) {
        val wasPlaying = c.isPlaying
        c.setMediaItems(tracks.map { it.toMediaItem() }, index, positionMs)
        c.prepare()
        if (wasPlaying) c.play()
    }

    /**
     * يقصّ الموضع لو تلاوة القارئ الجديد أقصر من القديمة.
     *
     * المدة مابتبانش غير بعد ما المصدر يجهّز، فالقصّ لازم يستنّاها. لو المشغّل
     * كان سبق وعدّى للمقطع اللي بعده بنسيبه — القصّ ساعتها هيرجّع المستمع
     * لمقطع هو خارج منه أصلًا.
     */
    private fun clampToDuration(expectedMediaId: String, requestedMs: Long) {
        clampJob?.cancel()
        if (requestedMs <= 0L) return
        clampJob = scope.launch {
            repeat(CLAMP_ATTEMPTS) {
                val c = controller ?: return@launch
                if (c.currentMediaItem?.mediaId != expectedMediaId) return@launch
                val duration = c.duration
                if (duration > 0L) {
                    if (requestedMs >= duration) {
                        c.seekTo((duration - END_MARGIN_MS).coerceAtLeast(0L))
                    }
                    return@launch
                }
                delay(CLAMP_POLL_MS)
            }
        }
    }

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun pause() = withController { it.pause() }

    fun stop() = withController { c ->
        c.pause()
        c.clearMediaItems()
        cancelSleepTimer()
    }

    fun next() = withController { c -> if (c.hasNextMediaItem()) c.seekToNextMediaItem() }

    fun previous() = withController { c ->
        // نفس سلوك أي مشغّل: لو عدّت ٣ ثوانٍ الزرار بيرجّع لأول المقطع،
        // وقبل كده بيروح للي قبله.
        if (c.currentPosition > 3_000L || !c.hasPreviousMediaItem()) c.seekTo(0L)
        else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) = withController { it.seekTo(ms.coerceAtLeast(0L)) }

    fun seekBy(deltaMs: Long) = withController { c ->
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0L, c.duration.coerceAtLeast(0L)))
    }

    fun setSpeed(speed: Float) = withController { it.setPlaybackSpeed(speed.coerceIn(0.5f, 2f)) }

    fun setRepeatMode(mode: RepeatMode) = withController { c ->
        c.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    /** يوقف التلاوة بعد المدة دي — الاستماع قبل النوم. */
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        sleepJob = scope.launch {
            var left = minutes
            while (left > 0) {
                _state.value = _state.value.copy(sleepTimerMinutesLeft = left)
                delay(60_000L)
                left--
            }
            _state.value = _state.value.copy(sleepTimerMinutesLeft = null)
            controller?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _state.value = _state.value.copy(sleepTimerMinutesLeft = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ── الحالة ─────────────────────────────────────────────────────────────

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else {
            pending += block
            connect()
        }
    }

    private fun publish() {
        val c = controller ?: return
        val item = c.currentMediaItem
        val parsed = item?.mediaId?.let { AudioTrack.parse(it) }
        val meta = item?.mediaMetadata

        _state.value = _state.value.copy(
            isConnected = true,
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            hasTrack = item != null,
            mode = if (parsed?.ayah != null) PlaybackMode.AYAH else PlaybackMode.SURAH,
            surah = parsed?.surah ?: 0,
            ayah = parsed?.ayah,
            title = meta?.title?.toString().orEmpty(),
            reciterName = meta?.artist?.toString().orEmpty(),
            riwayaLabel = meta?.albumTitle?.toString().orEmpty(),
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it > 0 } ?: 0L,
            speed = c.playbackParameters.speed,
            repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            },
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
            // رسالة الخطأ عمرها ما كانت بتتمسح: أول عطل في الشبكة كان بيسيبها
            // معلّقة في المشغّل حتى بعد ما التلاوة ترجع تشتغل عادي.
            error = if (c.playbackState == Player.STATE_READY) null else _state.value.error
        )
        syncTicker(c.isPlaying)
    }

    /**
     * موضع التشغيل ما بيرسلش أحداثًا وهو ماشي، فبنقراه كل نص ثانية —
     * وبس وهو شغّال، عشان ما نصحّيش الواجهة على الفاضي وهو واقف.
     */
    private fun syncTicker(playing: Boolean) {
        if (playing && ticker?.isActive != true) {
            ticker = scope.launch {
                while (true) {
                    val c = controller ?: break
                    _state.value = _state.value.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0L),
                        durationMs = c.duration.takeIf { it > 0 } ?: 0L
                    )
                    delay(500L)
                }
            }
        } else if (!playing) {
            ticker?.cancel()
            ticker = null
        }
    }

    private fun friendlyError(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "تعذّر الاتصال بالإنترنت — جرّب تاني أو شغّل تلاوة محمّلة"

        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "هذه السورة غير متاحة عند هذا القارئ"

        else -> "تعذّر تشغيل التلاوة"
    }

    private companion object {
        const val TAG = "QuranPlayerController"

        /** كام محاولة نستنّى فيها المدة تبان قبل ما نسيب القصّ. */
        const val CLAMP_ATTEMPTS = 30
        const val CLAMP_POLL_MS = 100L

        /** بنسيب شويّة قبل النهاية عشان التبديل ما يودّيش للمقطع اللي بعده. */
        const val END_MARGIN_MS = 5_000L
    }
}
