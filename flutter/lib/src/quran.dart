import 'package:flutter/services.dart' show rootBundle;

import 'surah_table.dart';

/// One ayah of the Madinah Mushaf (Hafs ʿan ʿĀṣim), in the KFGQPC Uthmanic
/// encoding.
class Ayah {
  const Ayah({
    required this.id,
    required this.surah,
    required this.number,
    required this.juz,
    required this.page,
    required this.lineStart,
    required this.lineEnd,
    required this.text,
  });

  /// 1‥6236, in mushaf order.
  final int id;
  final int surah;
  final int number;
  final int juz;

  /// 1‥604.
  final int page;

  /// First line of the page the ayah occupies, 1‥15.
  ///
  /// A hint only: the exact word-per-line layout comes from the bundled layout
  /// asset, and these numbers are wrong on some page boundaries.
  final int lineStart;
  final int lineEnd;

  /// The verse **without** its number — the renderer draws the rosette itself.
  final String text;

  @override
  String toString() => 'Ayah($surah:$number)';
}

/// The bundled Quran text.
///
/// The text loads from the package asset once (about 1.4 MB, a few tens of
/// milliseconds) and is cached for the life of the isolate.
abstract final class Quran {
  static const int pageCount = 604;
  static const int ayahCount = 6236;

  static const String _asset = 'packages/mushaf_text/assets/mushaf/hafs.tsv';

  static List<Ayah>? _cache;
  static Future<List<Ayah>>? _loading;

  /// The 114 surahs, in order. Needs no loading.
  static List<Surah> get surahs => surahTable;

  static Surah surah(int number) => surahTable[number - 1];

  /// Every ayah, in mushaf order.
  ///
  /// Safe to call repeatedly: the first call reads the asset, the rest return
  /// the cached list.
  static Future<List<Ayah>> ayahs() {
    final cached = _cache;
    if (cached != null) return Future.value(cached);
    return _loading ??= _load();
  }

  static Future<List<Ayah>> _load() async {
    final raw = await rootBundle.loadString(_asset);
    final out = <Ayah>[];
    for (final line in raw.split('\n')) {
      if (line.trim().isEmpty) continue;
      final f = line.split('\t');
      out.add(
        Ayah(
          id: int.parse(f[0]),
          surah: int.parse(f[1]),
          number: int.parse(f[2]),
          juz: int.parse(f[3]),
          page: int.parse(f[4]),
          lineStart: int.parse(f[5]),
          lineEnd: int.parse(f[6]),
          text: f[7].trimRight(),
        ),
      );
    }
    _cache = out;
    _loading = null;
    return out;
  }

  /// The ayahs printed on [page], in order.
  static Future<List<Ayah>> page(int page) async {
    final all = await ayahs();
    return all.where((a) => a.page == page).toList(growable: false);
  }

  /// The ayahs of [surah], in order.
  static Future<List<Ayah>> surahAyahs(int surah) async {
    final all = await ayahs();
    return all.where((a) => a.surah == surah).toList(growable: false);
  }

  /// A single ayah by surah and number, or `null` if there is no such verse.
  static Future<Ayah?> ayah(int surah, int number) async {
    final all = await ayahs();
    for (final a in all) {
      if (a.surah == surah && a.number == number) return a;
    }
    return null;
  }
}
