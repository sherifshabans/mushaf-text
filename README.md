# mushaf-text

[![](https://jitpack.io/v/sherifshabans/mushaf-text.svg)](https://jitpack.io/#sherifshabans/mushaf-text)

مصحف المدينة **كنص** (مش صور) في Jetpack Compose — الصفحة مطابقة للمطبوع سطرًا
بسطر، بخط مجمّع الملك فهد، ومعاه وضع **مصحف التجويد** بالألوان.

The Madinah Mushaf rendered as **text** in Jetpack Compose — every page laid out
line-for-line like the printed copy, in the King Fahd Complex font, with an
optional colour-coded **tajweed** mode.

| عادي · Plain | تجويد · Tajweed | ليلي · Night |
|---|---|---|
| ![](docs/page.png) | ![](docs/page_tajweed.png) | ![](docs/page_dark.png) |

- ٦٠٤ صفحة، ١٥ سطرًا، وكسر السطور **نفس** المطبوع (مش تقدير) — والسطور الممدودة
  والمتوسّطة مقاسة من صور المصحف.
- خط **KFGQPC HAFS Uthmanic Script** — التنوين المرصوص والوردة برقمها مظبوطين.
- ١٦ حكم تجويد، كل حكم بلونه، ولمس الحرف الملوّن بيرجّع الحكم وشرحه.
  التلوين **مابيحرّكش** ولا حرف في الصفحة (متأكَّد منه باختبار).
- شغّال أوفلاين بالكامل: النص والخط والتخطيط جوّه المكتبة. مفيش نت ولا API.

## التثبيت · Installation

**1.** ضيف JitPack في `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**2.** ضيف المكتبة في `build.gradle.kts` بتاع الموديول:

```kotlin
dependencies {
    implementation("com.github.sherifshabans:mushaf-text:1.0.0")
}
```

Groovy (`build.gradle`):

```groovy
repositories { maven { url 'https://jitpack.io' } }
dependencies { implementation 'com.github.sherifshabans:mushaf-text:1.0.0' }
```

متطلبات: `minSdk 21`، Jetpack Compose. المكتبة بتزوّد التطبيق حوالي ١٫٧ ميجا
(النص ١٫٤ + الخط ٠٫٢٤).

## الاستخدام · Usage

### المصحف كامل (صفحة بصفحة)

```kotlin
MushafPager(
    modifier = Modifier.fillMaxSize(),
    state = rememberMushafPagerState(initialPage = 1),   // رقم الصفحة ١‥٦٠٤
    tajweed = false,                                     // true = مصحف التجويد
    colors = MushafColors.Light,                         // أو MushafColors.Dark
    onAyahClick = { ayah -> /* ayah.surah, ayah.number, ayah.text */ }
)
```

الصفحات بتتقلّب من اليمين للشمال زي المصحف المطبوع مهما كانت لغة التطبيق.

### صفحة واحدة

```kotlin
MushafPage(
    page = 50,
    modifier = Modifier.fillMaxSize().background(MushafColors.Light.paper),
    tajweed = true
)
```

الصفحة بتاخد أكبر حجم خط بيخلّي أعرض سطر يدخل في العرض المتاح، وبتتجاهل حجم
خط النظام عن قصد (هي نسخة من صفحة مطبوعة؛ التكبير بيكسر الـ١٥ سطر). لو عايز نص
بيكبر مع إعدادات القارئ استخدم `QuranText`.

### مصحف التجويد

```kotlin
var hit by remember { mutableStateOf<TajweedHit?>(null) }

MushafPage(
    page = 3,
    tajweed = true,
    naturalMadd = false,          // المدّ الطبيعي مقفول افتراضيًا — بيلوّن الصفحة كلها أخضر
    onTajweedClick = { hit = it } // لمس حرف ملوّن
)

hit?.let { Text("${it.rule.label}: ${it.rule.definition} — ${it.rule.amount}") }

TajweedLegend()                   // مفتاح الألوان
```

كل `TajweedRule` فيه: `label` (الاسم بالعربي)، `englishName`، `definition`،
`amount` (المقدار)، `letters`، و`color`.
ولتغيير ألوان أحكام معيّنة:

```kotlin
val colors = MushafColors.Light.copy(
    tajweed = mapOf(TajweedRule.GHUNNA to Color(0xFFE91E63))
)
```

`focusRule` + `focusIndex` بيحطّوا إطار حوالين موضع حكم معيّن على الصفحة (مفيد
لشاشة «أحكام هذه الصفحة»).

### تظليل الآيات (تحديد، علامات، التلاوة)

```kotlin
MushafPage(
    page = 2,
    selectedAyahIds = setOf(ayahId),
    highlightedAyahs = mapOf(ayahId to Color.Yellow),   // علامات المستخدم
    playingAyahIds = setOf(nowPlayingId),               // الآية اللي بتتلى — ليها الأولوية
    onAyahClick = { },
    onAyahLongClick = { }
)
```

### نص متدفّق (آية اليوم، تفسير، بحث…)

```kotlin
val context = LocalContext.current
val kursi = remember { Quran.ayah(context, surah = 2, number = 255) }

kursi?.let { QuranText(ayah = it, fontSize = 22.sp, tajweed = true) }

// أو كذا آية في فقرة واحدة، مع لمس الآية
QuranText(ayahs = Quran.ayahsOfSurah(context, 112), onAyahClick = { })
```

### البيانات

```kotlin
Quran.surahs                         // ١١٤ سورة: الاسم عربي/إنجليزي، عدد الآيات، الصفحات
Quran.page(context, 1)               // آيات صفحة
Quran.ayahsOfSurah(context, 18)
Quran.pageOf(context, 18, 10)        // صفحة آية
Quran.load(context)                  // suspend — كل الآيات (٦٢٣٦) من غير ما يقفل الـUI
```

أول نداء بيقرأ النص من الـassets (عشرات الملّي ثانية) وبعدها كله من الكاش.
`MushafPage(page = …)` و`MushafPager` بيحمّلوا لوحدهم برّه الـmain thread.

## ملاحظات تقنية · Technical notes

- **النص** بترميز KFGQPC Hafs (السكون `U+06E1`، والتنوين المرصوص `U+0656/0657/065E`)،
  فلازم يترسم بـ`MushafFont`: أي خط عربي تاني بيرسم التنوين غلط.
- **علامة نهاية الآية** هي الأرقام لوحدها؛ الخط بيلمّها في حرف واحد فيه الوردة
  والرقم. أندرويد بيوصّل رَنّ الأرقام للمشكِّل مقلوبًا، فالمكتبة بتختار الترتيب
  **بالقياس** (الترتيب الصح بيطلع وردة واحدة بنصّ العرض) — فبتصحّح نفسها على أي إصدار.
- **أحكام التجويد مستخرجة من رسم النص نفسه**، مش من داتا خارجية: رسم KFGQPC بيفرّق
  أصلًا بين الإظهار (سكون ظاهر / تنوين جانبي) والإخفاء والإدغام (نون عريانة / تنوين
  مرصوص) والإقلاب (ميم صغيرة). فمفيش خطر إن اللون يقع على حرف غلط.
- **تخطيط السطور** من `layout.txt`: عدد الكلمات على كل سطر في كل صفحة، من رقم السطر
  لكل كلمة في بيانات quran.com. الاختبارات بتتأكد إن الـ٦٠٤ صفحة متطابقين مع النص كلمة بكلمة.

## الاختبارات · Tests

```bash
./gradlew :mushaf:testDebugUnitTest
```

- `MushafLayoutTest` — النص كامل (٦٢٣٦ آية، ١١٤ سورة) والتخطيط مطابق في كل الـ٦٠٤ صفحة.
- `TajweedAnnotatorTest` — مواضع سليمة على المصحف كله، الإظهار مابيتلوّنش، وأمثلة لكل حكم.
- `TajweedRenderTest` — التلوين مابيغيّرش مقاس ولا كلمة (Skia حقيقية + الخط الحقيقي).
- `MushafRenderHarness` — بيرسم صفحات PNG في `mushaf/build/mushaf-shots/` من غير محاكي:

```bash
./gradlew :mushaf:testDebugUnitTest --tests '*MushafRenderHarness*' -Pmushaf.pages=1,50,604
```

فيه كمان تطبيق تجريبي في `sample/`.

## الترخيص · License

الكود: [MIT](LICENSE).

خط **KFGQPC HAFS Uthmanic Script** © مجمّع الملك فهد لطباعة المصحف الشريف، مضمّن
**بدون أي تعديل** برخصته ([licenses/](licenses/KFGQPC-UthmanicHafs-LICENSE.txt)):
الاستخدام والنسخ والتوزيع مجانًا، والتعديل والبيع ممنوعين.
النص القرآني برواية حفص عن عاصم برسم مجمّع الملك فهد. تخطيط السطور مشتق من بيانات
[quran.com](https://quran.com).
