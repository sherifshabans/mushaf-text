package io.github.sherifshabans.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurahRevelationTest {
    @Test
    fun allSurahsHaveRevelationType() {
        assertEquals(114, Quran.surahs.size)
        assertEquals(86, Quran.surahs.count { it.revelationType == RevelationType.MECCAN })
        assertEquals(28, Quran.surahs.count { it.revelationType == RevelationType.MEDINAN })
    }

    @Test
    fun sampleSurahsMatchQuranyAppJson() {
        assertEquals(RevelationType.MECCAN, Quran.surah(1).revelationType) // الفاتحة
        assertEquals(RevelationType.MEDINAN, Quran.surah(2).revelationType) // البقرة
        assertEquals(RevelationType.MEDINAN, Quran.surah(9).revelationType) // التوبة
        assertEquals(RevelationType.MECCAN, Quran.surah(112).revelationType) // الإخلاص
        assertEquals(RevelationType.MEDINAN, Quran.surah(110).revelationType) // النصر
        assertEquals("مكية", Quran.surah(1).revelationType.labelArabic)
        assertEquals("مدنية", Quran.surah(2).revelationType.labelArabic)
    }

    @Test
    fun fromAppStringParsesRoomValues() {
        assertEquals(RevelationType.MECCAN, RevelationType.fromAppString("Meccan"))
        assertEquals(RevelationType.MEDINAN, RevelationType.fromAppString("Medinan"))
        assertEquals(RevelationType.MEDINAN, RevelationType.fromAppString("medinan"))
        assertTrue(RevelationType.fromAppString(null) == RevelationType.MECCAN)
    }
}
