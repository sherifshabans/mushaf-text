# Release notes

## Unreleased — page margins, header panels, page number

- **Reader milestone implemented** (the work specified in
  [`docs/roadmap/next-mushaf-reader.md`](docs/roadmap/next-mushaf-reader.md)):
  - **Public frame/header slots:** `PageFrame`, `PageFrameRequest`,
    `PageFrameGeometry`, `PageFrameContext`, `FittedLabel`, `LineSpan`, and
    `SurahHeader` / `SurahHeaderScope`. `MushafDecoration` gains `frame`
    (default `PageFrames.Ornate`), `divisionMarks` (default `false`), and
    `surahHeader` (default `null`) — source-compatible defaults that keep the
    1.5.0 pixels. The data-class constructor carries the usual Kotlin binary
    story, same as `surahHeaderOverrides` / `pageNumberScale`.
  - **Division corpus:** `MushafDivisions` ships the 240 Madinah quarters and
    the 15 sajdah positions; quarter 106 is **15:49**, following the in-line
    `۞` in `hafs.tsv`, not the sample map's 15:50.
  - **Division chrome:** `divisionMarks = true` draws the margin medallions
    (juz / hizb / rubʿ / nisf / three-quarters / sajdah, aligned to the `۞` /
    `۩` line) and the printed running head inside the frame.
    `MushafPager` then suppresses its outer header/footer even when
    `showHeaders` is true. Preset `MushafDecoration.Madinah`.
  - **Printed looks:** `PageFrames.Cairo` / `.Diyanet` / `.IndoPak` with matching
    `SurahHeader`s and the `MushafDecoration.Cairo` / `.Diyanet` / `.IndoPak`
    presets — **skins of the same Madinah pages**: page breaks, font, and star
    positions do not change.
  - **Leaf turn:** `MushafPager(pageTurn = PageTurn.Curl)` peels one leaf
    (fore-edge, back strip, shadow). `state.currentPageOffsetFraction` stays
    `0` during a peel — it is **not** the peel progress; callers that need the
    fraction stay on `PageTurn.Slide`. Taps in the text column stay ayah taps.
  - **Spread:** `MushafSpread.Auto` shows the page and its pair side by side on
    screens at least 600 dp wide (odd page right, even pair left, page 1 shows
    the inside cover). The pair is static for now — a leaf turn on a spread is
    a later task.
  - **Sample:** an off-by-default preview control for the presets, `PageTurn`,
    and the spread. The opening screen stays the current reader.
- **Page number in the frame medallion:** was a hardcoded `0.34 ×` the ayah font
  (about **6–9 sp** on a phone), so the digits sat small inside the rosette.
  The number is now fitted to the ornament. `MushafDecoration.pageNumberScale`
  (default **0.72**, also `DEFAULT_PAGE_NUMBER_SCALE`) is the fraction of the
  inner disk the digit ink fills. With Android's Arabic sans digits that is
  about **23 sp** for a one-digit page and about **13 sp** for a three-digit
  page — clearly larger, still inside the ring. Ayah text, surah headers, and
  the frame geometry are unchanged. `1` fills the disk; values are clamped to
  `0.05..1`.
- **Page number:** host apps that drew their own footer under `MushafPage` while
  `pageFrame` was on saw the number twice (footer + frame medallion). The medallion
  remains the single source when the frame is on; hosts should hide any extra footer.
- **Margins (configurable on `MushafDecoration`):**
  - `textGap` (default **8**, was 5) — more room between ayat and the ornate band.
  - `frameOuterMargin` / `frameInnerMargin` / `frameTopMargin` / `frameBottomMargin`
    (defaults **5 / 2 / 1.5 / 14**, were 9 / 3 / 2 / 19) — frame sits closer to the
    page edge.
- **Surah header panels:** drop the «آياتها» caption; left panel is just
  `ayahCountLabel` (e.g. `٧ آيات`). Right panel is one larger line
  «مكية ١» / «مدنية ٢». Panel text size raised (~0.55× page font).

## 1.5.0 — decoration, stable

`MushafDecoration` now actually owns the page it decorates. Previously the frame was drawn *over*
a layout measured against the full canvas, so every line ran past the inner rule and under the
rope of beads, and the surah banner spilled off both edges.

### Page frame

- **The frame geometry is the single source of truth.** `MushafFrameGeometry` produces both the
  frame rect and the text area; the renderer draws from it and the layout is inset by `content*`.
  Lines can no longer cross the frame, and callers no longer need to add padding themselves.
  (The old hidden 28 dp `framePad` is gone — it double-padded and shrank the type for nothing.)
- `pageFrame` palette follows `MushafColors`: `MushafColors.Dark` gets a lifted gold and a dark
  field instead of a cream frame on a black page.
- The page-number medallion is now the printed eight-point rosette and carries the page number
  (it was an empty circle).
- Hit-testing follows the content origin, so taps land on the right word when a frame is on.

### Surah header

- `CLASSIC` is a full port of `SuraHeaderSpan.drawClassic`: curved cartouche, inner double line,
  ornamental band with diamonds, and two side panels joined to the cartouche by short rules.
- The **left panel** shows the ayah count with its Arabic noun — `٢٨٦ آية`, `٧ آيات`, `آيتان`
  (new public helper: `ayahCountLabel(count)`).
- Name, count and order are measured and fitted to their boxes, so nothing overflows the
  cartouche or the panels on any page size. Long names shrink instead of spilling.
- Header numbers are drawn in a plain sans face with `TextDirection.Rtl`. The KFGQPC font folds
  any digit run into an ayah rosette, so `٢٨٦` used to come out as two rosettes and the page
  number as one.
- `MODERN` and `PLAIN` follow the same width and colour rules.
- **Revelation type** (`RevelationType`, from QuranyApp's `quran.json`): the right panel shows
  مكية / مدنية with the surah number beneath it, and `MushafDecoration.surahHeaderOverrides`
  can force a revelation type, ayah count or name per surah.
- The surah number left the name cartouche: the cartouche carries the name alone, and the number
  sits in the right panel, the slot the printed header uses for the revelation type.

### Ayah frame

- `MushafDecoration.ayahFrame` was a no-op; it now draws a padded, rounded frame around
  highlighted, selected and playing ayahs. A multi-line ayah joins into one block (interior
  corners are square).

### Tests and docs

- `MushafFrameGeometryTest` — the content rect stays inside the inner rule for all 604 pages
  across several widths and densities; the Arabic ayah-count label pluralises.
- `MushafDecorationRenderTest` — pixel scan: no ink may appear in the frame band on pages with a
  top banner, a bottom banner, or three banners; `ayahFrame` must change the output.
- `MushafDecorationHarness` — renders every decoration combination to
  `mushaf/build/decoration-shots/` with no emulator.
- README rewritten as two complete versions (English and Arabic), with fresh screenshots for
  Classic, Modern, Plain, the ayah frame and night mode.

### Breaking changes

- `Surah` gained a required `revelationType` field, so the 114-entry table now carries
  مكية/مدنية. Code that only reads `Quran.surahs` is unaffected; code that constructs a `Surah`
  must pass it.
- `MushafDecoration` gained `surahHeaderOverrides` (defaults to empty, so existing call sites
  compile unchanged), and the public helpers `ayahCountLabel(count)` and `RevelationType` are new.
- `MushafDecoration`, `SurahHeaderStyle` and `MushafFrameColors` keep their shapes.

## 1.4.1

- Stop reading the alef that seats a tanween as a letter of madd.
- Drop the tafkhim switch, and give the extra alef its own colour.

## 1.4.0

- `MushafDecoration` introduced: page frame plus `CLASSIC` / `MODERN` / `PLAIN` surah headers.
