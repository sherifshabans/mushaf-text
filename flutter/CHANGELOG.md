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
