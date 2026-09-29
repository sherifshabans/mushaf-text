# mushaf_text

مصحف المدينة **كنص** (مش صور) في Flutter — الصفحة مطابقة للمطبوع سطرًا بسطر،
بخط مجمّع الملك فهد، ومعاه وضع **التجويد** بالألوان.

The Madinah Mushaf rendered as **text** in Flutter — every page laid out
line-for-line like the printed copy, in the King Fahd Complex font, with an
optional colour-coded **tajweed** mode.

| عادي · Plain | تجويد · Tajweed |
|---|---|
| ![](doc/page.png) | ![](doc/page_tajweed.png) |

- ٦٠٤ صفحة، ١٥ سطرًا، وكسر السطور **نفس** المطبوع — مش تقدير. والسطور الممدودة
  والمتوسّطة مقاسة من صور المصحف نفسها.
- خط **KFGQPC HAFS Uthmanic Script** — التنوين المرصوص والوردة برقمها مظبوطين.
- ١٨ حكم تجويد، كل حكم بلونه، ولمس الحرف الملوّن بيرجّع الحكم وشرحه.
- شغّال أوفلاين بالكامل: النص والخط والتخطيط جوّه الحزمة. مفيش نت ولا API.
- التلوين **مابيحرّكش** ولا حرف في الصفحة — متأكَّد منه بالقياس، مش افتراض.

## Install

```yaml
dependencies:
  mushaf_text: ^1.0.0
```

## Use

```dart
import 'package:mushaf_text/mushaf_text.dart';

ColoredBox(
  color: MushafColors.light.paper,
  child: MushafPage(
    page: 3,
    tajweed: true,
    onTajweedTap: (hit) => showRule(hit.rule),   // tapped a coloured letter
    onAyahTap: (ayahId) => selectAyah(ayahId),
  ),
)
```

The page paints **no background of its own**, so put `MushafColors.paper` — or
your own — behind it, and it composes with any container.

### The text on its own

```dart
final ayahs = await Quran.page(3);
final spans = TajweedAnnotator.annotate(ayahs.first.text);
for (final s in spans) {
  print('${s.rule.label}: ${ayahs.first.text.substring(s.start, s.end)}');
}
```

`Quran.ayahs()`, `Quran.surahAyahs()`, `Quran.ayah(surah, number)` and
`Quran.surahs` give the whole text without drawing anything.

### Colours

`MushafColors.light` and `MushafColors.dark`, or your own. Individual rules can
be overridden:

```dart
MushafColors.light.copyWith(
  tajweedOverrides: {TajweedRule.ikhfa: Colors.deepPurple},
)
```

## Where the tajweed rules come from

**Not from an imported dataset.** The published ones index into the *Tanzil*
Uthmani text while this package carries the *KFGQPC* one, and the two differ in
how they encode the marks themselves — a sukun is `U+06E1` here and `U+0652`
there, and a stacked tanween is written `U+0656`/`U+0657`/`U+065E`. Applying
their offsets unchanged slides the colours onto the wrong letters.

The KFGQPC script already writes these rules out — that is what it is printed
for. Checked across all 6236 verses:

| written as | means | count |
|---|---|---|
| noon with a sukun, or a side-by-side tanween | **izhar**, left uncoloured | 1716 / 1911 |
| a bare noon, or a stacked tanween | ikhfa or idgham | 5139 / 6643 |
| a small high or low meem | **iqlab** | 609 |
| a maddah `U+0653` | a madd longer than two counts | 5652 |
| a rectangular zero `U+06E0` | a letter that is not pronounced | 66 |
| a small waw or yeh | **madd silah** | 2213 |

The letter that follows settles the rest. Two checks guard it: no izhar is ever
coloured — 3627 of them — and this Dart implementation produces **identical
spans to the Kotlin one for all 6236 verses**, 85,386 of them, which
`test/tajweed_parity_test.dart` enforces against a fixture the Kotlin side
writes.

> **The rule names, definitions and counts were written from knowledge and have
> not been checked against a named tajweed reference.** The *positions* are
> verified; the *wording* deserves a qualified reader's eye before anyone relies
> on it for teaching. The colours are a proposal, not a reproduction of any
> printed mushaf.

## Why colouring cannot break the page

A `TextStyle` that sets only a colour changes no metric, so a word measures the
same whether it carries one colour or ten — line breaks, the stretching, and the
font size derived from the widest line all stay exactly put.
`test/mushaf_render_test.dart` measures that with the real font over hundreds of
coloured words rather than assuming it.

One limit is real: a glyph the font folds out of several code points — a
lam-alef ligature — takes **one** colour. That is OpenType, not this package.

## Also for Android

The same renderer for Jetpack Compose lives in this repository as
`com.github.sherifshabans:mushaf-text` on JitPack. The two share their text,
their layout and their tajweed rules, verse for verse.

## Licence

The code is MIT. The bundled **KFGQPC HAFS Uthmanic Script** font is the King
Fahd Complex's: free to use and redistribute, **not** to modify or sell — it
ships here byte for byte. See `licenses/` in the repository root.
