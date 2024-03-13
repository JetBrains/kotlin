/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import java.util.Locale
import kotlin.time.*
import kotlin.test.*

class DurationJVMTest {

    @Test
    fun toStringAlwaysRootLocale() {
        val durations = listOf(
            "P1DT4H30M35.200300456S",
            "PT35.200300456S",
            "PT0.200300456S",
            "PT0.000300456S",
            "PT0.000000456S",
        ).map(Duration::parse)

        val currentDefault = Locale.getDefault()
        val allFormatted = try {
            Locale.setDefault(Locale.GERMAN)
            durations.flatMap { d ->
                buildList {
                    add(d.toString())
                    add(d.toIsoString())
                    for (unit in DurationUnit.entries) {
                        for (decimals in 0..12) {
                            add(d.toString(unit, decimals))
                        }
                    }
                }
            }
        } finally {
            Locale.setDefault(currentDefault)
        }

        val unexpected = allFormatted.filter { "," in it }
        if (unexpected.isNotEmpty()) {
            fail("The following representations contain invalid separator:\n${unexpected.joinToString("\n")}")
        }
    }
}