package io.github.sherifshabans.mushaf.sample.audio.download

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** جزء واحد من التنزيل: رابط ← ملف. */
data class DownloadPart(val url: String, val target: File)

/** أمر تنزيل كامل — سورة واحدة (ملف) أو سورة آية آية (عشرات الملفات). */
data class DownloadRequest(
    val key: String,
    val title: String,
    val parts: List<DownloadPart>
)

/** حالة التنزيل زي ما الواجهة بتقراها. */
data class DownloadProgress(
    val key: String,
    val title: String,
    val state: State,
    /** بايت اتحمّلت فعلًا (بيشمل اللي كان متحمّل قبل الاستئناف). */
    val bytes: Long = 0L,
    /** إجمالي متوقّع، أو `0` لو الخادم ما قالش. */
    val totalBytes: Long = 0L,
    /** للتنزيل متعدد الملفات: كام ملف خلص من كام. */
    val doneParts: Int = 0,
    val totalParts: Int = 1,
    val error: String? = null
) {
    enum class State { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

    /** نسبة من ٠ لـ١، وبتفضّل عدّ الملفات لما ما يكونش فيه حجم إجمالي. */
    val fraction: Float
        get() = when {
            state == State.DONE -> 1f
            totalParts > 1 -> doneParts.toFloat() / totalParts
            totalBytes > 0L -> (bytes.toFloat() / totalBytes).coerceIn(0f, 1f)
            else -> 0f
        }

    val isActive: Boolean get() = state == State.QUEUED || state == State.RUNNING
}

/**
 * طابور تنزيل التلاوات.
 *
 * ## ليه مش WorkManager
 * التنزيل هنا **تفاعلي**: المستخدم واقف على قائمة السور وبيتفرّج على النسبة
 * وممكن يلغي في أي لحظة. WorkManager بيدّي بقاء أطول لكن تقدّمه بيوصل
 * للواجهة متقطّع ومتأخّر، والإلغاء الفوري فيه مش مضمون. الطابور ده بيدّي
 * تقدّم لحظي بالبايت وإلغاء فوري، و[QuranDownloadService] بيمسك العملية
 * حيّة في الخلفية طول ما فيه شغل.
 *
 * ## الاستئناف
 * كل جزء بينزل في ملف `.part` جنب هدفه. لو الملف ده موجود من محاولة سابقة
 * بنكمّل من مكانه بـ`Range: bytes=N-`، وخوادم mp3quran/everyayah كلها بترد
 * بـ`Accept-Ranges: bytes`. من غير ده كان أي قطع نت في سورة زي البقرة
 * المجوّدة (قرابة ١٠٠ ميجا) بيرجّع المستخدم للصفر.
 */
class QuranAudioDownloader constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    /** الوظائف الجارية عشان الإلغاء يوصل لجوه الـ stream مش بس للطابور. */
    private val jobs = mutableMapOf<String, Job>()

    /** طابور بسيط: عاملان متوازيان بس عشان الشبكة ما تختنقش والتشغيل يفضل ناعم. */
    private val queue = Channel<DownloadRequest>(Channel.UNLIMITED)

    init {
        repeat(WORKERS) {
            scope.launch {
                for (request in queue) {
                    val job = scope.launch { runRequest(request) }
                    synchronized(jobs) { jobs[request.key] = job }
                    job.join()
                    synchronized(jobs) { jobs.remove(request.key) }
                }
            }
        }
    }

    fun isBusy(): Boolean = _progress.value.values.any { it.isActive }

    fun activeCount(): Int = _progress.value.values.count { it.isActive }

    /** أول نسبة إجمالية تقريبية للإشعار. */
    fun overallFraction(): Float {
        val active = _progress.value.values.filter { it.isActive }
        if (active.isEmpty()) return 1f
        return active.map { it.fraction }.average().toFloat()
    }

    fun enqueue(request: DownloadRequest) {
        if (request.parts.isEmpty()) return
        val existing = _progress.value[request.key]
        if (existing != null && existing.isActive) return
        // اللي متحمّل خلاص ما يتعادش
        if (request.parts.all { it.target.exists() && it.target.length() > 0 }) {
            update(
                DownloadProgress(
                    key = request.key,
                    title = request.title,
                    state = DownloadProgress.State.DONE,
                    doneParts = request.parts.size,
                    totalParts = request.parts.size
                )
            )
            return
        }
        update(
            DownloadProgress(
                key = request.key,
                title = request.title,
                state = DownloadProgress.State.QUEUED,
                totalParts = request.parts.size
            )
        )
        startService()
        queue.trySend(request)
    }

    fun enqueueAll(requests: List<DownloadRequest>) = requests.forEach(::enqueue)

    fun cancel(key: String) {
        synchronized(jobs) { jobs[key] }?.cancel()
        _progress.value[key]?.let {
            if (it.isActive) {
                update(it.copy(state = DownloadProgress.State.CANCELLED))
            }
        }
    }

    fun cancelAll() {
        synchronized(jobs) { jobs.values.toList() }.forEach { it.cancel() }
        _progress.value = _progress.value.mapValues {
            if (it.value.isActive) it.value.copy(state = DownloadProgress.State.CANCELLED)
            else it.value
        }
    }

    /** يشيل السطور الخلصانة من الخريطة عشان ما تكبرش بلا داعي. */
    fun clearFinished() {
        _progress.value = _progress.value.filterValues { it.isActive }
    }

    private suspend fun runRequest(request: DownloadRequest) {
        var done = 0
        var bytes = 0L
        update(
            DownloadProgress(
                key = request.key,
                title = request.title,
                state = DownloadProgress.State.RUNNING,
                totalParts = request.parts.size
            )
        )
        try {
            for (part in request.parts) {
                kotlin.coroutines.coroutineContext.ensureActive()
                if (part.target.exists() && part.target.length() > 0) {
                    done++
                    bytes += part.target.length()
                    publish(request, done, bytes, 0L)
                    continue
                }
                val size = downloadPart(part) { soFar, total ->
                    publish(request, done, bytes + soFar, total)
                }
                done++
                bytes += size
                publish(request, done, bytes, 0L)
            }
            update(
                DownloadProgress(
                    key = request.key,
                    title = request.title,
                    state = DownloadProgress.State.DONE,
                    bytes = bytes,
                    totalBytes = bytes,
                    doneParts = done,
                    totalParts = request.parts.size
                )
            )
        } catch (ce: CancellationException) {
            update(
                DownloadProgress(
                    key = request.key,
                    title = request.title,
                    state = DownloadProgress.State.CANCELLED,
                    bytes = bytes,
                    doneParts = done,
                    totalParts = request.parts.size
                )
            )
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "download failed: ${request.key}", t)
            update(
                DownloadProgress(
                    key = request.key,
                    title = request.title,
                    state = DownloadProgress.State.FAILED,
                    bytes = bytes,
                    doneParts = done,
                    totalParts = request.parts.size,
                    error = t.message
                )
            )
        }
    }

    /**
     * ينزّل جزءًا واحدًا مع الاستئناف، ويرجّع حجمه النهائي.
     *
     * الكتابة على `.part` ثم إعادة التسمية: الملف ما بيظهرش في مكانه النهائي
     * إلا وهو كامل، فالمشغّل عمره ما يفتح ملفًا نصّه ناقص ويقول «التلاوة
     * محمّلة» وهي مقطوعة.
     */
    private suspend fun downloadPart(
        part: DownloadPart,
        onBytes: (soFar: Long, total: Long) -> Unit
    ): Long {
        val target = part.target
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + ".part")
        val existing = if (partial.exists()) partial.length() else 0L

        val builder = Request.Builder().url(part.url)
        if (existing > 0) builder.header("Range", "bytes=$existing-")

        okHttpClient.newCall(builder.build()).execute().use { response ->
            // 416 = الملف خلص فعلًا على القرص لكن الرينج بره المدى.
            if (response.code == 416 && existing > 0) {
                partial.renameTo(target)
                return target.length()
            }
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} — ${part.url}")
            }
            val body = response.body ?: throw IOException("empty body")
            val resumed = response.code == 206 && existing > 0
            val startAt = if (resumed) existing else 0L
            val total = if (body.contentLength() > 0) startAt + body.contentLength() else 0L

            // لو الخادم تجاهل الـRange بنبدأ من الأول بدل ما نلزق البايتات غلط.
            if (!resumed && existing > 0) partial.delete()

            var written = startAt
            body.byteStream().use { input ->
                java.io.FileOutputStream(partial, resumed).use { output ->
                    val buffer = ByteArray(BUFFER)
                    var lastReport = 0L
                    while (true) {
                        kotlin.coroutines.coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        // تحديث الحالة كل ربع ميجا: التدفّق بيكتب آلاف المرات
                        // في الثانية، وتحديث StateFlow مع كل قطعة كان بيغرق
                        // الـ recomposition ويهنّج القائمة.
                        if (written - lastReport > REPORT_EVERY) {
                            lastReport = written
                            onBytes(written, total)
                        }
                    }
                    output.flush()
                }
            }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            return target.length()
        }
    }

    private fun publish(request: DownloadRequest, done: Int, bytes: Long, total: Long) {
        val current = _progress.value[request.key]
        update(
            (current ?: DownloadProgress(request.key, request.title, DownloadProgress.State.RUNNING))
                .copy(
                    state = DownloadProgress.State.RUNNING,
                    bytes = bytes,
                    totalBytes = if (total > 0) total else current?.totalBytes ?: 0L,
                    doneParts = done,
                    totalParts = request.parts.size
                )
        )
    }

    private fun update(progress: DownloadProgress) {
        _progress.value = _progress.value + (progress.key to progress)
    }

    private fun startService() {
        runCatching {
            val intent = Intent(context, QuranDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }.onFailure { Log.w(TAG, "download service refused to start", it) }
    }

    // ── مفيش stopService من هنا، عن قصد ──────────────────────────────────────
    //
    // كان فيه `stopServiceIfIdle()` بتنادي `context.stopService` أول ما الطابور
    // يفضى — ودي كانت كراش `ForegroundServiceDidNotStartInTimeException` في
    // أعطال جوجل بلاي. التسلسل: المستخدم يدوس تنزيل من غير نت ← `startService()`
    // فوق بتطلب خدمة أمامية ← العامل على IO بيفشل في أجزاء من الثانية
    // (UnknownHost) ← `stopService` توصل للنظام **قبل** ما الخيط الرئيسي يلحق
    // يشغّل `onCreate` بتاعة الخدمة وينادي `startForeground`. وأندرويد بيعتبر
    // إيقاف خدمة اتطلبت أمامية قبل ما تبقى أمامية مخالفة، فبيقفل التطبيق.
    //
    // الخدمة بتوقف نفسها لما الطابور يفضى ([QuranDownloadService])، بـ start id
    // بيخلّي النظام يتجاهل الإيقاف لو فيه طلب تشغيل أحدث لسه موصلش.

    companion object {
        private const val TAG = "QuranAudioDownloader"
        private const val WORKERS = 2
        private const val BUFFER = 64 * 1024
        private const val REPORT_EVERY = 256L * 1024
    }
}
