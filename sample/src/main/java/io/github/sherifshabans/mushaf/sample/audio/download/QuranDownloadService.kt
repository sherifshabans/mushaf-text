package io.github.sherifshabans.mushaf.sample.audio.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.sherifshabans.mushaf.sample.MainActivity
import io.github.sherifshabans.mushaf.sample.R
import io.github.sherifshabans.mushaf.sample.SampleGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * الخدمة اللي بتخلّي طابور التنزيل عايش لما المستخدم يخرج من التطبيق.
 *
 * هي **ما بتنزّلش بنفسها** — التنزيل كله في [QuranAudioDownloader] المفرد.
 * دور الخدمة إشعار دائم + رفع أولوية العملية، فأندرويد ما يقتلهاش وسط سورة
 * طولها ١٠٠ ميجا. وأول ما الطابور يفضى بتوقف نفسها.
 */
class QuranDownloadService : Service() {

    private val downloader: QuranAudioDownloader get() = SampleGraph.downloader

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * آخر start id وصل لـ[onStartCommand].
     *
     * أي إيقاف ذاتي بيتعمل بـ`stopSelf(lastStartId)` مش `stopSelf()`: لو
     * [QuranAudioDownloader] طلب تشغيل جديد لسه موصلش، النظام بيتجاهل الإيقاف.
     * `stopSelf()` من غير رقم كان بيقفل الخدمة وطلب التشغيل الأمامي معلّق،
     * وده بالظبط `ForegroundServiceDidNotStartInTimeException`.
     */
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        if (!startInForeground(buildNotification(0, 0f, null))) {
            // مفيش طريقة نكمّل بيها من غير ما نبقى أماميين، ومفيش طلب تاني هيعدّي
            // من نفس المنع — فإيقاف كامل مش مشروط.
            stopSelf()
            return
        }
        scope.launch {
            downloader.progress.collectLatest { map ->
                val active = map.values.filter { it.isActive }
                if (active.isEmpty()) {
                    stopSelf(lastStartId)
                    return@collectLatest
                }
                notificationManager().notify(
                    NOTIFICATION_ID,
                    buildNotification(
                        count = active.size,
                        fraction = active.map { it.fraction }.average().toFloat(),
                        title = active.firstOrNull { it.state == DownloadProgress.State.RUNNING }?.title
                    )
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        if (intent?.action == ACTION_CANCEL_ALL) {
            downloader.cancelAll()
        }
        // لازم نشيّك هنا كمان مش في المُجمِّع بس: لو التنزيل فشل قبل ما الخدمة
        // تتخلق، المُجمِّع شاف الطابور فاضي جوّه onCreate ونادى stopSelf(0)
        // والنظام تجاهله (الطلب ده كان لسه في السكة). StateFlow مش هيبعت نفس
        // القيمة تاني، فمن غير السطر ده الخدمة كانت هتفضل أمامية للأبد.
        if (!downloader.isBusy()) {
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    /**
     * أندرويد 15: الخدمات من نوع dataSync ليها ٦ ساعات في اليوم. لما تخلص،
     * النظام بينادي الدالة دي ولازم نقف خلال ثواني وإلا بيقفل التطبيق
     * (`ForegroundServiceDidNotStopInTimeException`). الطابور نفسه مش بيتلغي:
     * بيكمّل طول ما العملية عايشة، ولو اتقتلت الملفات الجزئية `.part` بتفضل
     * والتنزيل بيكمّل من مكانه في المرة الجاية.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * بترجع false لو النظام رفض إن الخدمة تبقى أمامية — أشهر سبب على أندرويد
     * 15 إن رصيد الـ٦ ساعات بتاع dataSync خلص النهارده.
     */
    private fun startInForeground(notification: Notification): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        true
    } catch (e: Exception) {
        Log.w(TAG, "startForeground refused", e)
        false
    }

    private fun buildNotification(count: Int, fraction: Float, title: String?): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancel = PendingIntent.getService(
            this,
            1,
            Intent(this, QuranDownloadService::class.java).setAction(ACTION_CANCEL_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val percent = (fraction.coerceIn(0f, 1f) * 100).toInt()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("تنزيل التلاوة")
            .setContentText(
                when {
                    title != null && count > 1 -> "$title و${count - 1} غيرها"
                    title != null -> title
                    else -> "جارٍ التحضير…"
                }
            )
            .setProgress(100, percent, count == 0)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(R.drawable.ic_stop, "إلغاء", cancel)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنزيل التلاوات",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "شريط تقدّم تنزيل سور المصحف الصوتية"
                setShowBadge(false)
            }
            notificationManager().createNotificationChannel(channel)
        }
    }

    private fun notificationManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val TAG = "QuranDownloadService"
        private const val CHANNEL_ID = "quran_audio_download"
        private const val NOTIFICATION_ID = 8801
        const val ACTION_CANCEL_ALL = "io.github.sherifshabans.mushaf.sample.CANCEL_QURAN_DOWNLOADS"
    }
}
