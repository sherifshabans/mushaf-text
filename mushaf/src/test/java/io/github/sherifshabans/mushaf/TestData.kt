package io.github.sherifshabans.mushaf

import java.io.File

/** The bundled assets, read straight from disk — no Android needed. */
internal object TestData {
    val ayahs: List<Ayah> by lazy {
        File("src/main/assets/mushaf/hafs.tsv").readLines(Charsets.UTF_8)
            .filter { it.isNotBlank() }
            .map(::parseAyahLine)
    }

    val layout: Map<Int, MushafPageLines> by lazy {
        File("src/main/assets/mushaf/layout.txt").reader(Charsets.UTF_8).use(MushafLineIndex::parse)
    }

    fun ayah(surah: Int, number: Int): Ayah = ayahs.first { it.surah == surah && it.number == number }
}
