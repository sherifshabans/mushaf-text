/// The Madinah Mushaf as real text for Flutter.
///
/// The page is drawn from the KFGQPC Uthmanic text in the King Fahd Complex
/// font, broken into lines exactly as the print does, with the tajweed rules
/// coloured letter by letter.
///
/// ```dart
/// MushafPage(page: 3, tajweed: true)
/// ```
///
/// The rules are derived from the bundled text's own orthography — the KFGQPC
/// script writes them out — and a parity test holds this implementation to
/// producing the same spans as the Kotlin one for all 6236 verses.
library;

export 'src/mushaf_colors.dart';
export 'src/mushaf_layout.dart' show toArabicNumerals;
export 'src/mushaf_page.dart'
    show MushafPage, TajweedHit, mushafFontFamily, pageTajweedCounts;
export 'src/quran.dart' show Ayah, Quran;
export 'src/surah_table.dart' show Surah;
export 'src/tajweed_annotator.dart' show TajweedAnnotator;
export 'src/tajweed_rule.dart' show TajweedFamily, TajweedRule, TajweedSpan;
