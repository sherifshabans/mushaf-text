# Release notes

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
- The **right panel** shows the surah number, in the slot the reference uses for the
  Makki/Madani panel (revelation data is not bundled).
- Name, count and order are measured and fitted to their boxes, so nothing overflows the
  cartouche or the panels on any page size. Long names shrink instead of spilling.
- Header numbers are drawn in a plain sans face with `TextDirection.Rtl`. The KFGQPC font folds
  any digit run into an ayah rosette, so `٢٨٦` used to come out as two rosettes and the page
  number as one.
- `MODERN` and `PLAIN` follow the same width and colour rules.

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

None to the public API apart from the added `ayahCountLabel(count)` helper. `MushafDecoration`,
`SurahHeaderStyle` and `MushafFrameColors` keep their shapes.

## 1.4.1

- Stop reading the alef that seats a tanween as a letter of madd.
- Drop the tafkhim switch, and give the extra alef its own colour.

## 1.4.0

- `MushafDecoration` introduced: page frame plus `CLASSIC` / `MODERN` / `PLAIN` surah headers.
