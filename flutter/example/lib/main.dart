import 'package:flutter/material.dart';
import 'package:mushaf_text/mushaf_text.dart';

/// The same reader the Android app ships, built on this package.
///
/// It is kept deliberately close to the Compose screen in `DailySeventy`: the
/// same toolbar order, the same browse sheet with a bottom switch, the same
/// floating focus navigator, and the same rule sheet — including the reference
/// each ruling comes from. Two ports that drift apart stop being one thing, and
/// the tajweed colouring is only worth trusting if both sides say the same.
void main() => runApp(const MushafApp());

class MushafApp extends StatelessWidget {
  const MushafApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'mushaf_text',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF6B4F3A)),
          useMaterial3: true,
        ),
        home: const MushafReader(),
      );
}

class MushafReader extends StatefulWidget {
  const MushafReader({super.key});

  @override
  State<MushafReader> createState() => _MushafReaderState();
}

class _MushafReaderState extends State<MushafReader> {
  final _controller = PageController();
  int _page = 1;
  bool _tajweed = true;
  bool _naturalMadd = false;
  bool _dark = false;

  /// The rule being walked through on this page, if any.
  TajweedRule? _focus;
  int _focusIndex = 0;
  List<MapEntry<TajweedRule, int>> _counts = const [];

  MushafColors get _colors => _dark ? MushafColors.dark : MushafColors.light;

  int get _focusTotal => _counts
      .firstWhere((e) => e.key == _focus,
          orElse: () => const MapEntry(TajweedRule.ghunna, 0))
      .value;

  @override
  void initState() {
    super.initState();
    _loadCounts();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _loadCounts() async {
    final page = _page;
    final counts = _tajweed
        ? await pageTajweedCounts(page, includeNaturalMadd: _naturalMadd)
        : const <MapEntry<TajweedRule, int>>[];
    if (!mounted || page != _page) return;
    setState(() => _counts = counts);
  }

  void _onPageChanged(int i) {
    setState(() {
      _page = i + 1;
      // Positions belong to their page, so turning the page ends the walk.
      _focus = null;
      _focusIndex = 0;
    });
    _loadCounts();
  }

  void _setTajweed(bool on) {
    setState(() {
      _tajweed = on;
      if (!on) _focus = null;
    });
    _loadCounts();
  }

  void _setNaturalMadd(bool on) {
    setState(() {
      _naturalMadd = on;
      _focus = null;
    });
    _loadCounts();
  }

  @override
  Widget build(BuildContext context) {
    final colors = _colors;
    return Scaffold(
      backgroundColor: colors.paper,
      appBar: AppBar(
        backgroundColor: colors.paper,
        foregroundColor: colors.accent,
        elevation: 0,
        title: Text(
          'صفحة ${toArabicNumerals(_page)}',
          style: TextStyle(
            fontFamily: mushafFontFamily,
            package: 'mushaf_text',
            color: colors.accent,
            fontSize: 17,
          ),
        ),
        actions: [
          IconButton(
            tooltip: 'التجويد',
            icon: Icon(Icons.palette,
                color: _tajweed ? TajweedRule.idghamGhunna.color : colors.gold),
            onPressed: () => _setTajweed(!_tajweed),
          ),
          IconButton(
            tooltip: 'ليل ونهار',
            icon: Icon(_dark ? Icons.light_mode : Icons.dark_mode,
                color: colors.accent),
            onPressed: () => setState(() => _dark = !_dark),
          ),
          // Last, as on Android: the one button that opens everything else.
          IconButton(
            tooltip: 'الفهرس والأحكام',
            icon: Icon(Icons.menu_book, color: colors.accent),
            onPressed: _showBrowse,
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: Stack(
              children: [
                // The mushaf reads right to left, so page 1 sits on the right.
                Directionality(
                  textDirection: TextDirection.rtl,
                  child: PageView.builder(
                    controller: _controller,
                    itemCount: Quran.pageCount,
                    onPageChanged: _onPageChanged,
                    itemBuilder: (context, i) => Padding(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 8),
                      child: MushafPage(
                        page: i + 1,
                        colors: colors,
                        tajweed: _tajweed,
                        tajweedNaturalMadd: _naturalMadd,
                        focusRule: i + 1 == _page ? _focus : null,
                        focusIndex: _focusIndex,
                        onTajweedTap: _showRule,
                        onAyahTap: (id) => _snack('آية رقم $id'),
                      ),
                    ),
                  ),
                ),
                // Floating, not a row in the column: the page is laid out for
                // fifteen lines, and anything that takes height from it shrinks
                // the text.
                if (_focus != null && _focusTotal > 0)
                  Align(
                    alignment: Alignment.bottomCenter,
                    child: _FocusBar(
                      colors: colors,
                      rule: _focus!,
                      index: _focusIndex,
                      total: _focusTotal,
                      onPrevious: () => setState(() => _focusIndex--),
                      onNext: () => setState(() => _focusIndex++),
                      onClose: () => setState(() => _focus = null),
                    ),
                  ),
              ],
            ),
          ),
          _Bar(
            colors: colors,
            page: _page,
            onJump: (p) {
              _controller.jumpToPage(p - 1);
              _onPageChanged(p - 1);
            },
          ),
        ],
      ),
    );
  }

  void _snack(String text) => ScaffoldMessenger.of(context)
    ..clearSnackBars()
    ..showSnackBar(SnackBar(
      content: Text(text, textDirection: TextDirection.rtl),
      duration: const Duration(seconds: 1),
    ));

  void _focusOn(TajweedRule rule) => setState(() {
        _focus = rule;
        _focusIndex = 0;
      });

  void _showRule(TajweedHit hit) => showModalBottomSheet<void>(
        context: context,
        backgroundColor: _colors.paper,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        builder: (_) => _RuleSheet(
          colors: _colors,
          rule: hit.rule,
          word: hit.word,
          start: hit.start,
          end: hit.end,
          onFollow: () {
            Navigator.pop(context);
            _focusOn(hit.rule);
          },
        ),
      );

  void _showBrowse() => showModalBottomSheet<void>(
        context: context,
        backgroundColor: _colors.paper,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        builder: (_) => _BrowseSheet(
          colors: _colors,
          page: _page,
          counts: _counts,
          tajweed: _tajweed,
          naturalMadd: _naturalMadd,
          onTajweed: (v) {
            _setTajweed(v);
            Navigator.pop(context);
          },
          onNaturalMadd: (v) {
            _setNaturalMadd(v);
            Navigator.pop(context);
          },
          onRule: (rule) {
            Navigator.pop(context);
            _focusOn(rule);
          },
          onSurah: (surah) {
            Navigator.pop(context);
            _controller.jumpToPage(surah.startPage - 1);
            _onPageChanged(surah.startPage - 1);
          },
        ),
      );
}

/// Walks the reader through every occurrence of one rule on the page.
///
/// Tapping a coloured letter stops on the first one and closes the sheet; the
/// arrows move to the next and the previous without reopening anything, and the
/// counter says where the reader is.
class _FocusBar extends StatelessWidget {
  const _FocusBar({
    required this.colors,
    required this.rule,
    required this.index,
    required this.total,
    required this.onPrevious,
    required this.onNext,
    required this.onClose,
  });

  final MushafColors colors;
  final TajweedRule rule;
  final int index;
  final int total;
  final VoidCallback onPrevious;
  final VoidCallback onNext;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final shown = ((index % total) + total) % total + 1;
    final color = colors.tajweedColor(rule);
    // Pinned RTL with arrows that do **not** self-mirror. An auto-mirroring
    // arrow flips with the layout direction, so the one written as "right"
    // draws pointing left — the two buttons then move the opposite way to the
    // way they point.
    return Directionality(
      textDirection: TextDirection.rtl,
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 5),
        decoration: BoxDecoration(
          color: colors.paper,
          borderRadius: BorderRadius.circular(22),
          border: Border.all(color: color.withValues(alpha: 0.55)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.12),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _round(Icons.close, colors.accent, onClose, 17, 'إغلاق'),
            Container(
              width: 11,
              height: 11,
              decoration: BoxDecoration(color: color, shape: BoxShape.circle),
            ),
            const SizedBox(width: 5),
            Text(rule.label,
                style: TextStyle(
                    fontSize: 14, fontWeight: FontWeight.bold, color: color)),
            const SizedBox(width: 7),
            Text('${toArabicNumerals(shown)} من ${toArabicNumerals(total)}',
                style: TextStyle(fontSize: 12.5, color: colors.accent)),
            const SizedBox(width: 3),
            // The mushaf reads right to left, so "previous" points right and
            // "next" points left — the way the eye moves along the line.
            _round(Icons.keyboard_arrow_right, color, onPrevious, 22,
                'الموضع السابق'),
            _round(Icons.keyboard_arrow_left, color, onNext, 22,
                'الموضع التالي'),
          ],
        ),
      ),
    );
  }

  Widget _round(IconData icon, Color tint, VoidCallback onTap, double size,
          String label) =>
      Semantics(
        button: true,
        label: label,
        child: InkResponse(
          onTap: onTap,
          radius: 22,
          child: SizedBox(
            width: 34,
            height: 34,
            child: Icon(icon, color: tint, size: size),
          ),
        ),
      );
}

/// What a tapped letter means, and where the ruling comes from.
class _RuleSheet extends StatelessWidget {
  const _RuleSheet({
    required this.colors,
    required this.rule,
    required this.word,
    required this.start,
    required this.end,
    required this.onFollow,
  });

  final MushafColors colors;
  final TajweedRule rule;
  final String word;
  final int start;
  final int end;
  final VoidCallback onFollow;

  @override
  Widget build(BuildContext context) {
    final color = colors.tajweedColor(rule);
    final a = start.clamp(0, word.length);
    final b = end.clamp(a, word.length);
    return Directionality(
      textDirection: TextDirection.rtl,
      child: DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.72,
        maxChildSize: 0.95,
        builder: (context, scroll) => ListView(
          controller: scroll,
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 30),
          children: [
            Row(children: [
              Container(
                width: 14,
                height: 14,
                decoration: BoxDecoration(
                  color: color,
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(rule.label,
                        style: TextStyle(
                            fontSize: 21,
                            fontWeight: FontWeight.bold,
                            color: color)),
                    Text(rule.family.label,
                        style: TextStyle(fontSize: 12.5, color: colors.accent)),
                  ],
                ),
              ),
              TextButton.icon(
                onPressed: onFollow,
                icon: const Icon(Icons.travel_explore, size: 18),
                label: const Text('تتبّعه'),
              ),
            ]),
            const SizedBox(height: 16),
            // The reader's own word, with the letter carrying the rule picked
            // out — an example they found beats a canned one.
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 12),
              decoration: BoxDecoration(
                color: colors.banner,
                border: Border.all(color: colors.gold),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Text.rich(
                TextSpan(
                  style: TextStyle(
                    fontFamily: mushafFontFamily,
                    package: 'mushaf_text',
                    fontSize: 34,
                    color: colors.ink,
                  ),
                  children: [
                    TextSpan(text: word.substring(0, a)),
                    TextSpan(
                        text: word.substring(a, b),
                        style: TextStyle(color: color)),
                    TextSpan(text: word.substring(b)),
                  ],
                ),
                textAlign: TextAlign.center,
              ),
            ),
            const SizedBox(height: 18),
            _row(colors, 'التعريف', rule.definition),
            _row(colors, 'المقدار', rule.amount),
            _row(colors, 'الحروف', rule.letters),
            _Source(colors: colors, rule: rule),
          ],
        ),
      ),
    );
  }
}

Widget _row(MushafColors colors, String label, String value) => Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 68,
            child: Text(label,
                style: TextStyle(fontSize: 13, color: colors.accent)),
          ),
          Expanded(
            child: Text(value,
                style: TextStyle(fontSize: 15, height: 1.7, color: colors.ink)),
          ),
        ],
      ),
    );

/// The reference a ruling is taken from, in its own words.
///
/// Naming the book alone asks the reader to trust and move on. The verse itself
/// lets them check it against what they memorised or against their teacher, and
/// catch our mistake if we made one — the difference between software that says
/// "trust me" and software that says "check me".
class _Source extends StatelessWidget {
  const _Source({required this.colors, required this.rule});

  final MushafColors colors;
  final TajweedRule rule;

  @override
  Widget build(BuildContext context) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Divider(color: colors.gold.withValues(alpha: 0.4)),
          const SizedBox(height: 8),
          Text('المرجع', style: TextStyle(fontSize: 13, color: colors.accent)),
          const SizedBox(height: 6),
          Text(rule.source,
              style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  height: 1.6,
                  color: colors.ink)),
          const SizedBox(height: 8),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 14),
            decoration: BoxDecoration(
              color: colors.banner.withValues(alpha: 0.6),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              // Verses are separated by ‖ and their two halves by *. Splitting
              // on the first makes it read as poetry rather than one long line.
              rule.evidence.replaceAll(' ‖ ', '\n'),
              textAlign: TextAlign.center,
              style: TextStyle(
                  fontSize: 15,
                  height: 2.0,
                  color: colors.ink.withValues(alpha: 0.9)),
            ),
          ),
        ],
      );
}

/// The surah index and this page's rules behind one button.
///
/// On Android these were two sheets reached from two toolbar icons, and the
/// toolbar got crowded enough to push the page down — which shrinks the text,
/// because the page sizes itself to the height it is given. One button with a
/// bottom switch keeps both a tap away and gives the page its height back.
class _BrowseSheet extends StatefulWidget {
  const _BrowseSheet({
    required this.colors,
    required this.page,
    required this.counts,
    required this.tajweed,
    required this.naturalMadd,
    required this.onTajweed,
    required this.onNaturalMadd,
    required this.onRule,
    required this.onSurah,
  });

  final MushafColors colors;
  final int page;
  final List<MapEntry<TajweedRule, int>> counts;
  final bool tajweed;
  final bool naturalMadd;
  final ValueChanged<bool> onTajweed;
  final ValueChanged<bool> onNaturalMadd;
  final ValueChanged<TajweedRule> onRule;
  final ValueChanged<Surah> onSurah;

  @override
  State<_BrowseSheet> createState() => _BrowseSheetState();
}

class _BrowseSheetState extends State<_BrowseSheet> {
  int _tab = 0;

  @override
  Widget build(BuildContext context) {
    final colors = widget.colors;
    return Directionality(
      textDirection: TextDirection.rtl,
      child: DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.85,
        maxChildSize: 0.95,
        builder: (context, scroll) => Column(
          children: [
            Expanded(
              child:
                  _tab == 0 ? _index(scroll, colors) : _rules(scroll, colors),
            ),
            NavigationBar(
              backgroundColor: colors.paper,
              selectedIndex: _tab,
              height: 62,
              onDestinationSelected: (i) => setState(() => _tab = i),
              destinations: const [
                NavigationDestination(icon: Icon(Icons.list), label: 'الفهرس'),
                NavigationDestination(
                    icon: Icon(Icons.palette_outlined), label: 'الأحكام'),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _index(ScrollController scroll, MushafColors colors) =>
      ListView.builder(
        controller: scroll,
        padding: const EdgeInsets.fromLTRB(12, 14, 12, 8),
        itemCount: Quran.surahs.length,
        itemBuilder: (context, i) {
          final s = Quran.surahs[i];
          return ListTile(
            dense: true,
            leading: Text(toArabicNumerals(s.number),
                style: TextStyle(color: colors.accent)),
            title: Text(s.nameArabic, style: TextStyle(color: colors.ink)),
            trailing: Text('ص ${toArabicNumerals(s.startPage)}',
                style: TextStyle(fontSize: 12, color: colors.accent)),
            onTap: () => widget.onSurah(s),
          );
        },
      );

  Widget _rules(ScrollController scroll, MushafColors colors) {
    final counts = widget.counts;
    return ListView(
      controller: scroll,
      padding: const EdgeInsets.fromLTRB(20, 14, 20, 8),
      children: [
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          title: const Text('تلوين الأحكام'),
          value: widget.tajweed,
          onChanged: widget.onTajweed,
        ),
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          title: const Text('المدّ الطبيعي'),
          subtitle: const Text('أكثر حكم تكرارًا — تلوينه يصبغ الصفحة'),
          value: widget.naturalMadd,
          onChanged: widget.tajweed ? widget.onNaturalMadd : null,
        ),
        const Divider(),
        Padding(
          padding: const EdgeInsets.only(top: 10, bottom: 4),
          child: Text('أحكام صفحة ${toArabicNumerals(widget.page)}',
              style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.bold,
                  color: colors.ink)),
        ),
        if (!widget.tajweed)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 18),
            child: Text('شغّل تلوين الأحكام لتظهر هنا',
                style: TextStyle(fontSize: 13, color: colors.accent)),
          )
        else if (counts.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 18),
            child: Text('لا أحكام على هذه الصفحة',
                style: TextStyle(fontSize: 13, color: colors.accent)),
          )
        else
          // Tapping a rule starts the walk through its positions on this page,
          // which is why the count is here and not just a colour key.
          for (final e in counts)
            ListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              leading: Container(
                width: 12,
                height: 12,
                decoration: BoxDecoration(
                  color: colors.tajweedColor(e.key),
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
              title: Text(e.key.label, style: TextStyle(color: colors.ink)),
              trailing: Text('${toArabicNumerals(e.value)} موضع',
                  style: TextStyle(fontSize: 12.5, color: colors.accent)),
              onTap: () => widget.onRule(e.key),
            ),
        const SizedBox(height: 14),
        Divider(color: colors.gold.withValues(alpha: 0.4)),
        Padding(
          padding: const EdgeInsets.only(top: 12),
          child: Text(
            'مواضع الأحكام مستخرجة من رسم المصحف نفسه، ومقابَلة على تحفة '
            'الأطفال والمقدمة الجزرية. المس أي حرف ملوّن لترى الحكم ومرجعه.',
            style: TextStyle(fontSize: 12, height: 1.8, color: colors.accent),
          ),
        ),
      ],
    );
  }
}

/// Page number and a jump slider.
class _Bar extends StatelessWidget {
  const _Bar({required this.colors, required this.page, required this.onJump});

  final MushafColors colors;
  final int page;
  final void Function(int) onJump;

  @override
  Widget build(BuildContext context) {
    return Directionality(
      textDirection: TextDirection.rtl,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        decoration: BoxDecoration(
          color: colors.paper,
          border:
              Border(top: BorderSide(color: colors.gold.withValues(alpha: 0.5))),
        ),
        child: Row(
          children: [
            Expanded(
              child: Slider(
                value: page.toDouble(),
                min: 1,
                max: Quran.pageCount.toDouble(),
                onChanged: (v) => onJump(v.round()),
              ),
            ),
            Text(
              '${toArabicNumerals(page)} / ${toArabicNumerals(Quran.pageCount)}',
              style: TextStyle(fontSize: 12, color: colors.accent),
            ),
          ],
        ),
      ),
    );
  }
}
