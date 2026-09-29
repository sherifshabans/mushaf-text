import 'package:flutter/services.dart' show rootBundle;

import 'quran.dart';

/// One word — or an end-of-ayah rosette — on a line.
class MushafToken {
  const MushafToken({
    required this.text,
    required this.ayahId,
    required this.isEndMark,
    this.charStart = -1,
  });

  final String text;
  final int ayahId;

  /// `true` when this is the rosette with the verse number, not a word.
  final bool isEndMark;

  /// Where the word starts inside its ayah's text, or `-1` for a rosette.
  ///
  /// Tajweed is computed over the **whole ayah**, because some rules cross word
  /// boundaries — the idgham of a noon into the next word's first letter, a
  /// separated madd. This offset is what maps those spans back onto a word.
  final int charStart;
}

/// What a reserved line carries.
sealed class ReservedRole {
  const ReservedRole(this.surah);
  final int surah;
}

/// The banner with the surah's name.
class BannerRole extends ReservedRole {
  const BannerRole(super.surah);
}

/// The basmala line.
class BasmalaRole extends ReservedRole {
  const BasmalaRole(super.surah);
}

/// How one page of the Madinah mushaf breaks into lines.
class MushafPageLines {
  MushafPageLines({
    required this.lineCount,
    required this.tokensPerLine,
    required this.reservedRoles,
    required this.centredLines,
  });

  /// 15, and eight on the two opening pages.
  final int lineCount;

  /// How many tokens sit on each line, from line 1. A zero means the line is
  /// reserved for a banner or the basmala.
  final List<int> tokensPerLine;

  /// The mushaf puts a surah's banner on the line right before its first verse
  /// **even when that falls on the previous page**: An-Nisa's banner is page 76
  /// line 15 while its basmala is page 77 line 1.
  final Map<int, ReservedRole> reservedRoles;

  /// Lines the print centres instead of stretching.
  ///
  /// Measured off the page images, not inferred: a stretched line spans at
  /// least 0.96 of the text column and a centred one at most 0.83, with nothing
  /// in between. It is **not** "centre the last line of a surah" — Al-Ikhlas's
  /// last line is stretched while Al-Falaq's is centred.
  final Set<int> centredLines;

  late final List<int> reservedLines = [
    for (var i = 1; i <= lineCount; i++)
      if ((i - 1 < tokensPerLine.length ? tokensPerLine[i - 1] : 0) == 0) i,
  ];

  int get totalTokens => tokensPerLine.fold(0, (a, b) => a + b);

  /// Splits a page's tokens across its lines, or `null` when the count does not
  /// match — in which case the caller must not trust the split.
  Map<int, List<MushafToken>>? sliceTokens(List<MushafToken> tokens) {
    if (tokens.length != totalTokens) return null;
    final out = <int, List<MushafToken>>{};
    var at = 0;
    for (var i = 0; i < tokensPerLine.length; i++) {
      final n = tokensPerLine[i];
      if (n > 0) {
        out[i + 1] = tokens.sublist(at, at + n);
        at += n;
      }
    }
    return out;
  }
}

/// The bundled Madinah line layout.
///
/// Built from quran.com's per-word line numbers — the same data the official
/// page images are rendered from — so the split **matches** the print instead
/// of guessing at it.
///
/// Format: `page:lineCount:tokensPerLine:reservedRoles:lineFill`
abstract final class MushafLineIndex {
  static const String _asset = 'packages/mushaf_text/assets/mushaf/layout.txt';

  static Map<int, MushafPageLines>? _cache;
  static Future<Map<int, MushafPageLines>>? _loading;

  static Future<Map<int, MushafPageLines>> load() {
    final cached = _cache;
    if (cached != null) return Future.value(cached);
    return _loading ??= _read();
  }

  static Future<MushafPageLines?> forPage(int page) async => (await load())[page];

  static Future<Map<int, MushafPageLines>> _read() async {
    final raw = await rootBundle.loadString(_asset);
    final out = parse(raw);
    _cache = out;
    _loading = null;
    return out;
  }

  /// Exposed so tests can parse the asset straight off disk.
  static Map<int, MushafPageLines> parse(String raw) {
    final out = <int, MushafPageLines>{};
    for (final rawLine in raw.split('\n')) {
      final line = rawLine.trim();
      if (line.isEmpty || line.startsWith('#')) continue;
      final parts = line.split(':');
      if (parts.length != 5) continue;
      final page = int.tryParse(parts[0]);
      final count = int.tryParse(parts[1]);
      if (page == null || count == null) continue;
      final counts =
          parts[2].split(',').map((c) => int.tryParse(c.trim()) ?? 0).toList();
      if (counts.length != count) continue;

      final roles = <int, ReservedRole>{};
      for (final entry in parts[3].split(';')) {
        if (entry.trim().isEmpty) continue;
        final f = entry.split(',');
        if (f.length != 3) continue;
        final at = int.tryParse(f[0]);
        final surah = int.tryParse(f[2]);
        if (at == null || surah == null) continue;
        if (f[1] == 'b') {
          roles[at] = BannerRole(surah);
        } else if (f[1] == 'm') {
          roles[at] = BasmalaRole(surah);
        }
      }

      final centred = <int>{};
      for (var i = 0; i < parts[4].length; i++) {
        if (parts[4][i] == 'c') centred.add(i + 1);
      }

      out[page] = MushafPageLines(
        lineCount: count,
        tokensPerLine: counts,
        reservedRoles: roles,
        centredLines: centred,
      );
    }
    return out;
  }
}

/// Turns a page's ayahs into ordered tokens.
///
/// Walks the text by index rather than splitting on spaces, so every word knows
/// where it sits inside its ayah ([MushafToken.charStart]).
///
/// A word keeps its marks exactly as written — pause signs and harakat stay
/// attached and the font stacks them over the letter. Splitting them off would
/// give each a full word's gap and leave it floating away from its letter.
List<MushafToken> buildPageTokens(
  List<Ayah> ayahs,
  String Function(int ayahNumber) markerText,
) {
  final tokens = <MushafToken>[];
  for (final ayah in ayahs) {
    final text = ayah.text;
    var at = 0;
    while (at < text.length) {
      if (text[at] == ' ') {
        at++;
        continue;
      }
      var end = at;
      while (end < text.length && text[end] != ' ') {
        end++;
      }
      tokens.add(
        MushafToken(
          text: text.substring(at, end),
          ayahId: ayah.id,
          isEndMark: false,
          charStart: at,
        ),
      );
      at = end;
    }
    tokens.add(
      MushafToken(
        text: markerText(ayah.number),
        ayahId: ayah.id,
        isEndMark: true,
      ),
    );
  }
  return tokens;
}

const List<String> _arabicDigits = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];

/// Arabic-Indic digits for [n].
String toArabicNumerals(int n) {
  if (n == 0) return _arabicDigits[0];
  final b = StringBuffer();
  for (final ch in n.toString().split('')) {
    final d = int.tryParse(ch);
    b.write(d == null ? ch : _arabicDigits[d]);
  }
  return b.toString();
}
