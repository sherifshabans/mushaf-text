import 'package:flutter/painting.dart';

import 'tajweed_rule.dart';

/// Colours of the page.
///
/// [MushafPage] does not fill the background itself — put [paper], or your own
/// colour, behind it — so the page composes with any container.
class MushafColors {
  const MushafColors({
    required this.paper,
    required this.ink,
    required this.accent,
    required this.banner,
    required this.gold,
    required this.highlight,
    this.tajweedOverrides = const <TajweedRule, Color>{},
  });

  /// The page ground. Draw it yourself behind the page.
  final Color paper;

  /// The text.
  final Color ink;

  /// Ayah rosettes and banner text.
  final Color accent;

  /// Fill behind a surah banner.
  final Color banner;

  /// Banner border, rules, frames.
  final Color gold;

  /// Wash behind a selected ayah.
  final Color highlight;

  /// Per-rule overrides; anything missing keeps [TajweedRule.color].
  final Map<TajweedRule, Color> tajweedOverrides;

  Color tajweedColor(TajweedRule rule) => tajweedOverrides[rule] ?? rule.color;

  /// Cream paper and near-black ink — the printed look.
  static const MushafColors light = MushafColors(
    paper: Color(0xFFFAF6EB),
    ink: Color(0xFF1A1208),
    accent: Color(0xFF6B4F3A),
    banner: Color(0xFFF4EDE0),
    gold: Color(0xFFC4B275),
    highlight: Color(0xFFEADDC9),
  );

  /// Night reading.
  ///
  /// The rule colours are lifted towards white: the defaults are tuned for
  /// cream paper and sink into a dark page.
  static final MushafColors dark = MushafColors(
    paper: const Color(0xFF15130F),
    ink: const Color(0xFFEDE6D6),
    accent: const Color(0xFFC9A77C),
    banner: const Color(0xFF221E17),
    gold: const Color(0xFF8C7A45),
    highlight: const Color(0xFF3A3122),
    tajweedOverrides: {
      for (final r in TajweedRule.values)
        r: Color.lerp(r.color, const Color(0xFFFFFFFF), 0.42)!,
    },
  );

  MushafColors copyWith({
    Color? paper,
    Color? ink,
    Color? accent,
    Color? banner,
    Color? gold,
    Color? highlight,
    Map<TajweedRule, Color>? tajweedOverrides,
  }) =>
      MushafColors(
        paper: paper ?? this.paper,
        ink: ink ?? this.ink,
        accent: accent ?? this.accent,
        banner: banner ?? this.banner,
        gold: gold ?? this.gold,
        highlight: highlight ?? this.highlight,
        tajweedOverrides: tajweedOverrides ?? this.tajweedOverrides,
      );
}
