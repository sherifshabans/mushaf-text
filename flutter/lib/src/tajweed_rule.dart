import 'package:flutter/painting.dart';

/// The family a rule belongs to — used only to group the colour key.
enum TajweedFamily {
  ghunna('الغنّة', 'Ghunnah'),
  noon('أحكام النون الساكنة والتنوين', 'Noon Sakinah & Tanween'),
  meem('أحكام الميم الساكنة', 'Meem Sakinah'),
  qalqala('القلقلة', 'Qalqalah'),
  madd('المدود', 'Madd'),
  mute('ما لا يُنطق', 'Not pronounced');

  const TajweedFamily(this.label, this.englishName);

  /// Arabic label.
  final String label;
  final String englishName;
}

/// A tajweed rule, its colour, and how to explain it.
///
/// **The declaration order is load-bearing.** The parity fixture that pins this
/// port to the Kotlin implementation stores rules by ordinal, so reordering
/// these values silently invalidates it. Add new rules at the end.
///
/// The colours are a proposal, not a reproduction of any printed mushaf. The
/// madd rules run along a length ramp (green → olive → orange → red) so the
/// reader learns them by shade; the noon and meem rules are scattered across
/// the wheel on purpose, because two of them often land in the same word and
/// two shades of one hue would lose the difference at reading size.
enum TajweedRule {
  ghunna(
    color: Color(0xFFA81D5D),
    label: 'غنّة مشدّدة',
    englishName: 'Ghunnah',
    family: TajweedFamily.ghunna,
    definition: 'النون والميم المشدّدتان تُغنّان — صوت يخرج من الخيشوم.',
    amount: 'حركتان',
    letters: 'نّ  مّ',
  ),

  ikhfa(
    color: Color(0xFF7A3DAE),
    label: 'إخفاء حقيقي',
    englishName: 'Ikhfa',
    family: TajweedFamily.noon,
    definition: 'نون ساكنة أو تنوين بعدها أحد حروف الإخفاء، '
        'فتُخفى النون بين الإظهار والإدغام مع الغنّة.',
    amount: 'حركتان',
    letters: 'ص ذ ث ك ج ش ق س د ط ز ف ت ض ظ',
  ),
  idghamGhunna(
    color: Color(0xFFB5179E),
    label: 'إدغام بغنّة',
    englishName: 'Idgham with Ghunnah',
    family: TajweedFamily.noon,
    definition: 'نون ساكنة أو تنوين بعدها أحد حروف «يَنْمُو»، '
        'فتُدغم النون في الحرف مع بقاء الغنّة.',
    amount: 'حركتان',
    letters: 'ي ن م و',
  ),
  idghamNoGhunna(
    color: Color(0xFF0F766E),
    label: 'إدغام بغير غنّة',
    englishName: 'Idgham without Ghunnah',
    family: TajweedFamily.noon,
    definition: 'نون ساكنة أو تنوين بعدها لام أو راء، فتُدغم النون فيها بلا غنّة.',
    amount: 'بلا غنّة',
    letters: 'ل ر',
  ),
  iqlab(
    color: Color(0xFF92400E),
    label: 'إقلاب',
    englishName: 'Iqlab',
    family: TajweedFamily.noon,
    definition: 'نون ساكنة أو تنوين بعدها باء، '
        'فتُقلب النون ميمًا مخفاة عند الباء مع الغنّة.',
    amount: 'حركتان',
    letters: 'ب',
  ),

  ikhfaShafawi(
    color: Color(0xFFC04A8A),
    label: 'إخفاء شفوي',
    englishName: 'Ikhfa Shafawi',
    family: TajweedFamily.meem,
    definition: 'ميم ساكنة بعدها باء، فتُخفى الميم عند الباء مع الغنّة — '
        'والشفتان لا تنطبقان انطباقًا تامًّا.',
    amount: 'حركتان',
    letters: 'ب',
  ),
  idghamShafawi(
    color: Color(0xFF5B21B6),
    label: 'إدغام شفوي',
    englishName: 'Idgham Shafawi',
    family: TajweedFamily.meem,
    definition: 'ميم ساكنة بعدها ميم، فتُدغم الأولى في الثانية مع الغنّة '
        '(إدغام متماثلين صغير).',
    amount: 'حركتان',
    letters: 'م',
  ),

  qalqala(
    color: Color(0xFF8A6A12),
    label: 'قلقلة',
    englishName: 'Qalqalah',
    family: TajweedFamily.qalqala,
    definition: 'حروف «قُطْبُ جَدٍّ» إذا سكنت، يهتزّ المخرج عند النطق بها '
        'فيُسمع لها نبرة.',
    amount: 'نبرة خفيفة',
    letters: 'ق ط ب ج د',
  ),

  maddNatural(
    color: Color(0xFF3E8E6E),
    label: 'مدّ طبيعي',
    englishName: 'Natural Madd',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ لا يأتي بعده همزة ولا سكون — '
        'وهو أصل المدّ الذي لا تقوم ذات الحرف دونه.',
    amount: 'حركتان',
    letters: 'ا و ي',
  ),
  maddMuttasil(
    color: Color(0xFF1B6E4B),
    label: 'مدّ واجب متّصل',
    englishName: 'Obligatory Madd',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ بعده همزة في الكلمة نفسها، فالمدّ واجب.',
    amount: '٤ أو ٥ حركات',
    letters: 'مدّ + ء',
  ),
  maddMunfasil(
    color: Color(0xFF6E8B18),
    label: 'مدّ جائز منفصل',
    englishName: 'Permissible Madd',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ في آخر الكلمة وبعده همزة في أول الكلمة التالية.',
    amount: '٤ أو ٥ حركات',
    letters: 'مدّ | ء',
  ),
  maddLazim(
    color: Color(0xFFB3261E),
    label: 'مدّ لازم',
    englishName: 'Necessary Madd',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ بعده سكون أصلي أو حرف مشدّد، فيلزم إشباع المدّ. '
        'ويدخل فيه المدّ اللازم الحرفي في فواتح السور.',
    amount: '٦ حركات',
    letters: 'مدّ + سكون أو شدّة',
  ),
  maddBadal(
    color: Color(0xFF57A07C),
    label: 'مدّ بدل',
    englishName: 'Madd Badal',
    family: TajweedFamily.madd,
    definition: 'همزة بعدها حرف مدّ، فتَقدَّمت الهمزة على المدّ — '
        'أصله همزتان أُبدلت الثانية مدًّا.',
    amount: 'حركتان',
    letters: 'ء + مدّ',
  ),
  maddSila(
    color: Color(0xFF2C7F6A),
    label: 'مدّ صلة',
    englishName: 'Madd Silah',
    family: TajweedFamily.madd,
    definition: 'هاء الضمير بين متحرّكين تُوصَل بواو أو ياء صغيرة — '
        'صغرى بحركتين، وكبرى إذا جاءت بعدها همزة.',
    amount: 'حركتان، أو ٤–٥ مع الهمزة',
    letters: 'ـهُۥ  ـهِۦ',
  ),
  maddArid(
    color: Color(0xFFD97706),
    label: 'مدّ عارض للسكون',
    englishName: 'Madd Arid',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ بعده حرف يُسكَّن من أجل الوقف، فالسكون عارض غير أصلي.',
    amount: '٢ أو ٤ أو ٦ حركات',
    letters: 'مدّ + حرف موقوف',
  ),

  hamzatWasl(
    color: Color(0xFF2F6FA8),
    label: 'همزة وصل',
    englishName: 'Hamzat Wasl',
    family: TajweedFamily.mute,
    definition: 'همزة تُنطق إذا بدأت بها، وتسقط من اللفظ إذا وصلتها بما قبلها.',
    amount: 'تسقط في الوصل',
    letters: 'ٱ',
  ),
  lamShamsiyya(
    color: Color(0xFF4E8CC0),
    label: 'لام شمسية',
    englishName: 'Solar Lam',
    family: TajweedFamily.mute,
    definition: 'لام «الـ» لا تُنطق، ويُدغم ما بعدها فيُنطق مشدّدًا.',
    amount: 'لا تُنطق',
    letters: 'ت ث د ذ ر ز س ش ص ض ط ظ ل ن',
  ),
  silent(
    color: Color(0xFF2F6FA8),
    label: 'حرف لا يُنطق',
    englishName: 'Silent Letter',
    family: TajweedFamily.mute,
    definition:
        'حرف مرسوم في المصحف غير ملفوظ، وعليه الصفر المستدير علامةً على ذلك.',
    amount: 'لا يُنطق',
    letters: 'ا و ي',
  );

  const TajweedRule({
    required this.color,
    required this.label,
    required this.englishName,
    required this.family,
    required this.definition,
    required this.amount,
    required this.letters,
  });

  final Color color;

  /// Arabic name, as tajweed books give it.
  final String label;
  final String englishName;
  final TajweedFamily family;
  final String definition;

  /// How long to hold it, or how it is pronounced.
  final String amount;

  /// The letters the rule applies to.
  final String letters;

  /// The natural madd alone.
  ///
  /// It is the most frequent rule in the mushaf by a wide margin (about 32,000
  /// positions), so colouring it tints most of the page and buries everything
  /// else — hence its own switch.
  bool get isNaturalMadd => this == TajweedRule.maddNatural;
}

/// A rule over `[start, end)` in UTF-16 code units of an ayah's text.
///
/// The range covers the letter **together with its marks**: colouring a letter
/// and leaving its harakah in ink gives a two-tone letter, which looks worse
/// than no colour at all.
class TajweedSpan {
  const TajweedSpan(this.start, this.end, this.rule);

  final int start;
  final int end;
  final TajweedRule rule;

  int get length => end - start;

  @override
  bool operator ==(Object other) =>
      other is TajweedSpan &&
      other.start == start &&
      other.end == end &&
      other.rule == rule;

  @override
  int get hashCode => Object.hash(start, end, rule);

  @override
  String toString() => 'TajweedSpan($start, $end, ${rule.name})';
}
