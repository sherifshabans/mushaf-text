import 'package:flutter/painting.dart';

/// The family a rule belongs to — used only to group the colour key.
enum TajweedFamily {
  ghunna('الغنّة', 'Ghunnah'),
  noon('أحكام النون الساكنة والتنوين', 'Noon Sakinah & Tanween'),
  meem('أحكام الميم الساكنة', 'Meem Sakinah'),
  idgham('المتماثلان والمتجانسان والمتقاربان', 'Mutamathilayn, Mutajanisayn & Mutaqaribayn'),
  qalqala('القلقلة', 'Qalqalah'),
  tafkhim('التفخيم والترقيق', 'Tafkhim & Tarqiq'),
  madd('المدود', 'Madd'),
  mute('ما لا يُنطق كاملًا', 'Not fully pronounced');

  const TajweedFamily(this.label, this.englishName);

  /// Arabic label.
  final String label;
  final String englishName;
}

/// أحكام التجويد المعروضة على صفحة المصحف: لون لكل حكم، واسمه وشرحه ومرجعه.
///
/// ## مولّد — لا تعدّل هذا الملف بيدك
/// هذا الملف يولّده `tools/gen_tajweed_rules.py` في مستودع mushaf-text. عدّل
/// الجدول هناك ثم أعد التوليد، وإلا افترقت نسخة أندرويد عن نسخة فلاتر.
///
/// ## المراجع
/// كل حكم يحمل مصدره في `source` ونصَّ مرجعه في `evidence`:
/// تحفة الأطفال للجمزوري لأحكام النون والميم ولام «أل» والمدود، والمقدمة الجزرية
/// لابن الجزري للقلقلة والغنّة، والتعريف بمصحف المدينة النبوية لعلامات الضبط.
/// الرواية حفص عن عاصم من طريق الشاطبية.
///
/// ثلاثة أحكام ليست في المتنين — مدّ الصلة وهمزة الوصل والألف الزائدة — ومصدرها
/// علامات ضبط المصحف، وهذا منصوص عليه في `source` لكل واحد منها.
///
/// ## ليه لون لكل حكم لا لون لكل عائلة
/// تجميع أحكام النون كلها في لون واحد يوفّر ألوانًا لكنه يضيّع المعلومة: القارئ
/// يريد أن يعرف أن هذا **إخفاء** لا **إدغام**، لأن النطق مختلف. فكل حكم بلونه،
/// والشرح باللمس يغني القارئ عن حفظ المفتاح.
///
/// ## الألوان
/// المدود على سلّم **الطول**: أخضر (حركتان) ← زيتوني/أخضر غامق (٤–٥) ← برتقالي
/// (عارض) ← أحمر (٦ حركات). وأحكام النون والميم متفرّقة على عجلة الألوان عمدًا لا
/// متدرّجة في عائلة واحدة، لأن حكمين منها كثيرًا ما يقعان في كلمة واحدة، فدرجتان
/// من لون واحد يضيع الفرق بينهما في حجم القراءة.
///
/// وألوان التفخيم اختيرت بالقياس لا بالنظر: لونها الأول كان على بُعد ٢١ وحدة من
/// لون الحبر فلم يكن يُرى أصلًا. فصار الشرط أن يبعد كل لون عن الحبر وعن الورق، وأن
/// يبعد عن كل حكم آخر، وأن يبقى في سجلّ الإضاءة والإشباع نفسه — والتفخيم خاصةً
/// مكتوم، لأنه يقع على كل سطر تقريبًا.
///
/// الألوان اجتهاد في العرض، وليست نقلًا عن مصحف ملوّن مطبوع.
///
/// ## الترتيب
/// **ترتيب التعريف محمول.** فيكسشر التطابق الذي يثبّت نسخة دارت على
/// نسخة كوتلن يخزّن الأحكام برقمها الترتيبي، فإعادة ترتيبها تبطل الفيكسشر بصمت.
/// أي حكم جديد يُضاف في الآخر.
enum TajweedRule {

  // ── الغنّة
  ghunna(
    color: Color(0xFFA81D5D),
    label: 'غنّة مشدّدة',
    englishName: 'Ghunnah',
    family: TajweedFamily.ghunna,
    definition: 'النون والميم المشدّدتان تُغنّان — صوت يخرج من الخيشوم.',
    amount: 'حركتان',
    letters: 'نّ  مّ',
    source: 'تحفة الأطفال ١٧ • المقدمة الجزرية',
    evidence: 'وَغُنَّ مِيماً ثُمَّ نُوناً شُدِّدَا * وَسَمِّ كُلاً حَرْفَ غُنَّةٍ بَدَا ‖ وَأَظْهِرِ الغُنَّةَ مِنْ نُونٍ وَمِنْ * مِيمٍ إِذَا مَا شُدِّدَا',
  ),

  // ── أحكام النون الساكنة والتنوين
  ikhfa(
    color: Color(0xFF7A3DAE),
    label: 'إخفاء حقيقي',
    englishName: 'Ikhfa',
    family: TajweedFamily.noon,
    definition: 'نون ساكنة أو تنوين بعدها أحد حروف الإخفاء الخمسة عشر، فتُخفى النون بين الإظهار والإدغام مع الغنّة.',
    amount: 'حركتان',
    letters: 'ص ذ ث ك ج ش ق س د ط ز ف ت ض ظ',
    source: 'تحفة الأطفال ١٤ و١٦',
    evidence: 'وَالرَّابِعُ الإِخْفَاءُ عِنْدَ الْفاضِلِ * مِنَ الحُرُوفِ وَاجِبٌ لِلْفَاضِلِ ‖ صِفْ ذَا ثَنَا كَمْ جَادَ شَخْصٌ قَدْ سمَا * دُمْ طَيَّباً زِدْ فِي تُقَىً ضَعْ ظَالِمَا',
  ),
  idghamGhunna(
    color: Color(0xFFB5179E),
    label: 'إدغام بغنّة',
    englishName: 'Idgham with Ghunnah',
    family: TajweedFamily.noon,
    definition: 'نون ساكنة أو تنوين بعدها أحد حروف «يَنْمُو»، فتُدغم النون في الحرف مع بقاء الغنّة. وهو ناقص في الياء والواو (تبقى الغنّة ولا يكتمل التشديد)، كامل في النون والميم. فإن اجتمعا في كلمة واحدة فلا إدغام بل إظهار مطلق، كـ«دُنْيَا» و«صِنْوَانٍ».',
    amount: 'حركتان',
    letters: 'ي ن م و',
    source: 'تحفة الأطفال ١٠ و١١',
    evidence: 'لَكِنَّهَا قِسْمَانِ قِسْمٌ يُدْغَمَا * فِيهِ بِغُنَّةٍ بِيَنْمُو عُلِمَا ‖ إِلاَّ إِذَا كَانَا بِكِلْمَةٍ فَلاَ * تُدْغِمْ كَدُنْيَا ثُمَّ صِنْوَانٍ تَلاَ',
  ),
  idghamNoGhunna(
    color: Color(0xFF0F766E),
    label: 'إدغام بغير غنّة',
    englishName: 'Idgham without Ghunnah',
    family: TajweedFamily.noon,
    definition: 'نون ساكنة أو تنوين بعدها لام أو راء، فتُدغم النون فيها بلا غنّة.',
    amount: 'بلا غنّة',
    letters: 'ل ر',
    source: 'تحفة الأطفال ١٢',
    evidence: 'وَالثَّاني إِدْغَامٌ بِغَيْرِ غُنَّةْ * في اللاَّمِ وَالرَّا ثُمَّ كَرّرَنَّهْ',
  ),
  iqlab(
    color: Color(0xFF92400E),
    label: 'إقلاب',
    englishName: 'Iqlab',
    family: TajweedFamily.noon,
    definition: 'نون ساكنة أو تنوين بعدها باء، فتُقلب النون ميمًا مخفاة عند الباء مع الغنّة. وعلامته في المصحف ميم صغيرة فوق النون أو تحتها.',
    amount: 'حركتان',
    letters: 'ب',
    source: 'تحفة الأطفال ١٣',
    evidence: 'وَالثَالثُ الإِقْلاَبُ عِنْدَ الْبَاءِ * مِيماً بِغُنَّةٍ مَعَ الإِخْفَاءِ',
  ),

  // ── أحكام الميم الساكنة
  ikhfaShafawi(
    color: Color(0xFFC04A8A),
    label: 'إخفاء شفوي',
    englishName: 'Ikhfa Shafawi',
    family: TajweedFamily.meem,
    definition: 'ميم ساكنة بعدها باء، فتُخفى الميم عند الباء مع الغنّة — والشفتان لا تنطبقان انطباقًا تامًّا.',
    amount: 'حركتان',
    letters: 'ب',
    source: 'تحفة الأطفال ٢٠ • المقدمة الجزرية',
    evidence: 'فَالأَوَّلُ الإِخْفَاءُ عِنْدَ الْبَاءِ * وَسَمِّهِ الشَّفْوِىَّ لِلْقُرَّاءِ ‖ وَأَخْفِيَنْ المِيمَ إِنْ تَسْكُنْ بِغُنَّةٍ * عِنْدَ البَاءِ عَلَى المُخْتَارِ مِنْ أَهْلِ الأَدَاءِ',
  ),
  idghamShafawi(
    color: Color(0xFF5B21B6),
    label: 'إدغام شفوي',
    englishName: 'Idgham Shafawi',
    family: TajweedFamily.meem,
    definition: 'ميم ساكنة بعدها ميم، فتُدغم الأولى في الثانية مع الغنّة. وسمّاه الناظم إدغامًا صغيرًا، ويُعرف بإدغام المتماثلين.',
    amount: 'حركتان',
    letters: 'م',
    source: 'تحفة الأطفال ٢١',
    evidence: 'وَالثَّانى إِدْغَامٌ بِمِثْلِهَا أَتَى * وَسَمِّ إدغاماً صَغِيراً يَا فَتَى',
  ),

  // ── القلقلة
  qalqala(
    color: Color(0xFF8A6A12),
    label: 'قلقلة',
    englishName: 'Qalqalah',
    family: TajweedFamily.qalqala,
    definition: 'حروف «قُطْبُ جَدٍ» إذا سكنت، يهتزّ المخرج عند النطق بها فيُسمع لها نبرة. وهي صغرى في وسط الكلمة، وكبرى عند الوقف على الحرف — ونصّ الناظم أنها في الوقف أبين.',
    amount: 'نبرة، وهي عند الوقف أبين',
    letters: 'ق ط ب ج د',
    source: 'المقدمة الجزرية — باب صفات الحروف وباب استعمال الحروف',
    evidence: 'قَلْقَلَةٌ قُطْبُ جَدٍ ‖ وَبَيِّنَنْ مُقَلْقَلاً إِنْ سَكَنَا * وَإِنْ يَكُنْ فِي الوَقْفِ كَانَ أَبْيَنَا',
  ),

  // ── المدود
  maddNatural(
    color: Color(0xFF3E8E6E),
    label: 'مدّ طبيعي',
    englishName: 'Natural Madd',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ لا يأتي بعده همزة ولا سكون — وهو أصل المدّ الذي لا تقوم ذات الحرف دونه.',
    amount: 'حركتان',
    letters: 'ا و ي',
    source: 'تحفة الأطفال ٣٦ و٣٧',
    evidence: 'بلْ أَىُّ حَرْفٍ غَيْرُ هَمْزٍ أَوْ سُكُونْ * جَا بَعْدَ مَدٍّ فَالطَّبِيعىَّ يَكُونْ',
  ),
  maddMuttasil(
    color: Color(0xFF1B6E4B),
    label: 'مدّ واجب متّصل',
    englishName: 'Obligatory Madd',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ بعده همزة في الكلمة نفسها، فالمدّ واجب.',
    amount: '٤ أو ٥ حركات',
    letters: 'مدّ + ء',
    source: 'تحفة الأطفال ٤٣ — والمقدار لحفص من طريق الشاطبية',
    evidence: 'فَوَاجِبٌ إِنْ جَاءَ هَمْزٌ بَعْدَ مَدْ * فِي كِلْمَةٍ وَذَا بِمُتَّصِلٍ يُعَدْ',
  ),
  maddMunfasil(
    color: Color(0xFF6E8B18),
    label: 'مدّ جائز منفصل',
    englishName: 'Permissible Madd',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ في آخر الكلمة وبعده همزة في أوّل الكلمة التالية. والناظم يجيز فيه المدّ والقصر، وحفص من طريق الشاطبية على أربع أو خمس.',
    amount: '٤ أو ٥ حركات',
    letters: 'مدّ | ء',
    source: 'تحفة الأطفال ٤٤ — والمقدار لحفص من طريق الشاطبية',
    evidence: 'وَجَائزٌ مَدٌ وَقَصْرٌ إِنْ فُصِل * كُلٌّ بِكِلْمَةٍ وَهَذَا المُنْفَصِلْ',
  ),
  maddLazim(
    color: Color(0xFFB3261E),
    label: 'مدّ لازم',
    englishName: 'Necessary Madd',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ بعده سكون أصلي وصلًا ووقفًا، أو حرف مشدّد، فيلزم إشباع المدّ. ويدخل فيه اللازم الحرفي في فواتح السور، وهو في حروف «كَمْ عَسَلْ نَقَصْ» وحدها — وأمّا «حَيٍّ طَاهِرٍ» فمدّها طبيعي، والألف لا مدَّ فيها. والعين فيها وجهان والطُّول أرجح.',
    amount: '٦ حركات',
    letters: 'مدّ + سكون أو شدّة • ك م ع س ل ن ق ص',
    source: 'تحفة الأطفال ٤٧ و٥٤ و٥٥',
    evidence: 'وَلاَزِمٌ إِنِ السُّكُونُ أُصِّلاَ * وَصْلاَ وَوَقْفاً بَعْدَ مَدٍّ طُوّلاَ ‖ يَجْمَعُهَا حُرُوفُ كَمْ عَسَلْ نَقَصْ * وَعَيْنُ ذُو وَجْهَيْنِ والطُّولُ أَخَصْ ‖ وَمَا سِوَي الحَرْفِ الثُّلاَثِي لاَ أَلِفْ * فَمُدُّه مَدّاً طَبِيعِيَّا أُلِفْ',
  ),
  maddBadal(
    color: Color(0xFF57A07C),
    label: 'مدّ بدل',
    englishName: 'Madd Badal',
    family: TajweedFamily.madd,
    definition: 'همزة بعدها حرف مدّ، فتقدَّمت الهمزة على المدّ — أصله همزتان أُبدلت الثانية مدًّا، كـ«آمَنُوا» و«إِيمَانًا».',
    amount: 'حركتان',
    letters: 'ء + مدّ',
    source: 'تحفة الأطفال ٤٦',
    evidence: 'أَوْ قُدِّمَ الْهَمْزُ عَلَي المَدِّ وَذَا * بَدَلْ كَآمَنُوا وَإِيَماناً خُذَا',
  ),
  maddSila(
    color: Color(0xFF2C7F6A),
    label: 'مدّ صلة',
    englishName: 'Madd Silah',
    family: TajweedFamily.madd,
    definition: 'هاء الضمير بين متحرّكين تُوصَل بواو أو ياء صغيرة — صغرى بحركتين، وكبرى إذا جاءت بعدها همزة.',
    amount: 'حركتان، أو ٤–٥ مع الهمزة',
    letters: 'ـهُۥ  ـهِۦ',
    source: 'التعريف بمصحف المدينة النبوية — مجمع الملك فهد — وليس في المتنين',
    evidence: 'واو صغيرة أو ياء صغيرة بعد هاء الكناية تدلّ على مدّ الصلة الصغرى والكبرى',
  ),
  maddArid(
    color: Color(0xFFD97706),
    label: 'مدّ عارض للسكون',
    englishName: 'Madd Arid',
    family: TajweedFamily.madd,
    definition: 'حرف مدّ بعده حرف يُسكَّن من أجل الوقف، فالسكون عارض غير أصلي. وهو معلَّم هنا عند مواضع الوقف، فإن وصلتَ فلا مدَّ زائد.',
    amount: '٢ أو ٤ أو ٦ حركات — عند الوقف',
    letters: 'مدّ + حرف موقوف عليه',
    source: 'تحفة الأطفال ٤٥',
    evidence: 'وَمِثْلُ ذَا إِنْ عَرَضَ السُّكُونُ * وَقْفَاً كَتَعْلَمُونَ نَسْتَعِينُ',
  ),

  // ── ما لا يُنطق كاملًا
  hamzatWasl(
    color: Color(0xFF2F6FA8),
    label: 'همزة وصل',
    englishName: 'Hamzat Wasl',
    family: TajweedFamily.mute,
    definition: 'همزة تُنطق إذا ابتدأت بها، وتسقط من اللفظ إذا وصلتها بما قبلها.',
    amount: 'تسقط في الوصل',
    letters: 'ٱ',
    source: 'التعريف بمصحف المدينة النبوية — مجمع الملك فهد — وليست في المتنين',
    evidence: 'همزة الوصل تُنطق في الابتداء وتسقط في الوصل، وعلامتها رأس الصاد الصغيرة على الألف ﭐ',
  ),
  lamShamsiyya(
    color: Color(0xFF4E8CC0),
    label: 'لام شمسية',
    englishName: 'Solar Lam',
    family: TajweedFamily.mute,
    definition: 'لام «أل» لا تُنطق، ويُدغم ما بعدها فيُنطق مشدّدًا. وحروفها أربعة عشر جمعها الناظم في أوائل كلمات بيته.',
    amount: 'لا تُنطق',
    letters: 'ت ث د ذ ر ز س ش ص ض ط ظ ل ن',
    source: 'تحفة الأطفال ٢٦–٢٨',
    evidence: 'طِبْ ثُمَّ صِلْ رُحْمَاً تَفُزْ ضِفْ ذَا نِعَم * دَعْ سُوءَ ظَنٍ زُرْ شَرِيفَاً لِلْكَرَم ‖ وَاللاَّمَ الاُولَى سَمِّهَا قَمْرِيَّهْ * وَاللاَّمَ الاُخْرىَ سَمِّهَا شَمْسِيَّهْ',
  ),
  silent(
    color: Color(0xFF2F6FA8),
    label: 'ألف زائدة',
    englishName: 'Extra Alef',
    family: TajweedFamily.mute,
    definition: 'ألف زائدة في الرسم عليها الصفر المستطيل القائم: تسقط في الوصل وتثبت ألفًا في الوقف — كـ﴿أَنَا۠﴾ و﴿ٱلسَّبِيلَا۠﴾.',
    amount: 'تسقط وصلًا، وتثبت وقفًا',
    letters: 'ا ۟',
    source: 'التعريف بمصحف المدينة النبوية — مجمع الملك فهد — وليست في المتنين',
    evidence: 'الصفر المستطيل القائم فوق الألف بعدها متحرّك يدلّ على زيادتها وصلًا لا وقفًا، نحو ﴿أَنَا۠ خَيْرٌ مِنْهُ﴾',
  ),

  // ── المتماثلان والمتجانسان والمتقاربان
  idghamMutamathilayn(
    color: Color(0xFF7C3AED),
    label: 'إدغام متماثلين',
    englishName: 'Idgham Mutamathilayn',
    family: TajweedFamily.idgham,
    definition: 'حرفان اتّفقا مخرجًا وصفةً، أوّلهما ساكن، فيُدغم في الثاني فيُنطقان حرفًا واحدًا مشدّدًا — كـ«بَل لَّا» و«يُدْرِككُّم».',
    amount: 'حرف واحد مشدّد',
    letters: 'حرف + مثله',
    source: 'تحفة الأطفال ٣٠ و٣٣',
    evidence: 'إِنْ فِي الصِّفَاتِ وَالمَخَارِجِ اتَّفَقْ * حَرْفَانِ فَالْمِثْلاَنِ فِيهِمَا أَحَقْ ‖ بِالْمُتَجَانِسَيْنِ ثُمَّ إِنْ سَكَنْ * أَوَّلُ كُلٍّ فَالصَّغِيرَ سَمِّيَنْ',
  ),
  idghamMutajanisayn(
    color: Color(0xFF2858B8),
    label: 'إدغام متجانسين',
    englishName: 'Idgham Mutajanisayn',
    family: TajweedFamily.idgham,
    definition: 'حرفان اتّفقا مخرجًا واختلفا صفةً، أوّلهما ساكن، فيُدغم في الثاني — كـ«قَد تَّبَيَّنَ» و«ٱرْكَب مَّعَنَا» و«إِذ ظَّلَمُوا».',
    amount: 'حرف واحد مشدّد',
    letters: 'د ت • ت د • ت ط • ذ ظ • ب م',
    source: 'تحفة الأطفال ٣٢ و٣٣',
    evidence: 'مُتْقَارِبَيْنِ أَوْ يَكُونَا اتَّفَقَا * فِي مَخْرَجٍ دُونَ الصِّفَاتِ حُقِّقَا ‖ بِالْمُتَجَانِسَيْنِ ثُمَّ إِنْ سَكَنْ * أَوَّلُ كُلٍّ فَالصَّغِيرَ سَمِّيَنْ',
  ),
  idghamMutaqaribayn(
    color: Color(0xFF0E7490),
    label: 'إدغام متقاربين',
    englishName: 'Idgham Mutaqaribayn',
    family: TajweedFamily.idgham,
    definition: 'حرفان تقاربا مخرجًا واختلفا صفةً، أوّلهما ساكن، فيُدغم في الثاني — كـ«بَل رَّانَ» و«نَخْلُقكُّم».',
    amount: 'حرف واحد مشدّد',
    letters: 'ل ر • ق ك',
    source: 'تحفة الأطفال ٣١ و٣٣',
    evidence: 'وَإِنْ يَكُونَا مَخْرَجاً تَقَارَبَا * وَفي الصِّفَاتِ اخْتَلَفَا يُلَقَّبَا ‖ بِالْمُتَجَانِسَيْنِ ثُمَّ إِنْ سَكَنْ * أَوَّلُ كُلٍّ فَالصَّغِيرَ سَمِّيَنْ',
  ),

  // ── المدود
  maddLeen(
    color: Color(0xFFCA8A04),
    label: 'مدّ لين',
    englishName: 'Madd Leen',
    family: TajweedFamily.madd,
    definition: 'واو أو ياء ساكنة قبلها فتح، فإن وُقِف عليها مُدَّت — كـ«خَوْف» و«قُرَيْش». وهو معلَّم هنا عند مواضع الوقف، فإن وصلتَ فلا مدَّ.',
    amount: '٢ أو ٤ أو ٦ حركات — عند الوقف',
    letters: 'ـَوْ  ـَيْ',
    source: 'تحفة الأطفال ٤١',
    evidence: 'وَاللِّينُ مِنْهَا الْيَا وَوَاوٌ سَكَنَا * إِنِ انْفِتَاحٌ قَبْلَ كُلٍّ أُعْلِنَا',
  ),

  // ── التفخيم والترقيق
  tafkhim(
    color: Color(0xFFA88080),
    label: 'تفخيم',
    englishName: 'Tafkhim',
    family: TajweedFamily.tafkhim,
    definition: 'حروف الاستعلاء السبعة «خُصَّ ضَغْطٍ قِظْ» تُفخَّم دائمًا، ويقوى التفخيم في المُطبَقة منها: ص ض ط ظ. وما سواها من الحروف المستفِلة يُرقَّق.',
    amount: 'تفخيم دائم',
    letters: 'خ ص ض غ ط ق ظ',
    source: 'المقدمة الجزرية — صفات الحروف وباب الترقيق وباب اللامات',
    evidence: 'وَسَبْعُ عُلْوٍ خُصَّ ضَغْطٍ قِظْ حَصَرْ ‖ وَحَرْفَ الاسْتِعْلَاءِ فَخِّمْ وَاخْصُصَا * الإِطْبَاقَ أَقْوَى نَحْوُ قَالَ وَالعَصَا ‖ فَرَقِّقَنْ مُسْتَفِلاً مِنْ أَحْرُفِ * وَحَاذِرَنْ تَفْخِيمَ لَفْظِ الأَلِفِ',
  ),
  raMufakhkhama(
    color: Color(0xFF704878),
    label: 'راء مفخّمة',
    englishName: 'Heavy Ra',
    family: TajweedFamily.tafkhim,
    definition: 'الراء مفخّمة في الأصل: إذا فُتحت أو ضُمّت، أو سكنت بعد فتح أو ضمّ، أو سكنت بعد كسر عارض، أو جاء بعدها حرف استعلاء.',
    amount: 'تفخيم',
    letters: 'رَ  رُ  رْ بعد فتح أو ضمّ',
    source: 'المقدمة الجزرية — باب الراءات',
    evidence: 'وَرَقِّقِ الرَّاءَ إِذَا مَا كُسِرَتْ * كَذَاكَ بَعْدَ الكَسْرِ حَيْثُ سَكَنَتْ ‖ إِنْ لَمْ تَكُنْ مِنْ قَبْلِ حَرْفِ اسْتِعْلَا * أَوْ كَانَتِ الكَسْرَةُ لَيْسَتْ أَصْلَا',
  ),
  raMuraqqaqa(
    color: Color(0xFFA8A070),
    label: 'راء مرقّقة',
    englishName: 'Light Ra',
    family: TajweedFamily.tafkhim,
    definition: 'الراء مرقّقة إذا كُسرت، أو سكنت بعد كسر أصلي ولم يأتِ بعدها حرف استعلاء. وفي «فِرْقٍ» وجهان.',
    amount: 'ترقيق',
    letters: 'رِ  رْ بعد كسر أصلي',
    source: 'المقدمة الجزرية — باب الراءات',
    evidence: 'وَرَقِّقِ الرَّاءَ إِذَا مَا كُسِرَتْ * كَذَاكَ بَعْدَ الكَسْرِ حَيْثُ سَكَنَتْ ‖ إِنْ لَمْ تَكُنْ مِنْ قَبْلِ حَرْفِ اسْتِعْلَا * أَوْ كَانَتِ الكَسْرَةُ لَيْسَتْ أَصْلَا',
  ),
  lamJalala(
    color: Color(0xFFB85858),
    label: 'تفخيم لام لفظ الجلالة',
    englishName: 'Heavy Lam of Allah',
    family: TajweedFamily.tafkhim,
    definition: 'لام لفظ الجلالة تُفخَّم إذا سبقها فتح أو ضمّ، وتُرقَّق إذا سبقها كسر. والملوَّن هنا موضع التفخيم وحده، لأن الترقيق هو الأصل في سائر اللامات.',
    amount: 'تفخيم بعد فتح أو ضمّ',
    letters: 'لام «اللّٰه»',
    source: 'المقدمة الجزرية — باب اللامات',
    evidence: 'وَفَخِّمِ اللَّامَ مِنِ اسْمِ اللَّهِ * عَنْ فَتْحٍ اوْ ضَمٍّ كَعَبْدِ اللَّهِ',
  );

  const TajweedRule({
    required this.color,
    required this.label,
    required this.englishName,
    required this.family,
    required this.definition,
    required this.amount,
    required this.letters,
    required this.source,
    required this.evidence,
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

  /// Where the ruling comes from: the matn and verse number, or the mushaf's
  /// own notation guide for the three rules the two matns do not cover.
  final String source;

  /// The reference in its own words, so a reader can check it rather than
  /// take this package's word for it.
  final String evidence;

  /// The natural madd alone.
  ///
  /// It is the most frequent rule in the mushaf by a wide margin, so colouring
  /// it tints most of the page and buries everything else — hence its own
  /// switch.
  bool get isNaturalMadd => this == TajweedRule.maddNatural;

  /// The tafkhim and tarqiq rules.
  ///
  /// The seven isti'la letters alone are 16,638 positions and the ra is
  /// 12,403 — about thirty thousand, enough to tint the page the way the
  /// natural madd would. Hence their own switch.
  bool get isTafkhim => family == TajweedFamily.tafkhim;
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
