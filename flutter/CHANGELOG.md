## 1.4.0

التفخيم والترقيق أحكام كسائر الأحكام، فلا مفتاح لها.

Tafkhim and tarqiq are rulings like any other, so they have no switch.

- حُذف `tajweedTafkhim` من `MushafPage` و`includeTafkhim` من
  `TajweedAnnotator.annotate` و`pageTajweedCounts`. الأحكام تُطبَّق دائمًا.
  <br>*`tajweedTafkhim` is gone from `MushafPage`, and `includeTafkhim` from
  `TajweedAnnotator.annotate` and `pageTajweedCounts`. The rules always apply.*
- لون **الألف الزائدة** صار مستقلًّا عن لون **همزة الوصل**، وكانا لونًا واحدًا.
  وهما حكمان مختلفان: همزة الوصل تسقط في الوصل وتُنطق في الابتداء، والألف
  الزائدة تسقط في الوصل وتثبت في الوقف.
  <br>*The extra alef now has its own colour, separate from hamzat al-wasl.
  They are different rulings: the wasl hamza is dropped when you join and
  sounded when you begin; the extra alef is dropped when you join and kept when
  you stop.*
- المدّ الطبيعي على حاله: مفتاحه باقٍ ومغلق افتراضيًا.
  <br>*The natural madd is unchanged: its switch stays, and stays off.*

## 1.3.0

التفخيم والترقيق تظهر افتراضيًا، بألوان تُرى.

Tafkhim and tarqiq show by default, in colours that can be seen.

- `tajweedTafkhim` و`includeTafkhim` صارت `true` افتراضيًا — فهي أحكام كسائر
  الأحكام. والمفتاح باقٍ لمن أراد صفحة أخفّ.
  <br>*`tajweedTafkhim` and `includeTafkhim` now default to `true`: they are
  rulings like any other. The switch stays for a lighter page.*
- ألوان التفخيم والراء ولام الجلالة اختيرت بالقياس: أن تبعد عن لون الحبر وعن
  لون الورق وعن كل حكم آخر، وأن تبقى في سجلّ الإضاءة والإشباع نفسه. والتفخيم
  أكتمها لأنه يقع على كل سطر تقريبًا.
  <br>*The tafkhim, ra and lam colours were chosen by measurement: far enough
  from the ink, from the paper, and from every other rule, while staying in the
  palette's own register. Tafkhim is the quietest of them, falling as it does on
  nearly every line.*

## 1.2.1

صياغة الوصف في الـREADME وسجلّ التغييرات.

Wording of the README and the changelog.

## 1.2.0

الأحكام صارت ستة وعشرين.

The rule set is now twenty-six.

- **التفخيم والترقيق**: حروف الاستعلاء «خُصَّ ضَغْطٍ قِظْ»، وباب الراءات —
  مفخّمة ومرقّقة — وتفخيم لام لفظ الجلالة، من المقدمة الجزرية. ولها مفتاحها
  `tajweedTafkhim` لأنها نحو ٣٠ ألف موضع، فتصبغ الصفحة كما يفعل المدّ الطبيعي.
  <br>*Tafkhim and tarqiq: the seven istiʿlāʾ letters, the chapter on the rāʾ,
  and the heavy lām of the divine name, from al-Jazariyya. Behind
  `tajweedTafkhim`, being some 30,000 positions.*
- **المتماثلان والمتجانسان والمتقاربان** (تحفة الأطفال ٣٠–٣٣). والرسم يكتبها
  بنفس إشارة إدغام النون، وأزواجها هي المعروفة عند القرّاء: المتجانسان
  (د ت، ت د، ت ط، ذ ظ، ب م) والمتقاربان (ل ر، ق ك).
  <br>*Mutamathilayn, mutajanisayn and mutaqaribayn. The script writes them with
  the same signal it uses for the noon, and their pairs are exactly the ones the
  reciters name.*
- **مدّ اللين** (تحفة ٤١) عند مواضع الوقف.
  <br>*The madd leen, at a stop.*
- لام «أل» بعد لام الجرّ — «لِلنَّاسِ» و«لِلتَّقْوَىٰ» — لام شمسية، وإن سقطت
  ألف «أل» من الرسم.
  <br>*The lam of «أل» after the preposition lam is a solar lam, even though the
  article's alef is not written.*
- الـREADME يذكر **ما لا تغطّيه** المكتبة: السكت والإمالة والتسهيل والإشمام، وما
  لا يتغيّر فيه النطق.
  <br>*The README states what the library does not cover.*
- فيكسشر التطابق يشغّل كل المفاتيح، فالأحكام الستة والعشرون كلها مقابَلة بين
  Kotlin وDart: **١١٤٬٣٣١ موضعًا**.
  <br>*The parity fixture switches every rule on, so all 26 are compared across
  the two ports: 114,331 spans.*

## 1.1.1

توثيق بالعربي والإنجليزي، بلا أي تغيير في الكود.

Documentation in Arabic and English, with no code change.

- صار الـREADME ثنائيَّ اللغة بالكامل: كل فقرة بالعربي وبالإنجليزي، بما فيها
  جدول علامات الرسم وقائمة المراجع والتنبيه على أن الصياغة لم يراجعها قارئ
  مُجاز بعد.
  <br>*The README is now fully bilingual — every section in Arabic and English,
  including the notation table, the list of references, and the note that the
  wording has not yet been reviewed by a qualified reciter.*

## 1.1.0

صار كلُّ حكمٍ يسمّي مرجعه، وصار الكود يُحاكَم إلى تلك المراجع لا إلى نفسه.

Every tajweed rule now names the reference it comes from, and the code is tested
against those references rather than against itself.

- `TajweedRule` اكتسب `source` و`evidence`: المتن، ورقم البيت، والبيت بنصّه.
  تحفة الأطفال لأحكام النون والميم ولام «أل» والمدود، والمقدمة الجزرية للقلقلة
  والغنّة، ودليل ضبط مجمّع الملك فهد للأحكام الثلاثة التي لا يغطّيها المتنان —
  وهي تقول ذلك عن نفسها.
  <br>*`TajweedRule` gains `source` and `evidence`: the matn, the verse number,
  and the verse in its own words. تحفة الأطفال for the noon, meem, lām and madd
  rules; المقدمة الجزرية for qalqalah and ghunnah; the King Fahd Complex's own
  notation guide for the three rules neither matn covers — which say so.*
- `pageTajweedCounts(page)` — كم كلمةً في الصفحة تحمل كلَّ حكم، معدودةً بنفس
  الطريقة التي يتنقّل بها إطار التوقيف بينها.
  <br>*How many words on a page carry each rule, counted the same way the focus
  frame steps through them.*
- **الألف الزائدة**: علامتها في هذا النصّ الصفر المستطيل القائم، ومعناها أن
  الألف تسقط في الوصل و**تثبت في الوقف** — أي تُنطق إذا وقفتَ عليها.
  <br>*The extra alef: its mark in this text is the upright rectangular zero,
  and it means the alef is dropped in waṣl but **kept in waqf** — it is
  pronounced when you stop on it.*
- صار `TajweedRule` و`TajweedFamily` مولَّدَين من جدول واحد مشترك مع حزمة
  Kotlin، فلا تصف النسختان الحرف نفسه وصفين مختلفين.
  <br>*`TajweedRule` and `TajweedFamily` are now generated from one table shared
  with the Kotlin package, so the two ports cannot describe the same letter
  differently.*
- صار تطبيق المثال مطابقًا لقارئ أندرويد: شريط التوقيف، وورقة التصفّح بمبدّل
  سفلي، والمرجع داخل ورقة الحكم.
  <br>*The example app matches the Android reader: the focus navigator, the
  browse sheet with a bottom switch, and the reference in the rule sheet.*

## 1.0.0

أول إصدار — الجانب الفلاتري من
[mushaf-text](https://github.com/sherifshabans/mushaf-text).

First release — the Flutter side of
[mushaf-text](https://github.com/sherifshabans/mushaf-text).

- `MushafPage` يرسم أيًّا من الصفحات الـ٦٠٤ من نصّ مجمّع الملك فهد المرفق،
  مكسورةً سطورًا كما يكسرها مصحف المدينة بالضبط.
  <br>*`MushafPage` draws any of the 604 pages from the bundled KFGQPC Uthmanic
  text, broken into lines exactly as the Madinah print does.*
- تلوين التجويد، ثمانية عشر حكمًا لكلٍّ لونه، ولمس الحرف الملوّن يرجّع حكمه.
  <br>*Tajweed colouring, 18 rules with a colour each, and a tap on a coloured
  letter returns the rule it belongs to.*
- `Quran` و`TajweedAnnotator` يصلحان وحدهما، دون رسم شيء.
  <br>*`Quran` and `TajweedAnnotator` are usable on their own, without drawing.*
- `MushafColors.light` و`.dark`، مع تغيير لون أي حكم على حدة.
  <br>*`MushafColors.light` / `.dark`, with per-rule overrides.*
- الأحكام مستخرجة من رسم النصّ المرفق نفسه، واختبارُ تطابقٍ يثبّتها على مخرجات
  تنفيذ Kotlin في الآيات الـ٦٢٣٦ — ٨٥٬٣٨٦ موضعًا.
  <br>*The rules are derived from the bundled script's own orthography, and a
  parity test holds them to the Kotlin implementation's output for all 6236
  verses — 85,386 spans.*
