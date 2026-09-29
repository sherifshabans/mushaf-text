import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart' show FontLoader;
import 'package:flutter_test/flutter_test.dart';
import 'package:mushaf_text/mushaf_text.dart';
import 'package:mushaf_text/src/mushaf_layout.dart';
import 'package:mushaf_text/src/tajweed_annotator.dart';
import 'package:mushaf_text/src/tajweed_rule.dart';

/// Renders pages to PNGs in `build/mushaf-shots/` so the page can be checked by
/// eye, and pins the one property the whole design rests on.
///
/// Run: `flutter test test/mushaf_render_test.dart`
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  /// Colouring letters must not change what a word measures.
  ///
  /// If it did, every line break, the stretching, and the font size computed
  /// from the widest line would all move the moment the switch is turned on —
  /// the page would stop matching the print. In Flutter a `TextStyle` that sets
  /// only a colour changes no metric, but that is checked here with the real
  /// font rather than assumed.
  testWidgets('colouring never changes a word width', (tester) async {
    await _loadFont();
    final ayahs = _ayahsFromDisk();

    var words = 0;
    var coloured = 0;
    var worst = 0.0;

    for (final ayah in ayahs.where((a) => a.page <= 10)) {
      final spans =
          TajweedAnnotator.annotate(ayah.text, includeNaturalMadd: true);
      var at = 0;
      while (at < ayah.text.length) {
        if (ayah.text[at] == ' ') {
          at++;
          continue;
        }
        var end = at;
        while (end < ayah.text.length && ayah.text[end] != ' ') {
          end++;
        }
        final word = ayah.text.substring(at, end);
        final from = at, to = end;
        final inWord = [
          for (final s in spans)
            if (s.start < to && s.end > from)
              TajweedSpan(
                (s.start < from ? from : s.start) - from,
                (s.end > to ? to : s.end) - from,
                s.rule,
              ),
        ];
        words++;
        if (inWord.isNotEmpty) {
          coloured++;
          final plain = _measure(TextSpan(text: word, style: _style));
          final painted = _measure(_coloured(word, inWord));
          final delta = (painted - plain).abs();
          if (delta > worst) worst = delta;
        }
        at = end;
      }
    }

    // ignore: avoid_print
    print('measured $words words, $coloured coloured, worst delta $worst');
    // Real Arabic metrics are never exact multiples of the font size; tofu is.
    // This is what tells a genuine measurement from a missing font.
    final sample = _measure(TextSpan(text: 'يُنفِقُونَ', style: _style));
    // ignore: avoid_print
    print('sample width: $sample');
    expect(sample % _style.fontSize!, isNot(0),
        reason: 'the font did not load — every glyph fell back to a box');
    expect(coloured, greaterThan(words ~/ 2));
    expect(worst, lessThan(0.01),
        reason: 'colouring moved a word — the page would stop matching print');
  });

  testWidgets('renders pages to PNG', (tester) async {
    await _loadFont();
    // A real phone's **logical** size, not a big canvas: the page picks its font
    // from the space it gets, capped by `maxFontSize`, so rendering into a
    // 1080-wide logical box leaves the text sitting small under that cap and
    // tells you nothing about how it looks in a hand.
    tester.view
      ..physicalSize = const Size(1080, 2180)
      ..devicePixelRatio = 2.625;
    addTearDown(tester.view.reset);

    // Warm the assets in **real** time first.
    //
    // Reading the bundle is genuine async IO, and inside `testWidgets` the
    // clock is fake, so the very first page painted before its text arrived and
    // came out blank — while every page after it looked right, because the load
    // was cached by then. A bug that hides itself after the first case.
    await tester.runAsync(() async {
      await Quran.ayahs();
      await MushafLineIndex.load();
    });

    final out = Directory('build/mushaf-shots')..createSync(recursive: true);

    for (final spec in const [
      (page: 1, tajweed: false, madd: false),
      (page: 3, tajweed: false, madd: false),
      (page: 3, tajweed: true, madd: false),
      (page: 3, tajweed: true, madd: true),
      (page: 604, tajweed: true, madd: false),
    ]) {
      await tester.pumpWidget(
        MaterialApp(
          // The boundary wraps the paper, **outside** the padding. Inside it,
          // the shot cuts the ink that Quranic letters throw past their advance
          // box — which reads as words clipped at both edges — and the page
          // draws no background of its own, so the PNG comes out transparent.
          home: RepaintBoundary(
            key: const ValueKey('page'),
            child: ColoredBox(
              color: MushafColors.light.paper,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 14),
                child: MushafPage(
                  page: spec.page,
                  tajweed: spec.tajweed,
                  tajweedNaturalMadd: spec.madd,
                ),
              ),
            ),
          ),
        ),
      );
      // The text loads from an asset. `pumpAndSettle` hangs here — something
      // keeps scheduling frames — so pump a fixed few instead, which is all the
      // load needs.
      for (var i = 0; i < 4; i++) {
        await tester.pump(const Duration(milliseconds: 20));
      }

      final boundary = tester.renderObject<RenderRepaintBoundary>(
        find.byKey(const ValueKey('page')),
      );
      // `toImage` is genuinely asynchronous; inside `testWidgets` the clock is
      // fake, so awaiting it directly never returns. `runAsync` gives it a real
      // one.
      final bytes = await tester.runAsync(() async {
        final image = await boundary.toImage(pixelRatio: 2.625);
        final data = await image.toByteData(format: ui.ImageByteFormat.png);
        image.dispose();
        return data;
      });
      expect(bytes, isNotNull);

      final name = 'page_${spec.page.toString().padLeft(3, '0')}'
          '${spec.tajweed ? '_tajweed' : ''}${spec.madd ? '_madd' : ''}.png';
      File('${out.path}/$name').writeAsBytesSync(bytes!.buffer.asUint8List());
      // ignore: avoid_print
      print('[mushaf] ${out.path}/$name');
    }
  });
}

const _style = TextStyle(
  fontFamily: 'MushafHafs',
  package: 'mushaf_text',
  fontSize: 34,
  color: Color(0xFF1A1208),
  height: 1.0,
);

double _measure(InlineSpan span) =>
    (TextPainter(text: span, textDirection: TextDirection.rtl)
          ..layout(maxWidth: double.infinity))
        .width;

InlineSpan _coloured(String word, List<TajweedSpan> spans) {
  final children = <TextSpan>[];
  var at = 0;
  for (final s in spans) {
    if (s.start > at) children.add(TextSpan(text: word.substring(at, s.start)));
    children.add(TextSpan(
      text: word.substring(s.start, s.end),
      style: TextStyle(color: s.rule.color),
    ));
    at = s.end;
  }
  if (at < word.length) children.add(TextSpan(text: word.substring(at)));
  return TextSpan(style: _style, children: children);
}

/// `flutter test` ships a stand-in font with no Arabic shaping and a fixed
/// advance per glyph, so any measurement taken with it is meaningless here —
/// it shows up as widths that are exact multiples of the font size.
Future<void> _loadFont() async {
  final bytes = File('assets/mushaf/uthmanic_hafs.ttf').readAsBytesSync();
  ByteData data() => ByteData.view(Uint8List.fromList(bytes).buffer);
  // A `TextStyle` carrying `package:` resolves to the qualified family name, so
  // registering the bare one is not enough — the widget finds nothing and draws
  // tofu. That is easy to miss: boxes all share one advance, so a width check
  // against them passes with a delta of zero while measuring nothing.
  for (final family in ['MushafHafs', 'packages/mushaf_text/MushafHafs']) {
    await (FontLoader(family)..addFont(Future.value(data()))).load();
  }
}

class _Row {
  _Row(this.page, this.text);
  final int page;
  final String text;
}

List<_Row> _ayahsFromDisk() => File('assets/mushaf/hafs.tsv')
    .readAsLinesSync()
    .where((l) => l.trim().isNotEmpty)
    .map((l) {
      final f = l.split('\t');
      return _Row(int.parse(f[4]), f[7]);
    })
    .toList(growable: false);
