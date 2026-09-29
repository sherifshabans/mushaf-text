package io.github.sherifshabans.mushaf.sample.quran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextPaint as AndroidTextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import androidx.core.content.res.ResourcesCompat
import io.github.sherifshabans.mushaf.sample.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Share card for Quran verses.
 *
 * Deliberately a different object from the azkar/dua card: that one is a modern
 * rounded card with a logo and a count badge, this one is a **mushaf leaf** —
 * double gold rule with corner arabesques, an ornate surah banner, the verses in
 * the app's Uthmanic font with gilded ﴿ ﴾ markers, and a page/juz footer.
 *
 * Size is not fixed. Both the leaf width and the text size come from measuring the
 * real wrapped text ([ShareTextFitter]), so one short verse gets a compact square
 * and a long passage gets a wider leaf instead of an unreadable ribbon.
 */
object AyahShareImage {

    /** One verse as the mushaf stores it. */
    data class Verse(
        val sura: Int,
        val suraName: String,
        val number: Int,
        /** Uthmani text WITHOUT the trailing verse number. */
        val text: String
    )

    enum class Skin { LIGHT, DARK }

    /**
     * How much may be shared at once — roughly two mushaf pages.
     * A page runs from about 4 verses (long Baqarah pages) to 40+ (juz 30), so the
     * cap is expressed both ways and whichever bites first wins.
     */
    const val MAX_VERSES = 50
    const val MAX_PAGES = 2

    suspend fun render(context: Context, verses: List<Verse>, skin: Skin): Bitmap =
        withContext(Dispatchers.Default) { Painter.draw(context, verses.take(MAX_VERSES), skin) }

    /**
     * Plain-text form, for copy and for text sharing.
     *
     * The passage is wrapped in `{ }` — how Quran is quoted in Arabic writing and
     * in messaging apps — with the ﴿٢﴾ verse marks kept inside it.
     */
    fun buildText(verses: List<Verse>, withAppTag: Boolean = true): String {
        if (verses.isEmpty()) return ""
        val body = verses.joinToString(" ") { "${it.text.trim()} ﴿${arabicNumerals(it.number)}﴾" }
        val sb = StringBuilder("{").append(body).append("}")
            .append('\n').append(reference(verses))
        if (withAppTag) sb.append("\n\nسبعون مرة ✦ رفيقك اليومي لذكر الله")
        return sb.toString()
    }

    /** "[سورة البقرة: ٢٥٥]" or "[سورة البقرة: ١-٥]". */
    fun reference(verses: List<Verse>): String {
        if (verses.isEmpty()) return ""
        val first = verses.first()
        val sameSura = verses.all { it.sura == first.sura }
        return if (!sameSura) {
            val names = verses.map { it.suraName }.distinct().joinToString("، ")
            "[$names]"
        } else if (verses.size == 1) {
            "[سورة ${first.suraName}: ${arabicNumerals(first.number)}]"
        } else {
            "[سورة ${first.suraName}: ${arabicNumerals(first.number)}-${arabicNumerals(verses.last().number)}]"
        }
    }

    private fun arabicNumerals(n: Int): String =
        n.toString().map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

    // ─────────────────────────────────────────────────────────────────────────

    private data class Skinning(
        val bgTop: Int, val bgBottom: Int,
        val leaf: Int, val leafStroke: Int,
        val rule: Int, val ornament: Int,
        val ink: Int, val inkSoft: Int,
        val bannerFill: Int, val bannerStroke: Int,
        val marker: Int,
        val watermarkAlpha: Float,
        val rounded: Boolean
    )

    private fun skinning(skin: Skin) = when (skin) {
        Skin.LIGHT -> Skinning(
            bgTop = 0xFFF3EAD6.toInt(), bgBottom = 0xFFE7D9BC.toInt(),
            leaf = 0xFFFDFAF1.toInt(), leafStroke = 0xFFDCCBA4.toInt(),
            rule = 0xFFC4B275.toInt(), ornament = 0xFFC4B275.toInt(),
            ink = 0xFF1A1208.toInt(), inkSoft = 0xFF6B4F3A.toInt(),
            bannerFill = 0xFFF4EDE0.toInt(), bannerStroke = 0xFFC4B275.toInt(),
            marker = 0xFFB08D3F.toInt(),
            watermarkAlpha = 0.10f, rounded = false
        )
        Skin.DARK -> Skinning(
            bgTop = 0xFF14110B.toInt(), bgBottom = 0xFF0B0906.toInt(),
            leaf = 0xFF1D1810.toInt(), leafStroke = 0xFF3A3122.toInt(),
            rule = 0xFFC9B47A.toInt(), ornament = 0xFFC9B47A.toInt(),
            ink = 0xFFF2E9D6.toInt(), inkSoft = 0xFFB3A387.toInt(),
            bannerFill = 0xFF272013.toInt(), bannerStroke = 0xFFC9B47A.toInt(),
            marker = 0xFFE0C98C.toInt(),
            watermarkAlpha = 0.07f, rounded = false
        )
    }

    // ─────────────────────────────────────────────────────────────────────────

    private object Painter {

        private const val S = 2f
        private val WIDTH_TIERS_DP = floatArrayOf(360f, 450f, 560f)
        private const val PAD_DP = 30f
        private const val MIN_SP = 15f
        private const val MAX_SP = 26f
        private const val COMFY_SP = 19f
        private const val MAX_RATIO = 1.75f

        /**
         * Uthmanic glyphs carry their own tall marks, so the airy 1.3/0.18 that
         * suits the azkar card left the verses swimming in white. Tightened until
         * the block reads as a paragraph, not a list of lines.
         */
        private const val LINE_MULT = 1.12f
        private const val LINE_ADD = 0.10f

        fun draw(context: Context, verses: List<Verse>, skin: Skin): Bitmap {
            val t = skinning(skin)
            val quranFont = runCatching {
                ResourcesCompat.getFont(context, io.github.sherifshabans.mushaf.R.font.uthmanic_hafs)
            }.getOrNull() ?: Typeface.SERIF
            val uiFont = runCatching {
                ResourcesCompat.getFont(context, R.font.amiri_bold)
            }.getOrNull() ?: Typeface.DEFAULT_BOLD

            val pad = PAD_DP * S

            val body = buildBody(verses, t.ink, t.marker, uiFont)

            fun bodyPaint(size: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = quranFont
                textSize = size
                color = t.ink
            }

            // The leaf sits inside a margin, so the text width budget is narrower
            // than the image by the margin *and* the padding.
            val margin = 16f * S
            val frameInset = 12f * S
            val sidePad = pad + margin + frameInset

            // Justified like a printed mushaf: both margins flush, no ragged gap
            // down one side. A one- or two-line passage is centred instead —
            // justifying two lines only stretches them oddly.
            val fit = ShareTextFitter.fit(
                text = body,
                widthTiersPx = WIDTH_TIERS_DP.map { (it * S).toInt() }.toIntArray(),
                sidePaddingPx = sidePad,
                minPx = MIN_SP * S,
                maxPx = MAX_SP * S,
                comfortablePx = COMFY_SP * S,
                maxHeightRatio = MAX_RATIO,
                lineSpacingMult = LINE_MULT,
                justify = true,
                spacingAddRatio = LINE_ADD,
                paintFactory = ::bodyPaint
            )

            val cw = fit.cardWidthPx
            val textWidth = (cw - sidePad * 2).toInt().coerceAtLeast(1)
            // Short passages read better centred than flush-right on one line.
            val bodyLayout = if (fit.layout.lineCount <= 3) {
                ShareTextFitter.rtlLayout(
                    body, bodyPaint(fit.textSizePx), textWidth, LINE_MULT,
                    alignment = android.text.Layout.Alignment.ALIGN_CENTER,
                    spacingAddRatio = LINE_ADD
                )
            } else fit.layout

            // ── header pieces ────────────────────────────────────────────────
            val bannerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiFont; textSize = 17f * S; color = t.inkSoft
                textAlign = Paint.Align.CENTER
            }
            val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiFont; textSize = 11.5f * S; color = t.inkSoft
                textAlign = Paint.Align.CENTER; alpha = 220
            }
            val footPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiFont; textSize = 10.5f * S; color = t.inkSoft
                textAlign = Paint.Align.CENTER; alpha = 190; letterSpacing = 0.06f
            }

            val bannerH = 42f * S
            val chipH = 26f * S
            val gapLg = 22f * S
            val gapMd = 16f * S
            val gapSm = 10f * S
            val ruleH = 2f * S
            val footerH = 22f * S

            var h = margin + frameInset + pad
            h += bannerH + gapSm
            h += chipH + gapMd
            h += ruleH + gapLg
            h += bodyLayout.height + gapLg
            h += ruleH + gapMd
            h += footerH + pad + frameInset + margin
            val ch = h.toInt()

            val bmp = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // ── background ───────────────────────────────────────────────────
            canvas.drawPaint(Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, ch.toFloat(),
                    intArrayOf(t.bgTop, t.bgBottom), null, Shader.TileMode.CLAMP
                )
            })
            drawStarLattice(canvas, cw, ch, t.ornament, t.watermarkAlpha)

            // ── the leaf ─────────────────────────────────────────────────────
            val leafRect = RectF(margin, margin, cw - margin, ch - margin)
            val radius = if (t.rounded) 28f * S else 6f * S
            canvas.drawRoundRect(leafRect, radius, radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.leaf })
            canvas.drawRoundRect(leafRect, radius, radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; color = t.leafStroke; strokeWidth = 1.5f * S
                })

            // classic mushaf double rule, inset from the leaf edge
            val inner = RectF(
                leafRect.left + frameInset, leafRect.top + frameInset,
                leafRect.right - frameInset, leafRect.bottom - frameInset
            )
            val innerR = if (t.rounded) radius * 0.7f else 3f * S
            canvas.drawRoundRect(inner, innerR, innerR,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; color = t.rule; strokeWidth = 2.5f * S
                })
            val inner2 = RectF(inner.left + 5f * S, inner.top + 5f * S,
                inner.right - 5f * S, inner.bottom - 5f * S)
            canvas.drawRoundRect(inner2, innerR, innerR,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; color = t.rule; strokeWidth = 0.9f * S
                    alpha = 150
                })
            corners(canvas, inner, t.ornament, S, t.rounded)

            // ── content ──────────────────────────────────────────────────────
            var y = margin + frameInset + pad
            val cx = cw / 2f

            // surah banner
            val suraLabel = "سورة ${verses.firstOrNull()?.suraName.orEmpty()}"
            val bw = bannerPaint.measureText(suraLabel) + 76f * S
            val bRect = RectF(cx - bw / 2f, y, cx + bw / 2f, y + bannerH)
            canvas.drawRoundRect(bRect, bannerH / 2f, bannerH / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.bannerFill })
            canvas.drawRoundRect(bRect, bannerH / 2f, bannerH / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; color = t.bannerStroke; strokeWidth = 1.6f * S
                })
            // little rosettes flanking the name
            star8(canvas, bRect.left + 20f * S, bRect.centerY(), 6.5f * S,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.ornament; alpha = 200 })
            star8(canvas, bRect.right - 20f * S, bRect.centerY(), 6.5f * S,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.ornament; alpha = 200 })
            canvas.drawText(suraLabel, cx,
                bRect.centerY() + bannerPaint.textSize * 0.36f, bannerPaint)

            y += bannerH + gapSm

            // ayah range chip
            val chip = ayahRangeLabel(verses)
            val chipW = refPaint.measureText(chip) + 34f * S
            val chipRect = RectF(cx - chipW / 2f, y, cx + chipW / 2f, y + chipH)
            canvas.drawRoundRect(chipRect, chipH / 2f, chipH / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = t.rule; alpha = 38
                })
            canvas.drawText(chip, cx, chipRect.centerY() + refPaint.textSize * 0.36f, refPaint)
            y += chipH + gapMd

            rule(canvas, inner2.left + 18f * S, inner2.right - 18f * S, y, t, S)
            y += ruleH + gapLg

            // verses
            // The text box is exactly the width the fitter measured, centred in the
            // leaf: left edge at sidePad, right edge at cw - sidePad.
            canvas.save()
            canvas.translate(sidePad, y)
            bodyLayout.draw(canvas)
            canvas.restore()
            y += bodyLayout.height + gapLg

            rule(canvas, inner2.left + 18f * S, inner2.right - 18f * S, y, t, S)
            y += ruleH + gapMd

            canvas.drawText("سبعون مرة  ✦  رفيقك اليومي لذكر الله", cx,
                y + footerH * 0.72f, footPaint)

            return bmp
        }


        private fun ayahRangeLabel(verses: List<Verse>): String {
            if (verses.isEmpty()) return ""
            val nums = verses.map { it.number }
            return if (verses.size == 1) "الآية ${arabicNumerals(nums.first())}"
            else "الآيات ${arabicNumerals(nums.first())} - ${arabicNumerals(nums.last())}" +
                    "  ✦  ${arabicNumerals(verses.size)} آيات"
        }

        /**
         * The verse text in ink and the ﴿ ﴾ marker in gold, as one spanned string so
         * StaticLayout wraps the whole passage as a single paragraph.
         */
        /** Forces a typeface on a span range, on every API level. */
        private class FaceSpan(private val face: Typeface) : MetricAffectingSpan() {
            override fun updateDrawState(tp: AndroidTextPaint) { tp.typeface = face }
            override fun updateMeasureState(tp: AndroidTextPaint) { tp.typeface = face }
        }

        private fun buildBody(
            verses: List<Verse>,
            ink: Int,
            marker: Int,
            markerFace: Typeface
        ): SpannableStringBuilder {
            val sb = SpannableStringBuilder()
            verses.forEachIndexed { i, v ->
                if (i > 0) sb.append(' ')
                val textStart = sb.length
                sb.append(v.text.trim())
                sb.setSpan(ForegroundColorSpan(ink), textStart, sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                // non-breaking: the ﴿٤﴾ marker must never wrap onto a line of its own
                sb.append(' ')
                val markStart = sb.length
                sb.append("﴿${arabicNumerals(v.number)}﴾")
                sb.setSpan(ForegroundColorSpan(marker), markStart, sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(FaceSpan(markerFace), markStart, sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return sb
        }

        private fun rule(canvas: Canvas, left: Float, right: Float, y: Float, t: Skinning, s: Float) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = t.rule; strokeWidth = 1.2f * s; alpha = 130
            }
            val mid = (left + right) / 2f
            val gap = 16f * s
            canvas.drawLine(left, y, mid - gap, y, p)
            canvas.drawLine(mid + gap, y, right, y, p)
            star8(canvas, mid, y, 6f * s,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.ornament; alpha = 210 })
        }

        private fun corners(canvas: Canvas, r: RectF, color: Int, s: Float, rounded: Boolean) {
            if (rounded) return   // the cartoon leaf is soft-cornered; arabesques would fight it
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; this.color = color
                strokeWidth = 1.3f * s; alpha = 190
            }
            val len = 26f * s
            listOf(
                Triple(r.left, r.top, 1f to 1f),
                Triple(r.right, r.top, -1f to 1f),
                Triple(r.left, r.bottom, 1f to -1f),
                Triple(r.right, r.bottom, -1f to -1f)
            ).forEach { (x, y, dir) ->
                val (dx, dy) = dir
                val path = Path().apply {
                    moveTo(x + dx * len, y + dy * 4f * s)
                    quadTo(x + dx * 10f * s, y + dy * 10f * s, x + dx * 4f * s, y + dy * len)
                }
                canvas.drawPath(path, p)
            }
        }

        private fun star8(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
            val path = Path()
            for (i in 0 until 16) {
                val rad = if (i % 2 == 0) r else r * 0.45f
                val a = (-Math.PI / 2 + i * Math.PI / 8).toFloat()
                val x = cx + rad * cos(a)
                val y = cy + rad * sin(a)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, paint)
        }

        /** Watermark: a lattice of eight-point stars — the mushaf's motif, not the card's. */
        private fun drawStarLattice(canvas: Canvas, w: Int, h: Int, color: Int, alpha: Float) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; this.color = color
                strokeWidth = 1f; this.alpha = (255 * alpha).toInt().coerceIn(0, 255)
            }
            val step = min(w, h) / 7f
            var y = -step / 2f
            var row = 0
            while (y < h + step) {
                var x = if (row % 2 == 0) -step / 2f else 0f
                while (x < w + step) {
                    star8(canvas, x, y, step * 0.22f, p)
                    x += step
                }
                y += step * 0.86f
                row++
            }
        }
    }
}
