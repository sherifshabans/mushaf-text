package io.github.sherifshabans.mushaf.sample.audio

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * فهرس القرّاء ومصاحفهم.
 *
 * الترتيب مقصود: **الأصل المحلّي أولًا**، والشبكة تحسين لا شرط.
 *
 * 1. `assets/quran/reciters.json` — مرفق مع التطبيق، فالقائمة بتظهر كاملة
 *    من غير إنترنت ومن غير لودينج.
 * 2. نسخة محدَّثة في `filesDir` لو نزلت قبل كده (بتفضّل على المرفق).
 * 3. تحديث في الخلفية من mp3quran مرة كل أسبوع — الفهرس بيكبر مع الوقت
 *    (المرفق ٢٢٧ قارئًا والخادم بقى ٢٤١)، ومن غير التحديث ده كان لازم
 *    تحديث كامل للتطبيق عشان يبان قارئ جديد.
 *
 * لو التحديث فشل لأي سبب بنسكت ونكمل بالمحلّي — القائمة موجودة أصلًا.
 */
class RecitersRepository constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    private val gson = Gson()
    private val loadLock = Mutex()

    @Volatile
    private var cached: List<Reciter>? = null

    /** كل القرّاء مرتّبين بالاسم العربي. */
    suspend fun reciters(): List<Reciter> {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            loadLock.withLock { cached ?: loadFromDisk().also { cached = it } }
        }
    }

    /** مصحف بعينه بمفتاحه (`m123`) — بيستعمله المشغّل لما يستعيد آخر اختيار. */
    suspend fun moshafByKey(key: String?): Moshaf? {
        if (key.isNullOrBlank()) return null
        return reciters().firstOrNull { r -> r.moshafs.any { it.key == key } }
            ?.moshafs?.firstOrNull { it.key == key }
    }

    /** المصحف الافتراضي: الحصري حفص مرتل، وإلا أول مصحف كامل في القائمة. */
    suspend fun defaultMoshaf(): Moshaf? {
        val all = reciters()
        val preferred = all.firstOrNull { it.name.contains("الحصري") }
            ?.moshafs?.firstOrNull { it.riwaya == Riwaya.HAFS && it.isComplete }
        return preferred
            ?: all.flatMap { it.moshafs }.firstOrNull { it.isComplete }
            ?: all.flatMap { it.moshafs }.firstOrNull()
    }

    /**
     * يحدّث الفهرس من الشبكة لو عدّى أسبوع على آخر تحديث.
     * بيتنادى من الواجهة عند فتح شاشة الاستماع، وما بيرميش استثناء.
     */
    suspend fun refreshIfStale() = withContext(Dispatchers.IO) {
        val cache = cacheFile()
        val age = System.currentTimeMillis() - cache.lastModified()
        if (cache.exists() && age < REFRESH_INTERVAL_MS) return@withContext
        runCatching {
            val request = Request.Builder().url(REMOTE_URL).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                val body = response.body?.string() ?: return@use
                // ما نكتبش الملف قبل ما نتأكد إنه بيتفكّ لقائمة معقولة —
                // صفحة خطأ من مزوّد الإنترنت كانت هتستبدل الفهرس بالفاضي.
                val parsed = parse(body)
                if (parsed.size >= MIN_SANE_RECITERS) {
                    cache.writeText(body)
                    cached = parsed
                }
            }
        }.onFailure { Log.d(TAG, "reciters refresh skipped: ${it.message}") }
    }

    private fun loadFromDisk(): List<Reciter> {
        val cache = cacheFile()
        if (cache.exists()) {
            runCatching { parse(cache.readText()) }
                .onSuccess { if (it.size >= MIN_SANE_RECITERS) return it }
                .onFailure { Log.w(TAG, "cached reciters unreadable, falling back to asset") }
        }
        return runCatching {
            context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        }.mapCatching { parse(it) }
            .getOrElse {
                Log.e(TAG, "reciters asset failed to load", it)
                emptyList()
            }
    }

    private fun parse(json: String): List<Reciter> {
        val dto = gson.fromJson(json, RecitersResponse::class.java) ?: return emptyList()
        return dto.reciters.orEmpty().mapNotNull { it.toDomain() }
            .filter { it.moshafs.isNotEmpty() }
            .sortedWith(compareBy({ it.letter }, { it.name }))
    }

    private fun cacheFile() = File(context.filesDir, CACHE_NAME)

    // ── DTO ────────────────────────────────────────────────────────────────
    //
    // ⚠️ كل حقل هنا **لازم** يبقى عليه `@SerializedName` صراحةً.
    //
    // Gson بيطابق بأسماء الحقول، وR8 في نسخة الـrelease بيعيد تسمية أي حقل
    // مش مذكور في قاعدة keep. القواعد الموجودة في `proguard-rules.pro`
    // بتحمي الحقول المعلَّمة بـ`@SerializedName` بس — فالحقول اللي كانت
    // بغير علامة (`id`, `name`, `letter`, `moshaf`, `server`) كانت بتتحوّل
    // لـ`a`/`b`/`c`، فGson ما بيلاقيش ليها مقابل في الملف ويرجّعها `null`،
    // و`toDomain()` بيرجّع `null`، فالفهرس بيطلع **فاضي**.
    //
    // النتيجة اللي كانت بتبان للمستخدم: تبويبة «سور كاملة» فاضية، من غير
    // روايات، وبرسالة «لا يوجد قارئ بهذا الاسم في هذه الرواية» — بينما
    // «آية آية» شغّالة لأنها كتالوج مكتوب في الكود مش JSON.
    private data class RecitersResponse(
        @SerializedName("reciters") val reciters: List<ReciterDto>?
    )

    private data class ReciterDto(
        @SerializedName("id") val id: Int?,
        @SerializedName("name") val name: String?,
        @SerializedName("letter") val letter: String?,
        @SerializedName("moshaf") val moshaf: List<MoshafDto>?
    ) {
        fun toDomain(): Reciter? {
            val rid = id ?: return null
            val rname = name?.trim().orEmpty().ifBlank { return null }
            val list = moshaf.orEmpty().mapNotNull { it.toDomain(rid, rname) }
            return Reciter(rid, rname, letter?.trim().orEmpty().ifBlank { rname.take(1) }, list)
        }
    }

    private data class MoshafDto(
        @SerializedName("id") val id: Int?,
        @SerializedName("name") val name: String?,
        @SerializedName("server") val server: String?,
        @SerializedName("surah_total") val surahTotal: Int?,
        @SerializedName("surah_list") val surahList: String?
    ) {
        fun toDomain(reciterId: Int, reciterName: String): Moshaf? {
            val mid = id ?: return null
            val srv = server?.trim().orEmpty().ifBlank { return null }
            if (!srv.startsWith("http")) return null
            val surahs = surahList.orEmpty()
                .split(',')
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..114 }
                .distinct()
                .sorted()
            if (surahs.isEmpty()) return null
            return Moshaf(
                id = mid,
                reciterId = reciterId,
                reciterName = reciterName,
                rawName = name?.trim().orEmpty().ifBlank { "حفص عن عاصم" },
                // بعض المدخلات بتيجي بـ http والمزوّدين بيعترضوها؛ https أأمن
                // والخوادم بتدعمه كلها.
                server = srv.replaceFirst("http://", "https://"),
                surahList = surahs,
                surahTotal = surahTotal ?: surahs.size
            )
        }
    }

    companion object {
        private const val TAG = "RecitersRepository"
        private const val ASSET_PATH = "quran/reciters.json"
        private const val CACHE_NAME = "reciters_cache.json"
        private const val REMOTE_URL = "https://www.mp3quran.net/api/v3/reciters?language=ar"
        private const val REFRESH_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
        private const val MIN_SANE_RECITERS = 50
    }
}
