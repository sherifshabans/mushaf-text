package io.github.sherifshabans.mushaf.sample.quran

import io.github.sherifshabans.mushaf.TajweedRule
import io.github.sherifshabans.mushaf.TajweedFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ورقة واحدة بأربعة تبويبات تحتها، بدل أربع مداخل متفرّقة.
 *
 * ## ليه
 * ترويسة المصحف كانت فيها خمس أيقونات والعنوان مخنوق بينهم، **وفوق كده** شريط
 * الختمة كان واخد ٥٠dp من ارتفاع الصفحة في كل صفحة — فصفحة المصحف نزلت لتحت
 * وضاقت. والشريط ده أصلًا اتعمل شريطًا لأن الترويسة كانت مليانة، فالحلّ كان
 * بيصلّح عَرَضًا ويدفع التمن من الارتفاع.
 *
 * دلوقتي الترويسة فيها أربعة بس، وآخر واحدة (**الفهرس**) بتفتح الورقة دي —
 * وجوّاها شريط تنقّل بيبدّل بين: الفهرس · علاماتي · التجويد.
 * الشريط اتشال خالص، فالصفحة رجعت تاخد ارتفاعها كامل.
 */
enum class BrowseTab(val label: String, val icon: ImageVector) {
    INDEX("الفهرس", Icons.AutoMirrored.Filled.List),
    MARKS("علاماتي", Icons.Default.Bookmarks),
    TAJWEED("التجويد", Icons.Default.Palette)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranBrowseSheet(
    currentPage: Int,
    marks: List<io.github.sherifshabans.mushaf.sample.quran.AyaMark>,
    tajweedEnabled: Boolean,
    tajweedNaturalMadd: Boolean,
    /** أحكام الصفحة المفتوحة دلوقتي وعددها — لتبويب التجويد. */
    pageRules: List<Pair<TajweedRule, Int>>,
    initialTab: BrowseTab = BrowseTab.INDEX,
    onSelectPage: (Int) -> Unit,
    onSelectMark: (io.github.sherifshabans.mushaf.sample.quran.AyaMark) -> Unit,
    onDeleteMark: (Int) -> Unit,
    onTajweedEnabledChange: (Boolean) -> Unit,
    onNaturalMaddChange: (Boolean) -> Unit,
    /** صفّ في «أحكام هذه الصفحة» — بيوقّف القارئ على أول موضع للحكم. */
    onPageRuleClick: (TajweedRule) -> Unit,
    /** صفّ في المفتاح الكامل — بيفتح شرح الحكم. */
    onRuleClick: (TajweedRule) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by rememberSaveable { mutableStateOf(initialTab) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuranPalette.Paper,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            // المحتوى ياخد كل الارتفاع، وشريط التنقّل ثابت تحته.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    BrowseTab.INDEX -> SuraPickerPane(
                        currentPage = currentPage,
                        onSelect = onSelectPage
                    )

                    BrowseTab.MARKS -> MarksListPane(
                        marks = marks,
                        onSelect = onSelectMark,
                        onDelete = onDeleteMark
                    )

                    BrowseTab.TAJWEED -> TajweedPane(
                        enabled = tajweedEnabled,
                        naturalMadd = tajweedNaturalMadd,
                        pageRules = pageRules,
                        currentPage = currentPage,
                        onEnabledChange = onTajweedEnabledChange,
                        onNaturalMaddChange = onNaturalMaddChange,
                        onPageRuleClick = onPageRuleClick,
                        onRuleClick = onRuleClick
                    )
                }
            }

            HorizontalDivider(color = QuranPalette.Gold.copy(alpha = 0.4f))
            BrowseBar(selected = tab, onSelect = { tab = it })
        }
    }
}

/**
 * شريط التنقّل.
 *
 * مرسوم بإيد لا `NavigationBar`: الأخير بيفرض ارتفاع ٨٠dp وحشوات ماتيريال
 * الكاملة، وده كتير جوّه ورقة أصلًا مرتفعها محدود — والورقة دي الغرض منها توفير
 * ارتفاع لا استهلاكه.
 */
@Composable
private fun BrowseBar(selected: BrowseTab, onSelect: (BrowseTab) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuranPalette.Paper)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BrowseTab.entries.forEach { item ->
            val active = item == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(item) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (active) primary else QuranPalette.Brown.copy(alpha = 0.55f),
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.label,
                    fontFamily = AmiriFont,
                    fontSize = 11.5.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) primary else QuranPalette.Brown.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * تبويب التجويد.
 *
 * ## ليه «أحكام هذه الصفحة» فوق المفتاح الكامل
 * مفتاح ألوان من ١٦ حكمًا قائمة بتتقرا مرة وتتنسي. لكن «الصفحة اللي قدّامك
 * دلوقتي فيها ٤ إدغام و٧ إخفاء» معلومة عن **اللي بيقراه فعلًا**، وبتخلّي المفتاح
 * مدخلًا للصفحة لا جدولًا للحفظ. والأرقام دي محسوبة من نفس الأحكام اللي
 * الصفحة مرسومة بيها، فمفيش حسبة زيادة.
 */
@Composable
private fun TajweedPane(
    enabled: Boolean,
    naturalMadd: Boolean,
    pageRules: List<Pair<TajweedRule, Int>>,
    currentPage: Int,
    onEnabledChange: (Boolean) -> Unit,
    onNaturalMaddChange: (Boolean) -> Unit,
    onPageRuleClick: (TajweedRule) -> Unit,
    onRuleClick: (TajweedRule) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        TajweedSwitchRow(
            title = "تلوين الأحكام",
            subtitle = null,
            checked = enabled,
            onChange = onEnabledChange
        )
        TajweedSwitchRow(
            title = "المدّ الطبيعي",
            subtitle = "أكثر حكم تكرارًا — تلوينه يصبغ الصفحة",
            checked = naturalMadd,
            enabled = enabled,
            onChange = onNaturalMaddChange
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = QuranPalette.Gold.copy(alpha = 0.4f))
        Spacer(Modifier.height(14.dp))

        if (enabled && pageRules.isNotEmpty()) {
            Text(
                text = "أحكام صفحة ${toArabicNumerals(currentPage)}",
                fontFamily = AmiriFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "اضغط أي حكم يوقّفك على أول موضع له في الصفحة.",
                fontFamily = AmiriFont,
                fontSize = 12.5.sp,
                color = QuranPalette.Brown.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            pageRules.forEach { (rule, count) ->
                TajweedKeyRow(
                    rule = rule,
                    trailing = toArabicNumerals(count),
                    onClick = { onPageRuleClick(rule) }
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        Text(
            text = "كل الأحكام",
            fontFamily = AmiriFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "دوس على أي حكم لشرحه، أو على أي حرف ملوّن في الصفحة.",
            fontFamily = AmiriFont,
            fontSize = 12.5.sp,
            color = QuranPalette.Brown.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        TajweedFamily.entries.forEach { family ->
            Text(
                text = family.label,
                fontFamily = AmiriFont,
                fontSize = 12.sp,
                color = QuranPalette.Brown.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            TajweedRule.entries.filter { it.family == family }.forEach { rule ->
                TajweedKeyRow(
                    rule = rule,
                    trailing = rule.amount,
                    onClick = { onRuleClick(rule) }
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}
