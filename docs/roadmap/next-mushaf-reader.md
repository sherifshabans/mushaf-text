# Next mushaf reader — implementation plan

Status: **plan only**. This document specifies the next milestone of the Kotlin
library. It does not implement page curl, hizb marks, new frames, or custom
header slots. Do not cut a version or a git tag from this document.

Published tag at the time of writing: **1.5.0**. `main` is ahead of that tag
with the page-number work from PR #3 (`pageNumberScale`, medallion-fitted
digits). JitPack consumers, including SaloAleh, depend on
`com.github.MahmoudMabrok:mushaf-text-customizable`. The Flutter package has no
`MushafDecoration` and is outside this milestone.

Suggested shipping version, when the work below actually lands: **1.6.0**.
Ship it as the pull requests in [§11](#11-sequenced-tasks), not as one change.

An implementer can start at [§3](#3-current-state) and [§7](#7-customization-api),
then take the tasks in [§11](#11-sequenced-tasks) in order.

---

## 1. Goals

1. **Page turn.** `MushafPager` can turn a leaf the way a bound mushaf does:
   the fore-edge peels, the back of the sheet shows, and the next page is
   revealed underneath. The current right-to-left slide stays available and
   remains the default.
2. **Division marks on the frame.** A framed page shows which kind of division
   starts on it — juz, hizb, rubʿ, nisf, three-quarters — as a medallion in the
   outer margin, aligned to the line, plus the printed running head (surah and
   juz). The fifteen sajdah positions get the same margin slot.
3. **More printed looks.** Three additional frame-and-header styles, taken from
   real editions and drawn with the library's existing geometry primitives.
   The 604-page, 15-line Hafs layout does not change.
4. **Host customization.** A caller can supply a surah-header composable and a
   `PageFrame` implementation. `pageFrame`, `SurahHeaderStyle.CLASSIC` /
   `MODERN` / `PLAIN`, and `MushafDecoration.Classic` / `.Modern` / `.None`
   keep today's look when the new fields are left at their defaults.

## 2. Non-goals

- A new riwayah, orthography, or page layout. Warsh, Qalun, the 1924 Amiri
  12-line mushaf (~827 pages), the Egyptian Shamarly layout (~522 pages), the
  Taj 16-line Indo-Pak mushaf (~548 pages), and the Diyanet orthography
  (Fatiha unnumbered, juz 30 broken differently) are different books. This
  milestone skins the Madinah 604-page text. It does not claim to be those
  editions.
- Stripping the inline `۞` (U+06DE) out of `hafs.tsv`. The star is part of the
  measured line. Removing it changes word counts and breaks `layout.txt`.
- Diyanet rukūʿ (`عين`) marks. Their positions are a different system and are
  not in the bundled text.
- A Maghrebi or Warsh skin. Recoloring Hafs text to look Maghrebi would read
  as a Warsh mushaf. Leave it out.
- A second tajweed palette. `tajweed = true` already colors letters. Dar
  al-Maʿrifah's printed tajweed mushaf is the same Madinah pages plus color.
  Pair it with whatever frame the host picks; do not invent new rule colors.
- Editing the KFGQPC font, bundling scanned ornaments, or tracing a
  copyrighted border bitmap into the repo. Frames are drawn with rules,
  diamonds, rosettes, and simple paths, the way `drawOrnatePageFrame` already
  works. Name the edition the structure comes from.
- Flutter parity, a version bump, a git tag, and any change to SaloAleh.
- Replacing `HorizontalPager` for callers who stay on the default turn.

## 3. Current state

### 3.1 What a host calls

`MushafPage` and `MushafPager` (`mushaf/src/main/java/.../MushafPage.kt`,
`MushafPager.kt`) take a `MushafDecoration`:

```kotlin
data class MushafDecoration(
    val pageFrame: Boolean = false,
    val surahHeaderStyle: SurahHeaderStyle = SurahHeaderStyle.CLASSIC,
    val ayahFrame: Boolean = false,
    val surahHeaderOverrides: Map<Int, SurahHeaderOverride> = emptyMap(),
    val textGap: Float = 8f,
    val frameOuterMargin: Float = 5f,
    val frameInnerMargin: Float = 2f,
    val frameTopMargin: Float = 1.5f,
    val frameBottomMargin: Float = 14f,
    val pageNumberScale: Float = DEFAULT_PAGE_NUMBER_SCALE, // 0.72
)
```

Presets: `None` (all defaults, no frame), `Classic` (frame + classic header +
ayah frame), `Modern` (same with the teal pill). `SurahHeaderStyle` is the
closed enum `CLASSIC`, `MODERN`, `PLAIN`.

`MushafPager` forces `LayoutDirection.Rtl` around a `HorizontalPager` so the
next page enters from the left, then restores the app direction inside each
page. `showHeaders` (default true) draws a plain row above the page — surah
name on the right, `الجزء N` on the left — and the page number under it.
`rememberMushafPagerState` is a `PagerState` over 604 pages; `currentPage` is
**0-based**, and the mushaf page number is `currentPage + 1`.

There is no page-turn enum, no frame interface, and no composable header slot.
Frame geometry (`MushafFrameGeometry`, `ornateFrameGeometry`) and the drawers
(`drawOrnatePageFrame`, `drawClassicSurahHeader`, `drawModernSurahHeader`) are
`internal` in `MushafFrameDrawing.kt`. The page is a single `Canvas`. Banner
lines are measured into `DrawnBanner` and painted in that canvas. A host
cannot replace either painter without forking.

Odd pages are right-hand pages: `isRightPage(page) = page % 2 == 1`. The wide
band and the bead rope sit on the outer edge (right on odd pages, left on even
pages). The page-number rosette sits on the bottom border at the outer corner
(`PAGE_MEDALLION_RADIUS = 16` dp). Digits are fitted to the inner disk with
`pageNumberScale` and drawn in a sans face, because the mushaf font ligates
any digit run into an ayah rosette.

`MushafColors.Light` / `.Dark` drive paper, ink, and the frame palette
(`MushafFrameColors.classic` / `.modern`). The page does not paint its own
paper; the parent does.

### 3.2 What the text already marks

`Ayah` carries `juz` (1..30) and nothing smaller. `hafs.tsv` already contains
the Madinah in-line signs, and the renderer draws them as ordinary tokens:

| Sign | Where | Count in `hafs.tsv` | What the frame still lacks |
|---|---|---|---|
| `۞` U+06DE | Its own token, after a space, at the start of a division ayah | **199** | A label that says which division it is. Every star looks the same. |
| `۩` U+06E9 | Stuck to the last word of a sajdah ayah | **15** | The margin mihrab the printed page adds beside that line. |

The star is omitted when the division falls on ayah 1 of a surah, because the
surah banner occupies that line. That is **41** of the 240 quarters (see
[§5.2](#52-the-240-quarters)). Those pages have a banner and no `۞`. The
margin medallion is the only division label there.

`layout.txt` word counts include the star as a token. Justification was
measured from the printed page with the star in the line. Any new mark must
be painted **outside** the token stream.

The sample app (`sample/.../QuranMetadata.kt`) has a 240-entry
`QuranQuartersMap` (`QuarterInfo`: index, hizb 1..60, part 0..3, surah, ayah),
keyed by page. It is not in the library. One entry disagrees with the text:
the map says **15:50**, and `hafs.tsv` puts `۞` on **15:49** (page 264). The
library table must follow the glyph.

### 3.3 Tests that a later change has to keep green

- `MushafFrameGeometryTest` — content rect inside the inner rule for all 604
  pages across widths and densities; `pageNumberScale` default `0.72`.
- `MushafDecorationRenderTest.textAndBannerNeverInvadeTheFrameBand` — pixels
  with r, g, and b all below 90 (body ink) must not appear in the frame band
  on pages 2, 76, and 604. Gold, cream, and the marker brown
  (`#6B4F3A`, r = 107) are not "ink". A black label in the band fails this
  test.
- `everyDecorationStyleIsApplied` — frame, classic, and modern each change
  the bitmap.
- `MushafLayoutTest` — 6236 ayahs, word-for-word layout. Do not touch
  `hafs.tsv` or `layout.txt` for this milestone.

## 4. Page-turn animation

### 4.1 Recommendation

Add `PageTurn` on `MushafPager`, default `Slide` (today's `HorizontalPager`).

`PageTurn.Curl` is a **full-height fore-edge peel** implemented in this
module: the next page is drawn underneath, the current page is clipped to the
part still covering it, a narrow strip of the sheet's back is drawn along the
peel, and a soft shadow falls on the revealed page. The peel runs the full
height of the page. Progress follows the finger. Releasing past the threshold
springs to the next page; releasing short springs back.

That is the turn to build. It is a leaf moving off the page underneath, which
is the physical gesture, and it stays inside minSdk 21 and the Compose-only
dependency set.

### 4.2 Approaches that were compared

| Approach | Verdict |
|---|---|
| `HorizontalPager` slide (current) | Keep as `PageTurn.Slide`. A slide moves a rigid rectangle; the sheet never lifts. |
| `graphicsLayer { rotationY }` with the pivot on the spine (the usual Compose cube/card sample) | A card rotating in space. The page does not bend, the fore-edge does not peel, and past 90° the composable mirrors. Do not ship this as the mushaf turn, and do not add it as a third mode in 1.6.0. |
| Crossfade | Does not read as paper. Out. |
| AGSL `RuntimeShader` mesh | API 33. The library is minSdk 21, so a shader cannot be the only path. |
| `drawVertices` cylindrical mesh (Harism-style) | Real foreshortening, more code, harder hit-testing. Leave it as a later replacement of the clip strip, behind the same `PageTurn.Curl` state. Do not block the peel on it. |
| Dependency on [`eu.wewox:pagecurl`](https://github.com/oleksandrbalan/pagecurl) | Same visual family (clip, back face, shadow, at most three children). Its author has confirmed it does not compose inside `HorizontalPager`, because the back face and the shadow have to paint over the neighbor. Taking the dependency also breaks the "Compose and nothing else" contract in the README. Reimplement the peel here. If a fragment of that Apache-2.0 file is copied, keep its notice; prefer an original implementation of the geometry below. |

### 4.3 Geometry

Pure functions, unit-tested without a device. Suggested home:
`MushafCurl.kt`.

```kotlin
enum class PageTurn { Slide, Curl }

/** progress 0 = current page fully covered; 1 = peeled away. */
data class CurlSlice(
    val boundaryX: Float,
    val backLeft: Float,
    val backRight: Float,
    val shadowLeft: Float,
    val shadowRight: Float,
)

fun curlSlice(
    widthPx: Float,
    progress: Float,          // 0..1
    forward: Boolean,         // true = toward the next (higher) page
    backWidthPx: Float,
    shadowWidthPx: Float,
): CurlSlice
```

Forward peel (higher page number), which matches the RTL pager — the next
page is to the left, and the reader lifts the **left** fore-edge toward the
right:

- `boundaryX = progress * width`.
- The current page is clipped to `x >= boundaryX` (the right-hand portion
  still lying flat).
- The next page is drawn full-bleed underneath, visible for `x < boundaryX`.
- The back of the sheet is the vertical strip
  `[boundaryX - backWidth, boundaryX]`, fill
  `lerp(colors.paper, Color.Black, 0.08f)`. About 18 dp.
- The shadow is a horizontal gradient on the revealed page, alpha about 0.22
  at the boundary, falling to 0 over about 24 dp, on the left of the
  boundary.

Backward peel mirrors this: the boundary travels from the right edge toward
the left, the previous page is revealed on the right, and the current page
stays clipped to the left of the boundary.

Progress 0 and progress 1 must show exactly one full page and no strip. A
test pins both ends so a rounding error cannot leave a sliver of the back
face on a settled page.

Draw order, bottom to top: revealed page, shadow, back strip, clipped
current page. Both pages are composed at full size **before** the clip.
Curl progress must not be a `remember` key of `MushafPage` or of
`buildPageDrawing`. The peel only clips layers that are already laid out.
Compose the current page and one neighbor; do not compose the other 602.

### 4.4 Gesture and programmatic turns

- A horizontal drag past touch slop, anywhere on the page, drives `progress`
  from `dragDelta / width`. This matches today's swipe. Vertical movement
  past slop cancels the gesture.
- A tap **inside** the text column stays an ayah tap or long-press. The curl
  container must not put a full-size pointer interceptor above `MushafPage`'s
  `detectTapGestures`.
- A tap in the frame margin, outside the content rect, on the left side
  animates forward; on the right side animates back. That is the margin tap
  of a bound book, and it does not compete with ayah hits.
- Release at `progress >= 0.35` springs to 1 with
  `spring(stiffness = Spring.StiffnessMedium)`. Otherwise it springs to 0.
- `PagerState.animateScrollToPage` / a reciter advancing the page plays the
  same peel. A new target during an in-flight peel **replaces** the target.
  Do not queue peels.

### 4.5 Keeping `PagerState`

Hosts, including a reader that follows recitation, already hold the
`PagerState` from `rememberMushafPagerState`. `PageTurn.Curl` must keep that
parameter.

`MushafPager` still accepts `state: PagerState`. In `Slide` mode nothing
changes. In `Curl` mode the pager owns an internal `Animatable` and syncs:

- When a peel settles on a page, `state.scrollToPage(index)` (no animation).
- `snapshotFlow { state.currentPage }` starting a change that did not come
  from the peel launches `animateTo` on the curl. A generation counter, or
  "ignore the echo of the write we just did", stops the loop.
- During a peel, `state.currentPageOffsetFraction` stays 0. The slide
  fraction is not the peel progress. Document that. Callers that need the
  fraction stay on `PageTurn.Slide`. A `MushafCurlState` with `progress` can
  be added in the same task if a host asks; it is not required for SaloAleh
  to opt in.

```kotlin
@Composable
fun MushafPager(
    // ...existing parameters, unchanged...
    pageTurn: PageTurn = PageTurn.Slide,
    state: PagerState = rememberMushafPagerState(),
)
```

### 4.6 Two-page spread (after the single-page peel works)

`MushafSpread`, default `Single`.

```kotlin
enum class MushafSpread { Single, Auto }
```

`Auto` uses a spread when `maxWidth >= 600.dp`. Odd page on the right, even
page on the left, which is `isRightPage` and an RTL row. Page 1 is a right
page with an empty left side (the inside cover). There are 302 spreads.

`state.currentPage` remains the 0-based index of the focused mushaf page (the
one recitation follows). The spread shows that page and its pair: page 50
shows 49 on the right and 50 on the left.

The leaf turn on a spread is the second curl task, not the first. The leaf's
front is the left (even) page, its back is the next odd page, and the page
revealed on the left is the next even page. Until that geometry is tested,
`Auto` may show a static spread and peel only the left page, and the task
must say so in the README rather than describe a full codex turn that is not
what shipped.

## 5. Hizb, juz, and rubʿ on the frame

### 5.1 What a printed page actually shows

Two layers exist on real pages. This library already draws the first.

1. **In the line (Madinah).** King Fahd Complex mushaf, Uthman Taha's hand:
   a star `۞` at the start of the ayah that begins a juz, a hizb, a half
   hizb, or a quarter hizb, and `۩` at the end of a sajdah ayah, with the
   sajdah phrase marked in the handwritten page. Sources: the Complex's own
   description of the mushaf (Arabic Wikipedia,
   [مصحف المدينة النبوية](https://ar.wikipedia.org/wiki/مصحف_المدينة_النبوية),
   citing *Ghayth al-nafʿ*, *Naẓimat al-zuhr*, *Taḥqīq al-bayān*, and
   *Irshād al-qurrāʾ*); Stylogeny's write-up of the printed page
   ([Madinah Mushaf](https://stylogeny.com/styles/madinah-mushaf/)); Unicode
   (U+06DE ARABIC START OF RUB EL HIZB, U+06E9 ARABIC PLACE OF SAJDAH).
   The star does not say *which* of the four divisions it is.
2. **In the margin.** The Turkish Diyanet mushaf puts a hizb rose on the
   edge and does not put a sign in the verse (Akdemir, *Yaygın İki Mushaftaki
   Bölümlemeler ve Mahiyeti*, Cumhuriyet İlahiyat Dergisi,
   [doi:10.18505/cuid.295252](https://doi.org/10.18505/cuid.295252)).
   Classical printed mushafs put an octagram in the margin labeled rubʿ,
   nisf, or three-quarters. The Madinah page also carries a running head:
   the surah name at the right of the top margin and the juz at the left,
   which `MushafPager`'s `PageHeader` already approximates **outside** the
   frame.

The work is layer 2, drawn on the frame, labeled, and aligned to the line of
the existing star. The Diyanet *numbering* (four hizbs per juz, numbered 1..4,
plus rukūʿ, and a few boundaries that differ) is **not** adopted. Every style
uses the Madinah 240. A Diyanet frame changes the ornament and the palette.
Marking a different ayah would describe a different book than the text on
screen.

### 5.2 The 240 quarters

Standard Madinah division, which the sample map already encodes:

- 30 juz. 26 of them start at the top of a page, 20 pages apart (pages 22,
  42, …, 582). Juz 1 starts at page 1.
- 60 hizb. Each juz is two hizb. Hizb `n` starts a juz when `n` is odd:
  juz = `(n + 1) / 2`.
- 240 quarters. `part` 0 is the start of the hizb, 1 is rubʿ, 2 is nisf,
  3 is three-quarters.

Checked against `hafs.tsv` while writing this plan:

- 240 sample entries, 199 stars.
- 41 quarters fall on ayah 1 of a surah and have no `۞`. That matches the
  printed omission.
- The sample's quarter index 106 says 15:50. The text has `۞` on **15:49**
  (page 264) and not on 15:50. One other star is unmapped, and it is that
  same 15:49. Moving index 106 to 15:49 makes the sets match: every star is
  a quarter, every quarter is either a star or a surah-opening ayah 1.

Ship the table in the library (`MushafDivisions.kt` or an asset), generated
or copied from the sample map **after** that one-ayah correction. Do not
read the sample class at runtime; the sample may keep its copy until a later
cleanup, and this milestone does not have to edit the sample.

```kotlin
enum class DivisionKind { JUZ, HIZB, RUB, NISF, THREE_QUARTERS, SAJDAH }

data class MushafMark(
    val kind: DivisionKind,
    val page: Int,
    val surah: Int,
    val ayah: Int,
    val hizb: Int,          // 1..60; 0 for a lone sajdah
    val juz: Int,           // 1..30
    val quarterIndex: Int,  // 1..240; 0 for sajdah
)

object MushafDivisions {
    /** 240 Madinah quarters. */
    val quarters: List<MushafMark>
    /** The 15 sajdah ayahs already carrying ۩. */
    val sajdat: List<MushafMark>
    fun marksOn(page: Int): List<MushafMark>
}
```

`kind` for a quarter:

| `part` | Hizb | `kind` | Label drawn in the medallion |
|---|---|---|---|
| 0 | odd | `JUZ` | جزء |
| 0 | even | `HIZB` | حزب |
| 1 | any | `RUB` | ربع |
| 2 | any | `NISF` | نصف |
| 3 | any | `THREE_QUARTERS` | ثلاثة |

One medallion per quarter. A juz start is also a hizb start; draw **جزء**
only. Sajdah is a second mark when it falls on the same page, at its own
line.

Sajdah ayahs, all present in `hafs.tsv` with `۩`, pages matching the Complex's
list of the five disputed positions and the standard fifteen:

7:206 (p. 176), 13:15 (251), 16:50 (272), 17:109 (293), 19:58 (309),
22:18 (334), 22:77 (341), 25:60 (365), 27:26 (379), 32:15 (416),
38:24 (454), 41:38 (480), 53:62 (528), 84:21 (589), 96:19 (597).

### 5.3 Where the medallion sits

The outer band is `BAND_OUTER` = 12 dp, which is too narrow for a word. Follow
the page-number rosette: center a medallion on the **outer rule**, so it
straddles the frame and the margin. `markAnchorX` is that x. `centerY` is the
vertical center of the line that contains the target token.

Target token:

- If the ayah text contains a `۞` token, the line of that token.
- Otherwise the first token of the ayah (the surah-opening case; the banner
  line is the one above it, and the mark aligns with the first verse line).
- Sajdah: the line of the token that contains `۩`.

The medallion reuses `drawRosette` at a smaller radius (about 11 dp). The
label is measured with the same sans face and the same ink-fitting as the
page number (`fittedPageNumberFontSize`), filling about 0.72 of the inner
disk. Color is `frameColors.frameLine` or `colors.marker`. It must not be
`colors.ink`. That keeps
`textAndBannerNeverInvadeTheFrameBand` meaningful: body ink stays out of the
band, and the ornament is gold or brown.

`ثلاثة` is the long label. Fit it the way a three-digit page number is fit.
If it cannot sit inside the disk at the floor size, draw `٣/٤` for that
style only and record the choice next to the harness shot. Do not shrink the
text column to make the word fit.

Two marks whose centers would overlap (a quarter and a sajdah on one line)
stack by one diameter toward the nearer page edge that still lies inside the
frame's vertical span.

### 5.4 Running head

Printed Madinah pages put the surah and the juz in the top margin, inside the
frame. `MushafPager` currently draws that **outside** `MushafPage`, and with
`pageFrame` on the page number is also in the bottom medallion, so a host
that leaves `showHeaders` on sees the page number twice. The unreleased notes
already tell hosts to hide their own footer.

When division chrome is on:

- Reserve a strip about 18 dp by increasing `contentTop` (and only then).
  Default geometry, with chrome off, stays byte-for-byte the current insets
  so `MushafFrameGeometryTest` does not move.
- Right side of the strip: `سورة ` + the Arabic name of the **first** ayah's
  surah. A surah that starts mid-page already has a banner; the running head
  stays the surah in progress at the top.
- Left side: `الجزء ` + Arabic-Indic digits of that ayah's `juz`. Digits in
  the sans face, `TextDirection.Rtl`, same rule as the header panels.
- `MushafPager` suppresses `PageHeader` and `PageFooter` when the decoration
  draws this chrome, even if `showHeaders` is true. `showHeaders` still
  controls the outer chrome when the frame chrome is off.

### 5.5 Flag

```kotlin
val divisionMarks: Boolean = false
```

Default **false**, so existing pixels, the band test, and every current
caller stay put. `MushafDecoration.Classic` and `.Modern` keep `false`.
A new preset turns it on:

```kotlin
val Madinah = Classic.copy(divisionMarks = true)
```

`divisionMarks` with `pageFrame = false` draws nothing in 1.6.0. The marks
belong to the frame. Document that. A host who wants marks without a frame
can still read `MushafDivisions.marksOn(page)` and draw them.

## 6. Style inventory

Every style below is a skin on the same 604 pages, the same font, and the
same `۞` / `۩` glyphs. The comparison is what a reader sees in the printed
edition, and what this library can take from it.

### 6.1 Editions compared

| Edition | Lines / pages | Frame | Header | Medallions | Margin | Numerals | Use here |
|---|---|---|---|---|---|---|---|
| **Madinah**, King Fahd Complex, Uthman Taha, Hafs, approved 1405 AH. Cream paper, black naskh, color in a floral frame, gold and teal. Surah cartouche with Makki/Madani and the ayah count. Verse numbers in round medallions (the font's rosette). `۞` in the line. Page number in a bottom ornament. Running head: surah and juz. Sources: [Stylogeny](https://stylogeny.com/styles/madinah-mushaf/) palette `#f6f1e2`, `#151413`, `#2a7f86`, `#bf9a4a`; [Arabic Wikipedia](https://ar.wikipedia.org/wiki/مصحف_المدينة_النبوية). | 15 lines, 604 pages. Pages 1–2 are the short opening pages. | Floral / geometric border. The library's current bead-rope frame is this family, ported from QuranyApp, not a scan. | Curved cartouche. Already `CLASSIC`. | Page number: 8-point rosette, already drawn. Division: in-line star, already in the text. | Outer band wider than the inner. Already `isRightPage`. | Arabic-Indic `٠١٢٣٤٥٦٧٨٩` in the sans labels. Ayah numbers are the font's rosette. | Default. Chrome from §5 completes it. New preset `Madinah`. |
| **Cairo / Amiri family.** King Fuʾād 1924 Amiri is 12 lines and about 827 pages ([Quran Archive](https://qrnr.org/egypt-kfe/)). Later Egyptian reading mushafs and the Complex's Shamarly-layout edition are 15 lines and about 522 pages ([Assabile](https://www.assabile.com/telecharger-le-coran-pdf/medine-shamarly)). | Not our pagination. | A quiet double rule, generous margin, little or no bead rope. | A rectangular title, traditionally in red, with the revelation and the count in the same bar. | Small circle for the page number under the rule. Small circle in the margin for the division, rather than a full rosette. | Wide, mostly empty. | Arabic-Indic. | Skin `Cairo`. Say in the API docs that the page breaks stay Madinah. |
| **Diyanet** (Turkish Presidency). Hafs, 15-line, verse-aligned, close to Madinah in juz length, different spelling, Fatiha often unnumbered so page numbers drift by one, juz 30 differs ([Korunmuş Kitap](https://korunmuskitap.blogspot.com/2024/07/medine.html); [Ilmify](https://ilmify.app/blog/mushaf-editions-hifz-teacher-guide/)). Hizb rose on the edge; four hizbs per juz in their own numbering; rukūʿ marks ([Akdemir](https://doi.org/10.18505/cuid.295252)). | Skin only. Do not move marks to Diyanet's boundaries. | Green border, floral corners. | Oval or round cartouche. | Edge rose for the hizb. Page number toward the bottom center. | The rose lives on the edge. | Arabic-Indic. | Skin `Diyanet`. Ornament and green/gold palette. Same 240 marks. |
| **Indo-Pak (Taj, Lahore, and the Complex's one Taj-based edition).** 16 lines, about 548 pages, verses split across pages, Indo-Pak naskh, each line underlined ([Tarteel QUL](https://qul.tarteel.ai/resources/mushaf-layout/11); [Ilmify](https://ilmify.app/blog/mushaf-editions-hifz-teacher-guide/); Stylogeny on the Complex's Taj edition). South Asian prints call the juz a para (پارہ). | Skin only. Font stays KFGQPC. | A flat colored band (red or green), not a bead rope. | Rectangular panel, surah name, a small circle with the surah number. | Ayah marks in those editions are not our rosette. Keep the font's rosette. | Underline under every verse line. | Eastern Arabic-Indic digits `۰۱۲۳۴۵۶۷۸۹` (U+06F0) for **labels**. Never feed U+06F0 to the mushaf font; it ligates Arabic-Indic digits into rosettes and is not a general digit font. | Skin `IndoPak`. |
| **Maghrebi / Moroccan (Warsh, Mohammadi).** Different rasm, Maghrebi script, saffron and red, round verse markers. | Out. A skin would be mistaken for Warsh. | — | — | — | — | — | Do not add. |
| **Tajweed print (Dar al-Maʿrifah).** Madinah pages, letters colored by rule. | Already `tajweed = true`. | Whatever frame the host set. | Unchanged. | Unchanged. | Unchanged. | Unchanged. | No new type. |

### 6.2 Styles to add

Keep the current drawer as `PageFrames.Ornate`. Add three frames and three
headers. Headers are not new `SurahHeaderStyle` enum values: adding an enum
constant breaks exhaustive `when` in host apps. See [§7](#7-customization-api).

**`PageFrames.Cairo`**

- Double rule, about 1.2 dp and 0.6 dp, color a dark brown close to ink but
  lighter than the band-test threshold (`#5C4030` fails the `< 90` test if
  the stroke is thick; use `#6B4F3A` / the existing marker, or a red that
  stays above the threshold). No diamonds, no bead rope.
- Content insets still come from one geometry object, and the content rect
  stays inside the inner rule. Outer margin wider than Ornate (about 16 dp)
  so the page looks like the quieter Cairo margin.
- Page number centered on the bottom rule, in a plain circle, sans
  Arabic-Indic digits, same fitting helper.
- Division mark: a circle on the outer rule, same labels as §5.
- Running head as §5.4.

**`SurahHeaders.Cairo`** (replaces the cartouche when selected)

- A rectangle the width of the text column, fill a deep red (`#8C2F2F`),
  text cream. Name in the mushaf font. Revelation and ayah count in sans, at
  the two ends, using the existing `revelationLayout` / `countLayout`
  measurements. Square corners with a small radius (2 dp), not the classic
  curve.

**`PageFrames.Diyanet`**

- Single green rule (`#0E6B4F` on light paper, lifted on `MushafColors.Dark`
  the way `MushafFrameColors` already lifts gold). Corner pieces are four
  small path florets: a circle and two leaves, drawn, not a bitmap.
- Division mark uses the same rosette as Ornate (the "rose on the edge"),
  centered on the outer rule, same labels. The in-line `۞` stays, because it
  is in the text.
- Page number centered at the bottom in a small circle.
- Header: `SurahHeaders.Diyanet`, an oval cartouche (a round rect whose
  corner radius is half the banner height), green fill, cream surah name,
  side panels for count and revelation as today.

**`PageFrames.IndoPak`**

- Flat band, red `#8E2F2F` on light paper, about 8 dp, no rope.
- After the verse lines are placed, draw a hairline under each
  `DrawnVerseLine` in `#8E2F2F` at about 30% alpha, inset to the text column.
  The underline is part of this frame's `draw`, so it needs the line tops.
  Pass `List<LineSpan>` (top, bottom, left, right) on `PageFrameContext`.
  Other frames ignore it.
- Running head uses `پارہ ` + U+06F0 digits of the juz. Surah name stays
  Arabic, mushaf font.
- Header: `SurahHeaders.IndoPak`, a rectangle with a small circle holding the
  surah number (sans, U+06F0) and the name beside it. Revelation and count
  stay available in the side measurements; put them under the name in sans
  if the banner height allows, otherwise omit them rather than overflow.

**Dark paper.** Each new palette branches on `colors.paper.luminance() < 0.5f`
the way `MushafFrameColors.classic` does. No separate "night" styles.

**Presets** (all source-compatible additions):

```kotlin
val Madinah = Classic.copy(divisionMarks = true)
val Cairo = MushafDecoration(
    pageFrame = true,
    frame = PageFrames.Cairo,
    surahHeader = SurahHeaders.Cairo,
    divisionMarks = true,
    ayahFrame = true,
)
val Diyanet = MushafDecoration(
    pageFrame = true,
    frame = PageFrames.Diyanet,
    surahHeader = SurahHeaders.Diyanet,
    divisionMarks = true,
    ayahFrame = true,
)
val IndoPak = MushafDecoration(
    pageFrame = true,
    frame = PageFrames.IndoPak,
    surahHeader = SurahHeaders.IndoPak,
    divisionMarks = true,
    ayahFrame = true,
)
```

`frame` and `surahHeader` default so that `Classic.copy(...)` without them
still draws Ornate + `CLASSIC`.

## 7. Customization API

Extend `MushafDecoration`. Do not replace `pageFrame` or `surahHeaderStyle`.

### 7.1 Frame

Promote a public geometry type. The internal `MushafFrameGeometry` can become
this type, or a thin wrapper. One source of truth for the content rect, which
is the invariant 1.5.0 fixed.

```kotlin
data class PageFrameRequest(
    val widthPx: Float,
    val heightPx: Float,
    val pageNumber: Int,
    val density: Float,
    val outerMargin: Float,
    val innerMargin: Float,
    val topMargin: Float,
    val bottomMargin: Float,
    val textGap: Float,
    val divisionMarks: Boolean,
)

data class PageFrameGeometry(
    val unit: Float,
    val rightPage: Boolean,
    val frameLeft: Float,
    val frameTop: Float,
    val frameRight: Float,
    val frameBottom: Float,
    val contentLeft: Float,
    val contentTop: Float,
    val contentWidth: Float,
    val contentHeight: Float,
    val markAnchorX: Float,
)

data class MarkAnchor(val mark: MushafMark, val centerY: Float)

data class PageFrameContext(
    val colors: MushafColors,
    val frameColors: MushafFrameColors,
    val pageNumber: Int,
    val pageNumberLabel: FittedLabel?,
    val marks: List<MarkAnchor>,
    val runningSurah: String?,
    val runningJuzLabel: String?,
    val lineSpans: List<LineSpan>,
)

interface PageFrame {
    fun geometry(request: PageFrameRequest): PageFrameGeometry
    fun DrawScope.draw(geometry: PageFrameGeometry, context: PageFrameContext)
}

object PageFrames {
    val Ornate: PageFrame   // today's ornateFrameGeometry + drawOrnatePageFrame
    val Cairo: PageFrame
    val Diyanet: PageFrame
    val IndoPak: PageFrame
}
```

`FittedLabel` is the public form of today's internal `PageNumberLabel`
(layout plus ink center). Header code and page numbers share it.

Contract for every `PageFrame`, including a host's:

- `contentLeft >= frameLeft`, `contentTop >= frameTop`, and the content
  rect ends inside `frameRight` / `frameBottom`.
- `contentWidth` and `contentHeight` are at least 1 px.
- `geometry` is a pure function of `PageFrameRequest`.
- `draw` paints only decoration. It does not draw ayah text.
- `MushafPage` lays text out in the content rect and translates the canvas
  by `contentLeft`, `contentTop`, exactly as it does now for the ornate
  frame.

```kotlin
data class MushafDecoration(
    // ...existing fields, same defaults...
    val frame: PageFrame = PageFrames.Ornate,
    val divisionMarks: Boolean = false,
    val surahHeader: SurahHeader? = null,
)
```

`pageFrame == false` means no frame, content rect = the full canvas, `frame`
ignored. `pageFrame == true` draws `frame`. Today's `pageFrame = true` with
no `frame` argument still selects `PageFrames.Ornate`.

### 7.2 Surah header

`surahHeader == null` means `surahHeaderStyle` paints in the canvas, as now.

```kotlin
sealed interface SurahHeader {
    data object Cairo : SurahHeader
    data object Diyanet : SurahHeader
    data object IndoPak : SurahHeader
    class Custom(
        val content: @Composable SurahHeaderScope.() -> Unit,
    ) : SurahHeader
}

class SurahHeaderScope internal constructor(
    val surah: Surah,
    val ayahCount: Int,
    val revelationType: RevelationType,
    val colors: MushafColors,
    val frameColors: MushafFrameColors,
)
```

`Custom` is a real composable, because a host UI is buttons, images, and
Compose layout, which a `DrawScope` callback is not.

Placement: `MushafPageBody`'s root becomes a `Box`. The canvas skips banner
painting when `surahHeader != null`. For each `DrawnBanner`, a child is
placed at `(contentLeft, contentTop + boxTop)` with size
`(width, boxHeight)` in **pixels**, `clipToBounds()`, and the composable is
invoked with a `SurahHeaderScope` built from that banner's surah, count, and
revelation (overrides already applied in `buildPageDrawing`). The banner box
is the line reserved for the header; clipping stops a tall composable from
covering the next line. Taps on the header hit the child, not `onAyahClick`.

Built-in `Cairo` / `Diyanet` / `IndoPak` may be implemented as canvas drawers
selected by the sealed type, or as composables. Canvas is the better fit for
the geometric ones (they need the same fitted text the classic header uses).
`Custom` is the only one that has to be a composable overlay. Either way the
public type stays the sealed interface above.

`surahHeaderStyle` still selects `MushafFrameColors` when the header is
`Custom` or null. A `Custom` header that wants the modern teal reads
`frameColors` from the scope; the host can also pass `surahHeaderStyle =
MODERN` to get that palette while drawing its own banner.

### 7.3 What a host writes

SaloAleh, or any other app, after 1.6.0, without forking:

```kotlin
implementation("com.github.MahmoudMabrok:mushaf-text-customizable:1.6.0")

class SaloAlehFrame : PageFrame {
    override fun geometry(request: PageFrameRequest): PageFrameGeometry =
        PageFrames.Ornate.geometry(request) // same insets, own paint

    override fun DrawScope.draw(geometry: PageFrameGeometry, context: PageFrameContext) {
        // rules, a bitmap the app owns, context.marks at markAnchorX / centerY
    }
}

@Composable
fun SaloAlehReader(page: Int, pagerState: PagerState) {
    MushafPager(
        state = pagerState,
        pageTurn = PageTurn.Curl,
        colors = MushafColors.Light,
        decoration = MushafDecoration.Classic.copy(
            divisionMarks = true,
            frame = SaloAlehFrame(),
            surahHeader = SurahHeader.Custom {
                SaloAlehSurahBanner(
                    name = surah.nameArabic,
                    ayahCount = ayahCount,
                    revelation = revelationType.labelArabic,
                )
            },
        ),
    )
}
```

A host that only wants a banner and the stock frame omits `frame`. A host
that wants a stock edition uses `MushafDecoration.Diyanet` (or `.Cairo`,
`.IndoPak`, `.Madinah`) and omits both. A host that stays on 1.5.0 source
with `MushafDecoration(pageFrame = true, surahHeaderStyle = CLASSIC)`
compiles unchanged and looks unchanged.

`PageFrame` is an interface with a default parameter on the data class, so
Kotlin call sites that name parameters keep compiling. Binary compatibility
of a new constructor parameter is the usual Kotlin data-class break; this
repo already accepted that for `surahHeaderOverrides` and `pageNumberScale`
and treats source compatibility as the bar. Say so in the 1.6.0 notes.

## 8. Compatibility and migration

| Caller code | After 1.6.0 |
|---|---|
| `MushafDecoration()`, `.None`, `.Classic`, `.Modern` | Same pixels, provided `divisionMarks` defaults false and `Classic` is not redefined. |
| `pageFrame = true` | `PageFrames.Ornate`. |
| `surahHeaderStyle = CLASSIC / MODERN / PLAIN` | Same drawers, when `surahHeader` is null. |
| `MushafPager()` with a `PagerState` | `PageTurn.Slide`. `currentPage` stays 0-based. |
| `showHeaders = true` and `divisionMarks = false` | Outer surah / juz / page-number chrome, as now. |
| `showHeaders = true` and `divisionMarks = true` with a frame | Outer chrome suppressed. The frame's running head and medallion own those labels. |
| Custom `Surah(...)` | Unchanged. `revelationType` stays required, as in 1.5.0. |
| Flutter | Untouched. |
| `version` in `mushaf/build.gradle.kts` | Stays `1.5.0` until the release PR. This plan does not edit it. |

Migration notes for the 1.6.0 README, written when the code lands:

- Opt in to the peel with `pageTurn = PageTurn.Curl`.
- Opt in to margin marks with `divisionMarks = true` or
  `MushafDecoration.Madinah`.
- Hide any footer the host draws under a framed page. The medallion is the
  page number. (Already true on `main` since PR #3.)
- `state.currentPageOffsetFraction` is not the peel progress.
- IndoPak, Diyanet, and Cairo presets are looks. The page breaks, the font,
  and the star positions stay Madinah. Say that next to the preset, in both
  README languages.

## 9. Risks

| Risk | What to do |
|---|---|
| Body ink in the frame band | Labels use `frameLine` or `marker`. Re-run `textAndBannerNeverInvadeTheFrameBand` on pages 2, 76, 604 with `divisionMarks = true` and assert the same zero. If a label fails, lighten the color; do not weaken the test to ignore the whole band. |
| Star and medallion disagree | The unit test in task T2 is the gate. A quarter with no star and `ayah != 1` fails the build. 15:49 is the known fix. |
| Custom frame returns a content rect over the ornament | `geometry` contract test, plus a test frame with a fixed inset that must move the longest line. Reject rects that invert. |
| Peel remeasures text every frame | `buildPageDrawing` keys must not include progress. A test can count invocations across an animation clock if Robolectric makes that cheap; otherwise a code-level assertion in review and a comment on the `remember` key. |
| Peel steals ayah taps | Tap-inside-content test: a tap on a known word still calls `onAyahClick` when `pageTurn` is `Curl`. Drag test: a horizontal drag settles on the next page. |
| `PagerState` sync loop | Generation counter. Test: external `scrollToPage(10)` results in one peel, and the settled page is 10. |
| Reciter jumps mid-peel | Replace the target. Test with two `scrollToPage` calls one frame apart. |
| Spread and page 604 | 604 is even, so the last spread is full. Page 1's left side is empty. Both need a harness shot. |
| Data-class constructor and Java callers | Source-compatible for Kotlin defaults. Note the binary break in the changelog, same as 1.5.0. |
| Hosts `when` on `SurahHeaderStyle` | Do not add enum values. New headers live on `SurahHeader`. |
| Looking like a mushaf the text is not | Preset KDoc and the README sentence in §8. No Warsh skin. |
| Font licence | Ornaments are paths. The font file is not edited. U+06F0 is only passed to the sans face. |
| `divisionMarks` on the narrow outer band | Medallion straddles the outer rule, like the page number. It does not grow the text gap. |
| Sample `QuranQuartersMap` drifts from the library | Library owns the table. Optional follow-up: sample calls `MushafDivisions`. Not required to close 1.6.0. |
| Curl on API 21 software rendering | The peel is clips and a gradient, which Skia does on API 21. Avoid `RenderEffect`. |

## 10. Documentation and sample when the code lands

This plan is the design. Implementation PRs update:

- `README.md` in both languages: one subsection under decoration for the new
  presets, the curl parameter, and the custom header / frame snippets. Keep
  the bilingual split the README already uses.
- `CHANGELOG.md` under the version that ships, with the source-compatible
  additions and the `PagerState` fraction note.
- `MushafDecorationHarness` shots for pages 5 (rubʿ, 2:26), 22 (juz 2),
  77 (surah opening, no star), 176 (sajdah), 264 (15:49), 604, for Ornate
  with marks, Cairo, Diyanet, and IndoPak, light and dark.
- The sample app: a small control that switches preset and `PageTurn`. The
  sample is the DailySeventy screen; keep the control off the default path
  so the sample still opens on the current reader. SaloAleh is not in this
  repo and is not modified.

## 11. Sequenced tasks

Each task is a later pull request. Acceptance is what that PR must show
before it merges. Defaults stay visually fixed through T1; pixels change
only where a task says they do.

### T1 — Public frame and header slots, no visual change

Depends on: nothing.

Files: `MushafDecoration.kt`, `MushafFrameDrawing.kt`, `MushafPage.kt`,
`MushafDecorationTest.kt`.

- Add `PageFrame`, `PageFrameGeometry`, `PageFrameRequest`,
  `PageFrameContext`, `FittedLabel`, `PageFrames.Ornate`.
- Move today's geometry and `drawOrnatePageFrame` behind `PageFrames.Ornate`.
- Add `MushafDecoration.frame`, default `PageFrames.Ornate`.
- Add `SurahHeader` and `surahHeader: SurahHeader? = null`. Wire `Custom` as
  an overlay. `null` keeps the canvas drawers.
- `divisionMarks` exists and defaults false. It draws nothing in this task
  if that keeps the diff smaller; T3 turns it on. Prefer shipping the
  parameter in T1 so later tasks are not constructor churn.

Acceptance:

- `./gradlew :mushaf:testDebugUnitTest` is green.
- A test constructs `MushafDecoration(pageFrame = true)` and checks
  `frame === PageFrames.Ornate` (or equality, if it is not an object).
- A test frame that insets the content by a known extra 40 px produces a
  smaller measured line than `PageFrames.Ornate` on the same canvas. That
  proves the layout reads `geometry`, not the ornate constants.
- A `SurahHeader.Custom` that draws a solid `Color.Red` rectangle changes
  the banner-line pixels and does not change a verse-line pixel on a page
  whose banner is on line 1 (page 2).
- Classic, Modern, and Plain screenshots through the harness match the
  pre-change bitmaps on pages 1, 2, and 604. If the harness is not a
  byte-compare today, the pixel-diff tests in `MushafDecorationTest` stay
  green without edited thresholds.

### T2 — Division corpus

Depends on: nothing. Can land in parallel with T1.

Files: new `MushafDivisions.kt`, new `MushafDivisionsTest.kt`. Data in source
or in `assets/mushaf/divisions.txt`. Source is easier to test and is 240
small rows.

Acceptance:

- `quarters.size == 240`, hizb in 1..60, every juz 1..30 occurs, quarter
  indexes are 1..240.
- Every ayah whose text starts with `۞` is exactly one quarter, and that
  quarter's surah and ayah match. The 15:49 case is asserted by name.
- Every quarter whose ayah number is not 1 has a star. Every quarter whose
  ayah number is 1 has no star.
- `sajdat.size == 15` and each text contains `۩`.
- `marksOn(page)` is empty for a page with neither, and contains the juz
  mark on page 22 (2:142).

### T3 — Margin marks and running head on Ornate

Depends on: T1, T2.

- `divisionMarks = true` draws the medallion and the running head inside
  `PageFrames.Ornate`.
- Geometry grows `contentTop` only when the flag is on.
- `MushafPager` drops the outer header and footer when the flag is on and
  `pageFrame` is true.
- Preset `MushafDecoration.Madinah`.

Acceptance:

- Flag false: geometry values equal the current `ornateFrameGeometry` for a
  fixed canvas (page 2, 1080×1920, density 2.625). The existing geometry
  test still passes.
- Flag true, page 5: a mark of kind `RUB` whose `centerY` lies inside the
  line band of the `۞` token of 2:26, and whose x is outside the content
  rect.
- Page 22: kind `JUZ`. Page 77 (4:1, surah opening): kind `NISF` or whatever
  T2 assigned, aligned to the first verse line, not the banner line.
- Page 176: a `SAJDAH` anchor on the line that contains `۩`.
- Page 264: the mark is 15:49, kind `RUB`.
- Band test still zero ink with the flag on, pages 2, 76, 604.
- Harness PNGs written for the pages above.
- `showHeaders = true` with `Madinah` does not draw a second page number
  under the frame. Assert by checking the footer slot is not composed, or
  by a pixel region under the frame that stays paper.

### T4 — Cairo, Diyanet, IndoPak

Depends on: T1, T3 (marks and running-head context exist so the new frames
can paint them).

One PR is fine if each frame is a single file object. Split if review is
hard.

Acceptance, per frame:

- Content rect inside that frame's own inner rule, all 604 pages, the same
  size grid as `MushafFrameGeometryTest`.
- Pixel diff against `PageFrames.Ornate` on page 2 is greater than 500, so
  the style is actually different.
- Cairo has no bead-rope pixels: a probe along the outer band finds no
  filled circles of the rope's radius. (Sample a column; do not depend on a
  golden bitmap.)
- IndoPak draws a horizontal line under verse line 1 of page 3 and does not
  draw it in the banner slot.
- IndoPak juz label uses U+06F0. A unit test on the label string is enough.
- Diyanet and Cairo page numbers are horizontally centered, unlike Ornate's
  outer-corner rosette. Assert `mark` x of the page number is within 1 dp
  of the frame's mid-x. Expose the page-number anchor on the context or
  return it from a test-visible function.
- Dark `MushafColors.Dark` renders without a crash and without body ink in
  the band.
- README preset section drafted in the same PR, both languages, including
  the "page breaks stay Madinah" sentence.

### T5 — Single-page curl

Depends on: T1. Independent of T3 and T4. The peel clips whatever
`MushafPage` draws.

Files: `MushafCurl.kt`, `MushafPager.kt`, `MushafCurlTest.kt`.

Acceptance:

- `curlSlice` at 0 and 1 has a zero-width back strip and a boundary on the
  correct edge, for both directions.
- At 0.5 forward, the boundary is mid-width and the back strip is on the
  left of it.
- `PageTurn.Slide` still uses `HorizontalPager`. A grep or a direct
  assertion that the curl composable is not in the slide tree.
- Compose test: start at page 1, drag left-to-right past the threshold
  (RTL forward — confirm the drag sign against `LayoutDirection` in the
  test and document it), settle on page 2. `state.currentPage == 1`.
- Compose test: a tap on a word on page 2 still invokes `onAyahClick` with
  `pageTurn = Curl`.
- Compose test: `state.scrollToPage` from 0 to 9 while a peel is running
  ends on page 10, and `build` count of the page content for unrelated
  pages stays 0 (only current and neighbor composed).
- `./gradlew :mushaf:testDebugUnitTest` green.
- README: the curl snippet and the sentence that `currentPageOffsetFraction`
  is not peel progress.

### T6 — Spread

Depends on: T5.

Acceptance:

- Width 400 dp: one page.
- Width 800 dp, `MushafSpread.Auto`: page 1 on the right, empty on the left;
  pages 49 and 50 shown together when the state is on page 50; pages 603
  and 604 together.
- `state.currentPage` still addresses a single mushaf page.
- If the full leaf-turn (front / back / revealed left page) is not done in
  this PR, the spread peels the left page only, and the README says that in
  one sentence. Do not describe a codex turn that the code does not do.

### T7 — Sample control and harness gallery

Depends on: T4, T5.

- Sample: a way to switch preset and `PageTurn`, off by default (a menu
  item or an existing sheet). The opening screen of the sample stays the
  current reader.
- Harness task documented in the module, with the page list from §10.
- This task does not change SaloAleh and does not publish a tag.

### Suggested order

T2 can run beside T1. After both merge: T3, then T4. T5 can start as soon
as T1 merges and can ship before T4. T6 after T5. T7 last.

A host can adopt the API after T1 (custom frame and header, slide turn),
margin marks after T3, stock styles after T4, and the peel after T5.

## 12. Sources

- King Fahd Complex mushaf: [مصحف المدينة النبوية](https://ar.wikipedia.org/wiki/مصحف_المدينة_النبوية) (approval 1 Jumada I 1405, 15 lines, the books used for juz and hizb, the five disputed sajdah pages). The Complex's font and reading notes: [qurancomplex.gov.sa](https://qurancomplex.gov.sa/en/) and the computer-publishing font page [nashr.qurancomplex.gov.sa](https://nashr.qurancomplex.gov.sa/en/).
- Printed-page structure (cartouche, in-line star, mihrab sajdah, floral frame, palette): [Stylogeny, Madinah Mushaf](https://stylogeny.com/styles/madinah-mushaf/), which cites Bruckmayr, *MIDÉO* 39 (2024), and the Quran.com Madani layout.
- In-line signs: Unicode U+06DE and U+06E9, summarized in [قائمة علامات ضبط المصحف](https://areq.net/m/%D9%82%D8%A7%D8%A6%D9%85%D8%A9_%D8%B9%D9%84%D8%A7%D9%85%D8%A7%D8%AA_%D8%B6%D8%A8%D8%B7_%D8%A7%D9%84%D9%85%D8%B5%D8%AD%D9%81.html).
- Madinah versus Diyanet divisions, the edge rose, and rukūʿ: Akdemir, [doi:10.18505/cuid.295252](https://doi.org/10.18505/cuid.295252).
- Diyanet page-number drift and shared Hafs reading: [Korunmuş Kitap](https://korunmuskitap.blogspot.com/2024/07/medine.html).
- Edition overview (Madinah 15-line, Taj 16-line, Diyanet): [Ilmify](https://ilmify.app/blog/mushaf-editions-hifz-teacher-guide/).
- 1924 Amiri, 12 lines, ~827 pages: [Quran Archive, King Fuʾād](https://qrnr.org/egypt-kfe/). Shamarly layout, 522 pages: [Assabile](https://www.assabile.com/telecharger-le-coran-pdf/medine-shamarly).
- Indo-Pak 16-line, 548 pages: [Tarteel QUL](https://qul.tarteel.ai/resources/mushaf-layout/11).
- Compose paging baseline: [HorizontalPager](https://developer.android.com/develop/ui/compose/quick-guides/content/display-paging-list). Card-flip `rotationY` samples (rejected above): [Pager animations](https://www.sinasamaki.com/pager-animations/). Peel prior art that does not fit inside `HorizontalPager`: [oleksandrbalan/pagecurl](https://github.com/oleksandrbalan/pagecurl) and [issue #15](https://github.com/oleksandrbalan/pagecurl/issues/15).
