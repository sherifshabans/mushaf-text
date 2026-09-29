import 'tajweed_rule.dart';

/// Finds the tajweed rules in an ayah of the bundled text.
///
/// ## Where the rules come from
/// Not from an imported dataset. The published ones (`cpfair/quran-tajweed` and
/// friends) index into the **Tanzil** Uthmani text while this package carries
/// the **KFGQPC** one, and the two differ in how they encode the marks
/// themselves — sukun `U+06E1` here against `U+0652` there, and a stacked
/// tanween written as `U+0656`/`U+0657`/`U+065E`. Applying their offsets
/// unchanged slides the colours onto the wrong letters.
///
/// The KFGQPC script already encodes these rules in its own orthography — that
/// is what it is printed for. Checked across all 6236 ayahs:
///
/// | written as | means | count |
/// |---|---|---|
/// | noon with a sukun, or a side-by-side tanween | **izhar** (left uncoloured) | 1716 / 1911 |
/// | a bare noon, or a stacked tanween | ikhfa or idgham | 5139 / 6643 |
/// | a small high or low meem | **iqlab** | 609 |
/// | a maddah `U+0653` | a madd longer than two counts | 5652 |
/// | a rectangular zero `U+06E0` | a letter that is not pronounced | 66 |
/// | a small waw or yeh | **madd silah** | 2213 |
///
/// The letter that follows settles the rest: **shadda** means a complete idgham
/// (lam and ra without ghunnah, noon and meem with it); a bare **yeh or waw**
/// means an incomplete idgham with ghunnah, which is how Hafs writes it — those
/// two never take a shadda; and any of the fifteen means ikhfa. Verified: once
/// the alef that seats a tanween is skipped, the letters that actually occur
/// are exactly the ikhfa set plus "waw" and "yeh", and nothing else.
///
/// ## Stopping
/// Each ayah is analysed on its own. Its closing rosette is a stop, so no rule
/// carries over into the next one — and that stop is what produces the
/// **madd arid**.
///
/// This is a direct port of the Kotlin implementation, and
/// `test/tajweed_parity_test.dart` holds it to producing identical spans for
/// every ayah in the mushaf.
abstract final class TajweedAnnotator {
  // ── marks ────────────────────────────────────────────────────────────────
  static const int _shadda = 0x0651;
  static const int _maddah = 0x0653;
  static const int _supAlef = 0x0670; // dagger alef
  static const int _subscriptAlef = 0x0656; // stacked kasratan
  static const int _invertedDamma = 0x0657; // stacked fathatan
  static const int _fathaTwoDots = 0x065E; // stacked dammatan
  static const int _hamzaAbove = 0x0654;
  static const int _hamzaBelow = 0x0655;
  static const int _rectZero = 0x06E0; // letter that is not pronounced
  static const int _smallWaw = 0x06E5;
  static const int _smallYeh = 0x06E6;
  static const int _alefWasla = 0x0671;
  static const int _damma = 0x064F;
  static const int _kasra = 0x0650;

  static const List<int> _sukuns = [0x0652, 0x06E1];

  /// The stacked tanween — how ikhfa, idgham and iqlab are written.
  static const List<int> _stackedTanween = [
    _subscriptAlef,
    _invertedDamma,
    _fathaTwoDots,
  ];

  /// The side-by-side tanween — how izhar is written.
  static const List<int> _openTanween = [0x064B, 0x064C, 0x064D];

  /// The small meem: the iqlab sign.
  static const List<int> _iqlabMarks = [0x06E2, 0x06ED];

  static const String _ikhfaLetters = 'صذثكجشقسدطزفتضظ';
  static const String _shamsiLetters = 'تثدذرزسشصضطظلن';
  static const String _qalqalaLetters = 'قطبجد';

  /// Hamzat **al-qat'** only.
  ///
  /// The connecting alef (`ٱ`) is deliberately **out**: it is not a glottal
  /// stop, so it causes no madd. «ٱهۡدِنَا ٱلصِّرَٰطَ» holds no separate madd —
  /// the alef drops in connected speech. While it was in this set the checker
  /// reported 4533 phantom munfasil madds, the first of them in al-Fatihah 6.
  static const String _hamzaLetters = 'ءأإؤئ';

  static bool _isMark(int c) =>
      (c >= 0x064B && c <= 0x065F) ||
      c == _supAlef ||
      (c >= 0x06D6 && c <= 0x06ED) ||
      c == 0x0640;

  static bool _isSpace(int c) => c == 0x20 || c == 0x00A0;

  static bool _isDigit(int c) => c >= 0x0660 && c <= 0x0669;

  /// Pause, sajdah and rub' signs — marks in their own right, not harakat on a
  /// letter.
  ///
  /// They stay in ink: they are no part of the rule, and colouring them makes a
  /// pause sign look like it belongs to the letter before it. It showed on the
  /// first render — «غِشَٰوَةٌ» carried its pause sign in the idgham colour.
  static bool _isPauseSign(int c) =>
      (c >= 0x06D6 && c <= 0x06DC) ||
      c == 0x06DE ||
      (c >= 0x06E9 && c <= 0x06EC);

  // ── walking the text ─────────────────────────────────────────────────────

  /// The marks attached to the letter at [i].
  static String _marksAfter(String t, int i) {
    final b = StringBuffer();
    var j = i + 1;
    while (j < t.length && _isMark(t.codeUnitAt(j))) {
      b.writeCharCode(t.codeUnitAt(j));
      j++;
    }
    return b.toString();
  }

  /// End of the cluster at [i] — the letter and its harakat, without pause signs.
  static int _clusterEnd(String t, int i) {
    var j = i + 1;
    while (j < t.length &&
        _isMark(t.codeUnitAt(j)) &&
        !_isPauseSign(t.codeUnitAt(j))) {
      j++;
    }
    return j;
  }

  /// The first real letter after [i], or `-1`. Marks, spaces and digits are skipped.
  static int _nextLetter(String t, int i) {
    var j = i + 1;
    while (j < t.length) {
      final c = t.codeUnitAt(j);
      if (!_isMark(c) && !_isSpace(c) && !_isDigit(c)) return j;
      j++;
    }
    return -1;
  }

  static int _prevLetter(String t, int i) {
    var j = i - 1;
    while (j >= 0) {
      final c = t.codeUnitAt(j);
      if (!_isMark(c) && !_isSpace(c) && !_isDigit(c)) return j;
      j--;
    }
    return -1;
  }

  static bool _hasShadda(String t, int i) =>
      _marksAfter(t, i).codeUnits.contains(_shadda);

  static bool _hasSukun(String t, int i) =>
      _marksAfter(t, i).codeUnits.any(_sukuns.contains);

  static bool _isBare(String t, int i) => _marksAfter(t, i).isEmpty;

  /// Is there a space between [i] and [j]? Then they sit in different words.
  static bool _crossesWord(String t, int i, int j) {
    for (var k = i + 1; k < j; k++) {
      if (_isSpace(t.codeUnitAt(k))) return true;
    }
    return false;
  }

  /// Is the letter at [i] a letter of prolongation?
  ///
  /// An alef or a dagger alef always; a waw after a dammah and a yeh after a
  /// kasrah — and both of those must carry no vowel of their own.
  static bool _isMaddLetter(String t, int i) {
    final c = t[i];
    if (t.codeUnitAt(i) == _supAlef) return true;
    if (c == 'ا' && _isBare(t, i)) return true;
    if (!_isBare(t, i)) return false;
    switch (c) {
      case 'ى':
        return true;
      case 'و':
      case 'ي':
        final p = _prevLetter(t, i);
        if (p < 0) return false;
        final pm = _marksAfter(t, p).codeUnits;
        return c == 'و' ? pm.contains(_damma) : pm.contains(_kasra);
      default:
        return false;
    }
  }

  /// A hamza can be written as a letter or as a mark over a tatweel — both count.
  static bool _isHamzaAt(String t, int i) =>
      _hamzaLetters.contains(t[i]) ||
      _marksAfter(t, i)
          .codeUnits
          .any((m) => m == _hamzaAbove || m == _hamzaBelow);

  // ── the rules ────────────────────────────────────────────────────────────

  /// The rules in one ayah.
  ///
  /// [text] is the verse without its number. [includeNaturalMadd] is off by
  /// default because the natural madd is by far the most frequent rule and
  /// tints the whole page.
  ///
  /// The spans come back sorted by [TajweedSpan.start] and never overlap.
  static List<TajweedSpan> annotate(
    String text, {
    bool includeNaturalMadd = false,
  }) {
    if (text.isEmpty) return const [];
    final rules = List<TajweedRule?>.filled(text.length, null);

    /// Puts a rule on a letter and its marks, without overwriting one already there.
    void mark(int i, TajweedRule rule) {
      if (i < 0 || i >= text.length || rules[i] != null) return;
      final end = _clusterEnd(text, i);
      for (var k = i; k < end; k++) {
        rules[k] ??= rule;
      }
    }

    // 1) what is not pronounced, first, so no other rule claims it
    for (var i = 0; i < text.length; i++) {
      final c = text.codeUnitAt(i);
      if (c == _rectZero) {
        rules[i] = TajweedRule.silent;
        final p = _prevLetter(text, i);
        if (p >= 0) {
          for (var k = p; k < i; k++) {
            rules[k] ??= TajweedRule.silent;
          }
        }
      } else if (c == _alefWasla) {
        mark(i, TajweedRule.hamzatWasl);
        // solar lam: alef wasla + lam + a doubled solar letter ⇒ the lam is silent
        final lam = i + 1;
        if (lam < text.length && text[lam] == 'ل') {
          final k = _nextLetter(text, lam);
          if (k > 0 && _shamsiLetters.contains(text[k]) && _hasShadda(text, k)) {
            mark(lam, TajweedRule.lamShamsiyya);
          }
        }
      }
    }

    // 2) madd silah — the small waw and yeh
    for (var i = 0; i < text.length; i++) {
      final c = text.codeUnitAt(i);
      if (c == _smallWaw || c == _smallYeh) {
        rules[i] = TajweedRule.maddSila;
        final p = _prevLetter(text, i);
        if (p >= 0 && rules[p] == null) mark(p, TajweedRule.maddSila);
      }
    }

    // 3) noon and tanween, meem, ghunnah, qalqalah
    for (var i = 0; i < text.length; i++) {
      final code = text.codeUnitAt(i);
      if (_isMark(code) || _isSpace(code) || _isDigit(code)) continue;
      if (rules[i] != null) continue;
      final c = text[i];
      final ms = _marksAfter(text, i).codeUnits;

      // iqlab is written out, with the small meem
      if (ms.any(_iqlabMarks.contains)) {
        mark(i, TajweedRule.iqlab);
        continue;
      }

      if ((c == 'ن' || c == 'م') && ms.contains(_shadda)) {
        mark(i, TajweedRule.ghunna);
        continue;
      }

      final openTanween = ms.any(_openTanween.contains);
      final stacked = ms.any(_stackedTanween.contains);
      final bareNoon = c == 'ن' && ms.isEmpty;
      if (stacked || bareNoon) {
        var k = _nextLetter(text, i);
        // a stacked tanween is written before the alef that seats it — skip it
        if (stacked &&
            k > 0 &&
            (text[k] == 'ا' || text[k] == 'ى') &&
            _isBare(text, k)) {
          k = _nextLetter(text, k);
        }
        if (k > 0) {
          final f = text[k];
          TajweedRule? rule;
          if (f == 'ب') {
            rule = TajweedRule.iqlab;
          } else if (_hasShadda(text, k) && (f == 'ل' || f == 'ر')) {
            rule = TajweedRule.idghamNoGhunna;
          } else if (_hasShadda(text, k) && (f == 'ن' || f == 'م')) {
            rule = TajweedRule.idghamGhunna;
          } else if (f == 'ي' || f == 'و') {
            // incomplete idgham: Hafs never doubles the yeh or the waw here
            rule = TajweedRule.idghamGhunna;
          } else if (_ikhfaLetters.contains(f)) {
            rule = TajweedRule.ikhfa;
          }
          if (rule != null) {
            mark(i, rule);
            continue;
          }
        }
      }
      if (openTanween) continue; // izhar — no colour

      // a silent meem: ikhfa or idgham shafawi.
      //
      // The meem is left **bare** in the KFGQPC script, exactly like the noon;
      // a sukun is drawn on it only for izhar shafawi. Checked: the meem before
      // a beh is bare in 481 places and the one before a doubled meem in 822,
      // and not one of them carries a sukun. Requiring one made both rules
      // score zero — which is what happened on the first run.
      if (c == 'م' && ms.isEmpty) {
        final k = _nextLetter(text, i);
        if (k > 0) {
          if (text[k] == 'ب') {
            mark(i, TajweedRule.ikhfaShafawi);
            continue;
          }
          if (text[k] == 'م' && _hasShadda(text, k)) {
            mark(i, TajweedRule.idghamShafawi);
            continue;
          }
        }
      }

      if (_qalqalaLetters.contains(c) && _hasSukun(text, i)) {
        mark(i, TajweedRule.qalqala);
        continue;
      }
    }

    // 4) the madds written with a maddah
    for (var i = 0; i < text.length; i++) {
      if (_isMark(text.codeUnitAt(i)) || rules[i] != null) continue;
      if (!_marksAfter(text, i).codeUnits.contains(_maddah)) continue;
      // a hamza carrying the maddah ⇒ madd badal, two counts, as in «ٱلۡأٓخِرَة»
      if (_hamzaLetters.contains(text[i])) {
        mark(i, TajweedRule.maddBadal);
        continue;
      }
      final k = _nextLetter(text, i);
      final TajweedRule rule;
      if (k < 0) {
        rule = TajweedRule.maddLazim;
      } else if (_hasShadda(text, k) || _hasSukun(text, k)) {
        rule = TajweedRule.maddLazim;
      } else if (_isHamzaAt(text, k)) {
        rule = _crossesWord(text, i, k)
            ? TajweedRule.maddMunfasil
            : TajweedRule.maddMuttasil;
      } else {
        // the opening letters of a surah: a maddah on a disjoined letter
        rule = TajweedRule.maddLazim;
      }
      mark(i, rule);
    }

    // 5) a separated madd with no maddah drawn: a letter of prolongation
    //    ending a word, with a hamza opening the next
    for (var i = 0; i < text.length; i++) {
      if (rules[i] != null) continue;
      final code = text.codeUnitAt(i);
      if (_isMark(code) || _isSpace(code)) continue;
      if (!_isMaddLetter(text, i)) continue;
      final k = _nextLetter(text, i);
      if (k > 0 && _isHamzaAt(text, k) && _crossesWord(text, i, k)) {
        mark(i, TajweedRule.maddMunfasil);
      }
    }

    // 6) madd arid — the ayah ends in a stop, so its last letter falls silent
    final last = _prevLetter(text, text.length);
    if (last >= 0) {
      final p = _prevLetter(text, last);
      if (p >= 0 && rules[p] == null && _isMaddLetter(text, p)) {
        mark(p, TajweedRule.maddArid);
      }
      // qalqalah at a stop: a final letter of "qutb jad" is bounced
      if (rules[last] == null &&
          _qalqalaLetters.contains(text[last]) &&
          _isBare(text, last)) {
        mark(last, TajweedRule.qalqala);
      }
    }

    // 7) the natural madd — every letter of prolongation left over
    if (includeNaturalMadd) {
      for (var i = 0; i < text.length; i++) {
        if (rules[i] != null) continue;
        final code = text.codeUnitAt(i);
        if (_isMark(code) || _isSpace(code)) continue;
        if (_isMaddLetter(text, i)) mark(i, TajweedRule.maddNatural);
      }
    }

    return _toSpans(rules);
  }

  /// Merges neighbouring slots that carry the same rule into one span.
  static List<TajweedSpan> _toSpans(List<TajweedRule?> rules) {
    final out = <TajweedSpan>[];
    var i = 0;
    while (i < rules.length) {
      final r = rules[i];
      if (r == null) {
        i++;
        continue;
      }
      var j = i;
      while (j < rules.length && rules[j] == r) {
        j++;
      }
      out.add(TajweedSpan(i, j, r));
      i = j;
    }
    return out;
  }
}
