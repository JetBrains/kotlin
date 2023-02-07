/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import junit.framework.TestCase
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.Test
import java.io.File

class LicensesTests {
    @Test
    fun testLinksDefinitions() {
        val licenseDir = File("license")

        val linkDefinitionRegExp = Regex(pattern = "\\[(\\w+)]:.+")
        val linkRegExp = Regex(pattern = "]\\s?\\[(\\w+)]")

        val linksUsages = mutableSetOf<String>()
        val linksDefinitions = mutableSetOf<String>()

        val readmeFile = File(licenseDir, "README.md")
        readmeFile.useLines { lineSequence ->
            lineSequence.forEach { line ->
                val definitionMatch = linkDefinitionRegExp.matchEntire(line)
                if (definitionMatch != null) {
                    linksDefinitions.add(definitionMatch.groups[1]?.value ?: error("Should be present because of match"))
                } else {
                    linksUsages.addAll(linkRegExp.findAll(line).map { it.groups[1]?.value ?: "Should be present because of match" })
                }
            }
        }

        KtUsefulTestCase.assertOrderedEquals(
            "Incorrect links definitions usage in $readmeFile. Expected - links definitions. Actual - links usages",
            linksUsages.sorted(),
            linksDefinitions.sorted()
        )
    }
}