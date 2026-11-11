package io.github.sherifshabans.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gate for the division corpus: the table, the in-line `۞` / `۩` glyphs in
 * `hafs.tsv`, and the standard Madinah 240 must all describe the same book.
 */
class MushafDivisionsTest {

    private val byAyah = TestData.ayahs.associateBy { it.surah to it.number }
    private val starAyahs = TestData.ayahs.filter { it.text.startsWith("۞") }
        .mapTo(HashSet()) { it.surah to it.number }
    private val sajdahAyahs = TestData.ayahs.filter { "۩" in it.text }
        .mapTo(HashSet()) { it.surah to it.number }

    @Test
    fun quartersCoverTheStandardMadinahDivisions() {
        val quarters = MushafDivisions.quarters
        assertEquals(240, quarters.size)
        assertEquals((1..240).toList(), quarters.map { it.quarterIndex })
        quarters.forEach {
            assertTrue("index ${it.quarterIndex}: hizb ${it.hizb}", it.hizb in 1..60)
            assertTrue("index ${it.quarterIndex}: juz ${it.juz}", it.juz in 1..30)
            assertEquals("index ${it.quarterIndex}", (it.hizb + 1) / 2, it.juz)
        }
        assertEquals((1..30).toList(), quarters.map { it.juz }.distinct().sorted())
        assertEquals(30, quarters.count { it.kind == DivisionKind.JUZ })
        assertEquals(30, quarters.count { it.kind == DivisionKind.HIZB })
        assertEquals(60, quarters.count { it.kind == DivisionKind.RUB })
        assertEquals(60, quarters.count { it.kind == DivisionKind.NISF })
        assertEquals(60, quarters.count { it.kind == DivisionKind.THREE_QUARTERS })
    }

    @Test
    fun everyStarIsExactlyOneQuarter() {
        assertEquals(199, starAyahs.size)
        val quarterAyahs = MushafDivisions.quarters.map { it.surah to it.ayah }
        assertEquals("quarters must not repeat an ayah", 240, quarterAyahs.toSet().size)
        starAyahs.forEach { key ->
            val hits = MushafDivisions.quarters.filter { (it.surah to it.ayah) == key }
            assertEquals("star at ${key.first}:${key.second} must be exactly one quarter", 1, hits.size)
            val star = byAyah.getValue(key)
            assertEquals("page of ${key.first}:${key.second}", star.page, hits[0].page)
        }
    }

    @Test
    fun quarter106FollowsTheGlyphTo1549() {
        // The sample map said 15:50; hafs.tsv carries the star on 15:49 (page 264).
        val fixed = MushafDivisions.quarters.single { it.quarterIndex == 106 }
        assertEquals(15, fixed.surah)
        assertEquals(49, fixed.ayah)
        assertEquals(264, fixed.page)
        assertEquals(DivisionKind.RUB, fixed.kind)
        assertTrue((15 to 49) in starAyahs)
    }

    @Test
    fun quarterStarPresenceMatchesTheAyahNumber() {
        MushafDivisions.quarters.forEach {
            val key = it.surah to it.ayah
            val text = byAyah.getValue(key)
            assertEquals("page of quarter ${it.quarterIndex}", text.page, it.page)
            if (it.ayah == 1) {
                assertTrue("quarter ${it.quarterIndex} on ayah 1 must have no star", key !in starAyahs)
            } else {
                assertTrue("quarter ${it.quarterIndex} on ${it.surah}:${it.ayah} must have a star", key in starAyahs)
            }
        }
    }

    @Test
    fun sajdahMarksMatchTheSajdahGlyphs() {
        val sajdat = MushafDivisions.sajdat
        assertEquals(15, sajdat.size)
        assertEquals(sajdat.map { it.surah to it.ayah }.toSet(), sajdahAyahs)
        sajdat.forEach {
            val key = it.surah to it.ayah
            assertTrue("sajdah ${key.first}:${key.second} must carry ۩", key in sajdahAyahs)
            val text = byAyah.getValue(key)
            assertEquals("page of ${key.first}:${key.second}", text.page, it.page)
            assertEquals("juz of ${key.first}:${key.second}", text.juz, it.juz)
            assertEquals(DivisionKind.SAJDAH, it.kind)
        }
    }

    @Test
    fun marksOnFindsPagesWithAndWithoutMarks() {
        assertTrue("page 4 has neither a quarter nor a sajdah", MushafDivisions.marksOn(4).isEmpty())

        val p22 = MushafDivisions.marksOn(22)
        assertTrue(
            "page 22 must carry the juz 2 mark at 2:142",
            p22.any { it.kind == DivisionKind.JUZ && it.surah == 2 && it.ayah == 142 && it.juz == 2 },
        )

        assertTrue(
            "page 264 must carry the corrected 15:49 mark",
            MushafDivisions.marksOn(264).any { it.surah == 15 && it.ayah == 49 },
        )
        assertTrue(
            "page 176 must carry the sajdah of 7:206",
            MushafDivisions.marksOn(176).any { it.kind == DivisionKind.SAJDAH && it.surah == 7 && it.ayah == 206 },
        )
    }
}
