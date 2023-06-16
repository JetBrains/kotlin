/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.string

import generators.requireExistingDir
import generators.unicode.PropertyLine
import generators.unicode.hexToInt
import generators.unicode.toHexIntLiteral
import generators.unicode.writeHeader
import java.io.File
import java.io.FileWriter

internal class StringCasingTestGenerator(private val outputDir: File) {
    private val casedRanges = mutableListOf<PropertyLine>()
    private val caseIgnorableRanges = mutableListOf<PropertyLine>()

    init {
        outputDir.requireExistingDir()
    }

    fun appendDerivedCorePropertiesLine(line: PropertyLine) {
        when (line.property) {
            "Cased" -> casedRanges.add(line)
            "Case_Ignorable" -> caseIgnorableRanges.add(line)
        }
    }

    fun generate() {
        generateIsCasedTest()
        generateIsCaseIgnorableTest()
    }

    private fun generateIsCasedTest() {
        val file = outputDir.resolve("_IsCasedTest.kt")
        generateRangesTest(casedRanges, file, "casedRanges", "isCased")
    }

    private fun generateIsCaseIgnorableTest() {
        val file = outputDir.resolve("_IsCaseIgnorableTest.kt")
        generateRangesTest(caseIgnorableRanges, file, "caseIgnorableRanges", "isCaseIgnorable")
    }

    private fun generateRangesTest(
        ranges: List<PropertyLine>,
        file: File,
        rangesArrayName: String,
        functionName: String
    ) {
        FileWriter(file).use { writer ->
            writer.writeHeader(file, "test.text")
            writer.appendLine()
            writer.appendLine("import kotlin.test.*")
            writer.appendLine()
            writer.appendLine("private val $rangesArrayName = arrayOf<IntRange>(")
            ranges.forEach {
                writer.appendLine("    ${it.hexIntRangeLiteral()},")
            }
            writer.appendLine(")")
            writer.appendLine()
            writer.appendLine(test(rangesArrayName, functionName))
        }
    }

    private fun test(rangesArrayName: String, functionName: String): String = """
        class ${functionName.replaceFirstChar { it.uppercase() }}Test {
            @Test
            fun $functionName() {
                var lastChecked = -1
                for (range in $rangesArrayName) {
                    for (codePoint in lastChecked + 1 until range.first) {
                        assertFalse(codePoint.$functionName())
                    }
                    for (codePoint in range.first..range.last) {
                        assertTrue(codePoint.$functionName())
                    }
                    lastChecked = range.last
                }
                for (codePoint in lastChecked + 1..0x10FFFF) {
                    assertFalse(codePoint.$functionName())
                }
            }
        }
    """.trimIndent()
}