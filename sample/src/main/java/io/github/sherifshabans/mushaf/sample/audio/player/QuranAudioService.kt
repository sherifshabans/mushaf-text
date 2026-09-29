package io.github.sherifshabans.mushaf.sample.audio.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import io.github.sherifshabans.mushaf.sample.MainActivity
import io.github.sherifshabans.mushaf.sample.audio.AudioTrack
import io.github.sherifshabans.mushaf.sample.audio.download.QuranAudioFiles
import io.github.sherifshabans.mushaf.sample.audio.player.QuranAudioTracks.toMediaItem

/**
 * جلسة تشغيل التلاوة.
 *
 * `MediaSessionService` بيدّينا من غير كود إضافي: إشعار فيه تشغيل/إيقاف
 * والتالي والسابق، تحكّم من شاشة القفل، أزرار السمّاعة، وإيقاف تلقائي لما
 * مكالمة تيجي أو تطبيق تاني ياخد الصوت.
 *
 * الخدمة **بتشتغل في نفس عملية التطبيق**، فالمشغّل والواجهة على نفس الحالة،
 * والـ`MediaController` بيوصل لها فورًا من غير IPC حقيقي.
 */
@OptIn(UnstableApi::class)
class QuranAudioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory()))
            .setLoadControl(
                // بافر أطول من الافتراضي: التلاوة بتتسمع على بيانات الموبايل
                // كتير وفي أماكن الشبكة فيها ضعيفة، والتقطيع وسط الآية أسوأ
                // بكتير من تأخير نص ثانية في البداية.
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(30_000, 120_000, 1_500, 3_000)
                    .build()
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(AyahChainListener(player))

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // لو المستخدم قفل التطبيق والتلاوة واقفة، مفيش داعي الخدمة تفضل
        // عايشة بإشعار ميّت. لو لسه بتشتغل بنسيبها — ده المطلوب أصلًا.
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun openAppIntent(): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags
        )
    }

    /**
     * يحوّل الرابط لملف محلّي **وقت القراءة** لو الملف متحمّل.
     *
     * الحسم وقت القراءة مش وقت بناء القائمة: قائمة السور بتتبني مرة واحدة
     * بكل الـ١١٤ سورة، والمستخدم ممكن ينزّل سورة وهو سامع اللي قبلها. لو
     * كنا ثبّتنا الروابط وقت البناء كانت السورة المتحمّلة هتتجاب من الشبكة
     * تاني رغم إنها على الجهاز.
     */
    private fun resolvingDataSourceFactory(): ResolvingDataSource.Factory {
        val upstream = DefaultDataSource.Factory(
            this,
            DefaultHttpDataSource.Factory()
                .setUserAgent("DailySeventy")
                .setConnectTimeoutMs(20_000)
                .setReadTimeoutMs(20_000)
                .setAllowCrossProtocolRedirects(true)
        )
        return ResolvingDataSource.Factory(upstream) { dataSpec: DataSpec ->
            val local = localFileFor(dataSpec)
            if (local != null) dataSpec.withUri(Uri.fromFile(local)) else dataSpec
        }
    }

    /**
     * الملف المحلّي المقابل لطلب، لو موجود وكامل.
     *
     * المفتاح هنا `dataSpec.key`، وهو نفس `mediaId` بتاع المقطع لأننا بنحطّه
     * في `customCacheKey` وقت بناء الـ`MediaItem`. من غيره كان الرابط لوحده
     * مش كفاية: `…/002.mp3` بيدلّ على السورة بس، ومش بيقول مصحف مين — فكان
     * ممكن نشغّل تلاوة قارئ محمّلة مكان القارئ اللي المستخدم مختاره.
     */
    private fun localFileFor(dataSpec: DataSpec): java.io.File? {
        if (dataSpec.uri.scheme == "file") return null
        val parsed = dataSpec.key?.let { AudioTrack.parse(it) } ?: return null
        val file = if (parsed.ayah != null) {
            QuranAudioFiles.ayahFile(this, parsed.source, parsed.surah, parsed.ayah)
        } else {
            QuranAudioFiles.surahFile(this, parsed.source, parsed.surah)
        }
        return file.takeIf { it.exists() && it.length() > 0 }
    }

    /**
     * بيلحّق آيات السورة اللي بعدها وإحنا على وشك نخلص.
     *
     * كده «سماع بدون توقف» في وضع الآية بيعدّي من سورة للي بعدها من غير سكتة
     * ومن غير ما نحمّل المصحف كله في قائمة واحدة.
     */
    private inner class AyahChainListener(private val player: Player) : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            appendNextSurahIfNeeded()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) appendNextSurahIfNeeded()
        }

        private fun appendNextSurahIfNeeded() {
            val lastId = runCatching {
                player.getMediaItemAt(player.mediaItemCount - 1).mediaId
            }.getOrNull() ?: return
            val parsed = AudioTrack.parse(lastId) ?: return
            if (parsed.ayah == null) return // وضع السور: القائمة كاملة أصلًا

            val remaining = player.mediaItemCount - 1 - player.currentMediaItemIndex
            if (remaining > QuranAudioTracks.AYAH_PREFETCH_MARGIN) return

            val reciter = QuranAudioTracks.ayahReciterOf(lastId) ?: return
            val next = QuranAudioTracks.nextSurahAyahItems(reciter, parsed.surah)
            if (next.isEmpty()) return
            runCatching { player.addMediaItems(next.map { it.toMediaItem() }) }
                .onFailure { Log.w(TAG, "could not chain next surah", it) }
        }
    }

    private companion object {
        const val TAG = "QuranAudioService"
    }
}
