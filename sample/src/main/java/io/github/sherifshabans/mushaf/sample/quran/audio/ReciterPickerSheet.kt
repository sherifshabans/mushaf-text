package io.github.sherifshabans.mushaf.sample.quran.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sherifshabans.mushaf.sample.audio.AyahReciter
import io.github.sherifshabans.mushaf.sample.audio.Moshaf
import io.github.sherifshabans.mushaf.sample.audio.PlaybackMode
import io.github.sherifshabans.mushaf.sample.audio.Reciter
import io.github.sherifshabans.mushaf.sample.audio.Riwaya
import io.github.sherifshabans.mushaf.sample.quran.AmiriFont
import io.github.sherifshabans.mushaf.sample.quran.QuranSheetColors
import io.github.sherifshabans.mushaf.sample.quran.rememberQuranSheetColors
import io.github.sherifshabans.mushaf.sample.quran.toArabicNumerals

/**
 * اختيار القارئ والرواية.
 *
 * ## ليه تبويبتين مش قائمة واحدة
 * «سور كاملة» و«آية آية» مش خيارين لنفس المحتوى — دول **فهرسين مختلفين من
 * مصدرين مختلفين**: ٢٢٧ قارئًا بمصاحف كاملة عند mp3quran، و٤٤ قارئًا بملف
 * لكل آية عند everyayah. لو خلطناهم في قائمة واحدة كان المستخدم هيدوّس على
 * قارئ ويلاقي «سماع الآية» مش شغّال عنده من غير تفسير. التبويبة بتقول الفرق
 * قبل الاختيار.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterPickerSheet(
    viewModel: QuranAudioViewModel,
    onDismiss: () -> Unit
) {
    val sheet = rememberQuranSheetColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selection by viewModel.selection.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val reciters by viewModel.reciters.collectAsState()

    var mode by rememberSaveable { mutableStateOf(selection.mode) }
    var query by rememberSaveable { mutableStateOf("") }
    var riwaya by rememberSaveable { mutableStateOf<Riwaya?>(null) }
    var onlyComplete by rememberSaveable { mutableStateOf(false) }
    var expandedReciter by rememberSaveable { mutableStateOf<Int?>(null) }

    // الروايات المعروضة في الفلتر مبنيّة من الداتا نفسها، فلو الفهرس اتحدّث
    // من الشبكة وجاب رواية جديدة بتظهر لوحدها من غير تعديل في الكود.
    val riwayatInData = remember(reciters, mode) {
        if (mode == PlaybackMode.AYAH) {
            io.github.sherifshabans.mushaf.sample.audio.AyahRecitersCatalog.riwayat
        } else {
            reciters.flatMap { r -> r.moshafs.map { it.riwaya } }
                .distinct()
                .sortedBy { it.ordinal }
        }
    }

    val surahResults = remember(reciters, query, riwaya, onlyComplete, mode) {
        if (mode == PlaybackMode.SURAH) viewModel.filtered(query, riwaya, onlyComplete)
        else emptyList()
    }
    val ayahResults = remember(query, riwaya, mode) {
        if (mode == PlaybackMode.AYAH) viewModel.filteredAyahReciters(query, riwaya)
        else emptyList()
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
            Text(
                text = "القرّاء والروايات",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = sheet.accent,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
            Text(
                text = if (mode == PlaybackMode.SURAH) {
                    "${toArabicNumerals(surahResults.sumOf { it.moshafs.size })} مصحفًا " +
                        "لـ${toArabicNumerals(surahResults.size)} قارئًا"
                } else {
                    "${toArabicNumerals(ayahResults.size)} قارئًا يدعمون الاستماع آية آية"
                },
                fontFamily = AmiriFont,
                fontSize = 13.sp,
                color = sheet.inkSoft,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            ModeSwitch(
                mode = mode,
                sheet = sheet,
                onChange = {
                    mode = it
                    riwaya = null
                    expandedReciter = null
                }
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث باسم الشيخ", fontFamily = AmiriFont, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = sheet.accent)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = sheet.ink,
                    unfocusedTextColor = sheet.ink,
                    focusedPlaceholderColor = sheet.inkSoft,
                    unfocusedPlaceholderColor = sheet.inkSoft,
                    focusedBorderColor = sheet.accent,
                    unfocusedBorderColor = sheet.divider,
                    cursorColor = sheet.accent
                )
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioChip("كل الروايات", riwaya == null, sheet) { riwaya = null }
                riwayatInData.forEach { r ->
                    AudioChip(r.label, riwaya == r, sheet) { riwaya = if (riwaya == r) null else r }
                }
                if (mode == PlaybackMode.SURAH) {
                    AudioChip("المصحف كامل", onlyComplete, sheet) { onlyComplete = !onlyComplete }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = sheet.divider, thickness = 0.8.dp)

            when {
                loading && mode == PlaybackMode.SURAH -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = sheet.accent)
                }

                mode == PlaybackMode.SURAH && surahResults.isEmpty() ->
                    EmptyHint("لا يوجد قارئ بهذا الاسم في هذه الرواية", sheet)

                mode == PlaybackMode.AYAH && ayahResults.isEmpty() ->
                    EmptyHint("لا يوجد قارئ بهذا الاسم في هذه الرواية", sheet)

                mode == PlaybackMode.SURAH -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    val favoriteIds = favorites.toSet()
                    val (starred, rest) = surahResults.partition { reciter ->
                        reciter.moshafs.any { it.key in favoriteIds }
                    }
                    if (starred.isNotEmpty()) {
                        item(key = "fav_header") { AudioSectionTitle("المفضّلة", sheet) }
                        items(starred, key = { "f${it.id}" }) { reciter ->
                            ReciterCard(
                                reciter = reciter,
                                sheet = sheet,
                                favorites = favoriteIds,
                                selectedKey = selection.moshaf?.key,
                                expanded = expandedReciter == reciter.id,
                                onToggleExpand = {
                                    expandedReciter = if (expandedReciter == reciter.id) null else reciter.id
                                },
                                onSelect = {
                                    viewModel.selectMoshaf(it)
                                    onDismiss()
                                },
                                onFavorite = { viewModel.toggleFavorite(it.key) }
                            )
                        }
                        item(key = "fav_divider") {
                            HorizontalDivider(
                                color = sheet.divider,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    items(rest, key = { it.id }) { reciter ->
                        ReciterCard(
                            reciter = reciter,
                            sheet = sheet,
                            favorites = favoriteIds,
                            selectedKey = selection.moshaf?.key,
                            expanded = expandedReciter == reciter.id,
                            onToggleExpand = {
                                expandedReciter = if (expandedReciter == reciter.id) null else reciter.id
                            },
                            onSelect = {
                                viewModel.selectMoshaf(it)
                                onDismiss()
                            },
                            onFavorite = { viewModel.toggleFavorite(it.key) }
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    items(ayahResults, key = { it.id }) { reciter ->
                        AyahReciterRow(
                            reciter = reciter,
                            sheet = sheet,
                            selected = selection.ayahReciter.id == reciter.id &&
                                selection.mode == PlaybackMode.AYAH,
                            onClick = {
                                viewModel.selectAyahReciter(reciter)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/** مبدّل «سور كاملة / آية آية». */
@Composable
private fun ModeSwitch(
    mode: PlaybackMode,
    sheet: QuranSheetColors,
    onChange: (PlaybackMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(sheet.accent.copy(alpha = 0.08f))
            .border(1.dp, sheet.divider, RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        listOf(
            PlaybackMode.SURAH to "سور كاملة",
            PlaybackMode.AYAH to "آية آية"
        ).forEach { (value, label) ->
            val selected = mode == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) sheet.accent else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = AmiriFont,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) sheet.onAccent else sheet.inkSoft
                )
            }
        }
    }
}

/**
 * كارت قارئ: الاسم، وتحته رواياته.
 *
 * القارئ اللي له رواية واحدة بيتشغّل بضغطة واحدة على الكارت — وده الغالب
 * (٢٠٣ من ٢٧١ مصحفًا حفص مرتل). اللي له أكتر بيفتح ويوّري رواياته.
 */
@Composable
private fun ReciterCard(
    reciter: Reciter,
    sheet: QuranSheetColors,
    favorites: Set<String>,
    selectedKey: String?,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: (Moshaf) -> Unit,
    onFavorite: (Moshaf) -> Unit
) {
    val single = reciter.moshafs.singleOrNull()
    val isSelected = reciter.moshafs.any { it.key == selectedKey }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) sheet.accent.copy(alpha = 0.10f) else sheet.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) sheet.accent else sheet.divider,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { if (single != null) onSelect(single) else onToggleExpand() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(sheet.accent.copy(alpha = 0.14f))
                    .border(1.dp, sheet.divider, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reciter.letter.take(1),
                    fontFamily = AmiriFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = sheet.accent
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = reciter.name,
                    fontFamily = AmiriFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = sheet.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (single != null) {
                        "${single.riwaya.label} • ${surahCountLabel(single)}"
                    } else {
                        reciter.riwayat.joinToString("، ") { it.label }
                    },
                    fontFamily = AmiriFont,
                    fontSize = 12.sp,
                    color = sheet.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (single != null) {
                FavoriteButton(
                    isFavorite = single.key in favorites,
                    sheet = sheet,
                    onClick = { onFavorite(single) }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "إخفاء الروايات" else "عرض الروايات",
                    tint = sheet.inkSoft,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotation)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded && single == null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(Modifier.padding(top = 8.dp)) {
                reciter.moshafs.forEach { moshaf ->
                    MoshafRow(
                        moshaf = moshaf,
                        sheet = sheet,
                        isSelected = moshaf.key == selectedKey,
                        isFavorite = moshaf.key in favorites,
                        onClick = { onSelect(moshaf) },
                        onFavorite = { onFavorite(moshaf) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun MoshafRow(
    moshaf: Moshaf,
    sheet: QuranSheetColors,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) sheet.accent.copy(alpha = 0.16f)
                else sheet.accent.copy(alpha = 0.05f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.GraphicEq,
            contentDescription = null,
            tint = sheet.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = moshaf.riwaya.label,
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = sheet.ink
            )
            Text(
                text = "${moshaf.styleLabel} • ${surahCountLabel(moshaf)}",
                fontFamily = AmiriFont,
                fontSize = 11.sp,
                color = sheet.inkSoft
            )
        }
        FavoriteButton(isFavorite, sheet, onFavorite)
    }
}

@Composable
private fun AyahReciterRow(
    reciter: AyahReciter,
    sheet: QuranSheetColors,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) sheet.accent.copy(alpha = 0.12f) else sheet.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) sheet.accent else sheet.divider,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(sheet.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.Check else Icons.Default.GraphicEq,
                contentDescription = null,
                tint = sheet.accent,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = reciter.name,
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = sheet.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${reciter.riwaya.label} • ${reciter.styleLabel}",
                fontFamily = AmiriFont,
                fontSize = 12.sp,
                color = sheet.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // البِتريت مؤشّر جودة مباشر، ومهم هنا: نفس الشيخ ممكن يكون متاحًا
        // بـ٣٢ و١٩٢ كيلوبت، والفرق مسموع.
        Text(
            text = "${toArabicNumerals(reciter.bitrateKbps)} ك.ب/ث",
            fontFamily = AmiriFont,
            fontSize = 11.sp,
            color = sheet.inkSoft
        )
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, sheet: QuranSheetColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "إزالة من المفضّلة" else "إضافة للمفضّلة",
            tint = if (isFavorite) sheet.accent else sheet.inkSoft,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptyHint(text: String, sheet: QuranSheetColors) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                tint = sheet.inkSoft.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(text, fontFamily = AmiriFont, fontSize = 14.sp, color = sheet.inkSoft)
        }
    }
}

private fun surahCountLabel(moshaf: Moshaf): String =
    if (moshaf.isComplete) "المصحف كاملًا"
    else "${toArabicNumerals(moshaf.surahList.size)} سورة"
