package io.github.sherifshabans.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page layout is only exact while the text and `layout.txt` agree token for
 * token. If they drift, [MushafPage] silently falls back to the estimated line
 * breaker — so every one of the 604 pages is checked here.
 */
class MushafLayoutTest {

    @Test
    fun textIsComplete() {
        val ayahs = TestData.ayahs
        assertEquals(Quran.AYAH_COUNT, ayahs.size)
        assertEquals((1..Quran.AYAH_COUNT).toList(), ayahs.map { it.id })
        assertEquals((1..Quran.PAGE_COUNT).toSet(), ayahs.map { it.page }.toSet())
        Quran.surahs.forEach { s ->
            val of = ayahs.filter { it.surah == s.number }
            assertEquals("surah ${s.number} ayah count", s.ayahCount, of.size)
            assertEquals("surah ${s.number} start page", s.startPage, of.first().page)
            assertEquals("surah ${s.number} end page", s.endPage, of.last().page)
        }
        // لا رقم آية ملزوق في آخر النص — الراندرر بيرسم الوردة بنفسه.
        ayahs.forEach { a ->
            assertTrue("ayah ${a.id} ends with a digit", a.text.last() !in '٠'..'٩' && !a.text.last().isDigit())
        }
    }

    @Test
    fun everyPageMatchesTheReferenceLayout() {
        val byPage = TestData.ayahs.groupBy { it.page }
        (1..Quran.PAGE_COUNT).forEach { page ->
            val lines = TestData.layout[page]
            assertNotNull("page $page missing from layout.txt", lines)
            val (tokens, _) = buildPageTokens(byPage.getValue(page)) { toArabicDigits(it) }
            assertEquals("page $page token count", lines!!.totalTokens, tokens.size)
            assertNotNull(lines.sliceTokens(tokens))
            assertEquals(if (page <= 2) 8 else 15, lines.lineCount)
        }
    }

    @Test
    fun bannerOfAnNisaSitsOnThePreviousPage() {
        val p76 = TestData.layout.getValue(76).reservedRoles
        val p77 = TestData.layout.getValue(77).reservedRoles
        assertEquals(ReservedRole.Banner(4), p76[15])
        assertEquals(ReservedRole.Basmala(4), p77[1])
    }
}
