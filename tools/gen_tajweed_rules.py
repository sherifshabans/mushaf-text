# -*- coding: utf-8 -*-
"""مصدر واحد لأحكام التجويد — يولّد نسخة كوتلن ونسخة دارت.

## ليه مولّد أصلًا
الأحكام كانت مكتوبة مرّتين بالإيد: مرّة في `TajweedRules.kt` ومرّة في
`tajweed_rule.dart`. أي تعديل على شرح حكم لازم يتعمل في الاتنين، وأول مرة
تُنسى واحدة تبقى النسختان بتقولا كلامًا مختلفًا عن نفس الآية. الترتيب كمان
**محمول**: فيكسشر التطابق بيخزّن الأحكام بترتيبها الرقمي، فأي إعادة ترتيب في
ملف واحد بتبطّل الفيكسشر بصمت.

فالجدول هنا هو الأصل، والملفات الثلاثة مولّدة منه. `python tools/gen_tajweed_rules.py`

## المراجع
كل حكم بيحمل مصدره ونصّه:

* **تحفة الأطفال والغلمان** للشيخ سليمان الجمزوري (ت ١١٩٨ هـ) — وهي المرجع
  المتعارف عليه لأحكام النون الساكنة والتنوين، والميم الساكنة، ولام «أل»،
  والمدود. النص منقول من ويكي مصدر بعد حذف التطويل، ومقابَل على نسخة ثانية
  (surahquran.com) وهما متطابقان في كل بيت مستشهَد به هنا.
* **المقدمة الجزرية** لابن الجزري (ت ٨٣٣ هـ) — للقلقلة والغنّة وإخفاء الميم.
* **التعريف بمصحف المدينة النبوية** (مجمع الملك فهد) — لعلامات الضبط: الصفر
  المستطيل القائم، والواو والياء الصغيرتين، وهمزة الوصل. وهذه الثلاثة ليست في
  المتنين، والمصدر مذكور عليها صراحةً.

الرواية: **حفص عن عاصم من طريق الشاطبية**، وهي رواية النص المرفق.

## اللي اتصلّح لمّا اتقابل الكود على المتن
* «الصفر المستدير» كان غلطًا — الموجود في النص هو **المستطيل القائم** (٦٦ موضعًا
  كلها ألف)، ومعناه «زائدة وصلًا لا وقفًا» لا «لا تُنطق» مطلقًا. الحرف يثبت
  ألفًا عند الوقف.
* الإدغام بغنّة كان ناقص الوصف: بيت ١١ يستثني اجتماع النون بحرف «ينمو» في كلمة
  واحدة (دُنْيَا، صِنْوَان، قِنْوَان، بُنْيَان) فهو إظهار مطلق.
* المدّ اللازم في فواتح السور كان موصوفًا وصفًا عامًّا؛ المتن يفصّله: «كم عسل
  نقص» ستّ حركات، و«حي طهر» مدٌّ طبيعي، والألف لا مدَّ فيها.
* القلقلة والمدّ العارض كان عليهما تحفّظ «بافتراض أن القارئ واقف» — والمتنان
  ينصّان على الوقف صراحةً، فصار شرطًا منصوصًا لا افتراضًا.
"""

import io
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

TUHFA = "تحفة الأطفال"
JAZ = "المقدمة الجزرية"
DABT = "التعريف بمصحف المدينة النبوية — مجمع الملك فهد"

# ── أبيات المتن، بنصّها ─────────────────────────────────────────────────────
V = {
    10: "لَكِنَّهَا قِسْمَانِ قِسْمٌ يُدْغَمَا * فِيهِ بِغُنَّةٍ بِيَنْمُو عُلِمَا",
    11: "إِلاَّ إِذَا كَانَا بِكِلْمَةٍ فَلاَ * تُدْغِمْ كَدُنْيَا ثُمَّ صِنْوَانٍ تَلاَ",
    12: "وَالثَّاني إِدْغَامٌ بِغَيْرِ غُنَّةْ * في اللاَّمِ وَالرَّا ثُمَّ كَرّرَنَّهْ",
    13: "وَالثَالثُ الإِقْلاَبُ عِنْدَ الْبَاءِ * مِيماً بِغُنَّةٍ مَعَ الإِخْفَاءِ",
    14: "وَالرَّابِعُ الإِخْفَاءُ عِنْدَ الْفاضِلِ * مِنَ الحُرُوفِ وَاجِبٌ لِلْفَاضِلِ",
    16: "صِفْ ذَا ثَنَا كَمْ جَادَ شَخْصٌ قَدْ سمَا * دُمْ طَيَّباً زِدْ فِي تُقَىً ضَعْ ظَالِمَا",
    17: "وَغُنَّ مِيماً ثُمَّ نُوناً شُدِّدَا * وَسَمِّ كُلاً حَرْفَ غُنَّةٍ بَدَا",
    20: "فَالأَوَّلُ الإِخْفَاءُ عِنْدَ الْبَاءِ * وَسَمِّهِ الشَّفْوِىَّ لِلْقُرَّاءِ",
    21: "وَالثَّانى إِدْغَامٌ بِمِثْلِهَا أَتَى * وَسَمِّ إدغاماً صَغِيراً يَا فَتَى",
    27: "طِبْ ثُمَّ صِلْ رُحْمَاً تَفُزْ ضِفْ ذَا نِعَم * دَعْ سُوءَ ظَنٍ زُرْ شَرِيفَاً لِلْكَرَم",
    28: "وَاللاَّمَ الاُولَى سَمِّهَا قَمْرِيَّهْ * وَاللاَّمَ الاُخْرىَ سَمِّهَا شَمْسِيَّهْ",
    37: "بلْ أَىُّ حَرْفٍ غَيْرُ هَمْزٍ أَوْ سُكُونْ * جَا بَعْدَ مَدٍّ فَالطَّبِيعىَّ يَكُونْ",
    43: "فَوَاجِبٌ إِنْ جَاءَ هَمْزٌ بَعْدَ مَدْ * فِي كِلْمَةٍ وَذَا بِمُتَّصِلٍ يُعَدْ",
    44: "وَجَائزٌ مَدٌ وَقَصْرٌ إِنْ فُصِل * كُلٌّ بِكِلْمَةٍ وَهَذَا المُنْفَصِلْ",
    45: "وَمِثْلُ ذَا إِنْ عَرَضَ السُّكُونُ * وَقْفَاً كَتَعْلَمُونَ نَسْتَعِينُ",
    46: "أَوْ قُدِّمَ الْهَمْزُ عَلَي المَدِّ وَذَا * بَدَلْ كَآمَنُوا وَإِيَماناً خُذَا",
    47: "وَلاَزِمٌ إِنِ السُّكُونُ أُصِّلاَ * وَصْلاَ وَوَقْفاً بَعْدَ مَدٍّ طُوّلاَ",
    54: "يَجْمَعُهَا حُرُوفُ كَمْ عَسَلْ نَقَصْ * وَعَيْنُ ذُو وَجْهَيْنِ والطُّولُ أَخَصْ",
    55: "وَمَا سِوَي الحَرْفِ الثُّلاَثِي لاَ أَلِفْ * فَمُدُّه مَدّاً طَبِيعِيَّا أُلِفْ",
    30: "إِنْ فِي الصِّفَاتِ وَالمَخَارِجِ اتَّفَقْ * حَرْفَانِ فَالْمِثْلاَنِ فِيهِمَا أَحَقْ",
    31: "وَإِنْ يَكُونَا مَخْرَجاً تَقَارَبَا * وَفي الصِّفَاتِ اخْتَلَفَا يُلَقَّبَا",
    32: "مُتْقَارِبَيْنِ أَوْ يَكُونَا اتَّفَقَا * فِي مَخْرَجٍ دُونَ الصِّفَاتِ حُقِّقَا",
    33: "بِالْمُتَجَانِسَيْنِ ثُمَّ إِنْ سَكَنْ * أَوَّلُ كُلٍّ فَالصَّغِيرَ سَمِّيَنْ",
    41: "وَاللِّينُ مِنْهَا الْيَا وَوَاوٌ سَكَنَا * إِنِ انْفِتَاحٌ قَبْلَ كُلٍّ أُعْلِنَا",
}

J_QALQALA_LETTERS = "قَلْقَلَةٌ قُطْبُ جَدٍ"
J_QALQALA_WAQF = "وَبَيِّنَنْ مُقَلْقَلاً إِنْ سَكَنَا * وَإِنْ يَكُنْ فِي الوَقْفِ كَانَ أَبْيَنَا"
J_GHUNNA = "وَأَظْهِرِ الغُنَّةَ مِنْ نُونٍ وَمِنْ * مِيمٍ إِذَا مَا شُدِّدَا"
J_IKHFA_SHAFAWI = "وَأَخْفِيَنْ المِيمَ إِنْ تَسْكُنْ بِغُنَّةٍ * عِنْدَ البَاءِ عَلَى المُخْتَارِ مِنْ أَهْلِ الأَدَاءِ"
J_ISTILA = "وَسَبْعُ عُلْوٍ خُصَّ ضَغْطٍ قِظْ حَصَرْ"
J_TAFKHIM = "وَحَرْفَ الاسْتِعْلَاءِ فَخِّمْ وَاخْصُصَا * الإِطْبَاقَ أَقْوَى نَحْوُ قَالَ وَالعَصَا"
J_TARQIQ = "فَرَقِّقَنْ مُسْتَفِلاً مِنْ أَحْرُفِ * وَحَاذِرَنْ تَفْخِيمَ لَفْظِ الأَلِفِ"
J_RA = ("وَرَقِّقِ الرَّاءَ إِذَا مَا كُسِرَتْ * كَذَاكَ بَعْدَ الكَسْرِ حَيْثُ سَكَنَتْ ‖ "
        "إِنْ لَمْ تَكُنْ مِنْ قَبْلِ حَرْفِ اسْتِعْلَا * أَوْ كَانَتِ الكَسْرَةُ لَيْسَتْ أَصْلَا")
J_LAM = "وَفَخِّمِ اللَّامَ مِنِ اسْمِ اللَّهِ * عَنْ فَتْحٍ اوْ ضَمٍّ كَعَبْدِ اللَّهِ"

D_SIFR = ("الصفر المستطيل القائم فوق الألف بعدها متحرّك يدلّ على زيادتها "
          "وصلًا لا وقفًا، نحو ﴿أَنَا۠ خَيْرٌ مِنْهُ﴾")
D_SILA = ("واو صغيرة أو ياء صغيرة بعد هاء الكناية تدلّ على مدّ الصلة الصغرى "
          "والكبرى")
D_WASL = ("همزة الوصل تُنطق في الابتداء وتسقط في الوصل، وعلامتها رأس الصاد "
          "الصغيرة على الألف ﭐ")

# ── الجدول ──────────────────────────────────────────────────────────────────
# الترتيب محمول: الفيكسشر بيخزّن الأحكام برقمها. أي حكم جديد يتضاف في الآخر.
FAMILIES = [
    ("GHUNNA", "ghunna", "الغنّة", "Ghunnah"),
    ("NOON", "noon", "أحكام النون الساكنة والتنوين", "Noon Sakinah & Tanween"),
    ("MEEM", "meem", "أحكام الميم الساكنة", "Meem Sakinah"),
    ("IDGHAM", "idgham", "المتماثلان والمتجانسان والمتقاربان",
     "Mutamathilayn, Mutajanisayn & Mutaqaribayn"),
    ("QALQALA", "qalqala", "القلقلة", "Qalqalah"),
    ("TAFKHIM", "tafkhim", "التفخيم والترقيق", "Tafkhim & Tarqiq"),
    ("MADD", "madd", "المدود", "Madd"),
    ("MUTE", "mute", "ما لا يُنطق كاملًا", "Not fully pronounced"),
]

RULES = [
    dict(
        kt="GHUNNA", dart="ghunna", color="0xFFA81D5D",
        label="غنّة مشدّدة", en="Ghunnah", family="GHUNNA",
        definition="النون والميم المشدّدتان تُغنّان — صوت يخرج من الخيشوم.",
        amount="حركتان",
        letters="نّ  مّ",
        source="%s ١٧ • %s" % (TUHFA, JAZ),
        evidence=V[17] + " ‖ " + J_GHUNNA,
    ),
    dict(
        kt="IKHFA", dart="ikhfa", color="0xFF7A3DAE",
        label="إخفاء حقيقي", en="Ikhfa", family="NOON",
        definition="نون ساكنة أو تنوين بعدها أحد حروف الإخفاء الخمسة عشر، "
                   "فتُخفى النون بين الإظهار والإدغام مع الغنّة.",
        amount="حركتان",
        letters="ص ذ ث ك ج ش ق س د ط ز ف ت ض ظ",
        source="%s ١٤ و١٦" % TUHFA,
        evidence=V[14] + " ‖ " + V[16],
    ),
    dict(
        kt="IDGHAM_GHUNNA", dart="idghamGhunna", color="0xFFB5179E",
        label="إدغام بغنّة", en="Idgham with Ghunnah", family="NOON",
        definition="نون ساكنة أو تنوين بعدها أحد حروف «يَنْمُو»، فتُدغم النون "
                   "في الحرف مع بقاء الغنّة. وهو ناقص في الياء والواو (تبقى "
                   "الغنّة ولا يكتمل التشديد)، كامل في النون والميم. فإن اجتمعا "
                   "في كلمة واحدة فلا إدغام بل إظهار مطلق، كـ«دُنْيَا» "
                   "و«صِنْوَانٍ».",
        amount="حركتان",
        letters="ي ن م و",
        source="%s ١٠ و١١" % TUHFA,
        evidence=V[10] + " ‖ " + V[11],
    ),
    dict(
        kt="IDGHAM_NO_GHUNNA", dart="idghamNoGhunna", color="0xFF0F766E",
        label="إدغام بغير غنّة", en="Idgham without Ghunnah", family="NOON",
        definition="نون ساكنة أو تنوين بعدها لام أو راء، فتُدغم النون فيها "
                   "بلا غنّة.",
        amount="بلا غنّة",
        letters="ل ر",
        source="%s ١٢" % TUHFA,
        evidence=V[12],
    ),
    dict(
        kt="IQLAB", dart="iqlab", color="0xFF92400E",
        label="إقلاب", en="Iqlab", family="NOON",
        definition="نون ساكنة أو تنوين بعدها باء، فتُقلب النون ميمًا مخفاة عند "
                   "الباء مع الغنّة. وعلامته في المصحف ميم صغيرة فوق النون أو "
                   "تحتها.",
        amount="حركتان",
        letters="ب",
        source="%s ١٣" % TUHFA,
        evidence=V[13],
    ),
    dict(
        kt="IKHFA_SHAFAWI", dart="ikhfaShafawi", color="0xFFC04A8A",
        label="إخفاء شفوي", en="Ikhfa Shafawi", family="MEEM",
        definition="ميم ساكنة بعدها باء، فتُخفى الميم عند الباء مع الغنّة — "
                   "والشفتان لا تنطبقان انطباقًا تامًّا.",
        amount="حركتان",
        letters="ب",
        source="%s ٢٠ • %s" % (TUHFA, JAZ),
        evidence=V[20] + " ‖ " + J_IKHFA_SHAFAWI,
    ),
    dict(
        kt="IDGHAM_SHAFAWI", dart="idghamShafawi", color="0xFF5B21B6",
        label="إدغام شفوي", en="Idgham Shafawi", family="MEEM",
        definition="ميم ساكنة بعدها ميم، فتُدغم الأولى في الثانية مع الغنّة. "
                   "وسمّاه الناظم إدغامًا صغيرًا، ويُعرف بإدغام المتماثلين.",
        amount="حركتان",
        letters="م",
        source="%s ٢١" % TUHFA,
        evidence=V[21],
    ),
    dict(
        kt="QALQALA", dart="qalqala", color="0xFF8A6A12",
        label="قلقلة", en="Qalqalah", family="QALQALA",
        definition="حروف «قُطْبُ جَدٍ» إذا سكنت، يهتزّ المخرج عند النطق بها "
                   "فيُسمع لها نبرة. وهي صغرى في وسط الكلمة، وكبرى عند الوقف "
                   "على الحرف — ونصّ الناظم أنها في الوقف أبين.",
        amount="نبرة، وهي عند الوقف أبين",
        letters="ق ط ب ج د",
        source="%s — باب صفات الحروف وباب استعمال الحروف" % JAZ,
        evidence=J_QALQALA_LETTERS + " ‖ " + J_QALQALA_WAQF,
    ),
    dict(
        kt="MADD_NATURAL", dart="maddNatural", color="0xFF3E8E6E",
        label="مدّ طبيعي", en="Natural Madd", family="MADD",
        definition="حرف مدّ لا يأتي بعده همزة ولا سكون — وهو أصل المدّ الذي لا "
                   "تقوم ذات الحرف دونه.",
        amount="حركتان",
        letters="ا و ي",
        source="%s ٣٦ و٣٧" % TUHFA,
        evidence=V[37],
    ),
    dict(
        kt="MADD_MUTTASIL", dart="maddMuttasil", color="0xFF1B6E4B",
        label="مدّ واجب متّصل", en="Obligatory Madd", family="MADD",
        definition="حرف مدّ بعده همزة في الكلمة نفسها، فالمدّ واجب.",
        amount="٤ أو ٥ حركات",
        letters="مدّ + ء",
        source="%s ٤٣ — والمقدار لحفص من طريق الشاطبية" % TUHFA,
        evidence=V[43],
    ),
    dict(
        kt="MADD_MUNFASIL", dart="maddMunfasil", color="0xFF6E8B18",
        label="مدّ جائز منفصل", en="Permissible Madd", family="MADD",
        definition="حرف مدّ في آخر الكلمة وبعده همزة في أوّل الكلمة التالية. "
                   "والناظم يجيز فيه المدّ والقصر، وحفص من طريق الشاطبية على "
                   "أربع أو خمس.",
        amount="٤ أو ٥ حركات",
        letters="مدّ | ء",
        source="%s ٤٤ — والمقدار لحفص من طريق الشاطبية" % TUHFA,
        evidence=V[44],
    ),
    dict(
        kt="MADD_LAZIM", dart="maddLazim", color="0xFFB3261E",
        label="مدّ لازم", en="Necessary Madd", family="MADD",
        definition="حرف مدّ بعده سكون أصلي وصلًا ووقفًا، أو حرف مشدّد، فيلزم "
                   "إشباع المدّ. ويدخل فيه اللازم الحرفي في فواتح السور، وهو في "
                   "حروف «كَمْ عَسَلْ نَقَصْ» وحدها — وأمّا «حَيٍّ طَاهِرٍ» "
                   "فمدّها طبيعي، والألف لا مدَّ فيها. والعين فيها وجهان "
                   "والطُّول أرجح.",
        amount="٦ حركات",
        letters="مدّ + سكون أو شدّة • ك م ع س ل ن ق ص",
        source="%s ٤٧ و٥٤ و٥٥" % TUHFA,
        evidence=V[47] + " ‖ " + V[54] + " ‖ " + V[55],
    ),
    dict(
        kt="MADD_BADAL", dart="maddBadal", color="0xFF57A07C",
        label="مدّ بدل", en="Madd Badal", family="MADD",
        definition="همزة بعدها حرف مدّ، فتقدَّمت الهمزة على المدّ — أصله همزتان "
                   "أُبدلت الثانية مدًّا، كـ«آمَنُوا» و«إِيمَانًا».",
        amount="حركتان",
        letters="ء + مدّ",
        source="%s ٤٦" % TUHFA,
        evidence=V[46],
    ),
    dict(
        kt="MADD_SILA", dart="maddSila", color="0xFF2C7F6A",
        label="مدّ صلة", en="Madd Silah", family="MADD",
        definition="هاء الضمير بين متحرّكين تُوصَل بواو أو ياء صغيرة — صغرى "
                   "بحركتين، وكبرى إذا جاءت بعدها همزة.",
        amount="حركتان، أو ٤–٥ مع الهمزة",
        letters="ـهُۥ  ـهِۦ",
        source="%s — وليس في المتنين" % DABT,
        evidence=D_SILA,
    ),
    dict(
        kt="MADD_ARID", dart="maddArid", color="0xFFD97706",
        label="مدّ عارض للسكون", en="Madd Arid", family="MADD",
        definition="حرف مدّ بعده حرف يُسكَّن من أجل الوقف، فالسكون عارض غير "
                   "أصلي. وهو معلَّم هنا عند مواضع الوقف، فإن وصلتَ فلا مدَّ "
                   "زائد.",
        amount="٢ أو ٤ أو ٦ حركات — عند الوقف",
        letters="مدّ + حرف موقوف عليه",
        source="%s ٤٥" % TUHFA,
        evidence=V[45],
    ),
    dict(
        kt="HAMZAT_WASL", dart="hamzatWasl", color="0xFF2F6FA8",
        label="همزة وصل", en="Hamzat Wasl", family="MUTE",
        definition="همزة تُنطق إذا ابتدأت بها، وتسقط من اللفظ إذا وصلتها بما "
                   "قبلها.",
        amount="تسقط في الوصل",
        letters="ٱ",
        source="%s — وليست في المتنين" % DABT,
        evidence=D_WASL,
    ),
    dict(
        kt="LAM_SHAMSIYYA", dart="lamShamsiyya", color="0xFF4E8CC0",
        label="لام شمسية", en="Solar Lam", family="MUTE",
        definition="لام «أل» لا تُنطق، ويُدغم ما بعدها فيُنطق مشدّدًا. وحروفها "
                   "أربعة عشر جمعها الناظم في أوائل كلمات بيته.",
        amount="لا تُنطق",
        letters="ت ث د ذ ر ز س ش ص ض ط ظ ل ن",
        source="%s ٢٦–٢٨" % TUHFA,
        evidence=V[27] + " ‖ " + V[28],
    ),
    dict(
        kt="SILENT", dart="silent", color="0xFF2F6FA8",
        label="ألف زائدة", en="Extra Alef", family="MUTE",
        definition="ألف زائدة في الرسم عليها الصفر المستطيل القائم: تسقط في "
                   "الوصل وتثبت ألفًا في الوقف — كـ﴿أَنَا۠﴾ و﴿ٱلسَّبِيلَا۠﴾.",
        amount="تسقط وصلًا، وتثبت وقفًا",
        letters="ا ۟",
        source="%s — وليست في المتنين" % DABT,
        evidence=D_SIFR,
    ),

    # ── أُضيفت بعد مقابلة الأحكام على أبواب المتنين ──────────────────────────
    #
    # الثمانية التالية كانت ناقصة: ثلاثة أبواب كاملة من الجزرية (الترقيق
    # والراءات واللامات)، وباب المثلين والمتجانسين والمتقاربين من التحفة،
    # واللين. وترتيبها في الآخر **عمدًا**: فيكسشر التطابق يخزّن الأحكام برقمها،
    # فأي إدراج في الوسط يبطله بصمت.
    dict(
        kt="IDGHAM_MUTAMATHILAYN", dart="idghamMutamathilayn", color="0xFF7C3AED",
        label="إدغام متماثلين", en="Idgham Mutamathilayn", family="IDGHAM",
        definition="حرفان اتّفقا مخرجًا وصفةً، أوّلهما ساكن، فيُدغم في الثاني "
                   "فيُنطقان حرفًا واحدًا مشدّدًا — كـ«بَل لَّا» و«يُدْرِككُّم».",
        amount="حرف واحد مشدّد",
        letters="حرف + مثله",
        source="%s ٣٠ و٣٣" % TUHFA,
        evidence=V[30] + " ‖ " + V[33],
    ),
    dict(
        kt="IDGHAM_MUTAJANISAYN", dart="idghamMutajanisayn", color="0xFF9D174D",
        label="إدغام متجانسين", en="Idgham Mutajanisayn", family="IDGHAM",
        definition="حرفان اتّفقا مخرجًا واختلفا صفةً، أوّلهما ساكن، فيُدغم في "
                   "الثاني — كـ«قَد تَّبَيَّنَ» و«ٱرْكَب مَّعَنَا» و«إِذ ظَّلَمُوا».",
        amount="حرف واحد مشدّد",
        letters="د ت • ت د • ت ط • ذ ظ • ب م",
        source="%s ٣٢ و٣٣" % TUHFA,
        evidence=V[32] + " ‖ " + V[33],
    ),
    dict(
        kt="IDGHAM_MUTAQARIBAYN", dart="idghamMutaqaribayn", color="0xFF0E7490",
        label="إدغام متقاربين", en="Idgham Mutaqaribayn", family="IDGHAM",
        definition="حرفان تقاربا مخرجًا واختلفا صفةً، أوّلهما ساكن، فيُدغم في "
                   "الثاني — كـ«بَل رَّانَ» و«نَخْلُقكُّم».",
        amount="حرف واحد مشدّد",
        letters="ل ر • ق ك",
        source="%s ٣١ و٣٣" % TUHFA,
        evidence=V[31] + " ‖ " + V[33],
    ),
    dict(
        kt="MADD_LEEN", dart="maddLeen", color="0xFFCA8A04",
        label="مدّ لين", en="Madd Leen", family="MADD",
        definition="واو أو ياء ساكنة قبلها فتح، فإن وُقِف عليها مُدَّت — "
                   "كـ«خَوْف» و«قُرَيْش». وهو معلَّم هنا عند مواضع الوقف، فإن "
                   "وصلتَ فلا مدَّ.",
        amount="٢ أو ٤ أو ٦ حركات — عند الوقف",
        letters="ـَوْ  ـَيْ",
        source="%s ٤١" % TUHFA,
        evidence=V[41],
    ),

    dict(
        kt="TAFKHIM", dart="tafkhim", color="0xFF44403C",
        label="تفخيم", en="Tafkhim", family="TAFKHIM",
        definition="حروف الاستعلاء السبعة «خُصَّ ضَغْطٍ قِظْ» تُفخَّم دائمًا، "
                   "ويقوى التفخيم في المُطبَقة منها: ص ض ط ظ. وما سواها من "
                   "الحروف المستفِلة يُرقَّق.",
        amount="تفخيم دائم",
        letters="خ ص ض غ ط ق ظ",
        source="%s — صفات الحروف وباب الترقيق وباب اللامات" % JAZ,
        evidence=J_ISTILA + " ‖ " + J_TAFKHIM + " ‖ " + J_TARQIQ,
    ),
    dict(
        kt="RA_MUFAKHKHAMA", dart="raMufakhkhama", color="0xFF7C2D12",
        label="راء مفخّمة", en="Heavy Ra", family="TAFKHIM",
        definition="الراء مفخّمة في الأصل: إذا فُتحت أو ضُمّت، أو سكنت بعد فتح "
                   "أو ضمّ، أو سكنت بعد كسر عارض، أو جاء بعدها حرف استعلاء.",
        amount="تفخيم",
        letters="رَ  رُ  رْ بعد فتح أو ضمّ",
        source="%s — باب الراءات" % JAZ,
        evidence=J_RA,
    ),
    dict(
        kt="RA_MURAQQAQA", dart="raMuraqqaqa", color="0xFF8FA7B8",
        label="راء مرقّقة", en="Light Ra", family="TAFKHIM",
        definition="الراء مرقّقة إذا كُسرت، أو سكنت بعد كسر أصلي ولم يأتِ بعدها "
                   "حرف استعلاء. وفي «فِرْقٍ» وجهان.",
        amount="ترقيق",
        letters="رِ  رْ بعد كسر أصلي",
        source="%s — باب الراءات" % JAZ,
        evidence=J_RA,
    ),
    dict(
        kt="LAM_JALALA", dart="lamJalala", color="0xFF713F12",
        label="تفخيم لام لفظ الجلالة", en="Heavy Lam of Allah",
        family="TAFKHIM",
        definition="لام لفظ الجلالة تُفخَّم إذا سبقها فتح أو ضمّ، وتُرقَّق إذا "
                   "سبقها كسر. والملوَّن هنا موضع التفخيم وحده، لأن الترقيق هو "
                   "الأصل في سائر اللامات.",
        amount="تفخيم بعد فتح أو ضمّ",
        letters="لام «اللّٰه»",
        source="%s — باب اللامات" % JAZ,
        evidence=J_LAM,
    ),
]

HEAD_AR = """أحكام التجويد المعروضة على صفحة المصحف: لون لكل حكم، واسمه وشرحه ومرجعه.

## مولّد — لا تعدّل هذا الملف بيدك
هذا الملف يولّده `tools/gen_tajweed_rules.py` في مستودع mushaf-text. عدّل
الجدول هناك ثم أعد التوليد، وإلا افترقت نسخة أندرويد عن نسخة فلاتر.

## المراجع
كل حكم يحمل مصدره في `source` ونصَّ مرجعه في `evidence`:
تحفة الأطفال للجمزوري لأحكام النون والميم ولام «أل» والمدود، والمقدمة الجزرية
لابن الجزري للقلقلة والغنّة، والتعريف بمصحف المدينة النبوية لعلامات الضبط.
الرواية حفص عن عاصم من طريق الشاطبية.

ثلاثة أحكام ليست في المتنين — مدّ الصلة وهمزة الوصل والألف الزائدة — ومصدرها
علامات ضبط المصحف، وهذا منصوص عليه في `source` لكل واحد منها.

## ليه لون لكل حكم لا لون لكل عائلة
تجميع أحكام النون كلها في لون واحد يوفّر ألوانًا لكنه يضيّع المعلومة: القارئ
يريد أن يعرف أن هذا **إخفاء** لا **إدغام**، لأن النطق مختلف. فكل حكم بلونه،
والشرح باللمس يغني القارئ عن حفظ المفتاح.

## الألوان
المدود على سلّم **الطول**: أخضر (حركتان) ← زيتوني/أخضر غامق (٤–٥) ← برتقالي
(عارض) ← أحمر (٦ حركات). وأحكام النون والميم متفرّقة على عجلة الألوان عمدًا لا
متدرّجة في عائلة واحدة، لأن حكمين منها كثيرًا ما يقعان في كلمة واحدة، فدرجتان
من لون واحد يضيع الفرق بينهما في حجم القراءة.

الألوان اجتهاد في العرض، وليست نقلًا عن مصحف ملوّن مطبوع."""


def kdoc(text, indent=""):
    lines = text.split("\n")
    out = [indent + "/**"]
    for ln in lines:
        out.append((indent + " * " + ln).rstrip())
    out.append(indent + " */")
    return "\n".join(out)


def ddoc(text, indent=""):
    return "\n".join((indent + "/// " + ln).rstrip() for ln in text.split("\n"))


def kstr(s):
    return '"%s"' % s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")


def dstr(s):
    return "'%s'" % s.replace("\\", "\\\\").replace("'", "\\'").replace("$", "\\$")


ORDER_NOTE = """**ترتيب التعريف محمول.** فيكسشر التطابق الذي يثبّت نسخة دارت على
نسخة كوتلن يخزّن الأحكام برقمها الترتيبي، فإعادة ترتيبها تبطل الفيكسشر بصمت.
أي حكم جديد يُضاف في الآخر."""


def gen_kotlin(package, with_english, with_hit=True):
    o = io.StringIO()
    o.write("package %s\n\n" % package)
    o.write("import androidx.compose.ui.graphics.Color\n\n")
    o.write(kdoc(HEAD_AR + "\n\n## الترتيب\n" + ORDER_NOTE) + "\n")
    o.write("enum class TajweedRule(\n")
    o.write("    val color: Color,\n")
    o.write("    /** اسم الحكم كما في كتب التجويد. */\n")
    o.write("    val label: String,\n")
    if with_english:
        o.write("    val englishName: String,\n")
    o.write("    /** العائلة — لترتيب مفتاح الألوان فقط. */\n")
    o.write("    val family: TajweedFamily,\n")
    o.write("    val definition: String,\n")
    o.write("    /** المقدار أو الكيفية. */\n")
    o.write("    val amount: String,\n")
    o.write("    /** حروف الحكم، للعرض في ورقة الشرح. */\n")
    o.write("    val letters: String,\n")
    o.write("    /** المرجع: اسم المتن ورقم البيت، أو مرجع الضبط. */\n")
    o.write("    val source: String,\n")
    o.write("    /** نصّ المرجع بحروفه — ما يقرؤه المستخدم ليتحقّق بنفسه. */\n")
    o.write("    val evidence: String\n")
    o.write(") {\n")
    last_family = None
    for i, r in enumerate(RULES):
        if r["family"] != last_family:
            fam = next(f for f in FAMILIES if f[0] == r["family"])
            o.write("\n    // ── %s %s\n" % (fam[2], "─" * max(3, 62 - len(fam[2]))))
            last_family = r["family"]
        o.write("    %s(\n" % r["kt"])
        o.write("        color = Color(%s),\n" % r["color"])
        o.write("        label = %s,\n" % kstr(r["label"]))
        if with_english:
            o.write("        englishName = %s,\n" % kstr(r["en"]))
        o.write("        family = TajweedFamily.%s,\n" % r["family"])
        o.write("        definition = %s,\n" % kstr(r["definition"]))
        o.write("        amount = %s,\n" % kstr(r["amount"]))
        o.write("        letters = %s,\n" % kstr(r["letters"]))
        o.write("        source = %s,\n" % kstr(r["source"]))
        o.write("        evidence = %s\n" % kstr(r["evidence"]))
        o.write("    )%s\n" % ("," if i < len(RULES) - 1 else ";"))
    o.write("""
    /**
     * `true` للمدّ الطبيعي وحده.
     *
     * هو أكثر الأحكام تكرارًا في المصحف بفارق كبير، فتلوينه يصبغ الصفحة كلها
     * ويغطّي على باقي الأحكام. فله مفتاح منفصل.
     */
    val isNaturalMadd: Boolean get() = this == MADD_NATURAL

    /**
     * `true` لأحكام التفخيم والترقيق.
     *
     * حروف الاستعلاء وحدها ١٦٬٦٣٨ موضعًا، والراءات ١٢٬٤٠٣ — أي قرابة
     * ثلاثين ألفًا، وهي تصبغ الصفحة كما كان المدّ الطبيعي يفعل. فلها
     * مفتاح منفصل.
     */
    val isTafkhim: Boolean get() = family == TajweedFamily.TAFKHIM
}

/** عائلة الحكم — ترتّب مفتاح الألوان فقط، ولا أثر لها على الرسم. */
enum class TajweedFamily(val label: String%s) {
""" % (", val englishName: String" if with_english else ""))
    for i, f in enumerate(FAMILIES):
        args = kstr(f[2]) + ((", " + kstr(f[3])) if with_english else "")
        o.write("    %s(%s)%s\n" % (f[0], args, "," if i < len(FAMILIES) - 1 else ""))
    o.write("""}

/**
 * موضع حكم في نص آية: `[start, end)` بوحدات الحرف (UTF-16) داخل نص الآية.
 *
 * المدى يشمل **الحرف وعلاماته** معًا: تلوين الحرف وترك حركته بلون الحبر يخرج
 * حرفًا بلونين، وهو أسوأ من ترك التلوين.
 */
data class TajweedSpan(
    val start: Int,
    val end: Int,
    val rule: TajweedRule
)

""")
    if with_hit:
        o.write("""
/**
 * لمسة على حرف ملوّن.
 *
 * تحمل الكلمة ومدى الحرف داخلها لتعرض ورقةُ الشرح **المثالَ الذي لمسه القارئ
 * بنفسه** لا مثالًا عامًّا — أن يرى الحكم في كلمته أوضح من أي شرح.
 */
data class TajweedHit(
    val rule: TajweedRule,
    val word: String,
    val start: Int,
    val end: Int
)
""")
    return o.getvalue()


def gen_dart():
    o = io.StringIO()
    o.write("import 'package:flutter/painting.dart';\n\n")
    o.write(ddoc("The family a rule belongs to — used only to group the colour key.") + "\n")
    o.write("enum TajweedFamily {\n")
    for i, f in enumerate(FAMILIES):
        o.write("  %s(%s, %s)%s\n" % (f[1], dstr(f[2]), dstr(f[3]),
                                      "," if i < len(FAMILIES) - 1 else ";"))
    o.write("""
  const TajweedFamily(this.label, this.englishName);

  /// Arabic label.
  final String label;
  final String englishName;
}

""")
    o.write(ddoc(HEAD_AR + "\n\n## الترتيب\n" + ORDER_NOTE) + "\n")
    o.write("enum TajweedRule {\n")
    last_family = None
    for i, r in enumerate(RULES):
        fam = next(f for f in FAMILIES if f[0] == r["family"])
        if r["family"] != last_family:
            o.write("\n  // ── %s\n" % fam[2])
            last_family = r["family"]
        o.write("  %s(\n" % r["dart"])
        o.write("    color: Color(%s),\n" % r["color"])
        o.write("    label: %s,\n" % dstr(r["label"]))
        o.write("    englishName: %s,\n" % dstr(r["en"]))
        o.write("    family: TajweedFamily.%s,\n" % fam[1])
        o.write("    definition: %s,\n" % dstr(r["definition"]))
        o.write("    amount: %s,\n" % dstr(r["amount"]))
        o.write("    letters: %s,\n" % dstr(r["letters"]))
        o.write("    source: %s,\n" % dstr(r["source"]))
        o.write("    evidence: %s,\n" % dstr(r["evidence"]))
        o.write("  )%s\n" % ("," if i < len(RULES) - 1 else ";"))
    o.write("""
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
""")
    return o.getvalue()


TARGETS = [
    (os.path.join(ROOT, "mushaf", "src", "main", "java", "io", "github",
                  "sherifshabans", "mushaf", "TajweedRules.kt"),
     lambda: gen_kotlin("io.github.sherifshabans.mushaf", True,
                        with_hit=False)),
    (os.path.join(ROOT, "flutter", "lib", "src", "tajweed_rule.dart"),
     gen_dart),
]

APP = os.environ.get(
    "DAILYSEVENTY",
    os.path.join(os.path.dirname(ROOT), "DailySeventy"))
APP_FILE = os.path.join(
    APP, "app", "src", "main", "java", "com", "elsharif", "dailyseventy",
    "presentation", "quran", "TajweedRules.kt")
if os.path.isdir(APP):
    TARGETS.append((
        APP_FILE,
        lambda: gen_kotlin(
            "com.elsharif.dailyseventy.presentation.quran", True)))


def main():
    names = [r["kt"] for r in RULES]
    assert len(names) == len(set(names)) == 26, names
    for path, fn in TARGETS:
        if not os.path.isdir(os.path.dirname(path)):
            print("skip (missing dir):", path)
            continue
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(fn())
        print("wrote", path)


if __name__ == "__main__":
    main()
