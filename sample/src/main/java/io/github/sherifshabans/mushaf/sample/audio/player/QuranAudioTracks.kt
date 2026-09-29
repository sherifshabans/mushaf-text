package io.github.sherifshabans.mushaf.sample.audio.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.sherifshabans.mushaf.sample.audio.AudioTrack
import io.github.sherifshabans.mushaf.sample.audio.AyahReciter
import io.github.sherifshabans.mushaf.sample.audio.AyahRecitersCatalog
import io.github.sherifshabans.mushaf.sample.audio.Moshaf
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioMeta

/**
 * بناء قوائم التشغيل.
 *
 * القاعدة هنا إن **الاستمرار خاصية قائمة مش خاصية مشغّل**: بدل ما نمسك حالة
 * «شغّل اللي بعده» ونتعامل مع نهاية كل مقطع بإيدينا، بنحطّ السور اللي بعدها
 * في نفس القائمة من الأول. ExoPlayer بيمشّيها بالترتيب من غير سكتة، وأزرار
 * «التالي/السابق» في الإشعار بتشتغل لوحدها، والتكرار كمان.
 *
 * التشغيل بالآية استثناء: ٦٢٣٦ آية في قائمة واحدة تبقى تُخمة بلا داعي، فبنحطّ
 * السورة الحالية بس و[nextSurahAyahItems] بيلحّق اللي بعدها وإحنا قربنا نخلص.
 */
object QuranAudioTracks {

    const val EXTRA_SURAH = "quran_surah"
    const val EXTRA_AYAH = "quran_ayah"
    const val EXTRA_RECITER = "quran_reciter"

    /** كام آية فاضلة قبل ما نلحّق السورة اللي بعدها. */
    const val AYAH_PREFETCH_MARGIN = 3

    /**
     * قائمة سور كاملة: **كل** سور المصحف المتاحة، والبداية عند المختارة.
     *
     * بنرجّع القائمة كلها لا من المختارة وبعدها فقط، عشان زر «السابق» يشتغل
     * والتكرار الكلي يلفّ على المصحف من أوله.
     */
    fun surahPlaylist(moshaf: Moshaf, startSurah: Int): Pair<List<AudioTrack>, Int> {
        val tracks = moshaf.surahList.map { AudioTrack.surahTrack(moshaf, it) }
        val index = moshaf.surahList.indexOf(startSurah).let { if (it < 0) 0 else it }
        return tracks to index
    }

    /** قائمة آيات سورة واحدة، والبداية عند الآية المختارة. */
    fun ayahPlaylist(
        reciter: AyahReciter,
        surah: Int,
        startAyah: Int
    ): Pair<List<AudioTrack>, Int> {
        val count = QuranAudioMeta.ayahCount(surah)
        val tracks = (1..count).map { AudioTrack.ayahTrack(reciter, surah, it) }
        return tracks to (startAyah - 1).coerceIn(0, (count - 1).coerceAtLeast(0))
    }

    /** آيات السورة اللي بعد دي — بيستعملها المشغّل للتلحيق أثناء التشغيل. */
    fun nextSurahAyahItems(reciter: AyahReciter, afterSurah: Int): List<AudioTrack> {
        val next = afterSurah + 1
        if (next > QuranAudioMeta.SURAH_COUNT) return emptyList()
        return (1..QuranAudioMeta.ayahCount(next)).map {
            AudioTrack.ayahTrack(reciter, next, it)
        }
    }

    /** القارئ صاحب المقطع الشغّال دلوقتي — من الـ mediaId مباشرة. */
    fun ayahReciterOf(mediaId: String): AyahReciter? {
        val parsed = AudioTrack.parse(mediaId) ?: return null
        if (parsed.ayah == null) return null
        return AyahRecitersCatalog.all.firstOrNull { it.folder == parsed.source }
    }

    fun toMediaItems(tracks: List<AudioTrack>): List<MediaItem> = tracks.map { it.toMediaItem() }

    fun AudioTrack.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putInt(EXTRA_SURAH, surah)
            putInt(EXTRA_AYAH, ayah ?: 0)
            putString(EXTRA_RECITER, reciterKey)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(reciterName)
            .setAlbumTitle(riwayaLabel)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(url))
            // `customCacheKey` بيوصل للـ`DataSpec.key` جوّه المشغّل، وهو
            // الطريق الوحيد اللي بيوصل هوية المقطع لمرحلة القراءة. من غيره
            // كان الـresolver شايف الرابط بس (`…/002.mp3`) ومش عارف مصحف
            // مين، فما يقدرش يقرّر ياخد الملف المتحمّل ولا لأ.
            .setCustomCacheKey(id)
            .setMediaMetadata(metadata)
            .build()
    }
}
