import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mushaf_text/mushaf_text.dart';

/// Holds the colouring to its stated references rather than to its own habits.
///
/// Every rule in [TajweedRule] carries a `source` and an `evidence` string —
/// the matn and the verse it comes from. A test that only checked the rules
/// against each other would pass just as happily if the reading were wrong, so
/// these cases check the *rulings the references state* against what the
/// annotator actually does over the whole mushaf.
///
/// Where the KFGQPC script encodes the ruling itself — and it does, for every
/// case below — that is stated as a count, so a change in either the text or
/// the annotator breaks the test instead of quietly changing the Qur'an on the
/// page.
void main() {
  final ayahs = File('assets/mushaf/hafs.tsv')
      .readAsLinesSync()
      .where((l) => l.trim().isNotEmpty)
      .map((l) => l.split('\t'))
      .toList(growable: false);
  String textOf(List<String> r) => r[7];
  int suraOf(List<String> r) => int.parse(r[1]);

  bool isMark(int c) =>
      (c >= 0x064B && c <= 0x0670) || (c >= 0x06D6 && c <= 0x06ED);

  group('كل حكم يحمل مرجعه', () {
    test('لا حكم بلا مصدر ولا نصّ مرجع', () {
      for (final r in TajweedRule.values) {
        expect(r.source.trim(), isNotEmpty, reason: '${r.name} بلا مصدر');
        expect(r.evidence.trim(), isNotEmpty, reason: '${r.name} بلا نصّ');
        expect(r.label.trim(), isNotEmpty);
        expect(r.definition.trim(), isNotEmpty);
      }
    });

    test('المتنان مذكوران بالاسم، والثلاثة الخارجة عنهما مُعلَّمة', () {
      // مدّ الصلة وهمزة الوصل والألف الزائدة ليست في تحفة الأطفال ولا في
      // الجزرية، ومصدرها ضبط المصحف. الادّعاء بأنها من المتنين هو بالضبط
      // الخطأ الذي يفترض أن يمنعه هذا الملف.
      const fromDabt = {
        TajweedRule.maddSila,
        TajweedRule.hamzatWasl,
        TajweedRule.silent,
      };
      for (final r in TajweedRule.values) {
        if (fromDabt.contains(r)) {
          expect(r.source, contains('في المتنين'),
              reason: '${r.name} ليست في المتنين ويجب أن يقول مصدرُها ذلك');
        } else {
          expect(
            r.source.contains('تحفة الأطفال') ||
                r.source.contains('المقدمة الجزرية'),
            isTrue,
            reason: '${r.name} مصدره ليس أحد المتنين: ${r.source}',
          );
        }
      }
    });
  });

  group('تحفة الأطفال — البيت ١١: الإظهار المطلق', () {
    // «إِلاَّ إِذَا كَانَا بِكِلْمَةٍ فَلاَ * تُدْغِمْ كَدُنْيَا ثُمَّ
    // صِنْوَانٍ تَلاَ» — النون الساكنة إذا لقيت حرفًا من «ينمو» في الكلمة
    // نفسها فلا إدغام، بل إظهار مطلق. والرسم يكتبها بالسكون، والمُعنون لا
    // يلوّن المسكَّنة، فالحكم يخرج صحيحًا من الرسم لا من استثناء مكتوب بيدنا.
    test('لا تُلوَّن دُنْيَا ولا صِنْوَان ولا قِنْوَان ولا بُنْيَان إدغامًا', () {
      const yanmu = {'ي', 'ن', 'م', 'و'};
      var sameWordPositions = 0;
      final coloured = <String>[];

      for (final r in ayahs) {
        final text = textOf(r);
        final spans = TajweedAnnotator.annotate(text, includeNaturalMadd: true);
        var at = 0;
        for (final word in text.split(' ')) {
          final from = at;
          at += word.length + 1;
          for (var i = 0; i < word.length; i++) {
            if (word[i] != 'ن') continue;
            var j = i + 1;
            while (j < word.length && isMark(word.codeUnitAt(j))) {
              j++;
            }
            // نون ساكنة فقط: المرسومة بالسكون، أو العارية منه. والمتحرّكة
            // ليست من الباب أصلًا، فعدّها يُسقط معنى الرقم.
            final marks = word.substring(i + 1, j);
            final isSakin = marks.isEmpty ||
                marks.contains('ۡ') ||
                marks.contains('ْ');
            if (!isSakin) continue;
            if (j >= word.length || !yanmu.contains(word[j])) continue;
            sameWordPositions++;
            final abs = from + i;
            for (final s in spans) {
              final isIdgham = s.rule == TajweedRule.idghamGhunna ||
                  s.rule == TajweedRule.idghamNoGhunna;
              if (isIdgham && s.start <= abs && s.end > abs) {
                coloured.add('$word (${r[1]}:${r[2]})');
              }
            }
          }
        }
      }

      expect(coloured, isEmpty,
          reason: 'إظهار مطلق لُوِّن إدغامًا — مخالف للبيت ١١: $coloured');
      expect(sameWordPositions, 125,
          reason: 'عدد مواضع النون الساكنة قبل حرف من ينمو في كلمة واحدة');
    });
  });

  group('تحفة الأطفال — الأبيات ٥٤–٥٦: المدّ في فواتح السور', () {
    // «يَجْمَعُهَا حُرُوفُ كَمْ عَسَلْ نَقَصْ» — هذه الثمانية وحدها مدُّها
    // لازم ستُّ حركات. «وَمَا سِوَي الحَرْفِ الثُّلاَثِي لاَ أَلِفْ *
    // فَمُدُّه مَدّاً طَبِيعِيَّا أُلِفْ» في «حَيٍّ طَاهِرٍ»، والألف لا مدَّ
    // فيها.
    const openers = [
      2, 3, 7, 10, 11, 12, 13, 14, 15, 19, 20, 26, 27, 28, 29,
      30, 31, 32, 36, 38, 40, 41, 42, 43, 44, 45, 46, 50, 68,
    ];
    const lazim = 'كمعسلنقص';
    const natural = 'حيطهر';
    const maddah = 'ٓ';

    test('«كم عسل نقص» وحدها عليها المدّة، و«حي طهر» والألف لا', () {
      final wrong = <String>[];
      var lazimSeen = 0, naturalSeen = 0, alefSeen = 0;

      for (final s in openers) {
        final row = ayahs.firstWhere((r) => suraOf(r) == s);
        final text = textOf(row);
        // الفاتحة هي أوّل كلمة، وحروفها مفردة.
        final opener = text.split(' ').first;
        for (var i = 0; i < opener.length; i++) {
          final c = opener[i];
          if (isMark(c.codeUnitAt(0))) continue;
          final hasMaddah = i + 1 < opener.length && opener[i + 1] == maddah;
          if (c == 'ا') {
            alefSeen++;
            if (hasMaddah) wrong.add('سورة $s: الألف عليها مدّة');
          } else if (lazim.contains(c)) {
            lazimSeen++;
            if (!hasMaddah) wrong.add('سورة $s: $c من «كم عسل نقص» بلا مدّة');
          } else if (natural.contains(c)) {
            naturalSeen++;
            if (hasMaddah) wrong.add('سورة $s: $c من «حي طهر» عليها مدّة');
          }
        }
      }

      expect(wrong, isEmpty, reason: wrong.join(' | '));
      // لو تغيّر النص فجأة وخلت الفواتح، الفحص أعلاه يمرّ فارغًا — فنثبّت
      // الأعداد حتى يظلّ الاختبار يفحص شيئًا.
      expect(lazimSeen, greaterThanOrEqualTo(30));
      expect(naturalSeen, greaterThanOrEqualTo(14));
      expect(alefSeen, greaterThanOrEqualTo(13));
    });
  });

  group('ضبط المصحف — الصفر المستطيل القائم', () {
    // «يدلّ على زيادتها وصلًا لا وقفًا» — وهي ألف دائمًا، لا واو ولا ياء.
    // هذا ما صحّح الوصفَ السابق: كان مكتوبًا «الصفر المستدير» و«لا يُنطق»،
    // وكلاهما خطأ — الحرف يثبت ألفًا عند الوقف.
    test('كل مواضع الألف الزائدة ألفٌ، وعددها ٦٦', () {
      var n = 0;
      for (final r in ayahs) {
        final text = textOf(r);
        for (final s in TajweedAnnotator.annotate(text)) {
          if (s.rule != TajweedRule.silent) continue;
          n++;
          expect(text.substring(s.start, s.end), contains('ا'),
              reason: 'الألف الزائدة على حرف غير الألف: '
                  '${text.substring(s.start, s.end)}');
        }
      }
      expect(n, 66);
      expect(TajweedRule.silent.label, 'ألف زائدة');
      expect(TajweedRule.silent.amount, contains('وقفًا'));
    });
  });

  group('تحفة الأطفال — الأبيات ٧ و٨: الإظهار الحلقي لا يُلوَّن', () {
    // «فَالأَوَّلُ الإظْهَارُ قَبْلَ أَحْرُفِ * لِلْحَلْقِ سِتٍ» — الإظهار
    // ليس له لون، لأن التلوين يعرض ما يُغيَّر فيه النطق. تلوين الإظهار يجعل
    // القارئ يظنّ أن فيه عملًا.
    test('لا نون مظهَرة ملوَّنة', () {
      var izhar = 0, wrong = 0;
      for (final r in ayahs) {
        final text = textOf(r);
        final spans = TajweedAnnotator.annotate(text, includeNaturalMadd: true);
        for (var i = 0; i < text.length; i++) {
          if (text[i] != 'ن') continue;
          var j = i + 1;
          final marks = <String>[];
          while (j < text.length && isMark(text.codeUnitAt(j))) {
            marks.add(text[j]);
            j++;
          }
          if (!marks.contains('ۡ') && !marks.contains('ْ')) continue;
          if (j >= text.length) continue;
          if (!'ءهعحغخ'.contains(text[j])) continue;
          izhar++;
          for (final s in spans) {
            if (s.start <= i && s.end > i && s.rule != TajweedRule.qalqala) {
              wrong++;
            }
          }
        }
      }
      expect(izhar, greaterThan(400), reason: 'لم يُفحص إظهار حلقي أصلًا');
      expect(wrong, 0, reason: 'نون مظهَرة لُوِّنت $wrong مرة');
    });
  });
}
