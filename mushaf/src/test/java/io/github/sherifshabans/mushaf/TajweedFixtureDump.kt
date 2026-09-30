package io.github.sherifshabans.mushaf

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Writes the Kotlin annotator's output for all 6236 ayahs to a fixture the Dart
 * port is tested against.
 *
 * ## Why a fixture and not "both test suites pass"
 * Two implementations can each satisfy their own assertions and still disagree
 * on thousands of verses. The only claim worth making about a port is that it
 * produces **the same spans**, verse by verse — so Kotlin writes what it
 * actually computes, and Dart has to reproduce it exactly.
 *
 * Natural madd is included: it is the widest rule (about 32k positions) and
 * touches nearly every ayah, so leaving it out would hide most disagreements.
 *
 * Format, one line per ayah that has any span:
 * ```
 * <ayah id>\t<start>,<length>,<rule ordinal>;<start>,<length>,<rule ordinal>;…
 * ```
 * Rule ordinals are [TajweedRule.ordinal], so the Dart enum must declare its
 * values in the same order — which is itself part of what this pins down.
 *
 * Run: `./gradlew :mushaf:testDebugUnitTest --tests '*TajweedFixtureDump*'`
 */
class TajweedFixtureDump {

    @Test
    fun writeFixture() {
        val out = File("../flutter/test/fixtures/tajweed_spans.tsv")
        out.parentFile.mkdirs()

        var spans = 0
        out.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("# tajweed spans from the Kotlin annotator, includeNaturalMadd = true\n")
            w.write("# ayahId\\tstart,length,ruleOrdinal;…\n")
            w.write("# rule order: " + TajweedRule.entries.joinToString(",") { it.name } + "\n")
            TestData.ayahs.forEach { ayah ->
                val list = TajweedAnnotator.annotate(
                    ayah.text,
                    includeNaturalMadd = true,
                    // Both switches on, so the fixture covers all 26 rules. With
                    // tafkhim off, that whole family would cross to Dart unchecked.
                    includeTafkhim = true
                )
                if (list.isEmpty()) return@forEach
                spans += list.size
                w.write(ayah.id.toString())
                w.write("\t")
                w.write(
                    list.joinToString(";") { "${it.start},${it.end - it.start},${it.rule.ordinal}" }
                )
                w.write("\n")
            }
        }

        println("[tajweed] wrote $spans spans to ${out.absolutePath}")
        assertTrue("suspiciously few spans: $spans", spans > 80_000)
    }
}
