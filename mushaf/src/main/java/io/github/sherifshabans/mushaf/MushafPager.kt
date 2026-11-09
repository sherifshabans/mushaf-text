package io.github.sherifshabans.mushaf

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A pager state over the 604 pages. [initialPage] is the **mushaf page number** (1‥604);
 * `state.currentPage + 1` is the page on screen.
 */
@Composable
fun rememberMushafPagerState(initialPage: Int = 1): PagerState =
    rememberPagerState(initialPage = (initialPage - 1).coerceIn(0, Quran.PAGE_COUNT - 1)) {
        Quran.PAGE_COUNT
    }

/** عرض شريط ظهر الورقة وعرض الظلّ في تقليب الورقة (dp). */
private val CURL_BACK_WIDTH = 18.dp
private val CURL_SHADOW_WIDTH = 24.dp

/** عتبة إتمام التقليب بالسحب — فوقها تكمل الورقة، وتحتها ترجع. */
private const val CURL_COMMIT_THRESHOLD = 0.35f

/** عرض منطقة الصفحة (dp) اللي بعده تتحطّ صفحتين جنب بعض في [MushafSpread.Auto]. */
private val SPREAD_MIN_WIDTH = 600.dp

/** صفحات واحدة لكل شاشة أو صفحتين (ازدواجية) في الشاشات العريضة. */
enum class MushafSpread {
    /** Always one page per screen. */
    Single,

    /**
     * A spread when the page area is at least [SPREAD_MIN_WIDTH] wide: the odd
     * page on the right, its even pair on the left, like the open book. Page 1
     * shows the inside cover on its left. The pair is **static** — it does not
     * turn like a leaf yet.
     */
    Auto,
}

/**
 * ازدواجية الصفحة: الزوجية شمالًا (يسار الكتاب) والفردية يمينًا، وأول صفحة
 * شمالها داخل الكتاب (فارغ). ٦٠٤ صفحات = ٣٠٢ ازدواجية.
 */
private fun spreadPair(page: Int): Pair<Int, Int?> {
    if (page == 1) return 1 to null
    val left = if (page % 2 == 0) page else page + 1
    return (left - 1) to left
}

/**
 * The whole mushaf, one page per screen, turning right-to-left like a printed copy
 * (the next page comes in from the left) whatever the app's locale.
 *
 * Each page gets a small header (surah · juz) and footer (page number) unless
 * [showHeaders] is false or the decoration's frame owns that chrome. Every other
 * parameter is passed through to [MushafPage].
 *
 * @param pageTurn [PageTurn.Slide] (default) pages with a [HorizontalPager].
 *   [PageTurn.Curl] peels one leaf the way a bound mushaf does. During a peel
 *   `state.currentPageOffsetFraction` stays `0` — it is **not** the peel
 *   progress; callers that need the fraction stay on [PageTurn.Slide].
 * @param spread [MushafSpread.Single] (default) one page per screen;
 *   [MushafSpread.Auto] shows the page and its pair side by side on wide
 *   screens. `state.currentPage` keeps addressing a single mushaf page.
 */
@Composable
fun MushafPager(
    modifier: Modifier = Modifier,
    state: PagerState = rememberMushafPagerState(),
    tajweed: Boolean = false,
    naturalMadd: Boolean = false,
    colors: MushafColors = MushafColors.Light,
    decoration: MushafDecoration = MushafDecoration.None,
    showHeaders: Boolean = true,
    pageTurn: PageTurn = PageTurn.Slide,
    spread: MushafSpread = MushafSpread.Single,
    selectedAyahIds: Set<Int> = emptySet(),
    highlightedAyahs: Map<Int, Color> = emptyMap(),
    playingAyahIds: Set<Int> = emptySet(),
    onAyahClick: ((Ayah) -> Unit)? = null,
    onAyahLongClick: ((Ayah) -> Unit)? = null,
    onTajweedClick: ((TajweedHit) -> Unit)? = null,
    maxFontSize: Float = DEFAULT_MAX_FONT_SIZE,
    pagePadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
) {
    val outerDirection = LocalLayoutDirection.current
    // الإطار اللي بيملك رأس الصفحة ورقمها بيلغي الأطراف الخارجية — مهما كان
    // `showHeaders` — عشان الرقم ما يظهرش مرتين.
    val frameOwnsChrome = decoration.pageFrame && decoration.divisionMarks
    val singleContent: @Composable (Int) -> Unit = { index ->
        CompositionLocalProvider(LocalLayoutDirection provides outerDirection) {
            val page = index + 1
            val context = LocalContext.current
            val ayahs by produceState<List<Ayah>?>(null, page) {
                value = Quran.load(context).filter { it.page == page }
            }
            Column(Modifier.fillMaxSize().padding(pagePadding)) {
                if (showHeaders && !frameOwnsChrome) PageHeader(ayahs, colors)
                MushafPage(
                    ayahs = ayahs ?: emptyList(),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    tajweed = tajweed,
                    naturalMadd = naturalMadd,
                    colors = colors,
                    decoration = decoration,
                    selectedAyahIds = selectedAyahIds,
                    highlightedAyahs = highlightedAyahs,
                    playingAyahIds = playingAyahIds,
                    onAyahClick = onAyahClick,
                    onAyahLongClick = onAyahLongClick,
                    onTajweedClick = onTajweedClick,
                    maxFontSize = maxFontSize
                )
                if (showHeaders && !frameOwnsChrome) PageFooter(page, colors)
            }
        }
    }
    // الصفحة الجاية تيجي من الشمال زي المصحف المطبوع، مهما كانت لغة التطبيق.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(modifier.background(colors.paper)) {
            val spreadActive = spread == MushafSpread.Auto && maxWidth >= SPREAD_MIN_WIDTH
            val pageContent: @Composable (Int) -> Unit = { index ->
                if (!spreadActive) {
                    singleContent(index)
                } else {
                    // الزوجية: الفردية يمينًا (أول صف في RTL) والزوجية شمالًا،
                    // وأول صفحة شمالها داخل الكتاب الفاضي.
                    val (right, left) = spreadPair(index + 1)
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            singleContent(right - 1)
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            if (left != null) singleContent(left - 1)
                        }
                    }
                }
            }
            when (pageTurn) {
                PageTurn.Slide -> HorizontalPager(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { it }
                ) { index -> pageContent(index) }

                PageTurn.Curl -> CurlPager(
                    state = state,
                    colors = colors,
                    decoration = decoration,
                    pagePadding = pagePadding,
                    layoutDirection = outerDirection,
                    modifier = Modifier.fillMaxSize(),
                    pageContent = pageContent,
                )
            }
        }
    }
}

/**
 * تقليب الورقة: صفحة مكشوفة تحت، وظلّ، وشريط ظهر الورقة، والصفحة الحالية
 * مقصوصة فوق. مركّبة صفحتين بس — الحالية وجارها — والباقي مش مركّب.
 *
 * مزامنة `PagerState`: لما التكشيرة تهدأ على صفحة، `scrollToPage` من غير
 * حركة؛ وأي تغيير خارجي في `currentPage` (قفزة قارئ مثلًا) بيستبدل وجهة
 * التكشيرة الجارية — مفيش طوابير.
 */
@Composable
private fun CurlPager(
    state: PagerState,
    colors: MushafColors,
    decoration: MushafDecoration,
    pagePadding: PaddingValues,
    layoutDirection: LayoutDirection,
    modifier: Modifier = Modifier,
    pageContent: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // مصدر واحد لتقدّم التكشيرة: الكتابة من السحب متزامنة، والحركة (نابض
    // التسويّة) بتكتب فيه من كوروتين منفصلة.
    var progress by remember { mutableFloatStateOf(0f) }
    var peelFrom by remember { mutableStateOf(state.currentPage) }
    var peelTo by remember { mutableStateOf(state.currentPage) }
    var settledPage by remember { mutableStateOf(state.currentPage) }
    var peelJob by remember { mutableStateOf<Job?>(null) }

    fun animateProgressTo(target: Float, onEnd: suspend () -> Unit) {
        peelJob?.cancel()
        peelJob = scope.launch {
            animate(
                initialValue = progress,
                targetValue = target,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
            ) { value, _ -> progress = value }
            onEnd()
        }
    }

    fun commit(target: Int) {
        animateProgressTo(1f) {
            // سباق القفزات الخارجية: لو الحالة اتحرّكت لصفحة لا هي الصفحة
            // المستقرة ولا وجهة التكشيرة، فالقفزة هي الحكم — التكشيرة تتبدّل.
            val settled = when (state.currentPage) {
                settledPage, target -> target
                else -> state.currentPage
            }
            settledPage = settled
            progress = 0f
            peelFrom = settled
            peelTo = settled
            state.scrollToPage(settled)
        }
    }

    fun retreat() {
        animateProgressTo(0f) {
            peelTo = peelFrom
        }
    }

    // تغيير خارجي في الصفحة (قفزة قارئ/مُتِل) بيستبدل وجهة التكشيرة الجارية.
    // الـpager المخفي ده هو مُضيف دورة حياة `PagerState`: `scrollToPage`
    // مستني أول layout (AwaitFirstLayoutModifier) اللي بيركّبه الـpager بس.
    // من غيره أي قفزة على الحالة بتعلّق للأبد. ارتفاعه صفر وما بيرسمش حاجة
    // وما بيتركّبش غير الصفحة الحالية بخانة فاضية.
    HorizontalPager(
        state = state,
        modifier = Modifier.height(0.dp),
        beyondViewportPageCount = 0,
        userScrollEnabled = false,
    ) {}

    LaunchedEffect(Unit) {
        snapshotFlow { state.currentPage }.collect { page ->
            if (page != settledPage) {
                peelFrom = settledPage
                peelTo = page
                commit(page)
            }
        }
    }

    val widthPxState = remember { mutableFloatStateOf(0f) }
    Box(
        modifier
            .fillMaxSize()
            .background(colors.paper)
            .onSizeChanged { widthPxState.floatValue = it.width.toFloat().coerceAtLeast(1f) }
    ) {
        val density = LocalDensity.current
        val viewConfig = LocalViewConfiguration.current
        val widthPx by rememberUpdatedState(widthPxState.floatValue.coerceAtLeast(1f))
        val backPx = with(density) { CURL_BACK_WIDTH.toPx() }
        val shadowPx = with(density) { CURL_SHADOW_WIDTH.toPx() }

        // فجوة الهامش لتقليب بلمسة: برّة عمود النص أفقيًا (الرأس والتذييل
        // مش بيغيّروا الإحداثيات الأفقية).
        val contentRect by rememberUpdatedState(
            if (!decoration.pageFrame) {
                null
            } else {
                val leftPad = with(density) { pagePadding.calculateLeftPadding(layoutDirection).toPx() }
                val rightPad = with(density) { pagePadding.calculateRightPadding(layoutDirection).toPx() }
                val g = decoration.frame.geometry(
                    PageFrameRequest(
                        widthPx = (widthPx - leftPad - rightPad).coerceAtLeast(1f),
                        heightPx = 1f,
                        pageNumber = settledPage + 1,
                        density = density.density,
                        outerMargin = decoration.frameOuterMargin,
                        innerMargin = decoration.frameInnerMargin,
                        topMargin = decoration.frameTopMargin,
                        bottomMargin = decoration.frameBottomMargin,
                        textGap = decoration.textGap,
                        divisionMarks = decoration.divisionMarks,
                    )
                )
                (leftPad + g.contentLeft)..(leftPad + g.contentLeft + g.contentWidth)
            }
        )

        Box(
            Modifier
                .fillMaxSize()
                .curlGestures(
                    touchSlop = viewConfig.touchSlop,
                    longPressTimeoutMs = viewConfig.longPressTimeoutMillis,
                    marginTurn = { pos ->
                        val rect = contentRect
                        rect != null && pos.x !in rect
                    },
                    onDragStart = { forward ->
                        peelJob?.cancel()
                        peelFrom = settledPage
                        peelTo = (settledPage + if (forward) 1 else -1)
                            .coerceIn(0, Quran.PAGE_COUNT - 1)
                    },
                    onDrag = { delta ->
                        if (peelTo != peelFrom) {
                            val sign = if (peelTo > peelFrom) 1f else -1f
                            progress = (progress + sign * delta / widthPx).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        when {
                            peelTo == peelFrom -> Unit
                            progress >= CURL_COMMIT_THRESHOLD -> commit(peelTo)
                            else -> retreat()
                        }
                    },
                    onTapMargin = { forward ->
                        val target = (settledPage + if (forward) 1 else -1)
                            .coerceIn(0, Quran.PAGE_COUNT - 1)
                        if (target != settledPage) {
                            peelFrom = settledPage
                            peelTo = target
                            commit(target)
                        }
                    },
                )
        ) {
            if (peelFrom == peelTo) {
                CurlPage(peelFrom, pageContent)
            } else {
                // الصفحة المكشوفة تحت.
                CurlPage(peelTo, pageContent)
                // الظلّ على المكشوف + شريط ظهر الورقة.
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        val slice = curlSlice(
                            widthPx = size.width,
                            progress = progress,
                            forward = peelTo > peelFrom,
                            backWidthPx = backPx,
                            shadowWidthPx = shadowPx,
                        )
                        val p = progress
                        val alpha = (0.22f * 4f * p * (1f - p)).coerceIn(0f, 0.22f)
                        val s0 = minOf(slice.shadowLeft, slice.shadowRight)
                        val s1 = maxOf(slice.shadowLeft, slice.shadowRight)
                        if (alpha > 0f && s1 > s0) {
                            val edge = Color.Black.copy(alpha = alpha)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = if (peelTo > peelFrom) {
                                        listOf(Color.Transparent, edge)
                                    } else {
                                        listOf(edge, Color.Transparent)
                                    },
                                    startX = s0,
                                    endX = s1,
                                ),
                                topLeft = Offset(s0, 0f),
                                size = Size(s1 - s0, size.height),
                            )
                        }
                        val b0 = minOf(slice.backLeft, slice.backRight)
                        val b1 = maxOf(slice.backLeft, slice.backRight)
                        if (b1 > b0) {
                            drawRect(
                                color = lerp(colors.paper, Color.Black, 0.08f),
                                topLeft = Offset(b0, 0f),
                                size = Size(b1 - b0, size.height),
                            )
                        }
                    }
                ) {}
                // الصفحة الحالية مقصوصة عند خط التكشيرة.
                Box(
                    Modifier.fillMaxSize().drawWithContent {
                        val boundary = curlSlice(
                            widthPx = size.width,
                            progress = progress,
                            forward = peelTo > peelFrom,
                            backWidthPx = backPx,
                            shadowWidthPx = shadowPx,
                        ).boundaryX
                        if (peelTo > peelFrom) {
                            clipRect(left = boundary, right = size.width) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            clipRect(left = 0f, right = boundary) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
                ) {
                    CurlPage(peelFrom, pageContent)
                }
            }
        }
    }
}

/** صفحة واحدة في تقليب الورقة — مع عدّاد التركيب للاختبارات. */
@Composable
private fun CurlPage(
    index: Int,
    pageContent: @Composable (Int) -> Unit,
) {
    SideEffect { MushafCurlProbe.composedPages.add(index) }
    pageContent(index)
}

/**
 * خط رأس الصفحة وتذييلها.
 *
 * الأرقام (الجزء والصفحة) **لازم** تتكتب بخط غير خط المصحف: خط المصحف بيحوّل أي
 * رَنّ أرقام عربية لوردة آية (رابطة `rlig`)، فرقم الصفحة كان بيطلع جوّه وردة.
 * اتشاف على الموبايل مش في الـharness، لأن الـharness بيصوّر `MushafPage` لوحدها.
 */
private fun chromeStyle(colors: MushafColors, font: FontFamily = FontFamily.Default) =
    TextStyle(fontFamily = font, fontSize = 15.sp, color = colors.marker)

@Composable
private fun PageHeader(ayahs: List<Ayah>?, colors: MushafColors) {
    val first = ayahs?.firstOrNull()
    // السورة يمين والجزء شمال — زي رأس الصفحة المطبوعة.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                text = first?.let { "سُورَةُ " + Quran.surah(it.surah).nameArabic } ?: "",
                style = chromeStyle(colors, MushafFont)
            )
            BasicText(
                text = first?.let { "الجزء " + toArabicDigits(it.juz) } ?: "",
                style = chromeStyle(colors)
            )
        }
    }
}

@Composable
private fun PageFooter(page: Int, colors: MushafColors) {
    Box(Modifier.fillMaxWidth().padding(top = 2.dp), contentAlignment = Alignment.Center) {
        BasicText(text = toArabicDigits(page), style = chromeStyle(colors))
    }
}

/**
 * The colour key for the tajweed rules — a wrapping row of swatch + name.
 *
 * @param english show [TajweedRule.englishName] instead of the Arabic name.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TajweedLegend(
    modifier: Modifier = Modifier,
    colors: MushafColors = MushafColors.Light,
    includeNaturalMadd: Boolean = false,
    english: Boolean = false,
    textColor: Color = colors.ink
) {
    val rules = TajweedRule.entries.filter { includeNaturalMadd || !it.isNaturalMadd }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (english) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rules.forEach { rule ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape).background(colors.tajweedColor(rule))
                    )
                    BasicText(
                        text = if (english) rule.englishName else rule.label,
                        modifier = Modifier.padding(horizontal = 5.dp),
                        style = TextStyle(fontSize = 13.sp, color = textColor)
                    )
                }
            }
        }
    }
}
