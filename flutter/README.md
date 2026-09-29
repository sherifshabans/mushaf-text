# mushaf_text

مصحف المدينة **كنصّ** (لا صور) في Flutter — الصفحة مطابقة للمطبوع سطرًا بسطر،
بخط مجمّع الملك فهد، ومعه وضع **التجويد** بالألوان، وكل حكم يحمل مرجعه.

The Madinah Mushaf rendered as **text** in Flutter — every page laid out
line-for-line like the printed copy, in the King Fahd Complex font, with an
optional colour-coded **tajweed** mode in which every rule carries its source.

| عادي · Plain | تجويد · Tajweed |
|---|---|
| ![](doc/page.png) | ![](doc/page_tajweed.png) |

**بالعربي**

- ٦٠٤ صفحات، ١٥ سطرًا، وكسر السطور **نفس** المطبوع لا تقديرًا له. والسطور
  الممدودة والمتوسّطة مقيسة من صور المصحف نفسه.
- خط **KFGQPC HAFS Uthmanic Script** — التنوين المرصوص والوردة برقمها مضبوطان.
- ١٨ حكم تجويد، لكل حكم لونه، ولمس الحرف الملوّن يعطيك الحكم وشرحه **ومرجعه**
  من تحفة الأطفال أو المقدمة الجزرية برقم البيت ونصّه.
- يعمل بلا إنترنت تمامًا: النصّ والخط والتخطيط داخل الحزمة. لا شبكة ولا API.
- التلوين **لا يحرّك** حرفًا واحدًا في الصفحة — متحقَّق منه بالقياس لا بالافتراض.

**In English**

- 604 pages, 15 lines each, and the line breaks are **the printed ones**, not an
  approximation. The stretched and half-width lines were measured from images of
  the mushaf itself.
- The **KFGQPC HAFS Uthmanic Script** font, so the stacked tanween and the
  numbered ayah rosette come out right.
- 18 tajweed rules, a colour each. Tapping a coloured letter returns the rule,
  its explanation, and **its reference** — the matn, the verse number, and the
  verse itself.
- Fully offline: text, font and layout all ship inside the package. No network,
  no API.
- Colouring **moves nothing** on the page. That is measured, not assumed.

## التركيب · Install

```yaml
dependencies:
  mushaf_text: ^1.1.1
```

## الاستعمال · Use

```dart
import 'package:mushaf_text/mushaf_text.dart';

ColoredBox(
  color: MushafColors.light.paper,
  child: MushafPage(
    page: 3,
    tajweed: true,
    onTajweedTap: (hit) => showRule(hit.rule),   // لُمس حرف ملوّن
    onAyahTap: (ayahId) => selectAyah(ayahId),
  ),
)
```

الصفحة **لا ترسم خلفية لنفسها**، فضع `MushafColors.paper` — أو لونك أنت — خلفها،
وهي تتركّب مع أي حاوية.

The page paints **no background of its own**, so put `MushafColors.paper` — or
your own — behind it, and it composes with any container.

### النصّ وحده · The text on its own

```dart
final ayahs = await Quran.page(3);
final spans = TajweedAnnotator.annotate(ayahs.first.text);
for (final s in spans) {
  print('${s.rule.label}: ${ayahs.first.text.substring(s.start, s.end)}');
}
```

`Quran.ayahs()` و`Quran.surahAyahs()` و`Quran.ayah(surah, number)` و
`Quran.surahs` تعطيك النصّ كلّه دون رسم شيء، و`pageTajweedCounts(page)` تعطيك
عدد كلمات كلّ حكم في الصفحة.

`Quran.ayahs()`, `Quran.surahAyahs()`, `Quran.ayah(surah, number)` and
`Quran.surahs` give the whole text without drawing anything, and
`pageTajweedCounts(page)` counts the words carrying each rule on a page.

### الألوان · Colours

`MushafColors.light` و`MushafColors.dark`، أو ألوانك أنت. ويمكن تغيير لون حكم
بعينه:

`MushafColors.light` and `MushafColors.dark`, or your own. Individual rules can
be overridden:

```dart
MushafColors.light.copyWith(
  tajweedOverrides: {TajweedRule.ikhfa: Colors.deepPurple},
)
```

## من أين جاءت الأحكام · Where the tajweed rules come from

شيئان يجب أن يصحّا، ويثبت كلٌّ منهما على حدة: **أين** يقع الحكم، و**ما** هو.

Two things have to be right, and they are established separately: **where** a
rule falls, and **what** the rule is.

### أين — من الرسم نفسه · Where — from the script itself

**ليست من بيانات مستوردة.** المنشور منها يفهرس نصّ *تنزيل* العثماني، وهذه الحزمة
تحمل نصّ *مجمّع الملك فهد*، والاثنان يختلفان في كتابة العلامات نفسها: السكون هنا
`U+06E1` وهناك `U+0652`، والتنوين المرصوص يُكتب `U+0656`/`U+0657`/`U+065E`. فنقل
مواضعهم كما هي يُزحلق الألوان إلى حروف أخرى.

ورسم المجمّع يكتب هذه الأحكام أصلًا — لأجل ذلك طُبع. فُحص على الآيات الـ٦٢٣٦ كلها:

**Not from an imported dataset.** The published ones index into the *Tanzil*
Uthmani text while this package carries the *KFGQPC* one, and the two differ in
how they encode the marks themselves — a sukun is `U+06E1` here and `U+0652`
there, and a stacked tanween is written `U+0656`/`U+0657`/`U+065E`. Applying
their offsets unchanged slides the colours onto the wrong letters.

The KFGQPC script already writes these rules out — that is what it is printed
for. Checked across all 6236 verses:

| الرسم · written as | المعنى · means | العدد · count |
|---|---|---|
| نون بسكون، أو تنوين جانبي — noon with a sukun, or a side-by-side tanween | **إظهار**، بلا لون — **izhar**, left uncoloured | 1716 / 1911 |
| نون عارية، أو تنوين مرصوص — a bare noon, or a stacked tanween | إخفاء أو إدغام — ikhfa or idgham | 5139 / 6643 |
| ميم صغيرة فوق أو تحت — a small high or low meem | **إقلاب** — **iqlab** | 609 |
| مدّة `U+0653` — a maddah `U+0653` | مدّ أطول من حركتين — a madd longer than two counts | 5652 |
| صفر مستطيل قائم `U+06E0` — an upright rectangular zero | ألف تسقط وصلًا وتثبت وقفًا — an alef dropped in wasl, kept in waqf | 66 |
| واو أو ياء صغيرة — a small waw or yeh | **مدّ صلة** — **madd silah** | 2213 |

وهذا التنفيذ بلغة Dart يُخرج **مواضع مطابقة تمامًا لتنفيذ Kotlin في الآيات
الـ٦٢٣٦**، وعددها ٨٥٬٣٨٦ موضعًا، يثبّتها `test/tajweed_parity_test.dart` على
فيكسشر يكتبه طرف Kotlin.

This Dart implementation produces **identical spans to the Kotlin one for all
6236 verses**, 85,386 of them, which `test/tajweed_parity_test.dart` enforces
against a fixture the Kotlin side writes.

### ما هو — من مراجع مسمّاة · What — from named references

كل حكم يحمل `source` و`evidence`: المتن الذي جاء منه، ورقم البيت، والبيت بنصّه
معروضًا على القارئ في التطبيق.

Every rule carries `source` and `evidence`: the matn it comes from, its verse
number, and the verse in its own words, shown to the reader in the app.

- **تحفة الأطفال والغلمان** للجمزوري (ت ١١٩٨ هـ) — أحكام النون الساكنة والتنوين،
  والميم الساكنة، ولام «أل»، والمدود.
  *al-Jamzūrī (d. 1198 AH) — the noon sākinah and tanwīn rules, the meem sākinah
  rules, the lām of «أل», and the madd rules.*
- **المقدمة الجزرية** لابن الجزري (ت ٨٣٣ هـ) — القلقلة والغنّة وإخفاء الميم.
  *Ibn al-Jazarī (d. 833 AH) — qalqalah, ghunnah, and the ikhfāʾ of the meem.*
- **التعريف بمصحف المدينة النبوية** (مجمّع الملك فهد) — علامات الضبط: الصفر
  المستطيل القائم، والواو والياء الصغيرتان، وهمزة الوصل.
  *(King Fahd Complex) — the notation: the upright rectangular zero, the small
  wāw and yāʾ, and hamzat al-waṣl.*

ثلاثة أحكام — مدّ الصلة وهمزة الوصل والألف الزائدة — **ليست** في المتنين، ومصدرها
يقول ذلك صراحةً بدل أن ينسب إليهما ما ليس فيهما. والرواية في الجميع **حفص عن عاصم
من طريق الشاطبية**.

Three rules — madd ṣilah, hamzat al-waṣl and the extra alef — are **not** in
either matn; their `source` says so rather than claiming an authority they do
not have. The riwāyah throughout is **Ḥafṣ ʿan ʿĀṣim** by the Shāṭibiyyah.

و`test/tajweed_reference_test.dart` يحاكم الكود إلى تلك المراجع لا إلى عاداته، على
المصحف كلّه:

`test/tajweed_reference_test.dart` holds the code to those references rather than
to its own habits, over the whole mushaf:

- **تحفة ١١** — النون الساكنة إذا لقيت حرفًا من «ينمو» **في كلمة واحدة** فهي
  إظهار مطلق لا إدغام. المواضع الـ١٢٥ كلها (دُنْيَا، صِنْوَان، قِنْوَان،
  بُنْيَان) تبقى بلا لون.
  *Tuhfa v.11 — a noon sākinah meeting a yanmū letter inside one word is iẓhār
  muṭlaq, not idghām. All 125 positions stay uncoloured.*
- **تحفة ٥٤–٥٦** — في فواتح السور، «كم عسل نقص» وحدها ستّ حركات، و«حي طهر» مدّ
  طبيعي، والألف لا مدَّ فيها. الفواتح الـ٢٩ كلها متّفقة، بلا استثناء واحد.
  *Tuhfa vv.54–56 — in the surah openings, only «كم عسل نقص» takes six counts;
  «حي طهر» is a natural madd and the alef takes none. All 29 openings agree,
  with no exceptions.*
- **تحفة ٧–٨** — لا يُلوَّن إظهار حلقي أبدًا.
  *Tuhfa vv.7–8 — no iẓhār ḥalqī is ever coloured.*
- **دليل الضبط** — مواضع الألف الزائدة الـ٦٦ كلها ألف، ونصّ الحكم «تسقط وصلًا
  وتثبت وقفًا» لا «لا تُنطق».
  *The notation guide — all 66 extra-alef positions are alef, and the rule reads
  «dropped in waṣl, kept in waqf», not «silent».*

> الأحكام الـ١٨ صار كلٌّ منها مسنودًا إلى مرجع مسمّى، ومواضعها مختبَرة عليه.
> والذي لم يحدث بعدُ أن يقرأ **قارئ مُجاز** الشروح الثمانية عشر كاملة. الأحكام لها
> مصادرها؛ أمّا صياغة الشروح فمنّا، وعين الشيخ فيها أنفع من اختبار جديد.
>
> Each of the 18 rulings is now tied to a named reference, and the positions are
> tested against it. What has still not happened is a **qualified reader** going
> through the 18 explanatory texts end to end. The rulings are sourced; the
> phrasing of the explanations is ours, and a teacher's eye would be worth more
> than another test.

## لماذا لا يكسر التلوينُ الصفحةَ · Why colouring cannot break the page

`TextStyle` الذي لا يضبط إلا لونًا لا يغيّر مقياسًا، فالكلمة تُقاس كما هي سواء
حملت لونًا واحدًا أو عشرة — فكسر السطور، والتمديد، وحجم الخط المشتقّ من أعرض سطر،
كلّها تبقى في مكانها. و`test/mushaf_render_test.dart` يقيس ذلك بالخط الحقيقي على
مئات الكلمات الملوّنة بدل افتراضه.

A `TextStyle` that sets only a colour changes no metric, so a word measures the
same whether it carries one colour or ten — line breaks, the stretching, and the
font size derived from the widest line all stay exactly put.
`test/mushaf_render_test.dart` measures that with the real font over hundreds of
coloured words rather than assuming it.

وثمّة حدٌّ حقيقي واحد: الحرف الذي يطويه الخط من عدّة رموز — كلام-ألف — يأخذ لونًا
**واحدًا**. وذلك من OpenType لا من هذه الحزمة.

One limit is real: a glyph the font folds out of several code points — a lam-alef
ligature — takes **one** colour. That is OpenType, not this package.

## ولأندرويد أيضًا · Also for Android

نفس المحرّك لـ Jetpack Compose في هذا المستودع باسم
`com.github.sherifshabans:mushaf-text` على JitPack. والاثنان يتشاركان النصّ
والتخطيط وأحكام التجويد، آيةً بآية.

The same renderer for Jetpack Compose lives in this repository as
`com.github.sherifshabans:mushaf-text` on JitPack. The two share their text,
their layout and their tajweed rules, verse for verse.

## الترخيص · Licence

الكود بترخيص MIT. أمّا خط **KFGQPC HAFS Uthmanic Script** فهو لمجمّع الملك فهد:
يُستعمل ويُعاد نشره بحرّية، و**لا** يُعدَّل ولا يُباع — وهو مشحون هنا حرفًا بحرف.
انظر `licenses/` في جذر المستودع.

The code is MIT. The bundled **KFGQPC HAFS Uthmanic Script** font is the King
Fahd Complex's: free to use and redistribute, **not** to modify or sell — it
ships here byte for byte. See `licenses/` in the repository root.

ويحمل تطبيق المثال إضافةً خط **Amiri** (بترخيص SIL Open Font License 1.1)
لعناوين واجهته، ليقرأ كما يقرأ تطبيق أندرويد الذي يحاكيه. وAmiri ليس جزءًا من
الحزمة نفسها — فـ`MushafPage` لا يحتاج إلا خط المجمّع، وهو يسافر معها.

The example app additionally bundles **Amiri** (SIL Open Font License 1.1) for
its interface labels, so it reads like the Android app it mirrors. Amiri is not
part of the package itself — `MushafPage` needs only the KFGQPC font, which
travels with it.
