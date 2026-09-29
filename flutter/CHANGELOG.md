## 1.1.0

Every tajweed rule now names the reference it comes from, and the code is
tested against those references rather than against itself.

- `TajweedRule` gains `source` and `evidence`: the matn, the verse number, and
  the verse in its own words. تحفة الأطفال for the noon, meem, lām and madd
  rules; المقدمة الجزرية for qalqalah and ghunnah; the King Fahd Complex's own
  notation guide for the three rules neither matn covers — which say so.
- `pageTajweedCounts(page)` — how many words on a page carry each rule, counted
  the same way the focus frame steps through them.
- **Fixed:** the extra alef was described as «الصفر المستدير» and «لا يُنطق».
  Both were wrong. The mark in this text is the upright rectangular zero, and
  it means the alef is dropped in waṣl but **kept in waqf** — it is pronounced
  when you stop on it.
- `TajweedRule` and `TajweedFamily` are now generated from one table shared
  with the Kotlin package, so the two ports cannot describe the same letter
  differently.
- The example app matches the Android reader: the focus navigator, the browse
  sheet with a bottom switch, and the reference in the rule sheet.

## 1.0.0

First release — the Flutter side of
[mushaf-text](https://github.com/sherifshabans/mushaf-text).

* `MushafPage` draws any of the 604 pages from the bundled KFGQPC Uthmanic text,
  broken into lines exactly as the Madinah print does.
* Tajweed colouring, 18 rules with a colour each, and a tap on a coloured letter
  returns the rule it belongs to.
* `Quran` and `TajweedAnnotator` are usable on their own, without drawing.
* `MushafColors.light` / `.dark`, with per-rule overrides.
* The rules are derived from the bundled script's own orthography, and a parity
  test holds them to the Kotlin implementation's output for all 6236 verses —
  85,386 spans.
