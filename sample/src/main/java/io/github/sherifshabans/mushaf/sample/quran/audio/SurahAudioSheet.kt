package io.github.sherifshabans.mushaf.sample.quran.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sherifshabans.mushaf.sample.audio.PlaybackMode
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioMeta
import io.github.sherifshabans.mushaf.sample.audio.download.DownloadProgress
import io.github.sherifshabans.mushaf.sample.audio.formatBytes
import io.github.sherifshabans.mushaf.sample.quran.AmiriFont
import io.github.sherifshabans.mushaf.sample.quran.QuranSheetColors
import io.github.sherifshabans.mushaf.sample.quran.SuraList
import io.github.sherifshabans.mushaf.sample.quran.rememberQuranSheetColors
import io.github.sherifshabans.mushaf.sample.quran.toArabicNumerals

/**
 * قائمة سور القارئ المختار: تشغيل، تنزيل، وحذف.
 *
 * السور اللي القارئ ما سجّلهاش **بتتشال من القائمة** مش بتتعرض معطّلة — فيه
 * قرّاء مسجّلين جزء عمّ بس (٣٧ سورة)، وعرض ١١٤ سورة نصّها بيرجّع 404 كان
 * بيبان كأن التطبيق باظ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahAudioSheet(
    viewModel: QuranAudioViewModel,
    /** الصفحة المفتوحة في المصحف — القائمة بتبدأ عندها وبتعرض «ابدأ من هنا». */
    currentPage: Int,
    onOpenReciters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheet = rememberQuranSheetColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selection by viewModel.selection.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val downloaded by viewModel.downloadedSurahs.collectAsState()
    val storage by viewModel.storageBytes.collectAsState()

    LaunchedEffect(selection.sourceKey) { viewModel.refreshDownloaded() }

    // تنزيل مصحف كامل ممكن يعدّي الجيجابايت، وحذف الكل ما بيترجعش. الاتنين
    // بيتأكّدوا الأول — الأزرار جنب بعض في شريط واحد وسهل تلمس الغلط.
    var confirmDownloadAll by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val availableSurahs = remember(selection) {
        if (selection.mode == PlaybackMode.AYAH) SuraList
        else SuraList.filter { selection.moshaf?.hasSurah(it.number) == true }
    }

    // سورة الصفحة المفتوحة — نقطة الوصل بين القراءة والاستماع.
    val pageSurah = remember(currentPage) { QuranAudioMeta.surahOfPage(currentPage) }
    val pageSurahAvailable = availableSurahs.any { it.number == pageSurah }

    // القائمة بتفتح عند السورة الشغّالة، وإلا عند سورة الصفحة اللي المستخدم
    // واقف عليها — أقرب سورة لنيّته في الحالتين.
    val listState = rememberLazyListState()
    LaunchedEffect(playback.surah, pageSurah, availableSurahs.size) {
        val target = if (playback.surah > 0) playback.surah else pageSurah
        val index = availableSurahs.indexOfFirst { it.number == target }
        if (index > 2) listState.scrollToItem(index - 2)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheet.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // ── رأس: القارئ الحالي، وضغطة واحدة لتغييره ────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(sheet.accent.copy(alpha = 0.10f))
                    .border(1.dp, sheet.divider, RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenReciters)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(sheet.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = sheet.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = selection.displayName.ifBlank { "اختر القارئ" },
                        fontFamily = AmiriFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = sheet.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selection.displayRiwaya.ifBlank { "اضغط للاختيار من القرّاء" },
                        fontFamily = AmiriFont,
                        fontSize = 12.sp,
                        color = sheet.inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "تغيير القارئ",
                    tint = sheet.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── ابدأ من سورة الصفحة المفتوحة ──────────────────────────────
            if (pageSurahAvailable && playback.surah != pageSurah) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(sheet.accent)
                        .clickable {
                            if (selection.mode == PlaybackMode.AYAH) {
                                viewModel.playAyah(pageSurah, 1)
                            } else {
                                viewModel.playSurah(pageSurah)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = sheet.onAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "استمع لسورة ${QuranAudioMeta.surahName(pageSurah)}",
                            fontFamily = AmiriFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = sheet.onAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "السورة المفتوحة أمامك — ثم ما بعدها بلا توقف",
                            fontFamily = AmiriFont,
                            fontSize = 11.sp,
                            color = sheet.onAccent.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── تنزيل الكل / حذف الكل / المساحة ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioChip(
                    label = "تنزيل الكل",
                    selected = false,
                    sheet = sheet,
                    icon = Icons.Default.Download
                ) { confirmDownloadAll = true }

                if (downloads.values.any { it.isActive }) {
                    AudioChip(
                        label = "إيقاف التنزيل",
                        selected = true,
                        sheet = sheet,
                        icon = Icons.Default.Close
                    ) { viewModel.cancelAllDownloads() }
                }

                Spacer(Modifier.weight(1f))

                if (storage > 0) {
                    AudioChip(
                        label = formatBytes(storage),
                        selected = false,
                        sheet = sheet,
                        icon = Icons.Default.Delete
                    ) { confirmDeleteAll = true }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = sheet.divider, thickness = 0.8.dp)

            if (availableSurahs.isEmpty()) {
                // الفهرس بيتقرا من القرص، فاللحظة دي قصيرة — بس من غير سبينر
                // كانت بتبان كأن القائمة فاضية فعلًا.
                if (loading && selection.mode == PlaybackMode.SURAH) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = sheet.accent)
                    }
                } else {
                    // مفيش قارئ مختار: الرسالة نفسها هي الزرار، عشان الشيت
                    // ما يبقاش طريق مسدود زي ما كان.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onOpenReciters),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = sheet.accent,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "اضغط هنا لاختيار قارئ",
                                fontFamily = AmiriFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = sheet.ink
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "بعد الاختيار تظهر لك سوره كلها هنا",
                                fontFamily = AmiriFont,
                                fontSize = 12.sp,
                                color = sheet.inkSoft
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    items(availableSurahs, key = { it.number }) { sura ->
                        val key = viewModel.downloadKey(sura.number)
                        SurahAudioRow(
                            number = sura.number,
                            name = sura.nameAr,
                            ayahCount = QuranAudioMeta.ayahCount(sura.number),
                            sheet = sheet,
                            isPlaying = playback.surah == sura.number && playback.isPlaying,
                            isCurrent = playback.surah == sura.number,
                            isDownloaded = sura.number in downloaded,
                            download = downloads[key],
                            onPlay = {
                                if (playback.surah == sura.number && playback.hasTrack) {
                                    viewModel.togglePlayPause()
                                } else if (selection.mode == PlaybackMode.AYAH) {
                                    viewModel.playAyah(sura.number, 1)
                                } else {
                                    viewModel.playSurah(sura.number)
                                }
                            },
                            onDownload = { viewModel.download(sura.number) },
                            onCancel = { viewModel.cancelDownload(sura.number) },
                            onDelete = { viewModel.deleteDownload(sura.number) }
                        )
                    }
                }
            }
        }
    }

    if (confirmDownloadAll) {
        val remaining = availableSurahs.count { it.number !in downloaded }
        ConfirmDialog(
            title = "تنزيل كل السور؟",
            body = "سيتم تنزيل ${toArabicNumerals(remaining)} سورة بصوت " +
                "${selection.displayName}. المصحف الكامل قد يتجاوز الجيجابايت، " +
                "ويُفضّل أن تكون على شبكة Wi-Fi.",
            confirmLabel = "تنزيل",
            sheet = sheet,
            onConfirm = { viewModel.downloadAll() },
            onDismiss = { confirmDownloadAll = false }
        )
    }

    if (confirmDeleteAll) {
        ConfirmDialog(
            title = "حذف كل التلاوات المحمّلة؟",
            body = "سيُحذف ${formatBytes(storage)} من التلاوات المحمّلة لكل القرّاء، " +
                "ويمكن تنزيلها مرة أخرى في أي وقت.",
            confirmLabel = "حذف",
            sheet = sheet,
            onConfirm = { viewModel.deleteAllDownloads() },
            onDismiss = { confirmDeleteAll = false }
        )
    }
}

/** تأكيد بسيط بنفس ألوان شيتات المصحف. */
@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    sheet: QuranSheetColors,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = sheet.surface,
        title = {
            Text(
                title,
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = sheet.ink
            )
        },
        text = { Text(body, fontFamily = AmiriFont, fontSize = 14.sp, color = sheet.inkSoft) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, fontFamily = AmiriFont, fontSize = 15.sp, color = sheet.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontFamily = AmiriFont, fontSize = 15.sp, color = sheet.inkSoft)
            }
        }
    )
}

@Composable
private fun SurahAudioRow(
    number: Int,
    name: String,
    ayahCount: Int,
    sheet: QuranSheetColors,
    isPlaying: Boolean,
    isCurrent: Boolean,
    isDownloaded: Boolean,
    download: DownloadProgress?,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) sheet.accent.copy(alpha = 0.12f) else sheet.surface)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) sheet.accent else sheet.divider,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(sheet.accent.copy(alpha = 0.12f))
                .border(1.dp, sheet.divider, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = toArabicNumerals(number),
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = sheet.accent
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "سُورَةُ $name",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = sheet.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = downloadSubtitle(download, isDownloaded, ayahCount),
                fontFamily = AmiriFont,
                fontSize = 11.sp,
                color = sheet.inkSoft,
                maxLines = 1
            )
        }

        // زر التنزيل بحالاته الأربع: نزّل / جارٍ / متحمّل / أعد المحاولة.
        DownloadButton(
            sheet = sheet,
            isDownloaded = isDownloaded,
            download = download,
            onDownload = onDownload,
            onCancel = onCancel,
            onDelete = onDelete
        )

        Spacer(Modifier.width(4.dp))

        AudioCircleButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
            sheet = sheet,
            size = 40.dp,
            filled = isCurrent,
            onClick = onPlay
        )
    }
}

@Composable
private fun DownloadButton(
    sheet: QuranSheetColors,
    isDownloaded: Boolean,
    download: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    when {
        download != null && download.isActive -> Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            // الدائرة بتوري النسبة، وجوّاها × للإلغاء — الاتنين في نفس المكان
            // عشان الصف ما يترجرجش لما التنزيل يبدأ ويخلص.
            CircularProgressIndicator(
                progress = { download.fraction },
                modifier = Modifier.size(28.dp),
                color = sheet.accent,
                trackColor = sheet.divider,
                strokeWidth = 2.dp
            )
            Icon(
                Icons.Default.Close,
                contentDescription = "إلغاء التنزيل",
                tint = sheet.inkSoft,
                modifier = Modifier.size(13.dp)
            )
        }

        isDownloaded -> Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DownloadDone,
                contentDescription = "محمّلة — اضغط للحذف",
                tint = sheet.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        download?.state == DownloadProgress.State.FAILED -> Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onDownload),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "إعادة المحاولة",
                tint = sheet.inkSoft,
                modifier = Modifier.size(20.dp)
            )
        }

        else -> Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onDownload),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = "تنزيل",
                tint = sheet.inkSoft,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun downloadSubtitle(
    download: DownloadProgress?,
    isDownloaded: Boolean,
    ayahCount: Int
): String = when {
    download != null && download.state == DownloadProgress.State.RUNNING ->
        if (download.totalParts > 1) {
            "جارٍ التنزيل — ${toArabicNumerals(download.doneParts)} من ${toArabicNumerals(download.totalParts)} آية"
        } else {
            "جارٍ التنزيل — ${formatBytes(download.bytes)}"
        }

    download?.state == DownloadProgress.State.QUEUED -> "في الانتظار…"
    download?.state == DownloadProgress.State.FAILED -> "تعذّر التنزيل — اضغط للإعادة"
    isDownloaded -> "متاحة بدون إنترنت"
    else -> "${toArabicNumerals(ayahCount)} آية"
}

/** أيقونة صغيرة تدل على أن الصف يشتغل الآن (بدون نص). */
@Composable
@Suppress("unused")
private fun PlayingBadge(sheet: QuranSheetColors) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(sheet.accent.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BufferingDots(sheet.accent)
        Spacer(Modifier.width(5.dp))
        Text("يُتلى الآن", fontFamily = AmiriFont, fontSize = 10.sp, color = sheet.accent)
    }
}
