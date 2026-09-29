package io.github.sherifshabans.mushaf.sample.quran.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sherifshabans.mushaf.sample.audio.PlaybackMode
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioMeta
import io.github.sherifshabans.mushaf.sample.audio.RepeatMode
import io.github.sherifshabans.mushaf.sample.audio.formatDuration
import io.github.sherifshabans.mushaf.sample.audio.player.QuranPlaybackState
import io.github.sherifshabans.mushaf.sample.quran.AmiriFont
import io.github.sherifshabans.mushaf.sample.quran.QuranFont
import io.github.sherifshabans.mushaf.sample.quran.QuranSheetColors
import io.github.sherifshabans.mushaf.sample.quran.rememberQuranSheetColors
import io.github.sherifshabans.mushaf.sample.quran.toArabicNumerals

/**
 * شريط التشغيل الصغير — بيقعد فوق حافة الشاشة السفلية وهو ما يحجبش المصحف.
 *
 * الشريط ده هو **الحضور الدائم** للتلاوة: طول ما فيه مقطع محمّل بيفضل ظاهرًا،
 * فالمستخدم عمره ما «يفقد» التلاوة الشغّالة وهو بيقلّب صفحات. الضغط عليه
 * بيفتح المشغّل الكامل.
 */
@Composable
fun QuranMiniPlayer(
    state: QuranPlaybackState,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val sheet = rememberQuranSheetColors()

    AnimatedVisibility(
        visible = state.hasTrack,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(sheet.surface)
                .border(1.dp, sheet.divider, RoundedCornerShape(18.dp))
                .clickable(onClick = onExpand)
        ) {
            ThinProgress(
                fraction = state.progress,
                color = sheet.accent,
                trackColor = sheet.divider.copy(alpha = 0.4f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(sheet.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isBuffering) {
                        BufferingDots(sheet.accent)
                    } else {
                        Text(
                            text = toArabicNumerals(state.surah.coerceAtLeast(1)),
                            fontFamily = AmiriFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = sheet.accent
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = miniTitle(state),
                        fontFamily = AmiriFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = sheet.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.reciterName,
                        fontFamily = AmiriFont,
                        fontSize = 12.sp,
                        color = sheet.inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AudioCircleButton(
                    icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                    sheet = sheet,
                    size = 42.dp,
                    filled = true,
                    onClick = onPlayPause
                )
                Spacer(Modifier.width(6.dp))
                AudioCircleButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "التالي",
                    sheet = sheet,
                    size = 36.dp,
                    enabled = state.hasNext,
                    onClick = onNext
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "إغلاق المشغّل",
                        tint = sheet.inkSoft,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * المشغّل الكامل.
 *
 * كل تحكّم هنا موجود لأنه بيحلّ مشكلة في الاستماع للقرآن تحديدًا: التكرار
 * لحفظ الآية، السرعة لمن يقرأ خلف الشيخ، مؤقّت النوم لمن يسمع قبل النوم،
 * والتنزيل لمن يسمع في طريق مافيهوش شبكة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranPlayerSheet(
    viewModel: QuranAudioViewModel,
    state: QuranPlaybackState,
    isDownloaded: Boolean,
    onOpenReciters: () -> Unit,
    onOpenSurahs: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheet = rememberQuranSheetColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val followPage by viewModel.followPlayback.collectAsState()

    // أثناء سحب المؤشّر بنعرض مكان الإصبع لا مكان التشغيل، وإلا كان المؤشّر
    // بيرجع لمكانه كل نص ثانية مع تحديث الموضع فيبقى السحب مستحيل.
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheet.surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SurahMedallion(state = state, sheet = sheet)

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (state.mode == PlaybackMode.AYAH && state.ayah != null) {
                    "سُورَةُ ${state.surahName} — آية ${toArabicNumerals(state.ayah)}"
                } else {
                    "سُورَةُ ${state.surahName}"
                },
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = sheet.ink,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(sheet.accent.copy(alpha = 0.10f))
                    .clickable(onClick = onOpenReciters)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.reciterName.ifBlank { "اختر القارئ" },
                    fontFamily = AmiriFont,
                    fontSize = 14.sp,
                    color = sheet.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 220.dp)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "تغيير القارئ",
                    tint = sheet.accent,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (state.riwayaLabel.isNotBlank()) {
                Text(
                    text = state.riwayaLabel,
                    fontFamily = AmiriFont,
                    fontSize = 12.sp,
                    color = sheet.inkSoft,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── شريط الموضع ────────────────────────────────────────────────
            Slider(
                value = if (scrubbing) scrubValue else state.progress,
                onValueChange = {
                    scrubbing = true
                    scrubValue = it
                },
                onValueChangeFinished = {
                    if (state.durationMs > 0) {
                        viewModel.seekTo((scrubValue * state.durationMs).toLong())
                    }
                    scrubbing = false
                },
                enabled = state.durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = sheet.accent,
                    activeTrackColor = sheet.accent,
                    inactiveTrackColor = sheet.divider
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(
                        if (scrubbing) (scrubValue * state.durationMs).toLong()
                        else state.positionMs
                    ),
                    fontFamily = AmiriFont,
                    fontSize = 12.sp,
                    color = sheet.inkSoft
                )
                Text(
                    text = formatDuration(state.durationMs),
                    fontFamily = AmiriFont,
                    fontSize = 12.sp,
                    color = sheet.inkSoft
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── أزرار التشغيل ─────────────────────────────────────────────
            // الاتجاه مثبّت LTR عن قصد: «السابق» على الشمال و«التالي» على
            // اليمين زي أي مشغّل صوت، مهما كانت لغة الجهاز. لو سِبناها تتقلب
            // مع RTL كان السهم هيشاور ناحية والزرار يودّي ناحية تانية.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Ltr
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AudioCircleButton(
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = "السابق",
                        sheet = sheet,
                        size = 46.dp,
                        onClick = viewModel::previous
                    )
                    Spacer(Modifier.width(10.dp))
                    AudioCircleButton(
                        icon = Icons.Default.Replay10,
                        contentDescription = "رجوع ١٠ ثوانٍ",
                        sheet = sheet,
                        size = 42.dp,
                        onClick = { viewModel.seekBy(-10_000L) }
                    )
                    Spacer(Modifier.width(12.dp))
                    AudioCircleButton(
                        icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                        sheet = sheet,
                        size = 68.dp,
                        filled = true,
                        onClick = viewModel::togglePlayPause
                    )
                    Spacer(Modifier.width(12.dp))
                    AudioCircleButton(
                        icon = Icons.Default.Forward10,
                        contentDescription = "تقدّم ١٠ ثوانٍ",
                        sheet = sheet,
                        size = 42.dp,
                        onClick = { viewModel.seekBy(10_000L) }
                    )
                    Spacer(Modifier.width(10.dp))
                    AudioCircleButton(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "التالي",
                        sheet = sheet,
                        size = 46.dp,
                        enabled = state.hasNext,
                        onClick = viewModel::next
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── تكرار / سرعة / مؤقّت / تنزيل / قائمة السور ────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioChip(
                    label = repeatLabel(state.repeatMode),
                    selected = state.repeatMode != RepeatMode.OFF,
                    sheet = sheet,
                    icon = if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne
                    else Icons.Default.Repeat,
                    onClick = viewModel::cycleRepeatMode
                )
                AudioChip(
                    label = "قائمة السور",
                    selected = false,
                    sheet = sheet,
                    icon = Icons.Default.MenuBook,
                    onClick = onOpenSurahs
                )
                AudioChip(
                    label = if (isDownloaded) "محمّلة" else "تنزيل السورة",
                    selected = isDownloaded,
                    sheet = sheet,
                    icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                    onClick = { if (!isDownloaded) viewModel.download(state.surah) }
                )
                // متابعة الصفحة اختيارية: فيه ناس بتسمع وهي بتقرأ في مكان
                // تاني من المصحف، والقفز التلقائي كان بيسحب الصفحة من تحت
                // إيديهم مع كل آية.
                AudioChip(
                    label = "متابعة الصفحة",
                    selected = followPage,
                    sheet = sheet,
                    icon = Icons.Default.AutoStories,
                    onClick = { viewModel.setFollowPlayback(!followPage) }
                )
            }

            Spacer(Modifier.height(10.dp))

            SpeedRow(state = state, sheet = sheet, onSpeed = viewModel::setSpeed)

            Spacer(Modifier.height(8.dp))

            SleepTimerRow(
                minutesLeft = state.sleepTimerMinutesLeft,
                sheet = sheet,
                onSet = viewModel::startSleepTimer,
                onCancel = viewModel::cancelSleepTimer
            )

            if (state.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.error,
                    fontFamily = AmiriFont,
                    fontSize = 13.sp,
                    color = sheet.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(sheet.accent.copy(alpha = 0.10f))
                        .clickable { viewModel.clearError() }
                        .padding(10.dp)
                )
            }
        }
    }
}

/**
 * «ميدالية» السورة — بديل غلاف الألبوم.
 *
 * صورة غلاف مالهاش معنى في المصحف، وشعار التطبيق مكرّر. فبنرسم زخرفة
 * هندسية بسيطة (مربّعان متقاطعان ودائرة — «خاتم سليمان» المعروف في الزخرفة
 * الإسلامية) واسم السورة بخط المصحف في نصّها.
 */
@Composable
private fun SurahMedallion(state: QuranPlaybackState, sheet: QuranSheetColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        sheet.accent.copy(alpha = 0.20f),
                        sheet.accent.copy(alpha = 0.06f)
                    )
                )
            )
            .border(1.dp, sheet.divider, RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            val stroke = Stroke(width = 1.2.dp.toPx())
            val side = size.minDimension * 0.72f
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val square = Size(side, side)
            val color = sheet.accent.copy(alpha = 0.28f)
            // المربّعان متقاطعان بزاوية ٤٥°، والدوران حوالين مركز اللوحة —
            // الافتراضي في withTransform هو مركز الكانفس، وهو نفسه هنا لأن
            // المربّع متمركز أصلًا.
            drawRect(color = color, topLeft = topLeft, size = square, style = stroke)
            rotate(45f) {
                drawRect(color = color, topLeft = topLeft, size = square, style = stroke)
            }
            drawCircle(color = color, radius = side * 0.60f, style = stroke)
            drawCircle(
                color = sheet.accent.copy(alpha = 0.16f),
                radius = side * 0.44f,
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.surahName,
                fontFamily = QuranFont,
                fontSize = 30.sp,
                color = sheet.ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 30.dp)
            )
            if (state.surah > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${toArabicNumerals(QuranAudioMeta.ayahCount(state.surah))} آية",
                    fontFamily = AmiriFont,
                    fontSize = 12.sp,
                    color = sheet.inkSoft
                )
            }
        }
    }
}

@Composable
private fun SpeedRow(
    state: QuranPlaybackState,
    sheet: QuranSheetColors,
    onSpeed: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Speed,
            contentDescription = null,
            tint = sheet.inkSoft,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                AudioChip(
                    label = speedLabel(speed),
                    // المقارنة بهامش: المشغّل بيرجّع السرعة float فممكن ترجع
                    // ٠٫٩٩٩٩ بدل ١ فتبان كل الأزرار غير مختارة.
                    selected = kotlin.math.abs(state.speed - speed) < 0.01f,
                    sheet = sheet,
                    onClick = { onSpeed(speed) }
                )
            }
        }
    }
}

@Composable
private fun SleepTimerRow(
    minutesLeft: Int?,
    sheet: QuranSheetColors,
    onSet: (Int) -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Bedtime,
            contentDescription = null,
            tint = sheet.inkSoft,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (minutesLeft != null) {
                AudioChip(
                    label = "يتوقّف بعد ${toArabicNumerals(minutesLeft)} دقيقة",
                    selected = true,
                    sheet = sheet,
                    onClick = onCancel
                )
            } else {
                listOf(10, 15, 30, 60).forEach { minutes ->
                    AudioChip(
                        label = "${toArabicNumerals(minutes)} دقيقة",
                        selected = false,
                        sheet = sheet,
                        onClick = { onSet(minutes) }
                    )
                }
            }
        }
    }
}

private fun miniTitle(state: QuranPlaybackState): String =
    if (state.mode == PlaybackMode.AYAH && state.ayah != null) {
        "${state.surahName} — آية ${toArabicNumerals(state.ayah)}"
    } else {
        "سُورَةُ ${state.surahName}"
    }

private fun repeatLabel(mode: RepeatMode): String = when (mode) {
    RepeatMode.OFF -> "بدون تكرار"
    RepeatMode.ONE -> "تكرار المقطع"
    RepeatMode.ALL -> "تكرار القائمة"
}

private fun speedLabel(speed: Float): String = when (speed) {
    0.75f -> "٠٫٧٥×"
    1f -> "عادي"
    1.25f -> "١٫٢٥×"
    1.5f -> "١٫٥×"
    else -> "٢×"
}

/** سهم صغير يوحي إن الشريط بيتفتح — يظهر في المشغّل الصغير على الشاشات الواسعة. */
@Composable
@Suppress("unused")
private fun ExpandHint(sheet: QuranSheetColors) {
    Icon(
        Icons.Default.ExpandLess,
        contentDescription = null,
        tint = sheet.inkSoft,
        modifier = Modifier.size(16.dp)
    )
}
