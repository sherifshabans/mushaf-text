package io.github.sherifshabans.mushaf.sample.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioPrefs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import io.github.sherifshabans.mushaf.sample.R
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayCircle
import io.github.sherifshabans.mushaf.sample.audio.PlaybackMode
import io.github.sherifshabans.mushaf.sample.quran.audio.QuranAudioViewModel
import io.github.sherifshabans.mushaf.sample.quran.audio.QuranMiniPlayer
import io.github.sherifshabans.mushaf.sample.quran.audio.QuranPlayerSheet
import io.github.sherifshabans.mushaf.sample.quran.audio.ReciterPickerSheet
import io.github.sherifshabans.mushaf.sample.quran.audio.SurahAudioSheet
import io.github.sherifshabans.mushaf.sample.data.DomainAya
import io.github.sherifshabans.mushaf.sample.data.DomainAyaWithTafseer
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sherifshabans.mushaf.MushafColors
import io.github.sherifshabans.mushaf.MushafFont
import io.github.sherifshabans.mushaf.MushafPage
import io.github.sherifshabans.mushaf.TajweedAnnotator
import io.github.sherifshabans.mushaf.TajweedRule
import io.github.sherifshabans.mushaf.sample.SampleGraph
import io.github.sherifshabans.mushaf.sample.data.QuranData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ─── Design Tokens ─────────────────────────────────────────────────────────────
// The primary (interactive/highlight) color is always pulled from the active theme
// via MaterialTheme.colorScheme.primary. The constants below are the fixed
// "Mus'haf paper" palette that frames that primary color.
internal object QuranPalette {
    val Paper = Color(0xFFFAF6EB)        // Warm, soft cream page background
    val Gold = Color(0xFFC4B275)         // Premium gold for borders / decorative frames
    val Banner = Color(0xFFF4EDE0)       // Light beige for the surah header banner
    val Ink = Color(0xFF1A1208)          // Near-black ink for the Quranic text
    val Brown = Color(0xFF6B4F3A)        // Refined dark brown for header / page numbers
    val Highlight = Color(0xFFEADDC9)    // Soft peach/beige for the selected verse
    // الآية اللي بتتلى دلوقتي — أخضر مريح مختلف عن ذهبي التحديد، عشان اللي
    // بيتابع التلاوة يفرّق بلمحة بين «اللي اخترته» و«اللي بيتقال».
    val Playing = Color(0xFFCFE3CC)
    @Suppress("unused")
    val Band = Color(0xFFEFE6D3)         // (لم يعد مستخدمًا — أُزيل تظليل صفحتي الفاتحة وأول البقرة)
    val CardSurface = Color(0xFFFDFCFA)  // Off-white floating card surface
}

/**
 * حجم خط المصحف — ثابت.
 *
 * الراندرر بيصغّره تلقائيًا لو صفحة معيّنة محتاجة كده عشان سطورها الـ١٥ تدخل،
 * فالمقاس ده هو **الحد الأعلى** مش قيمة مفروضة على كل صفحة. تكبير/تصغير يدوي
 * مالوش معنى هنا: الصفحة لازم تفضل ١٥ سطرًا زي المصحف المطبوع، وأي تكبير
 * بيكسّر ده.
 */
/**
 * الحد الأقصى لحجم خط المصحف.
 *
 * على الموبايل **عرض الصفحة** هو اللي بيحدّ الحجم مش الرقم ده (أوسع سطر لازم
 * يدخل)، فالرقم ده بيبان بس على الشاشات العريضة. كان ٢٢ فكان بيقصّر الخط من غير
 * داعٍ على التابلت.
 */
internal const val MUSHAF_FONT_SIZE = 34f

// ─── Fonts ─────────────────────────────────────────────────────────────────────
/**
 * خط واجهة التطبيق (العناوين، القوائم، الأزرار).
 * مبنيّ جوّه التطبيق مش downloadable — مفيش اعتماد على النت ولا خدمات Google.
 */
internal val AmiriFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.amiri_regular, FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.amiri_bold, FontWeight.Bold)
)

/**
 * خط نص المصحف: **KFGQPC HAFS Uthmanic Script** — خط مجمّع الملك فهد لطباعة
 * المصحف الشريف، وهو نفس الخط اللي مصحف المدينة مرسوم بيه.
 *
 * ## ليه هو بالذات
 * نص قاعدة البيانات بترميز KFGQPC (٣٧٬١٤٨ استخدام لـ U+06E1 كسكون مقابل ٣٬٩٨٨
 * للسكون القياسي). في الترميز ده `U+0657` و`U+065E` بيشيلوا **التنوين المرصوص
 * رأسيًا** (٤٬٧٠٨ حالة). أي خط مش مصمّم للترميز ده بيرسمهم حرفيًا غلط — Amiri
 * كان بيرسم U+0657 «ضمة مقلوبة» (خُطّيف واحد) و U+065E «فتحة بنقطتين»، فالتنوين
 * كان باين ناقصًا. والخط ده بيرسمهم شرطتين مرصوصتين وضمتين مرصوصتين زي المطبوع،
 * ومدّته مضغوطة (٥٦ وحدة مقابل ٩٣ في Amiri).
 *
 * بيغطّي الـ٨٣ حرفًا المستخدمة في نص المصحف كله — صفر حرف ناقص.
 *
 * ## علامة نهاية الآية
 * الخط ده بيعمل رابطة (`rlig`) من أرقام الآية لحرف واحد **بيرسم الوردة والرقم
 * جوّاها مظبوطين**. فالعلامة عندنا بقت **الأرقام لوحدها** بدون `U+06DD` — لو
 * حطّينا U+06DD قبلها بيطلع وردتين. شوف [ayaMarkerText].
 *
 * ## ممنوع تعديله
 * رخصة المجمّع بتسمح بالاستخدام والنسخ والتوزيع مجانًا، وبتمنع التعديل صراحةً.
 * فالملف مبنيّ كما هو بالظبط — أي إصلاح لازم يبقى في الكود أو في الأصول
 * المولّدة، مش في الخط. النص الكامل للرخصة في `licenses/`.
 */
internal val QuranFont = MushafFont

// ─── Aya Marker ────────────────────────────────────────────────────────────────
/**
 * علامة نهاية الآية = **أرقام الآية لوحدها**، من غير `U+06DD`.
 *
 * خط المصحف (KFGQPC Hafs) فيه رابطة `rlig` بتحوّل أرقام الآية لحرف واحد مركّب
 * من `uni06DD` + أرقامها — يعني **الوردة والرقم جوّاها في حرف واحد** بعرض
 * ٠٫٧٧٩em، والرقم متمركز بتصميم الخط نفسه.
 *
 * فلو كتبنا `U+06DD` قبل الأرقام بيطلع **وردتين**: واحدة من الرابطة وواحدة من
 * الحرف الصريح (العرض بيتضاعف لـ١٫٥٥٨em). متحقَّق بالتشكيل عبر HarfBuzz — نفس
 * محرّك أندرويد — والرابطة بتشتغل في الاتجاهين LTR وRTL فمافيش اعتماد على
 * ترتيب الـBiDi.
 *
 * وده كمان شال الرسم اليدوي للرقم جوّه الوردة اللي كان لازم مع Amiri (الخط ده
 * ماكانش بيقدر يحصر الرقم لأن `U+06DD` تصنيفه AL والأرقام AN فبيتفصلوا لرَنّين).
 *
 * @param reversed الأرقام بترتيب مقلوب. **مش تخمين** — بيتحدّد بالقياس وقت
 *   التشغيل عبر [markerDigitsReversed]، لأن أندرويد بيوصّل رَنّ الأرقام
 *   للمشكِّل مقلوبًا فالرابطة بتتكوّن غلط.
 */
internal fun ayaMarkerText(ayaNo: Int, reversed: Boolean): String =
    toArabicNumerals(ayaNo).let { if (reversed) it.reversed() else it }


/** نص البسملة بالرسم العثماني — نفس رسم قاعدة البيانات. مشترك بين الرسم والقياس. */
internal const val BASMALA = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

/** عنوان بانر السورة. مشترك بين الرسم والقياس. */
internal fun surahBannerText(nameArabic: String) = "سُورَةُ $nameArabic"

// ─── Ayah Text Cleaner ────────────────────────────────────────────────────────
/** Removes the trailing ayah-number token some sources append to the verse text. */
private fun String.trimAyaNumber(): String {
    val trimmed = this.trim().replace('\u00A0', ' ')
    val lastSpace = trimmed.lastIndexOf(' ')
    if (lastSpace != -1) {
        val lastWord = trimmed.substring(lastSpace + 1)
        val isNumber = lastWord.all { it.isDigit() || it in '٠'..'٩' }
        if (isNumber) {
            return trimmed.substring(0, lastSpace).trim()
        }
    }
    return trimmed
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuranScreen(
    onBackClick: () -> Unit,
    viewModel: QuranViewModel = viewModel(),
    audioViewModel: QuranAudioViewModel = viewModel {
        QuranAudioViewModel(
            context = SampleGraph.appContext,
            recitersRepository = SampleGraph.reciters,
            downloader = SampleGraph.downloader,
            prefs = SampleGraph.audioPrefs,
            player = SampleGraph.player
        )
    }
) {
    val primary = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    val ayahSkin = if (isDarkMode) AyahShareImage.Skin.DARK else AyahShareImage.Skin.LIGHT

    // null = الصفحة المحفوظة لم تصل من القرص بعد
    val savedPage by viewModel.savedPage.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val ayaMarks by viewModel.ayaMarks.collectAsState()
    val marksByAyaId = remember(ayaMarks) { ayaMarks.associateBy { it.ayaId } }

    // ـــ وضع التجويد ــــــــــــــــــــــــــــــــــــــــــــــــ
    //
    // المفتاح بيتقرا مرة عند فتح الشاشة وبيتكتب فورًا مع كل تبديل: ده وضع
    // بيتقفل ويتفتح وسط القراءة نفسها، فلازم يفتكر اختيار القارئ بين الجلسات.
    var tajweedOn by rememberSaveable { mutableStateOf(TajweedPrefs.isEnabled(ctx)) }
    var tajweedNaturalMadd by rememberSaveable {
        mutableStateOf(TajweedPrefs.showNaturalMadd(ctx))
    }
    var tajweedHit by remember { mutableStateOf<TajweedHit?>(null) }
    /**
     * الحكم اللي القارئ طلب يشوف أول موضع له.
     *
     * بيتمسح لوحده بعد ست ثوانٍ: الإطار غرضه يوجّه العين مرة، ولو فضل بيتحوّل
     * لتشويش على الصفحة.
     */
    var tajweedFocus by remember { mutableStateOf<TajweedRule?>(null) }
    var tajweedFocusIndex by remember { mutableIntStateOf(0) }


    val pagerState = rememberPagerState(
        initialPage = (viewModel.savedPage.value ?: 1) - 1,
        pageCount = { 604 }
    )

    /**
     * نستعيد آخر صفحة **مرة واحدة**، ولا نكتب أي صفحة على القرص قبل ذلك.
     *
     * القراءة من DataStore غير متزامنة، فأول تركيب للشاشة يبدأ عند الصفحة 1
     * وكان يكتبها فوراً فوق الصفحة المحفوظة قبل وصولها — فيرجع المصحف للفاتحة
     * في كل فتح. الحارس هنا يعكس الترتيب: نقرأ أولاً ثم نسمح بالكتابة.
     *
     * `rememberSaveable` كي لا تتكرر القفزة بعد تدوير الشاشة (الـ pager يحفظ
     * موضعه بنفسه).
     */
    var isPageRestored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(savedPage) {
        val page = savedPage ?: return@LaunchedEffect
        if (!isPageRestored) {
            val target = (page - 1).coerceIn(0, 603)
            if (pagerState.currentPage != target) pagerState.scrollToPage(target)
            isPageRestored = true
        }
    }
    LaunchedEffect(pagerState.currentPage, isPageRestored) {
        if (isPageRestored) viewModel.setPage(pagerState.currentPage + 1)
    }

    // Selected verse drives the highlight + the ayah bottom sheets.
    var activeAyah by remember { mutableStateOf<DomainAyaWithTafseer?>(null) }
    var ayahSheet by remember { mutableStateOf<AyahSheetType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // المشاركة تبدأ من الآية التي وقف عليها، ويحدّد مداها بالأرقام في شيتها.
    var shareAnchor by remember { mutableStateOf<DomainAyaWithTafseer?>(null) }

    // Navigation pickers.
    var browseTab by remember { mutableStateOf<BrowseTab?>(null) }
    var showAyaSearch by remember { mutableStateOf(false) }

    // ─── الاستماع ────────────────────────────────────────────────────────
    val playback by audioViewModel.playback.collectAsState()
    val audioSelection by audioViewModel.selection.collectAsState()
    val downloadedSurahs by audioViewModel.downloadedSurahs.collectAsState()
    val followPlayback by audioViewModel.followPlayback.collectAsState()
    var showReciters by remember { mutableStateOf(false) }
    var showSurahAudio by remember { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }

    // ─── الخروج والتلاوة شغّالة ──────────────────────────────────────────
    //
    // المشغّل خدمة بتعيش أطول من الشاشة عن قصد — التلاوة المفروض تكمّل
    // والمستخدم بيقرا حاجة تانية. بس ده مش واضح: ناس بتخرج وهي فاكرة إنها
    // وقّفت التلاوة وتلاقيها لسه شغّالة. فبنسأله مرة واحدة، ولو حبّ يفتكر
    // اختياره ما نسألوش تاني.
    val exitChoice by audioViewModel.exitPlaybackChoice.collectAsState()
    var askBeforeLeaving by remember { mutableStateOf(false) }

    fun leaveQuran() {
        when {
            !playback.hasTrack -> onBackClick()
            exitChoice == QuranAudioPrefs.ExitChoice.KEEP_PLAYING -> onBackClick()
            exitChoice == QuranAudioPrefs.ExitChoice.STOP -> {
                audioViewModel.stop()
                onBackClick()
            }
            else -> askBeforeLeaving = true
        }
    }

    // زرار الرجوع بتاع النظام بيعدّي على نفس السؤال زي سهم الترويسة.
    // الشيتات المفتوحة بتاخد الرجوع لنفسها الأول (بتسجّل معالجها بعد ده في
    // التركيب)، فالسؤال ما بيظهرش وإحنا بنقفل شيت.
    BackHandler { leaveQuran() }

    fun closeAyahSheets() {
        activeAyah = null
        ayahSheet = null
    }

    // أحكام الصفحة المفتوحة دلوقتي وعددها — بيغذّوا «أحكام هذه الصفحة».
    // بنعيد حسابهم هنا بدل ما نطلّعهم من الراندرر: ده بيخلّي الورقة مستقلة عن
    // دورة رسم الصفحة، والحسبة ١٥ سطرًا مش أكتر.
    val visiblePageAyat = rememberQuranPageContent(pagerState.currentPage + 1)
    val pageRuleCounts = remember(visiblePageAyat, tajweedOn, tajweedNaturalMadd) {
        if (!tajweedOn || visiblePageAyat == null) emptyList()
        else visiblePageAyat
            .flatMap { item ->
                val text = item.aya.ayaText.cleanAyaText()
                val spans = TajweedAnnotator.annotate(
                    text = text,
                    includeNaturalMadd = tajweedNaturalMadd
                )
                // موضع واحد = كلمة واحدة. الكلمة اللي فيها حكمين من نفس النوع
                // بتتعدّ مرة، عشان الرقم ده هو نفسه طول دورة التنقّل في
                // [TajweedFocusBar] — لو اتعدّت مرتين كان العدّاد هيوعد بموضع
                // مش موجود.
                val seen = HashSet<Pair<Int, TajweedRule>>()
                spans.mapNotNull { sp ->
                    var start = sp.start
                    while (start > 0 && text[start - 1] != ' ') start--
                    if (seen.add(start to sp.rule)) sp.rule else null
                }
            }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    // التوقيف بيتلغي لمّا القارئ يقلب الصفحة: مواضع الحكم بتخصّ صفحتها.
    LaunchedEffect(pagerState.currentPage) { tajweedFocus = null }

    // Instant jump (no animation) so far-apart pages don't scroll through everything.
    val goToPage: (Int) -> Unit = { page ->
        scope.launch { pagerState.scrollToPage((page - 1).coerceIn(0, 603)) }
    }

    Scaffold(
        containerColor = QuranPalette.Paper,
        topBar = {
            TopAppBar(
                title = {
                    // العنوان بيقاسم الشريط مع أربع أيقونات، فمساحته ضيّقة أصلًا.
                    // من غير `maxLines` كان بيتلفّ لسطرين مع خط الجهاز المكبّر
                    // ويطوّل الشريط على حساب الصفحة.
                    Text(
                        text = "صَفْحَة ${toArabicNumerals(pagerState.currentPage + 1)}",
                        fontFamily = AmiriFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = QuranPalette.Brown,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { leaveQuran() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = primary
                        )
                    }
                },
                actions = {
                    // التلاتة ظاهرين على طول بدل ما يكونوا جوّه قائمة «المزيد»،
                    // وفهرس السور أولهم من الشمال.
                    //
                    // الاتجاه هنا مثبّت LTR عمدًا: ترتيب الأيقونات بصريًا بقى
                    // مستقلًّا عن اتجاه اللغة، فأول واحدة مكتوبة هي أول واحدة
                    // على الشمال مهما كان الجهاز عربي أو إنجليزي.
                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // السمّاعة أول أيقونة: الاستماع أكتر حاجة بتتفتح
                            // من المصحف بعد التنقّل نفسه.
                            IconButton(
                                onClick = {
                                    if (playback.hasTrack) showPlayer = true
                                    else showSurahAudio = true
                                }
                            ) {
                                Icon(
                                    imageVector = if (playback.isPlaying) Icons.Default.PlayCircle
                                    else Icons.Default.Headphones,
                                    contentDescription = "الاستماع للتلاوة",
                                    tint = primary
                                )
                            }
                            // زرار التجويد: ضغطة = فتح/قفل، وضغطة مطوّلة =
                            // الورقة بالمفاتيح ومفتاح الألوان.
                            //
                            // الوضع بيتبدّل وسط القراءة، فلازم يبقى على بُعد
                            // لمسة واحدة — لو كان في الإعدادات بس كان كل تبديل
                            // خروج من الصفحة ورجوع.
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        onClick = {
                                            tajweedOn = !tajweedOn
                                            TajweedPrefs.setEnabled(ctx, tajweedOn)
                                        },
                                        onLongClick = { browseTab = BrowseTab.TAJWEED }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription =
                                        if (tajweedOn) "إغلاق تلوين التجويد"
                                        else "تلوين التجويد",
                                    tint = if (tajweedOn) TajweedRule.IDGHAM_GHUNNA.color
                                    else primary.copy(alpha = 0.55f)
                                )
                            }
                            IconButton(onClick = { showAyaSearch = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "بحث في القرآن",
                                    tint = primary
                                )
                            }
                            // آخر أيقونة: بتفتح ورقة فيها الفهرس والختمات
                            // وعلاماتي والتجويد في مكان واحد.
                            IconButton(onClick = { browseTab = BrowseTab.INDEX }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = "الفهرس والختمات",
                                    tint = primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = QuranPalette.Paper
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(QuranPalette.Paper)
        ) {
          Column(Modifier.fillMaxSize()) {
            // شريط الوِرد اتشال من هنا.
            //
            // كان واخد ٥٠dp من ارتفاع الصفحة في **كل** صفحة، وصفحة المصحف
            // محسوبة على إنها تطلّع ١٥ سطرًا — فأي حاجة بتاخد من ارتفاعها
            // بتصغّر الخط وتنزّل الصفحة. وهو أصلًا بقى شريطًا لأن الترويسة كانت
            // مخنوقة بأربع أيقونات، يعني كان بيصلّح العَرض ويدفع التمن من
            // الارتفاع.
            //
            // دلوقتي الختمة تبويب في [QuranBrowseSheet] — مدخلها لسه موجود من
            // جوّه التطبيق، والصفحة رجعت تاخد ارتفاعها كامل.

            // Quran reads right-to-left: page flow + text layout are forced to RTL.
            Box(Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    // نخفيه (لا نلغيه) حتى تُستعاد الصفحة المحفوظة، فلا تلمح
                    // الفاتحة للحظة قبل القفز — والـ pager يظل مركّباً كي تعمل
                    // عليه القفزة فوراً.
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = if (isPageRestored) 1f else 0f }
                ) { pageIndex ->
                    val pageNumber = pageIndex + 1
                    val content = rememberQuranPageContent(page = pageNumber)

                    when {
                        // Data is local & fast — show a blank page (no spinner) for the
                        // brief moment before the ayat resolve, to avoid a loading flash.
                        content == null -> Box(Modifier.fillMaxSize().background(QuranPalette.Paper))

                        content.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد آيات في هذه الصفحة",
                                fontFamily = AmiriFont,
                                fontSize = 14.sp,
                                color = QuranPalette.Brown.copy(alpha = 0.6f)
                            )
                        }

                        else -> QuranPageContent(
                            ayat = content,
                            fontSize = MUSHAF_FONT_SIZE,
                            pageNumber = pageNumber,
                            selectedAyaIds = setOfNotNull(activeAyah?.aya?.id),
                            marksByAyaId = marksByAyaId,
                            // الآية اللي بتتلى دلوقتي بتتحدّد بالسورة ورقم
                            // الآية لا بالمعرّف العام: المشغّل بيرجّع الاتنين
                            // دول في الـmediaId، والصفحة هي اللي عندها
                            // المعرّفات — فالمطابقة بتحصل هنا مرة لكل صفحة.
                            playingAyaIds = remember(content, playback.surah, playback.ayah) {
                                val ayah = playback.ayah
                                if (ayah == null) emptySet() else content
                                    .filter { it.aya.sora == playback.surah && it.aya.ayaNo == ayah }
                                    .map { it.aya.id }
                                    .toSet()
                            },
                            onAyaClick = { aya ->
                                activeAyah = aya
                                ayahSheet = AyahSheetType.ACTIONS
                            },
                            tajweed = tajweedOn,
                            tajweedNaturalMadd = tajweedNaturalMadd,
                            onTajweedClick = if (tajweedOn) {
                                { hit ->
                                    tajweedFocus = null
                                    tajweedHit = hit
                                }
                            } else null,
                            focusRule = if (pageNumber == pagerState.currentPage + 1) {
                                tajweedFocus
                            } else null,
                            focusIndex = tajweedFocusIndex
                        )
                    }
                }
            }

            }
          }

            // شريط التنقّل بين مواضع الحكم — عائم فوق الصفحة، ما بياخدش من
            // ارتفاعها.
            tajweedFocus?.let { rule ->
                val total = pageRuleCounts.firstOrNull { it.first == rule }?.second ?: 0
                TajweedFocusBar(
                    rule = rule,
                    index = tajweedFocusIndex,
                    total = total,
                    onPrevious = { tajweedFocusIndex-- },
                    onNext = { tajweedFocusIndex++ },
                    onClose = { tajweedFocus = null },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // شريط التلاوة الشغّالة — فوق حافة الشاشة، وبيختفي لوحده لما
            // مايكونش فيه تشغيل.
            QuranMiniPlayer(
                state = playback,
                modifier = Modifier.align(Alignment.BottomCenter),
                onExpand = { showPlayer = true },
                onPlayPause = { audioViewModel.togglePlayPause() },
                onNext = { audioViewModel.next() },
                onClose = { audioViewModel.stop() }
            )
        }
    }

    /**
     * المصحف بيتبع التلاوة.
     *
     * القفزة بتحصل على **صفحة** الآية أو أول صفحة في السورة، وبس لما تكون
     * الصفحة مختلفة فعلًا — من غير الشرط ده كان كل تحديث للموضع (كل نص ثانية)
     * بيعيد تحديد الصفحة فيقفل أي تصفّح يدوي للمستخدم.
     */
    LaunchedEffect(playback.surah, playback.ayah, followPlayback, isPageRestored) {
        if (!followPlayback || !isPageRestored || !playback.hasTrack) return@LaunchedEffect
        val target = when {
            playback.ayah != null -> viewModel.pageOfAyah(
                sura = playback.surah,
                ayaNo = playback.ayah!!,
                hintPage = pagerState.currentPage + 1
            )
            playback.surah > 0 -> SuraList.getOrNull(playback.surah - 1)?.startPage
            else -> null
        } ?: return@LaunchedEffect
        if (pagerState.currentPage != target - 1) {
            pagerState.scrollToPage((target - 1).coerceIn(0, 603))
        }
    }

    // ─── شيتات الاستماع ──────────────────────────────────────────────────
    if (showSurahAudio) {
        SurahAudioSheet(
            viewModel = audioViewModel,
            currentPage = pagerState.currentPage + 1,
            onOpenReciters = {
                showSurahAudio = false
                showReciters = true
            },
            onDismiss = { showSurahAudio = false }
        )
    }

    if (showReciters) {
        ReciterPickerSheet(
            viewModel = audioViewModel,
            onDismiss = { showReciters = false }
        )
    }

    if (askBeforeLeaving) {
        ExitPlaybackDialog(
            reciterName = playback.reciterName,
            trackTitle = playback.title,
            onKeepPlaying = { remember ->
                if (remember) {
                    audioViewModel.rememberExitChoice(QuranAudioPrefs.ExitChoice.KEEP_PLAYING)
                }
                askBeforeLeaving = false
                onBackClick()
            },
            onStop = { remember ->
                if (remember) {
                    audioViewModel.rememberExitChoice(QuranAudioPrefs.ExitChoice.STOP)
                }
                askBeforeLeaving = false
                audioViewModel.stop()
                onBackClick()
            },
            onDismiss = { askBeforeLeaving = false }
        )
    }

    if (showPlayer && playback.hasTrack) {
        QuranPlayerSheet(
            viewModel = audioViewModel,
            state = playback,
            isDownloaded = playback.surah in downloadedSurahs,
            onOpenReciters = {
                showPlayer = false
                showReciters = true
            },
            onOpenSurahs = {
                showPlayer = false
                showSurahAudio = true
            },
            onDismiss = { showPlayer = false }
        )
    }

    // المشاركة: من الآية ... إلى الآية ... ثم بأي صورة تخرج
    shareAnchor?.let { anchor ->
        AyahShareRangeSheet(
            anchor = anchor,
            loadPage = { page -> viewModel.ayatOfPage(page) },
            onAction = { format, verses ->
                shareAnchor = null
                val payload = verses.toVerses()
                scope.launch {
                    runCatching {
                        when (format) {
                            ShareFormat.COPY -> copyVersesToClipboard(ctx, verses)

                            ShareFormat.TEXT ->
                                ShareImageUtils.shareText(ctx, AyahShareImage.buildText(payload))

                            ShareFormat.IMAGE, ShareFormat.IMAGE_WITH_TEXT -> {
                                val bmp = AyahShareImage.render(ctx, payload, ayahSkin)
                                val uri = ShareImageUtils.saveBitmapToCache(ctx, bmp, "ayah_share")
                                if (format == ShareFormat.IMAGE) {
                                    ShareImageUtils.shareImageUri(ctx, uri)
                                } else {
                                    ShareImageUtils.shareImageWithText(
                                        ctx, uri, AyahShareImage.buildText(payload)
                                    )
                                }
                            }
                        }
                    }.onFailure {
                        Toast.makeText(ctx, "تعذّرت المشاركة", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { shareAnchor = null }
        )
    }

    // Tapping a verse → actions (Tafseer / Copy / Mark).
    val sheetAyah = activeAyah
    if (sheetAyah != null) {
        when (ayahSheet) {
            AyahSheetType.ACTIONS -> AyahActionSheet(
                ayah = sheetAyah,
                hasMark = marksByAyaId.containsKey(sheetAyah.aya.id),
                listenReciter = audioSelection.ayahReciter.name,
                onListen = {
                    closeAyahSheets()
                    audioViewModel.playAyah(sheetAyah.aya.sora, sheetAyah.aya.ayaNo)
                },
                onTafseer = { ayahSheet = AyahSheetType.TAFSEER },
                onShare = {
                    closeAyahSheets()
                    shareAnchor = sheetAyah
                },
                onMark = { ayahSheet = AyahSheetType.MARK },
                onDismiss = { closeAyahSheets() }
            )

            AyahSheetType.TAFSEER -> TafseerBottomSheet(
                ayah = sheetAyah,
                sheetState = sheetState,
                onDismiss = { closeAyahSheets() }
            )

            AyahSheetType.MARK -> MarkPaletteSheet(
                existing = marksByAyaId[sheetAyah.aya.id],
                onApply = { label, color ->
                    viewModel.addMark(
                        AyaMark(
                            ayaId = sheetAyah.aya.id,
                            page = sheetAyah.aya.page,
                            soraNameAr = sheetAyah.aya.soraNameAr,
                            ayaNo = sheetAyah.aya.ayaNo,
                            label = label,
                            colorArgb = color
                        )
                    )
                    closeAyahSheets()
                },
                onRemove = {
                    viewModel.removeMark(sheetAyah.aya.id)
                    closeAyahSheets()
                },
                onDismiss = { closeAyahSheets() }
            )

            null -> Unit
        }
    }

    // شرح الحكم — بتتفتح من اللمس على حرف ملوّن، أو من مفتاح الألوان.
    tajweedHit?.let { hit ->
        TajweedRuleSheet(hit = hit, onDismiss = { tajweedHit = null })
    }

    // الفهرس والختمات وعلاماتي والتجويد — ورقة واحدة بشريط تنقّل تحتها.
    browseTab?.let { startTab ->
        QuranBrowseSheet(
            currentPage = pagerState.currentPage + 1,
            marks = ayaMarks,
            tajweedEnabled = tajweedOn,
            tajweedNaturalMadd = tajweedNaturalMadd,
            pageRules = pageRuleCounts,
            initialTab = startTab,
            onSelectPage = { page ->
                goToPage(page)
                browseTab = null
            },
            onSelectMark = { mark ->
                goToPage(mark.page)
                browseTab = null
            },
            onDeleteMark = { viewModel.removeMark(it) },
            onTajweedEnabledChange = {
                tajweedOn = it
                TajweedPrefs.setEnabled(ctx, it)
            },
            onNaturalMaddChange = {
                tajweedNaturalMadd = it
                TajweedPrefs.setShowNaturalMadd(ctx, it)
            },
            // صفّ في «أحكام هذه الصفحة» ← وقّفني على أول موضع له.
            onPageRuleClick = { rule ->
                browseTab = null
                if (tajweedFocus == rule) tajweedFocusIndex++
                else {
                    tajweedFocus = rule
                    tajweedFocusIndex = 0
                }
            },
            // صفّ في المفتاح الكامل ← اشرحه؛ ممكن ما يكونش له موضع في الصفحة دي.
            onRuleClick = { rule ->
                browseTab = null
                tajweedHit = TajweedHit(rule = rule, word = "", start = 0, end = 0)
            },
            onDismiss = { browseTab = null }
        )
    }

    // Word search across the whole Quran.
    if (showAyaSearch) {
        AyaSearchSheet(
            onSelect = { aya ->
                goToPage(aya.page)
                showAyaSearch = false
            },
            onDismiss = { showAyaSearch = false }
        )
    }
}

// ─── Ayah Actions (Tafseer / Copy / Mark) ───────────────────────────────────────
/** The selected verses, in mushaf order, as the share renderer wants them. */
private fun List<DomainAyaWithTafseer>.toVerses(): List<AyahShareImage.Verse> = map {
    AyahShareImage.Verse(
        sura = it.aya.sora,
        suraName = it.aya.soraNameAr,
        number = it.aya.ayaNo,
        text = it.aya.ayaText.trimAyaNumber()
    )
}

private fun copyVersesToClipboard(ctx: Context, ayat: List<DomainAyaWithTafseer>) {
    if (ayat.isEmpty()) return
    val verses = ayat.toVerses()
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText("Ayat", AyahShareImage.buildText(verses, withAppTag = false))
    )
    Toast.makeText(
        ctx,
        if (verses.size == 1) "تم نسخ الآية"
        else "تم نسخ ${toArabicNumerals(verses.size)} آيات",
        Toast.LENGTH_SHORT
    ).show()
}

/** كيف تخرج الآيات المحدَّدة. */
private enum class ShareFormat { IMAGE, IMAGE_WITH_TEXT, TEXT, COPY }

/**
 * شيت المشاركة: يفتح على الآية التي وقفت عليها — «من الآية ٥ إلى الآية ٥» —
 * فتزوّد الرقم بالأزرار حتى يشمل المقطع الذي تريده، ثم تختار كيف يخرج.
 *
 * التحديد بالأرقام لا بالنقر على المصحف: أسهل في الوصول، ويريك المدى مكتوباً
 * قبل أن تشارك.
 *
 * الآيات تُجمع من صفحة الآية وما حولها (الحد صفحتان أصلاً)، ويُقصَر المدى على
 * السورة نفسها كي لا يخرج مقطع يعبر سورتين بلا معنى.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahShareRangeSheet(
    anchor: DomainAyaWithTafseer,
    loadPage: suspend (Int) -> List<DomainAyaWithTafseer>,
    onAction: (ShareFormat, List<DomainAyaWithTafseer>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheet = rememberQuranSheetColors()

    // آيات السورة نفسها في صفحة الآية وجارتيها، مرتبة
    val pool by produceState(initialValue = listOf(anchor), anchor.aya.id) {
        val page = anchor.aya.page
        val loaded = (page - 1..page + 1).flatMap { runCatching { loadPage(it) }.getOrDefault(emptyList()) }
        value = (loaded + anchor)
            .filter { it.aya.sora == anchor.aya.sora }
            .distinctBy { it.aya.id }
            .sortedBy { it.aya.ayaNo }
            .ifEmpty { listOf(anchor) }
    }

    val minNo = pool.first().aya.ayaNo
    val maxNo = pool.last().aya.ayaNo
    var fromNo by remember(anchor.aya.id) { mutableStateOf(anchor.aya.ayaNo) }
    var toNo by remember(anchor.aya.id) { mutableStateOf(anchor.aya.ayaNo) }

    val selected = pool.filter { it.aya.ayaNo in fromNo..toNo }
    val count = selected.size.coerceAtLeast(1)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheet.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "سورة ${anchor.aya.soraNameAr}",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = sheet.ink
            )

            AyahNumberDropdown(
                label = "من الآية",
                value = fromNo,
                options = (minNo..toNo).toList(),
                colors = sheet,
                onPick = { fromNo = it }
            )
            AyahNumberDropdown(
                label = "إلى الآية",
                // لا يتجاوز حدّ الآيات المسموح به في مقطع واحد
                options = (fromNo..minOf(maxNo, fromNo + AyahShareImage.MAX_VERSES - 1)).toList(),
                value = toNo,
                colors = sheet,
                onPick = { toNo = it }
            )

            Text(
                text = if (count == 1) "آية واحدة · المتاح ${toArabicNumerals(minNo)}" +
                        " - ${toArabicNumerals(maxNo)}"
                       else "${toArabicNumerals(count)} آيات · المتاح ${toArabicNumerals(minNo)}" +
                        " - ${toArabicNumerals(maxNo)}",
                fontFamily = AmiriFont,
                fontSize = 11.5.sp,
                color = sheet.inkSoft
            )

            Spacer(Modifier.height(2.dp))
            ShareFormatRow(Icons.Filled.Image, "صورة", "بطاقة المصحف بالآيات", sheet) {
                onAction(ShareFormat.IMAGE, selected)
            }
            ShareFormatRow(Icons.Filled.Share, "صورة ونص", "البطاقة ومعها الآيات مكتوبة", sheet) {
                onAction(ShareFormat.IMAGE_WITH_TEXT, selected)
            }
            ShareFormatRow(Icons.Filled.Share, "نص فقط", "الآيات مكتوبة مع اسم السورة", sheet) {
                onAction(ShareFormat.TEXT, selected)
            }
            ShareFormatRow(Icons.Filled.ContentCopy, "نسخ النص", "ينسخها للحافظة", sheet) {
                onAction(ShareFormat.COPY, selected)
            }
        }
    }
}

/**
 * صفّ «من الآية ٥» بقائمة منسدلة بأرقام الآيات المتاحة — أسرع من الضغط مرات
 * كثيرة على زرّ زيادة حين يكون المقطع طويلاً.
 */
@Composable
private fun AyahNumberDropdown(
    label: String,
    value: Int,
    options: List<Int>,
    colors: QuranSheetColors,
    onPick: (Int) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.accentAlt.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontFamily = AmiriFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.ink,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .clickable { open = true }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = toArabicNumerals(value),
                    fontFamily = AmiriFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.ink
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier
                    .background(colors.surface)
                    .heightIn(max = 280.dp)
            ) {
                options.forEach { n ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = toArabicNumerals(n),
                                fontFamily = AmiriFont,
                                fontWeight = if (n == value) FontWeight.Bold else FontWeight.Normal,
                                color = if (n == value) colors.accent else colors.ink
                            )
                        },
                        onClick = {
                            onPick(n)
                            open = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareFormatRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: QuranSheetColors,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.accentAlt.copy(alpha = 0.16f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontFamily = AmiriFont, fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = colors.ink)
            Text(subtitle, fontFamily = AmiriFont, fontSize = 11.5.sp, color = colors.inkSoft)
        }
    }
}

private fun copyAyahToClipboard(ctx: Context, ayah: DomainAyaWithTafseer) {
    val text = ayah.aya.ayaText.trimAyaNumber()
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText("Ayah", "{$text} [${ayah.aya.soraNameAr}: ${ayah.aya.ayaNo}]")
    )
    Toast.makeText(ctx, "تم نسخ الآية", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahActionSheet(
    ayah: DomainAyaWithTafseer,
    hasMark: Boolean,
    listenReciter: String,
    onTafseer: () -> Unit,
    onShare: () -> Unit,
    onMark: () -> Unit,
    onListen: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheet = rememberQuranSheetColors()
    val primary = sheet.headingColor()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheet.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "سورة ${ayah.aya.soraNameAr} — آية ${toArabicNumerals(ayah.aya.ayaNo)}",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = sheet.inkSoft,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = ayah.aya.ayaText.trimAyaNumber(),
                fontFamily = QuranFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = sheet.ink,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AyahActionButton(Icons.Filled.Description, "التفسير", primary, Modifier.weight(1f), onTafseer)
                AyahActionButton(Icons.Filled.Share, "مشاركة", primary, Modifier.weight(1f), onShare)
                AyahActionButton(
                    icon = Icons.Filled.Bookmark,
                    label = if (hasMark) "تعديل" else "علامة",
                    color = primary,
                    modifier = Modifier.weight(1f),
                    onClick = onMark
                )
            }

            Spacer(Modifier.height(10.dp))

            // زرار عريض لوحده: الاستماع مش إجراء لحظي زي النسخ — بيبدأ تلاوة
            // بتكمّل بعد الآية دي، والنص بيقول كده صراحة قبل الضغط.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(primary.copy(alpha = 0.12f))
                    .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onListen)
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "استماع من هذه الآية",
                    fontFamily = AmiriFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primary
                )
            }
            if (listenReciter.isNotBlank()) {
                Text(
                    text = "بصوت $listenReciter — ثم ما بعدها بلا توقف",
                    fontFamily = AmiriFont,
                    fontSize = 12.sp,
                    color = sheet.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AyahActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, fontFamily = AmiriFont, fontSize = 13.sp, color = QuranPalette.Brown, maxLines = 1)
    }
}

// ─── Mark Palette ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MarkPaletteSheet(
    existing: AyaMark?,
    onApply: (label: String, colorArgb: Long) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customLabel by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(MarkColorPalette.first()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuranPalette.Paper,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "اختر علامة",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DefaultMarkPresets.forEach { preset ->
                    MarkChip(
                        label = preset.label,
                        color = Color(preset.colorArgb),
                        onClick = { onApply(preset.label, preset.colorArgb) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = QuranPalette.Gold.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            Text(
                text = "أو أضف علامة مخصّصة",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = QuranPalette.Brown,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = customLabel,
                onValueChange = { customLabel = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("اسم العلامة", fontFamily = AmiriFont) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = QuranPalette.Ink,
                    unfocusedTextColor = QuranPalette.Ink,
                    focusedPlaceholderColor = QuranPalette.Ink.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = QuranPalette.Ink.copy(alpha = 0.5f),
                    focusedBorderColor = primary,
                    unfocusedBorderColor = QuranPalette.Gold,
                    cursorColor = primary
                )
            )

            Spacer(Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MarkColorPalette.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                width = if (selectedColor == c) 3.dp else 1.dp,
                                color = if (selectedColor == c) QuranPalette.Ink else QuranPalette.Gold,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = c },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == c) {
                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (customLabel.isNotBlank()) onApply(customLabel.trim(), selectedColor) },
                enabled = customLabel.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("حفظ العلامة", fontFamily = AmiriFont, color = Color.White)
            }

            if (existing != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("إزالة العلامة", fontFamily = AmiriFont)
                }
            }
        }
    }
}

@Composable
private fun MarkChip(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, fontFamily = AmiriFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = QuranPalette.Ink)
    }
}

// ─── My Marks List ───────────────────────────────────────────────────────────────
/** علاماتي — تبويب جوّه [QuranBrowseSheet]. */
@Composable
internal fun MarksListPane(
    marks: List<AyaMark>,
    onSelect: (AyaMark) -> Unit,
    onDelete: (Int) -> Unit
) {
    val sorted = remember(marks) { marks.sortedByDescending { it.timestamp } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            if (sorted.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "لا توجد علامات محفوظة بعد",
                        fontFamily = AmiriFont,
                        fontSize = 15.sp,
                        color = QuranPalette.Brown.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(sorted, key = { it.ayaId }) { mark ->
                        MarkRow(
                            mark = mark,
                            onClick = { onSelect(mark) },
                            onDelete = { onDelete(mark.ayaId) }
                        )
                    }
                }
            }
        }
}

@Composable
private fun MarkRow(mark: AyaMark, onClick: () -> Unit, onDelete: () -> Unit) {
    val color = Color(mark.colorArgb)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(QuranPalette.CardSurface)
            .border(1.dp, QuranPalette.Gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "سورة ${mark.soraNameAr} — آية ${toArabicNumerals(mark.ayaNo)}",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = QuranPalette.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${mark.label} • صفحة ${toArabicNumerals(mark.page)}",
                fontFamily = AmiriFont,
                fontSize = 12.sp,
                color = QuranPalette.Brown
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "حذف",
                tint = QuranPalette.Brown.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Surah Index Picker ─────────────────────────────────────────────────────────
/**
 * فهرس السور — تبويب جوّه [QuranBrowseSheet] لا ورقة مستقلة.
 *
 * العنوان اتشال: تبويب الورقة هو اللي بيسمّيه دلوقتي، وعنوان تاني جوّاه كان
 * بياخد سطرًا من غير معلومة.
 */
@Composable
internal fun SuraPickerPane(
    currentPage: Int,
    onSelect: (Int) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) SuraList
        else SuraList.filter {
            it.nameAr.contains(query.trim()) ||
                it.number.toString().contains(query.trim()) ||
                toArabicNumerals(it.number).contains(query.trim())
        }
    }
    // Highlight the surah the reader is currently inside.
    val activeSuraNumber = SuraList.lastOrNull { it.startPage <= currentPage }?.number

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("ابحث باسم السورة أو رقمها", fontFamily = AmiriFont, fontSize = 14.sp)
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = QuranPalette.Ink,
                    unfocusedTextColor = QuranPalette.Ink,
                    focusedPlaceholderColor = QuranPalette.Ink.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = QuranPalette.Ink.copy(alpha = 0.5f),
                    focusedBorderColor = primary,
                    unfocusedBorderColor = QuranPalette.Gold,
                    focusedLeadingIconColor = primary,
                    cursorColor = primary
                )
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filtered, key = { it.number }) { sura ->
                    SuraRow(
                        sura = sura,
                        isActive = sura.number == activeSuraNumber,
                        primary = primary,
                        onClick = { onSelect(sura.startPage) }
                    )
                }
            }
        }
}

@Composable
private fun SuraRow(
    sura: SuraInfo,
    isActive: Boolean,
    primary: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) primary.copy(alpha = 0.10f) else QuranPalette.CardSurface)
            .border(
                width = 1.dp,
                color = if (isActive) primary.copy(alpha = 0.5f) else QuranPalette.Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Gold/primary numbered badge.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(primary.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, QuranPalette.Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = toArabicNumerals(sura.number),
                    fontFamily = AmiriFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "سُورَةُ ${sura.nameAr}",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = QuranPalette.Ink
            )
        }

        Text(
            text = "صفحة ${toArabicNumerals(sura.startPage)}",
            fontFamily = AmiriFont,
            fontSize = 13.sp,
            color = QuranPalette.Brown
        )
    }
}

// ─── Ayah Word Search ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyaSearchSheet(
    onSelect: (DomainAya) -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DomainAya>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Debounced live search; matches against the imlaa'i text (no tashkeel).
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(250)
        QuranData.searchAya(ctx, q).collect {
            results = it
            isSearching = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuranPalette.Paper,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "البحث في القرآن",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("اكتب كلمة للبحث عنها في الآيات", fontFamily = AmiriFont, fontSize = 14.sp)
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = QuranPalette.Ink,
                    unfocusedTextColor = QuranPalette.Ink,
                    focusedPlaceholderColor = QuranPalette.Ink.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = QuranPalette.Ink.copy(alpha = 0.5f),
                    focusedBorderColor = primary,
                    unfocusedBorderColor = QuranPalette.Gold,
                    focusedLeadingIconColor = primary,
                    cursorColor = primary
                )
            )

            // Status line: searching / result count / hint.
            val status = when {
                query.trim().length < 2 -> "اكتب حرفين على الأقل"
                isSearching -> "جاري البحث…"
                results.isEmpty() -> "لا توجد نتائج"
                else -> "${toArabicNumerals(results.size)} نتيجة"
            }
            Text(
                text = status,
                fontFamily = AmiriFont,
                fontSize = 13.sp,
                color = QuranPalette.Brown,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(results, key = { it.id }) { aya ->
                    AyaSearchResultRow(aya = aya, primary = primary, onClick = { onSelect(aya) })
                }
            }
        }
    }
}

@Composable
private fun AyaSearchResultRow(
    aya: DomainAya,
    primary: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(QuranPalette.CardSurface)
            .border(1.dp, QuranPalette.Gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سُورَةُ ${aya.soraNameAr} • آية ${toArabicNumerals(aya.ayaNo)}",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = primary
            )
            Text(
                text = "صفحة ${toArabicNumerals(aya.page)}",
                fontFamily = AmiriFont,
                fontSize = 12.sp,
                color = QuranPalette.Brown
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = aya.ayaText.trimAyaNumber(),
            fontFamily = QuranFont,
            fontSize = 18.sp,
            color = QuranPalette.Ink,
            lineHeight = 32.sp,
            textAlign = TextAlign.Right,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


// ─── Tafseer Bottom Sheet ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TafseerBottomSheet(
    ayah: DomainAyaWithTafseer,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val sheet = rememberQuranSheetColors()
    val primary = sheet.headingColor()
    val cleanedText = ayah.aya.ayaText.trimAyaNumber()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheet.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "سورة ${ayah.aya.soraNameAr} - آية ${toArabicNumerals(ayah.aya.ayaNo)}",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = sheet.inkSoft,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = cleanedText,
                fontFamily = QuranFont,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = sheet.ink,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            HorizontalDivider(color = sheet.divider, thickness = 1.dp)

            Text(
                text = "التفسير:",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                Text(
                    text = ayah.ayaTafseer.text.ifEmpty { "لا يوجد تفسير متوفر لهذه الآية." },
                    fontSize = 15.sp,
                    color = sheet.ink,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                )
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "Ayah and Tafsir",
                                "﴿${cleanedText}﴾\n\nتفسير الآية:\n${ayah.ayaTafseer.text}"
                            )
                        )
                        Toast.makeText(ctx, "تم نسخ الآية والتفسير", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sheet.accent,
                        contentColor = sheet.onAccent
                    )
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ")
                    Spacer(Modifier.width(8.dp))
                    Text("نسخ")
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "﴿${cleanedText}﴾ [سورة ${ayah.aya.soraNameAr}: آية ${ayah.aya.ayaNo}]\n\nتفسير الآية:\n${ayah.ayaTafseer.text}"
                            )
                        }
                        ctx.startActivity(Intent.createChooser(shareIntent, "مشاركة التفسير"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sheet.accentAlt,
                        contentColor = sheet.onAccentAlt
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة")
                    Spacer(Modifier.width(8.dp))
                    Text("مشاركة")
                }
            }
        }
    }
}

// ─── Page Content Loader ────────────────────────────────────────────────────────
/**
 * آيات الصفحة.
 *
 * عمود `page` في قاعدة البيانات صحيح — اتأكد بمقارنة الـ٦٢٣٦ آية مع تخطيط مصحف
 * المدينة المرجعي وطلع مطابق في كل آية. (كان فيه هنا «تصحيح» لعضوية ٥٦ آية
 * مبنيّ على أرقام سطورها؛ اتشال لأن اللي غلط هو أرقام السطور مش الصفحة.)
 */
@Composable
fun rememberQuranPageContent(page: Int): List<DomainAyaWithTafseer>? {
    val ctx = LocalContext.current
    var content by remember(page) { mutableStateOf<List<DomainAyaWithTafseer>?>(null) }
    LaunchedEffect(page) {
        QuranData.getQuranPageAyaWithTafseer(ctx, page).collect {
            content = it.sortedBy { item -> item.aya.id }
        }
    }
    return content
}

// ─── Page Layout ────────────────────────────────────────────────────────────────
@Composable
fun QuranPageContent(
    ayat: List<DomainAyaWithTafseer>,
    fontSize: Float,
    pageNumber: Int,
    selectedAyaIds: Set<Int>,
    marksByAyaId: Map<Int, AyaMark>,
    onAyaClick: (DomainAyaWithTafseer) -> Unit,
    /** الآية اللي بتتلى دلوقتي على الصفحة دي (فاضية لو التلاوة في صفحة تانية). */
    playingAyaIds: Set<Int> = emptySet(),
    /** وضع التجويد: كل حكم بلونه. */
    tajweed: Boolean = false,
    tajweedNaturalMadd: Boolean = false,
    onTajweedClick: ((TajweedHit) -> Unit)? = null,
    focusRule: TajweedRule? = null,
    focusIndex: Int = 0
) {
    val groups = remember(ayat) { ayat.groupBy { it.aya.sora } }

    val isSpecialPage = pageNumber <= 2
    val scrollState = rememberScrollState()

    MushafFrame(
        isSpecialPage = isSpecialPage,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header: Surah (right) + Juz' (left)
            PageHeaderRow(ayat = ayat)

            HorizontalDivider(
                color = QuranPalette.Gold.copy(alpha = 0.35f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // 2. Verses — صفحة مصحف كاملة: عدد سطورها ثابت زي المطبوع، وكسر
            //    السطور مستنتَج من lineStart/lineEnd فبيطابق مصحف المدينة، وكل
            //    سطر ممدود لحافتي الصفحة.
            //
            //    صفحتا الفاتحة وأول البقرة استثناء: المصحف بيوسّط آياتهم بتباعد
            //    أوسع جوّه الإطار المزخرف، مش على شبكة الـ١٥ سطر.
            // الصفحة نفسها من المكتبة — ده الغرض من الـsample.
            val byId = remember(ayat) { ayat.associateBy { it.aya.id } }
            val sourceAyat = remember(ayat) { ayat.map { it.aya.source } }
            val markColors = remember(marksByAyaId) {
                marksByAyaId.mapValues { (_, mark) -> Color(mark.colorArgb) }
            }
            MushafPage(
                ayahs = sourceAyat,
                maxFontSize = fontSize,
                selectedAyahIds = selectedAyaIds,
                highlightedAyahs = markColors,
                playingAyahIds = playingAyaIds,
                onAyahClick = { a -> byId[a.id]?.let(onAyaClick) },
                tajweed = tajweed,
                naturalMadd = tajweedNaturalMadd,
                colors = MushafColors.Light.copy(
                    ink = QuranPalette.Ink,
                    marker = QuranPalette.Brown,
                    bannerFill = QuranPalette.Banner,
                    bannerBorder = QuranPalette.Gold,
                    bannerText = QuranPalette.Brown,
                    selection = QuranPalette.Highlight,
                    playing = QuranPalette.Playing
                ),
                onTajweedClick = onTajweedClick?.let { handler ->
                    { hit -> handler(TajweedHit(hit.rule, hit.word, hit.start, hit.end)) }
                },
                focusRule = focusRule,
                focusIndex = focusIndex,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // 3. Bottom alternating page/Hizb badge
            HorizontalDivider(
                color = QuranPalette.Gold.copy(alpha = 0.35f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            PageNumberRow(pageNumber = pageNumber)
        }
    }
}

// ─── Mus'haf Frame ────────────────────────────────────────────────────────────
/**
 * أبعاد إطار الصفحة — **مصدر واحد** للرسم ولحشوة المحتوى.
 *
 * الحشوة كانت رقمًا يدويًا مكتوبًا منفصلًا عن أرقام الرسم، فوقعت أقل من أعمق خط
 * في إطار الفاتحة (١٦dp حشوة مقابل خط ذهبي داخلي عند ٢٤dp) والنص كان بيمرّ من
 * تحت الذهب. دلوقتي بتتحسب من نفس الأرقام، فما ينفعش يفترقوا تاني.
 */
private object MushafFrameSpec {

    /** فراغ أمان بين أعمق حبر في الإطار وأول حرف في المحتوى. */
    private val Clearance = 1.5.dp

    /** جذر ٢ — قطر المعيّن المايل ٤٥° بالنسبة لضلعه. */
    private const val SQRT2 = 1.41422f

    // ── الصفحة العادية: تلات خطوط رفيعة + معيّنات على الأركان ──
    val OuterGold = 3.dp
    val PrimaryLine = 6.dp
    val PrimaryStroke = 2.4.dp
    val InnerGold = 10.dp
    val HairLine = 1.dp

    /** نصف ضلع معيّن الركن — مربع مايل ٤٥° مركزه على خط الـprimary. */
    val CornerDiamond = 4.dp

    // ── صفحتا الفاتحة وأول البقرة: شريط عريض + خطين داخليين + ميداليات ──
    val Band = 13.dp
    val BandInset = HairLine + 3.dp + Band / 2       // ١٠٫٥dp
    val InnerLine1 = BandInset + Band / 2 + 4.dp     // ٢١dp
    val InnerLine2 = InnerLine1 + 3.dp               // ٢٤dp
    val InnerLine1Stroke = 1.5.dp
    val InnerLine2Stroke = 0.8.dp

    /** ضلع ميدالية الركن، ومركزها على ركن الشريط. */
    val Medallion = Band * 1.9f

    /**
     * حشوة المحتوى = أعمق نقطة بيوصلها حبر الإطار + فراغ أمان.
     *
     * في الصفحة العادية أعمق حاجة **مش** الخط الداخلي (١٠٫٥dp) — المعيّن المايل
     * على خط الـprimary بيغوص أعمق: مربع ضلعه ٨dp مدوّر ٤٥° قطره ٨√٢، فنصفه
     * ٥٫٦٦dp من مركزه عند ٦dp = ١١٫٧dp.
     */
    fun contentPadding(isSpecialPage: Boolean): Dp = Clearance + if (isSpecialPage) {
        maxOf(
            InnerLine2 + InnerLine2Stroke / 2,
            Medallion / 2 + BandInset
        )
    } else {
        maxOf(
            InnerGold + HairLine / 2,
            PrimaryLine + CornerDiamond * SQRT2
        )
    }
}

/**
 * Page border around the content.
 *
 * Regular pages get a slim layered frame (thin gold + a primary-color band with a
 * gold dashed inlay + thin inner line). The special pages — Al-Fatiha and the
 * opening of Al-Baqarah — get a richer, Madinah-style illuminated frame: a wide
 * primary band edged in gold, a double inner gold line, and gold corner medallions.
 *
 * Content padding is sized to always clear the innermost line so text never touches
 * or overlaps the frame, top, bottom or sides.
 */
@Composable
fun MushafFrame(
    isSpecialPage: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val gold = QuranPalette.Gold

    // محسوبة من نفس أرقام الرسم — بتعدّي أعمق حبر في الإطار وبس. كل dp زيادة
    // هنا بيصغّر خط المصحف، لأن العرض هو القيد.
    val contentPadding = MushafFrameSpec.contentPadding(isSpecialPage)

    Box(
        modifier = modifier
            .background(QuranPalette.Paper, RoundedCornerShape(6.dp))
            .drawBehind {
                val thin = MushafFrameSpec.HairLine.toPx()
                val w = size.width
                val h = size.height

                if (!isSpecialPage) {
                    // ── Regular page frame — refined triple line + corner diamonds ─
                    // 1. Outer fine gold line.
                    val i1 = MushafFrameSpec.OuterGold.toPx()
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(i1, i1),
                        size = Size(w - i1 * 2, h - i1 * 2),
                        style = Stroke(width = thin),
                        cornerRadius = CornerRadius(9.dp.toPx())
                    )
                    // 2. Primary-color frame line (the main border).
                    val i2 = MushafFrameSpec.PrimaryLine.toPx()
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(i2, i2),
                        size = Size(w - i2 * 2, h - i2 * 2),
                        style = Stroke(width = MushafFrameSpec.PrimaryStroke.toPx()),
                        cornerRadius = CornerRadius(7.dp.toPx())
                    )
                    // 3. Inner fine gold line.
                    val i3 = MushafFrameSpec.InnerGold.toPx()
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(i3, i3),
                        size = Size(w - i3 * 2, h - i3 * 2),
                        style = Stroke(width = thin),
                        cornerRadius = CornerRadius(5.dp.toPx())
                    )
                    // 4. Small gold diamonds sitting on the four corners.
                    val d = MushafFrameSpec.CornerDiamond.toPx()
                    listOf(
                        Offset(i2, i2), Offset(w - i2, i2),
                        Offset(i2, h - i2), Offset(w - i2, h - i2)
                    ).forEach { c ->
                        rotate(degrees = 45f, pivot = c) {
                            drawRect(
                                color = gold,
                                topLeft = Offset(c.x - d, c.y - d),
                                size = Size(d * 2, d * 2)
                            )
                            drawRect(
                                color = QuranPalette.Paper,
                                topLeft = Offset(c.x - d / 2, c.y - d / 2),
                                size = Size(d, d)
                            )
                        }
                    }
                } else {
                    // ── Special illuminated frame (Al-Fatiha / Al-Baqarah) ───────
                    val r = CornerRadius(4.dp.toPx())

                    // 1. Outer thin gold line.
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(thin / 2, thin / 2),
                        size = Size(w - thin, h - thin),
                        style = Stroke(width = thin),
                        cornerRadius = r
                    )

                    // 2. Wide primary band, edged top & bottom with solid gold lines.
                    val band = MushafFrameSpec.Band.toPx()
                    val bandInset = MushafFrameSpec.BandInset.toPx()
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(bandInset, bandInset),
                        size = Size(w - bandInset * 2, h - bandInset * 2),
                        style = Stroke(width = band),
                        cornerRadius = r
                    )
                    // Gold edges framing the band.
                    listOf(bandInset - band / 2, bandInset + band / 2).forEach { inset ->
                        drawRoundRect(
                            color = gold,
                            topLeft = Offset(inset, inset),
                            size = Size(w - inset * 2, h - inset * 2),
                            style = Stroke(width = 1.2.dp.toPx()),
                            cornerRadius = r
                        )
                    }
                    // Gold dashed inlay running along the band centre.
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(bandInset, bandInset),
                        size = Size(w - bandInset * 2, h - bandInset * 2),
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f), 0f)
                        ),
                        cornerRadius = r
                    )

                    // 3. Double inner gold lines (classic Madinah look).
                    val inner1 = MushafFrameSpec.InnerLine1.toPx()
                    val inner2 = MushafFrameSpec.InnerLine2.toPx()
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(inner1, inner1),
                        size = Size(w - inner1 * 2, h - inner1 * 2),
                        style = Stroke(width = MushafFrameSpec.InnerLine1Stroke.toPx()),
                        cornerRadius = r
                    )
                    drawRoundRect(
                        color = gold.copy(alpha = 0.7f),
                        topLeft = Offset(inner2, inner2),
                        size = Size(w - inner2 * 2, h - inner2 * 2),
                        style = Stroke(width = MushafFrameSpec.InnerLine2Stroke.toPx()),
                        cornerRadius = r
                    )

                    // 4. Gold corner medallions over the band corners.
                    val m = MushafFrameSpec.Medallion.toPx()
                    val corners = listOf(
                        Offset(bandInset, bandInset),
                        Offset(w - bandInset, bandInset),
                        Offset(bandInset, h - bandInset),
                        Offset(w - bandInset, h - bandInset)
                    )
                    corners.forEach { c ->
                        drawRoundRect(
                            color = gold,
                            topLeft = Offset(c.x - m / 2, c.y - m / 2),
                            size = Size(m, m),
                            cornerRadius = CornerRadius(m / 4)
                        )
                        drawRoundRect(
                            color = primary,
                            topLeft = Offset(c.x - m / 2 + 2.dp.toPx(), c.y - m / 2 + 2.dp.toPx()),
                            size = Size(m - 4.dp.toPx(), m - 4.dp.toPx()),
                            cornerRadius = CornerRadius(m / 5)
                        )
                        drawCircle(color = gold, radius = m / 6, center = c)
                    }
                }
            }
            .padding(contentPadding),
        content = content
    )
}

/**
 * سقف مقياس خط النظام لنصوص الترويسة والتذييل جوّه الإطار.
 *
 * دول نصوص واجهة عادية فمن حقّها تكبر مع إعداد «حجم الخط» في الجهاز، إنما
 * الإطار اللي حواليهم مقاسه ثابت وارتفاعهم بيتاكل من ارتفاع الصفحة (فيصغّر خط
 * المصحف). فبنسيب لهم تكبيرًا محسوسًا ونقف عنده — و`maxLines` + `weight`
 * بيضمنوا إن حتى عند السقف مفيش حرف بيخرج برّه الإطار.
 */
private const val CHROME_MAX_FONT_SCALE = 1.3f

/** الفاصل بين أسماء السور في ترويسة الصفحة — مسافتان، زي الفراغ اللي كان بينهم. */
private const val SURA_NAME_SEPARATOR = "  "

/** بينفّذ [content] بمقياس خط محدود بـ[CHROME_MAX_FONT_SCALE]. */
@Composable
private fun CappedChromeTextScale(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    if (d.fontScale <= CHROME_MAX_FONT_SCALE) {
        content()
        return
    }
    CompositionLocalProvider(
        LocalDensity provides Density(d.density, fontScale = CHROME_MAX_FONT_SCALE),
        content = content
    )
}

@Composable
fun PageHeaderRow(ayat: List<DomainAyaWithTafseer>) {
    val jozz = ayat.firstOrNull()?.aya?.jozz ?: 1
    val soraNames = ayat.map { it.aya.soraNameAr }.distinct()

    CappedChromeTextScale {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // In RTL the first child sits on the right → Surah name(s) on the right.
            //
            // `weight(fill = false)` مش تزيين: الصفحة ممكن يكون فيها تلات سور
            // (٦٠٤ مثلًا) والأسماء مع خط مكبّر بتتخطّى نص العرض، فمن غير الوزن
            // كانت أسماء السور والجزء بيتصادموا ويخرجوا من حافتَي الإطار.
            // بالوزن كل جنب بياخد نصيبه وبس، والزيادة بتتقصّ بنقط.
            //
            // والأسماء نص واحد مش نص لكل سورة: لو كانوا منفصلين كل واحد بياخد
            // حصّته وبيتقصّ لوحده، فأول اسم بيبقى «الإ...» والباقي كامل. نص
            // واحد بيتقصّ من آخره زي أي جملة — «الإخلاص الفلق ال...».
            Text(
                text = soraNames.joinToString(SURA_NAME_SEPARATOR),
                fontFamily = AmiriFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = QuranPalette.Brown,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(Modifier.width(8.dp))

            // Juz' name on the left.
            Text(
                text = getJuzzNameAr(jozz),
                fontFamily = AmiriFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = QuranPalette.Brown,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Composable
fun PageNumberRow(pageNumber: Int) {
    val isEven = pageNumber % 2 == 0
    // RTL: CenterStart = right, CenterEnd = left. Even pages sit on the left of a
    // spread (number on the left), odd pages on the right (number on the right).
    val alignment = if (isEven) Alignment.CenterEnd else Alignment.CenterStart

    val quarterInfo = QuranQuartersMap[pageNumber]
    val badgeText = if (quarterInfo != null) {
        "الحزب ${toArabicNumerals(quarterInfo.hizb)}  ${toArabicNumerals(pageNumber)}"
    } else {
        toArabicNumerals(pageNumber)
    }

    CappedChromeTextScale {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .background(QuranPalette.CardSurface, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, QuranPalette.Gold, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    fontFamily = AmiriFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranPalette.Brown,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// بانر اسم السورة والبسملة بيترسموا جوّه سطح رسم الصفحة الواحد في
// MushafPageRenderer، مش كمركّبات منفصلة — كانوا بياخدوا خانة كاملة ويقصّوا
// علامات الضبط عند حدودها.

/**
 * السؤال اللي بيظهر لمّا المستخدم يخرج من المصحف والتلاوة شغّالة.
 *
 * الاختيارين متساويين في الوزن عن قصد — مفيش «إلغاء» و«تأكيد» هنا، فيه طريقين
 * الاتنين صح: يكمّل في الخلفية أو يقف. وقفل السؤال من برّه معناه «رجّعني
 * للمصحف»، فما بنخرجش وما بنوقّفش حاجة.
 */
@Composable
private fun ExitPlaybackDialog(
    reciterName: String,
    trackTitle: String,
    onKeepPlaying: (remember: Boolean) -> Unit,
    onStop: (remember: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheet = rememberQuranSheetColors()
    var rememberChoice by remember { mutableStateOf(false) }

    val nowPlaying = listOf(trackTitle, reciterName)
        .filter { it.isNotBlank() }
        .joinToString(" — ")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = sheet.surface,
        icon = {
            Icon(
                Icons.Default.Headphones,
                contentDescription = null,
                tint = sheet.accent,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                "التلاوة شغّالة",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = sheet.ink
            )
        },
        text = {
            Column {
                Text(
                    text = if (nowPlaying.isNotBlank()) {
                        "$nowPlaying\nتحب تكمّل في الخلفية ولا توقّفها؟"
                    } else {
                        "تحب تكمّل التلاوة في الخلفية ولا توقّفها؟"
                    },
                    fontFamily = AmiriFont,
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = sheet.inkSoft
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { rememberChoice = !rememberChoice }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it },
                        colors = CheckboxDefaults.colors(checkedColor = sheet.accent)
                    )
                    Text(
                        "افتكر اختياري وما تسألنيش تاني",
                        fontFamily = AmiriFont,
                        fontSize = 13.sp,
                        color = sheet.inkSoft
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onKeepPlaying(rememberChoice) }) {
                Text(
                    "كمّل في الخلفية",
                    fontFamily = AmiriFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = sheet.accent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onStop(rememberChoice) }) {
                Text(
                    "أوقف التلاوة",
                    fontFamily = AmiriFont,
                    fontSize = 15.sp,
                    color = sheet.inkSoft
                )
            }
        }
    )
}
