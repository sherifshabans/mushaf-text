import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'mushaf_colors.dart';
import 'mushaf_layout.dart';
import 'quran.dart';
import 'tajweed_annotator.dart';
import 'tajweed_rule.dart';

/// The font family the bundled KFGQPC Hafs face registers under.
const String mushafFontFamily = 'MushafHafs';
const String _fontPackage = 'mushaf_text';

/// Measuring size. Text width scales linearly with font size, so everything is
/// measured once at this size and multiplied.
const double _referenceFontSize = 100;

/// Floor before giving up.
const double _minFontSize = 9;

/// Smallest gap between two words, as a fraction of the font's space width.
///
/// The mushaf sets words closer than a full space. This gap decides the widest
/// line, and the widest line decides the font size — so tightening it makes the
/// text bigger. Stretched lines spread the remainder wider anyway when drawn.
const double _wordSpaceFactor = 0.62;

/// Line spacing on the two centred opening pages, which carry fewer than
/// fifteen lines inside a decorated frame.
const double _centredLineSpacing = 1.9;

const double _bannerInset = 0.08;
const double _bannerHeight = 0.84;

const String _basmala = 'بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ';

/// How a line spreads across its width.
enum _LineFill { justify, centre }

/// One page of the Madinah Mushaf, drawn as text.
///
/// ## Why it is painted rather than laid out with `Text`
/// Every line here is its own paragraph, and no text engine will stretch the
/// **last** line of a paragraph — which is every line, so none would stretch.
/// So each word is measured and placed, and the leftover space is shared
/// between the gaps. That also yields a rectangle per word, which is what makes
/// tapping and highlighting exact.
///
/// ## Why one canvas for the whole page
/// Quranic marks reach well above the line box, so slicing the page into a box
/// per line would clip them at every boundary. The page is one surface and each
/// line takes its band inside it, so marks extend freely into the space between
/// lines — exactly as in print.
class MushafPage extends StatefulWidget {
  const MushafPage({
    super.key,
    required this.page,
    this.colors,
    this.tajweed = false,
    this.tajweedNaturalMadd = false,
    this.tajweedTafkhim = false,
    this.maxFontSize = 34,
    this.onAyahTap,
    this.onTajweedTap,
    this.focusRule,
    this.focusIndex = 0,
    this.highlightedAyahs = const <int>{},
  });

  /// 1‥604.
  final int page;

  final MushafColors? colors;

  /// Colour each tajweed rule.
  final bool tajweed;

  /// Include the natural madd, by far the most frequent rule.
  final bool tajweedNaturalMadd;

  /// Also colour tafkhim and tarqiq: the isti'la letters, the ra, and the lam
  /// of the divine name.
  ///
  /// Off by default. They are some 30,000 positions, enough to tint the page
  /// the way the natural madd does.
  final bool tajweedTafkhim;

  /// Ceiling for the computed font size, in logical pixels.
  final double maxFontSize;

  /// Called with the ayah id when a word is tapped.
  final void Function(int ayahId)? onAyahTap;

  /// Called when a **coloured** letter is tapped. Takes priority over
  /// [onAyahTap]: the reader tapped the rule to ask about it, not to act on the
  /// verse.
  final void Function(TajweedHit hit)? onTajweedTap;

  /// Frames one occurrence of this rule on the page.
  final TajweedRule? focusRule;

  /// Which occurrence, counted in reading order. Wraps.
  final int focusIndex;

  /// Ayah ids to wash with [MushafColors.highlight].
  final Set<int> highlightedAyahs;

  @override
  State<MushafPage> createState() => _MushafPageState();
}

/// A tapped coloured letter: the rule, the word it sits in, and where inside it.
class TajweedHit {
  const TajweedHit({
    required this.rule,
    required this.word,
    required this.start,
    required this.end,
    required this.ayahId,
  });

  final TajweedRule rule;
  final String word;
  final int start;
  final int end;
  final int ayahId;
}

class _MushafPageState extends State<MushafPage> {
  List<Ayah>? _ayahs;
  MushafPageLines? _layout;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void didUpdateWidget(MushafPage old) {
    super.didUpdateWidget(old);
    if (old.page != widget.page) _load();
  }

  Future<void> _load() async {
    try {
      final ayahs = await Quran.page(widget.page);
      final layout = await MushafLineIndex.forPage(widget.page);
      if (!mounted) return;
      setState(() {
        _ayahs = ayahs;
        _layout = layout;
        _error = null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = widget.colors ??
        (Theme.of(context).brightness == Brightness.dark
            ? MushafColors.dark
            : MushafColors.light);
    final ayahs = _ayahs;

    if (_error != null) {
      return ColoredBox(
        color: colors.paper,
        child: Center(
          child: Text('$_error', style: TextStyle(color: colors.ink)),
        ),
      );
    }
    // The text is bundled and loads in tens of milliseconds, so a blank page
    // reads better than a spinner that flashes and vanishes.
    if (ayahs == null) return ColoredBox(color: colors.paper);

    return LayoutBuilder(
      builder: (context, constraints) {
        final drawing = _buildDrawing(
          ayahs: ayahs,
          reference: _layout,
          size: Size(constraints.maxWidth, constraints.maxHeight),
          colors: colors,
        );
        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTapUp: (details) => _onTap(drawing, details.localPosition),
          child: CustomPaint(
            size: Size(constraints.maxWidth, constraints.maxHeight),
            painter: _MushafPainter(drawing, colors, widget.focusRule,
                widget.focusIndex, widget.highlightedAyahs),
          ),
        );
      },
    );
  }

  void _onTap(_PageDrawing drawing, Offset pos) {
    final hit = drawing.hitTest(pos);
    if (hit == null) return;
    final onTajweed = widget.onTajweedTap;
    if (onTajweed != null) {
      final span = hit.token.spanAt(pos.dx - hit.token.left);
      if (span != null) {
        onTajweed(
          TajweedHit(
            rule: span.rule,
            word: hit.token.token.text,
            start: span.start,
            end: span.end,
            ayahId: hit.token.token.ayahId,
          ),
        );
        return;
      }
    }
    widget.onAyahTap?.call(hit.token.token.ayahId);
  }

  // ── layout ───────────────────────────────────────────────────────────────

  /// The page's text style.
  ///
  /// `height` is left alone on purpose. Pinning it to 1.0 collapses the line box
  /// to the font size, and the size the page picks is derived from that box — so
  /// the text came out small while the bands stayed tall, and the lines drifted
  /// apart. The font's own ascent and descent are what the print is set from.
  TextStyle _style(double size, Color color) => TextStyle(
        fontFamily: mushafFontFamily,
        package: _fontPackage,
        fontSize: size,
        color: color,
      );

  TextPainter _measure(InlineSpan span, {TextDirection dir = TextDirection.rtl}) =>
      TextPainter(text: span, textDirection: dir)..layout(maxWidth: double.infinity);

  /// The rosette digits, in the order that makes the font's ligature form.
  ///
  /// The font folds a run of Arabic-Indic digits into a single glyph that draws
  /// the rosette with the number inside it. Whether the shaper receives that run
  /// in the order given depends on the platform, so the order is **measured**
  /// rather than assumed: the right one ligates to one glyph and comes out
  /// roughly half as wide. That corrects itself anywhere the behaviour differs.
  bool _digitsReversed(TextStyle style) {
    const probe = '٢٥٣';
    final forward = _measure(TextSpan(text: probe, style: style),
            dir: TextDirection.ltr)
        .width;
    final backward = _measure(
            TextSpan(text: probe.split('').reversed.join(), style: style),
            dir: TextDirection.ltr)
        .width;
    return backward < forward;
  }

  _PageDrawing _buildDrawing({
    required List<Ayah> ayahs,
    required MushafPageLines? reference,
    required Size size,
    required MushafColors colors,
  }) {
    if (ayahs.isEmpty || size.isEmpty) {
      return _PageDrawing(const [], const [], const []);
    }

    final refStyle = _style(_referenceFontSize, colors.ink);
    final reversed = _digitsReversed(refStyle);
    String marker(int n) {
      final digits = toArabicNumerals(n);
      return reversed ? digits.split('').reversed.join() : digits;
    }

    final tokens = buildPageTokens(ayahs, marker);

    // Tajweed for each ayah on the page. Computed once per page — fifteen
    // lines' worth of work, not per frame.
    final tajweed = <int, List<TajweedSpan>>{};
    if (widget.tajweed) {
      for (final a in ayahs) {
        tajweed[a.id] = TajweedAnnotator.annotate(
          a.text,
          includeNaturalMadd: widget.tajweedNaturalMadd,
          includeTafkhim: widget.tajweedTafkhim,
        );
      }
    }

    final widthCache = <String, double>{};
    double refWidth(MushafToken t) => widthCache.putIfAbsent(
          t.text,
          () => _measure(
            TextSpan(text: t.text, style: refStyle),
            dir: t.isEndMark ? TextDirection.ltr : TextDirection.rtl,
          ).width,
        );

    final spaceRef =
        _measure(TextSpan(text: ' ', style: refStyle)).width * _wordSpaceFactor;
    final lineBoxRef = _measure(TextSpan(text: 'ا', style: refStyle)).height;

    final totalLines = reference?.lineCount ?? 15;
    // Without a reference for this page, fall back to filling lines in order.
    final lines = reference?.sliceTokens(tokens) ??
        _fillLines(tokens, totalLines, size.width, spaceRef, refWidth);

    var widest = 0.0;
    for (final line in lines.values) {
      var w = spaceRef * (line.length - 1);
      for (final t in line) {
        w += refWidth(t);
      }
      widest = math.max(widest, w);
    }
    if (widest <= 0) return _PageDrawing(const [], const [], const []);

    final fromWidth = size.width * _referenceFontSize / widest;
    final fromHeight = (size.height / totalLines) * _referenceFontSize / lineBoxRef;
    final fontSize = math.max(
      _minFontSize,
      math.min(math.min(fromWidth, fromHeight), widget.maxFontSize),
    );

    final centreAll = widget.page <= 2;
    final style = _style(fontSize, colors.ink);
    final markerStyle = _style(fontSize, colors.accent);
    final lineBox = _measure(TextSpan(text: 'ا', style: style)).height;

    final double pitch;
    final double offsetY;
    if (centreAll) {
      pitch = math.min(size.height / totalLines, lineBox * _centredLineSpacing);
      offsetY = (size.height - pitch * totalLines) / 2;
    } else {
      pitch = size.height / totalLines;
      offsetY = 0;
    }

    final verses = <_DrawnLine>[];
    final banners = <_DrawnBox>[];
    final basmalas = <_DrawnText>[];
    final spaceWidth =
        _measure(TextSpan(text: ' ', style: style)).width * _wordSpaceFactor;

    for (var lineNo = 1; lineNo <= totalLines; lineNo++) {
      final bandTop = offsetY + pitch * (lineNo - 1);
      final words = lines[lineNo];

      if (words == null) {
        final role = reference?.reservedRoles[lineNo];
        if (role is BannerRole) {
          final name = Quran.surah(role.surah).nameArabic;
          final painter = _measure(
            TextSpan(text: 'سُورَةُ $name', style: _style(fontSize * 0.82, colors.accent)),
          );
          final boxTop = bandTop + pitch * _bannerInset;
          final boxHeight = pitch * _bannerHeight;
          banners.add(
            _DrawnBox(
              top: boxTop,
              height: boxHeight,
              painter: painter,
              left: (size.width - painter.width) / 2,
              textTop: boxTop + (boxHeight - painter.height) / 2,
            ),
          );
        } else if (role is BasmalaRole) {
          final painter = _measure(
            TextSpan(text: _basmala, style: _style(fontSize * 1.05, colors.ink)),
          );
          basmalas.add(
            _DrawnText(
              painter: painter,
              left: (size.width - painter.width) / 2,
              top: bandTop + (pitch - painter.height) / 2,
            ),
          );
        }
        continue;
      }

      final fill = centreAll
          ? _LineFill.centre
          : (reference != null && reference.centredLines.contains(lineNo))
              ? _LineFill.centre
              : _LineFill.justify;

      verses.add(
        _DrawnLine(
          bandTop: bandTop,
          bandBottom: bandTop + pitch,
          textTop: bandTop + (pitch - lineBox) / 2,
          tokens: _placeTokens(
            tokens: words,
            lineWidth: size.width,
            fill: fill,
            style: style,
            markerStyle: markerStyle,
            spaceWidth: spaceWidth,
            tajweed: tajweed,
          ),
        ),
      );
    }

    return _PageDrawing(verses, banners, basmalas);
  }

  /// Lays a line out from the right. When stretched, the leftover width is
  /// shared equally between the gaps — the manual justification no text engine
  /// will do for a single line.
  List<_PlacedToken> _placeTokens({
    required List<MushafToken> tokens,
    required double lineWidth,
    required _LineFill fill,
    required TextStyle style,
    required TextStyle markerStyle,
    required double spaceWidth,
    required Map<int, List<TajweedSpan>> tajweed,
  }) {
    if (tokens.isEmpty) return const [];

    final spans = [
      for (final t in tokens) _tokenSpans(t, tajweed[t.ayahId]),
    ];
    final painters = <TextPainter>[];
    for (var i = 0; i < tokens.length; i++) {
      final t = tokens[i];
      painters.add(
        _measure(
          _tokenSpan(t, spans[i], t.isEndMark ? markerStyle : style),
          dir: t.isEndMark ? TextDirection.ltr : TextDirection.rtl,
        ),
      );
    }

    var natural = spaceWidth * (tokens.length - 1);
    for (final p in painters) {
      natural += p.width;
    }
    final slack = lineWidth - natural;
    final gaps = math.max(tokens.length - 1, 1);
    final extra =
        (fill == _LineFill.justify && tokens.length > 1 && slack > 0)
            ? slack / gaps
            : 0.0;

    // The right edge is the start of the line.
    var x = (fill == _LineFill.centre && slack > 0)
        ? lineWidth - slack / 2
        : lineWidth;

    final placed = <_PlacedToken>[];
    for (var i = 0; i < tokens.length; i++) {
      final w = painters[i].width;
      x -= w;
      placed.add(_PlacedToken(tokens[i], painters[i], x, w, spans[i]));
      x -= spaceWidth + extra;
    }
    return placed;
  }

  /// Fallback split when a page has no reference layout.
  Map<int, List<MushafToken>> _fillLines(
    List<MushafToken> tokens,
    int lineCount,
    double width,
    double spaceRef,
    double Function(MushafToken) refWidth,
  ) {
    final out = <int, List<MushafToken>>{};
    final perLine = (tokens.length / lineCount).ceil();
    for (var i = 0; i < lineCount; i++) {
      final from = i * perLine;
      if (from >= tokens.length) break;
      out[i + 1] = tokens.sublist(from, math.min(from + perLine, tokens.length));
    }
    return out;
  }
}

/// An ayah's spans mapped onto a word and clipped to it.
List<TajweedSpan> _tokenSpans(MushafToken token, List<TajweedSpan>? ayahSpans) {
  if (ayahSpans == null || ayahSpans.isEmpty || token.isEndMark ||
      token.charStart < 0) {
    return const [];
  }
  final from = token.charStart;
  final to = from + token.text.length;
  final out = <TajweedSpan>[];
  for (final s in ayahSpans) {
    final a = math.max(s.start, from);
    final b = math.min(s.end, to);
    if (a < b) out.add(TajweedSpan(a - from, b - from, s.rule));
  }
  return out;
}

/// A word with its rules' colours.
///
/// ## Why this cannot move the layout
/// A span that sets only a colour changes no metric, so the word measures the
/// same whether it carries one colour or ten — line breaks, stretching and the
/// computed font size all stay put. `test/tajweed_render_test.dart` measures
/// that with the real font rather than assuming it.
///
/// ## The one limit
/// A glyph the font folds out of several code points — a lam-alef ligature —
/// takes **one** colour. That is OpenType, not this code: the rule that starts
/// the ligature wins.
InlineSpan _tokenSpan(
    MushafToken token, List<TajweedSpan> spans, TextStyle style) {
  if (spans.isEmpty) return TextSpan(text: token.text, style: style);

  final children = <TextSpan>[];
  var at = 0;
  for (final s in spans) {
    if (s.start > at) {
      children.add(TextSpan(text: token.text.substring(at, s.start)));
    }
    children.add(
      TextSpan(
        text: token.text.substring(s.start, s.end),
        style: TextStyle(color: s.rule.color),
      ),
    );
    at = s.end;
  }
  if (at < token.text.length) {
    children.add(TextSpan(text: token.text.substring(at)));
  }
  return TextSpan(style: style, children: children);
}

// ── the computed page ──────────────────────────────────────────────────────

class _PlacedToken {
  _PlacedToken(this.token, this.painter, this.left, this.width, this.tajweed);

  final MushafToken token;
  final TextPainter painter;
  final double left;
  final double width;
  final List<TajweedSpan> tajweed;

  /// The rule under a point [dx] inside the word, or `null` if it is not coloured.
  ///
  /// The offset comes from the text layout itself — the same calculation that
  /// places a caret — so it is exact per character rather than estimated, which
  /// matters where ligatures make the drawn glyphs fewer than the code points.
  TajweedSpan? spanAt(double dx) {
    if (tajweed.isEmpty) return null;
    final pos = painter.getPositionForOffset(Offset(dx, painter.height / 2));
    final offset = pos.offset;
    for (final s in tajweed) {
      if (offset >= s.start && offset < s.end) return s;
    }
    return null;
  }
}

class _DrawnLine {
  _DrawnLine({
    required this.bandTop,
    required this.bandBottom,
    required this.textTop,
    required this.tokens,
  });

  final double bandTop;
  final double bandBottom;
  final double textTop;
  final List<_PlacedToken> tokens;
}

class _DrawnBox {
  _DrawnBox({
    required this.top,
    required this.height,
    required this.painter,
    required this.left,
    required this.textTop,
  });

  final double top;
  final double height;
  final TextPainter painter;
  final double left;
  final double textTop;
}

class _DrawnText {
  _DrawnText({required this.painter, required this.left, required this.top});

  final TextPainter painter;
  final double left;
  final double top;
}

class _TokenHit {
  _TokenHit(this.line, this.token);
  final _DrawnLine line;
  final _PlacedToken token;
}

class _PageDrawing {
  _PageDrawing(this.verses, this.banners, this.basmalas);

  final List<_DrawnLine> verses;
  final List<_DrawnBox> banners;
  final List<_DrawnText> basmalas;

  _TokenHit? hitTest(Offset pos) {
    for (final line in verses) {
      if (pos.dy < line.bandTop || pos.dy > line.bandBottom) continue;
      for (final t in line.tokens) {
        if (pos.dx >= t.left && pos.dx <= t.left + t.width) {
          return _TokenHit(line, t);
        }
      }
      // Between two words — take the nearest on the same line.
      _PlacedToken? best;
      var bestD = double.infinity;
      for (final t in line.tokens) {
        final d = (pos.dx - (t.left + t.width / 2)).abs();
        if (d < bestD) {
          bestD = d;
          best = t;
        }
      }
      if (best != null) return _TokenHit(line, best);
    }
    return null;
  }
}

class _MushafPainter extends CustomPainter {
  _MushafPainter(
    this.drawing,
    this.colors,
    this.focusRule,
    this.focusIndex,
    this.highlighted,
  );

  final _PageDrawing drawing;
  final MushafColors colors;
  final TajweedRule? focusRule;
  final int focusIndex;
  final Set<int> highlighted;

  @override
  void paint(Canvas canvas, Size size) {
    // 1) the wash behind a selected ayah, kept inside its own line band so it
    //    does not creep onto the lines above and below
    if (highlighted.isNotEmpty) {
      final paint = Paint()..color = colors.highlight;
      for (final line in drawing.verses) {
        double? from;
        double? to;
        void flush() {
          if (from != null && to != null) {
            canvas.drawRect(
              Rect.fromLTRB(from!, line.bandTop, to!, line.bandBottom),
              paint,
            );
          }
          from = null;
          to = null;
        }

        for (final t in line.tokens) {
          if (highlighted.contains(t.token.ayahId)) {
            from = math.min(from ?? t.left, t.left);
            to = math.max(to ?? t.left + t.width, t.left + t.width);
          } else {
            flush();
          }
        }
        flush();
      }
    }

    // 2) one occurrence of the focused rule, framed.
    //
    //    A whole word, not the letter: a single letter can be a dot in a packed
    //    line, and a frame around the word is far easier to find.
    final rule = focusRule;
    if (rule != null) {
      final spots = <MapEntry<_DrawnLine, _PlacedToken>>[];
      for (final line in drawing.verses) {
        for (final t in line.tokens) {
          if (t.tajweed.any((s) => s.rule == rule)) {
            spots.add(MapEntry(line, t));
          }
        }
      }
      if (spots.isNotEmpty) {
        final e = spots[((focusIndex % spots.length) + spots.length) % spots.length];
        final rect = RRect.fromRectAndRadius(
          Rect.fromLTRB(e.value.left - 3, e.key.bandTop,
              e.value.left + e.value.width + 3, e.key.bandBottom),
          const Radius.circular(7),
        );
        canvas
          ..drawRRect(rect, Paint()..color = rule.color.withValues(alpha: 0.16))
          ..drawRRect(
            rect,
            Paint()
              ..color = rule.color.withValues(alpha: 0.70)
              ..style = PaintingStyle.stroke
              ..strokeWidth = 1.5,
          );
      }
    }

    // 3) surah banners
    for (final b in drawing.banners) {
      final rect = RRect.fromRectAndRadius(
        Rect.fromLTWH(0, b.top, size.width, b.height),
        const Radius.circular(8),
      );
      canvas
        ..drawRRect(rect, Paint()..color = colors.banner)
        ..drawRRect(
          rect,
          Paint()
            ..color = colors.gold
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1,
        );
      b.painter.paint(canvas, Offset(b.left, b.textTop));
    }

    // 4) the basmala
    for (final t in drawing.basmalas) {
      t.painter.paint(canvas, Offset(t.left, t.top));
    }

    // 5) the words, each rosette drawn by the font as one glyph
    for (final line in drawing.verses) {
      for (final t in line.tokens) {
        t.painter.paint(canvas, Offset(t.left, line.textTop));
      }
    }
  }

  @override
  bool shouldRepaint(_MushafPainter old) =>
      old.drawing != drawing ||
      old.colors != colors ||
      old.focusRule != focusRule ||
      old.focusIndex != focusIndex ||
      old.highlighted != highlighted;
}

/// How many **words** on [page] carry each tajweed rule, most frequent first.
///
/// Counted by word rather than by span, and deliberately: this is the number
/// the focus navigator shows as «الموضع ن من م», and the frame [MushafPage]
/// draws with [MushafPage.focusIndex] goes around a whole word. A word that
/// carries the same rule twice is still one stop, so counting spans would
/// promise a position the navigator can never reach.
///
/// It walks the same token list the page draws, so the total here and the
/// stops there cannot drift apart.
Future<List<MapEntry<TajweedRule, int>>> pageTajweedCounts(
  int page, {
  bool includeNaturalMadd = false,
  bool includeTafkhim = false,
}) async {
  final ayahs = await Quran.page(page);
  final spans = <int, List<TajweedSpan>>{
    for (final a in ayahs)
      a.id: TajweedAnnotator.annotate(a.text,
          includeNaturalMadd: includeNaturalMadd,
          includeTafkhim: includeTafkhim),
  };
  final counts = <TajweedRule, int>{};
  for (final token in buildPageTokens(ayahs, (_) => '')) {
    final seen = <TajweedRule>{};
    for (final s in _tokenSpans(token, spans[token.ayahId])) {
      if (seen.add(s.rule)) {
        counts[s.rule] = (counts[s.rule] ?? 0) + 1;
      }
    }
  }
  final out = counts.entries.toList()
    ..sort((a, b) {
      final byCount = b.value.compareTo(a.value);
      // A stable tie-break, so the key does not reshuffle between rebuilds.
      return byCount != 0 ? byCount : a.key.index.compareTo(b.key.index);
    });
  return out;
}
