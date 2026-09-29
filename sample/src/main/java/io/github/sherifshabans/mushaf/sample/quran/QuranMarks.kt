package io.github.sherifshabans.mushaf.sample.quran

/**
 * A user "mark" placed on a single ayah (read / memorised / review / custom).
 * Stored denormalised (carries its own label + colour + location) so the marks list
 * and the ayah shading stay correct even if presets change later.
 */
data class AyaMark(
    val ayaId: Int,
    val page: Int,
    val soraNameAr: String,
    val ayaNo: Int,
    val label: String,
    val colorArgb: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * العلامات اللي تنفع تتعرض، علامة واحدة لكل آية (الأحدث).
 *
 * ## ليه
 * كراش من أعطال جوجل بلاي أول ما «علاماتي» تتفتح:
 * `IllegalArgumentException: Key "0" was already used` — القائمة مفتاحها `ayaId`.
 *
 * Gson بيبني الكائن من غير constructor. لو أسماء الحقول في الـJSON المحفوظ مش
 * مطابقة — اتكتب من نسخة ريليز R8 كان مغيّر فيها أسماء الحقول قبل قاعدة keep
 * بتاعة [AyaMark] — كل حقل بيرجع قيمته الافتراضية: `ayaId = 0`، و`soraNameAr`
 * و`label` بـ`null` رغم إن نوعهم مش nullable. علامتين بالشكل ده كفاية للكراش.
 *
 * وقاعدة keep لوحدها ماكانتش هتصلّح: `addMark` بيقرا القايمة ويكتبها تاني، فالعلامات
 * البايظة بتتنقل مع كل حفظ جديد والكراش بيفضل عند المستخدم ده في كل إصدار.
 *
 * `addMark` نفسه بيمنع تكرار الآية؛ الـ`distinctBy` هنا للبيانات اللي اتكتبت قبله.
 */
fun List<AyaMark>.usable(): List<AyaMark> =
    filterNotNull()
        .filter { mark ->
            mark.ayaId > 0 &&
                mark.page > 0 &&
                (mark.soraNameAr as String?) != null &&
                (mark.label as String?) != null
        }
        .sortedByDescending { it.timestamp }
        .distinctBy { it.ayaId }

/** A quick-pick mark type shown in the palette. */
data class MarkPreset(val label: String, val colorArgb: Long)

/** Which ayah-related bottom sheet is currently open. */
enum class AyahSheetType { ACTIONS, TAFSEER, MARK }

/** Built-in quick marks. */
val DefaultMarkPresets: List<MarkPreset> = listOf(
    MarkPreset("قرأ", 0xFF2E7D32),      // green
    MarkPreset("حفظ", 0xFF1565C0),      // blue
    MarkPreset("مراجعة", 0xFFEF6C00)    // orange
)

/** Colour swatches offered when adding a custom mark. */
val MarkColorPalette: List<Long> = listOf(
    0xFF2E7D32, 0xFF1565C0, 0xFFEF6C00, 0xFFC62828,
    0xFF6A1B9A, 0xFF00838F, 0xFFAD1457, 0xFF558B2F,
    0xFF4E342E, 0xFF283593
)
