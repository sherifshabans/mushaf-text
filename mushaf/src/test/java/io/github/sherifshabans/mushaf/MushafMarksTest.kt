package io.github.sherifshabans.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * علامات الأقسام في الهامش: الاتجاه بالحرف الداخلي (`۞` / `۩`)، والوردة
 * على ارتفاع السطر بالظبط، والهندسة ما تتغيرش إلا مع `divisionMarks`.
 */
class MushafMarksTest {

    private fun pageLines(page: Int): Pair<List<Ayah>, Map<Int, List<MushafToken>>> {
        val ayahs = TestData.ayahs.filter { it.page == page }
        val (tokens, _) = buildPageTokens(ayahs) { toArabicDigits(it) }
        val lines = TestData.layout.getValue(page).sliceTokens(tokens)
            ?: error("page $page does not match the reference layout")
        return ayahs to lines
    }

    /** سطر الحرف الداخلي (أو أول كلمة) لآية — بالمقارنة على نفس التوكِنز. */
    private fun targetLine(
        lines: Map<Int, List<MushafToken>>,
        ayahId: Int,
        glyph: Char?,
    ): Int {
        val sorted = lines.toSortedMap()
        var first: Int? = null
        for ((line, tokens) in sorted) {
            for (token in tokens) {
                if (token.ayahId != ayahId) continue
                if (first == null) first = line
                if (glyph != null && glyph in token.text) return line
            }
        }
        return glyph?.let { error("glyph $it not found for ayah $ayahId") } ?: first!!
    }

    private fun anchorsFor(
        page: Int,
        pitch: Float = 20f,
        offsetY: Float = 0f,
    ): Pair<List<Ayah>, List<MarkAnchor>> {
        val (ayahs, lines) = pageLines(page)
        val anchors = markAnchors(
            marks = MushafDivisions.marksOn(page),
            ayahs = ayahs,
            lineTokens = lines,
            pitch = pitch,
            offsetY = offsetY,
            frameTop = 0f,
            frameBottom = 1000f,
            stackDistance = 22f,
        )
        return ayahs to anchors
    }

    private fun assertOnLine(
        anchors: List<MarkAnchor>,
        ayahs: List<Ayah>,
        page: Int,
        surah: Int,
        number: Int,
        glyph: Char?,
        pitch: Float = 20f,
        offsetY: Float = 0f,
    ): MarkAnchor {
        val anchor = anchors.single { it.mark.surah == surah && it.mark.ayah == number }
        val id = ayahs.single { it.surah == surah && it.number == number }.id
        val line = targetLine(pageLines(page).second, id, glyph)
        val bandTop = offsetY + pitch * (line - 1)
        val bandBottom = offsetY + pitch * line
        assertTrue(
            "anchor of $surah:$number at ${anchor.centerY} outside line $line band [$bandTop, $bandBottom]",
            anchor.centerY > bandTop && anchor.centerY < bandBottom,
        )
        return anchor
    }

    @Test
    fun rubOf226AnchorsToTheStarLineOnPage5() {
        val (ayahs, anchors) = anchorsFor(5)
        assertEquals(1, anchors.size)
        val anchor = assertOnLine(anchors, ayahs, 5, surah = 2, number = 26, glyph = '۞')
        assertEquals(DivisionKind.RUB, anchor.mark.kind)

        // والوردة نفسها برّة مستطيل النص — في الهامش على الخط الخارجي.
        val g = PageFrames.Ornate.geometry(
            PageFrameRequest(
                widthPx = 1080f, heightPx = 1920f, pageNumber = 5, density = 2.625f,
                outerMargin = 5f, innerMargin = 2f, topMargin = 1.5f, bottomMargin = 14f,
                textGap = 8f, divisionMarks = true,
            )
        )
        assertTrue(
            "markAnchorX ${g.markAnchorX} must sit outside the content rect",
            g.markAnchorX < g.contentLeft || g.markAnchorX > g.contentLeft + g.contentWidth,
        )
    }

    @Test
    fun juzStartAnchorsOnPage22() {
        val (ayahs, anchors) = anchorsFor(22)
        val anchor = assertOnLine(anchors, ayahs, 22, surah = 2, number = 142, glyph = '۞')
        assertEquals(DivisionKind.JUZ, anchor.mark.kind)
        assertEquals(2, anchor.mark.juz)
    }

    @Test
    fun surahOpeningAnchorsToTheFirstVerseLineNotTheReservedLine() {
        val (ayahs, anchors) = anchorsFor(77)
        val anchor = assertOnLine(anchors, ayahs, 77, surah = 4, number = 1, glyph = null)
        assertEquals(DivisionKind.NISF, anchor.mark.kind)

        // أول سطر لآية ٤:١ سطر آيات — مش البانر ولا البسملة.
        val id = ayahs.single { it.surah == 4 && it.number == 1 }.id
        val line = targetLine(pageLines(77).second, id, null)
        assertTrue(
            "line $line is a reserved (banner/basmala) line",
            line !in TestData.layout.getValue(77).reservedLines,
        )
    }

    @Test
    fun sajdahAnchorsToTheSajdahGlyphLineOnPage176() {
        val (ayahs, anchors) = anchorsFor(176)
        val anchor = assertOnLine(anchors, ayahs, 176, surah = 7, number = 206, glyph = '۩')
        assertEquals(DivisionKind.SAJDAH, anchor.mark.kind)
    }

    @Test
    fun page264MarksTheCorrected1549() {
        val (ayahs, anchors) = anchorsFor(264)
        val anchor = assertOnLine(anchors, ayahs, 264, surah = 15, number = 49, glyph = '۞')
        assertEquals(DivisionKind.RUB, anchor.mark.kind)
    }

    @Test
    fun divisionMarksGrowContentTopOnlyWhenOn() {
        val request = PageFrameRequest(
            widthPx = 1080f, heightPx = 1920f, pageNumber = 2, density = 2.625f,
            outerMargin = 5f, innerMargin = 2f, topMargin = 1.5f, bottomMargin = 14f,
            textGap = 8f, divisionMarks = false,
        )
        val plain = PageFrames.Ornate.geometry(request)
        val withMarks = PageFrames.Ornate.geometry(request.copy(divisionMarks = true))
        val expected = ornateFrameGeometry(1080f, 1920f, 2, 2.625f)

        // العلم مقفول = نفس هندسة 1.5.0 بالظبط، حقلًا بحقل.
        assertEquals(expected.frameLeft, plain.frameLeft, 0f)
        assertEquals(expected.frameTop, plain.frameTop, 0f)
        assertEquals(expected.frameRight, plain.frameRight, 0f)
        assertEquals(expected.frameBottom, plain.frameBottom, 0f)
        assertEquals(expected.contentLeft, plain.contentLeft, 0f)
        assertEquals(expected.contentTop, plain.contentTop, 0f)
        assertEquals(expected.contentWidth, plain.contentWidth, 0f)
        assertEquals(expected.contentHeight, plain.contentHeight, 0f)

        // العلم مفتوح: شريط الرأس بس هو اللي بيكبّر contentTop.
        val strip = RUNNING_HEAD_HEIGHT * 2.625f
        assertEquals(plain.contentTop + strip, withMarks.contentTop, 0f)
        assertEquals(plain.contentHeight - strip, withMarks.contentHeight, 0f)
        assertEquals(plain.contentLeft, withMarks.contentLeft, 0f)
        assertEquals(plain.contentWidth, withMarks.contentWidth, 0f)
        assertEquals(plain.frameBottom, withMarks.frameBottom, 0f)
    }
}
