import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mushaf_text/src/tajweed_annotator.dart';
import 'package:mushaf_text/src/tajweed_rule.dart';

/// Holds the Dart annotator to producing **exactly** what the Kotlin one does,
/// for every ayah in the mushaf.
///
/// Two implementations can each pass their own suite and still disagree on
/// thousands of verses, so "the tests are green on both sides" says nothing
/// about a port. `fixtures/tajweed_spans.tsv` is written by the Kotlin
/// `TajweedFixtureDump` from what it actually computes; this test recomputes
/// every ayah here and requires the same spans, in the same order, with the
/// same rule.
///
/// Regenerate the fixture after any change to either implementation:
/// ```
/// ./gradlew :mushaf:testDebugUnitTest --tests '*TajweedFixtureDump*'
/// ```
void main() {
  test('the Dart port matches the Kotlin annotator on all 6236 ayahs', () {
    final ayahs = _loadAyahs();
    expect(ayahs.length, 6236, reason: 'the bundled text is not complete');

    final expected = _loadFixture();
    expect(expected.length, greaterThan(6000),
        reason: 'the fixture looks truncated');

    final mismatches = <String>[];
    var checkedSpans = 0;

    for (final ayah in ayahs) {
      final actual =
          TajweedAnnotator.annotate(ayah.text, includeNaturalMadd: true);
      final want = expected[ayah.id] ?? const <TajweedSpan>[];
      checkedSpans += want.length;

      if (actual.length != want.length) {
        mismatches.add(
          'ayah ${ayah.id} (${ayah.surah}:${ayah.number}) — '
          '${actual.length} spans, expected ${want.length}',
        );
        continue;
      }
      for (var i = 0; i < want.length; i++) {
        if (actual[i] != want[i]) {
          mismatches.add(
            'ayah ${ayah.id} (${ayah.surah}:${ayah.number}) span $i — '
            'got ${actual[i]}, expected ${want[i]}  '
            'at «${_wordAt(ayah.text, want[i].start)}»',
          );
          break;
        }
      }
    }

    // ignore: avoid_print
    print('checked $checkedSpans spans over ${ayahs.length} ayahs');

    expect(
      mismatches,
      isEmpty,
      reason: 'the port diverges from Kotlin:\n${mismatches.take(25).join('\n')}'
          '${mismatches.length > 25 ? '\n… and ${mismatches.length - 25} more' : ''}',
    );
  });

  test('natural madd stays off unless asked for', () {
    final ayahs = _loadAyahs();
    var off = 0;
    var on = 0;
    for (final a in ayahs) {
      off += TajweedAnnotator.annotate(a.text)
          .where((s) => s.rule == TajweedRule.maddNatural)
          .length;
      on += TajweedAnnotator.annotate(a.text, includeNaturalMadd: true)
          .where((s) => s.rule == TajweedRule.maddNatural)
          .length;
    }
    expect(off, 0);
    expect(on, greaterThan(10000),
        reason: 'the natural madd should be the widest rule');
  });
}

class _Ayah {
  _Ayah(this.id, this.surah, this.number, this.text);
  final int id;
  final int surah;
  final int number;
  final String text;
}

List<_Ayah> _loadAyahs() {
  final file = File('assets/mushaf/hafs.tsv');
  expect(file.existsSync(), isTrue, reason: 'missing ${file.path}');
  return file
      .readAsLinesSync()
      .where((l) => l.trim().isNotEmpty)
      .map((l) {
        final f = l.split('\t');
        return _Ayah(int.parse(f[0]), int.parse(f[1]), int.parse(f[2]), f[7]);
      })
      .toList(growable: false);
}

Map<int, List<TajweedSpan>> _loadFixture() {
  final file = File('test/fixtures/tajweed_spans.tsv');
  expect(file.existsSync(), isTrue,
      reason: 'missing ${file.path} — run the Kotlin TajweedFixtureDump');

  final rules = TajweedRule.values;
  final out = <int, List<TajweedSpan>>{};
  for (final line in file.readAsLinesSync()) {
    if (line.isEmpty || line.startsWith('#')) continue;
    final parts = line.split('\t');
    final id = int.parse(parts[0]);
    out[id] = parts[1].split(';').map((chunk) {
      final f = chunk.split(',');
      final start = int.parse(f[0]);
      return TajweedSpan(start, start + int.parse(f[1]), rules[int.parse(f[2])]);
    }).toList(growable: false);
  }
  return out;
}

String _wordAt(String text, int at) {
  var s = at;
  while (s > 0 && text[s - 1] != ' ' && text[s - 1] != ' ') {
    s--;
  }
  var e = at;
  while (e < text.length && text[e] != ' ' && text[e] != ' ') {
    e++;
  }
  return text.substring(s, e);
}
