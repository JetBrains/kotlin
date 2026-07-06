/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.string

import generators.unicode.*
import generators.unicode.PropertyLine
import generators.unicode.SpecialCasingLine
import generators.unicode.UnicodeDataLine
import generators.unicode.ranges.RangesWritingStrategy
import generators.unicode.ranges.builders.RangesBuilder
import templates.KotlinTarget
import java.io.File
import java.io.FileWriter

internal class StringLowercaseGenerator(
    private val outputFile: File,
    unicodeDataLines: List<UnicodeDataLine>,
    private val target: KotlinTarget,
) : StringCasingGenerator(unicodeDataLines) {

    private val casedRanges = mutableListOf<IntRange>()
    private val caseIgnorableRanges = mutableListOf<IntRange>()

    init {
        val casedRangesBuilder = CasedRangesBuilder()
        val caseIgnorableRangesBuilder = CaseIgnorableRangesBuilder()
        unicodeDataLines.forEach { line ->
            if (line.char.length > 4) {
                casedRangesBuilder.append(line.char, line.name, line.categoryCode)
                caseIgnorableRangesBuilder.append(line.char, line.name, line.categoryCode)
            }
        }
        casedRangesBuilder.build().let { [start, end, _] ->
            start.indices.forEach { casedRanges.add(start[it]..end[it]) }
        }
        caseIgnorableRangesBuilder.build().let { [start, end, _] ->
            start.indices.forEach { caseIgnorableRanges.add(start[it]..end[it]) }
        }
    }

    override fun SpecialCasingLine.mapping(): List<String> = lowercaseMapping

    override fun UnicodeDataLine.mapping(): String = lowercaseMapping

    fun appendWordBreakPropertyLine(line: PropertyLine) {
        when (line.property) {
            "MidLetter",
            "MidNumLet",
            "Single_Quote" -> caseIgnorableRanges.add(line.intRange())
        }
    }

    fun generate() {
        check(contextDependentMappings.size == 1 || contextDependentMappings[0].conditionList == listOf("Final_Sigma")) {
            "The locale-agnostic conditional mappings $contextDependentMappings are not handled."
        }

        casedRanges.sortBy { it.first }
        caseIgnorableRanges.sortBy { it.first }

        val strategy = RangesWritingStrategy.of(target)

        FileWriter(outputFile).use { writer ->
            writer.writeHeader(outputFile, "kotlin.text")
            writer.appendLine()
            writer.writeIntArray("casedStart", casedRanges.map { it.first }, strategy)
            writer.writeIntArray("casedEnd", casedRanges.map { it.last }, strategy)
            writer.appendLine()
            writer.appendLine(isCased())
            writer.appendLine()
            writer.writeIntArray("caseIgnorableStart", caseIgnorableRanges.map { it.first }, strategy)
            writer.writeIntArray("caseIgnorableEnd", caseIgnorableRanges.map { it.last }, strategy)
            writer.appendLine()
            writer.appendLine(isCaseIgnorable())
            writer.appendLine()
        }
    }

    private fun isCased(): String = """
        // Lu + Ll + Lt + Other_Lowercase + Other_Uppercase (PropList.txt of Unicode Character Database files)
        // Declared internal for testing
        internal fun isCased(code: Int): Boolean {
            if (code <= Char.MAX_VALUE.code) {
                when (getCategoryValue(code)) {
                    CharCategory.UPPERCASE_LETTER.value,
                    CharCategory.LOWERCASE_LETTER.value,
                    CharCategory.TITLECASE_LETTER.value -> return true
                }
            }
            if (isOtherUppercase(code) || isOtherLowercase(code)) {
                return true
            }
            val index = binarySearchRange(casedStart, code)
            return index >= 0 && code <= casedEnd[index]
        }
    """.trimIndent()

    private fun isCaseIgnorable(): String = """
        // Mn + Me + Cf + Lm + Sk + Word_Break=MidLetter + Word_Break=MidNumLet + Word_Break=Single_Quote (WordBreakProperty.txt of Unicode Character Database files)
        // Declared internal for testing
        internal fun isCaseIgnorable(code: Int): Boolean {
            if (code <= Char.MAX_VALUE.code) {
                when (getCategoryValue(code)) {
                    CharCategory.NON_SPACING_MARK.value,
                    CharCategory.ENCLOSING_MARK.value,
                    CharCategory.FORMAT.value,
                    CharCategory.MODIFIER_LETTER.value,
                    CharCategory.MODIFIER_SYMBOL.value -> return true
                }
            }
            val index = binarySearchRange(caseIgnorableStart, code)
            return index >= 0 && code <= caseIgnorableEnd[index]
        }
    """.trimIndent()
}

private class CasedRangesBuilder : RangesBuilder() {
    private val id = "Cased"

    override fun categoryId(categoryCode: String): String = when (categoryCode) {
        CharCategory.UPPERCASE_LETTER.code,
        CharCategory.LOWERCASE_LETTER.code,
        CharCategory.TITLECASE_LETTER.code -> id
        else -> "Else"
    }

    override fun shouldSkip(categoryId: String): Boolean {
        return categoryId != id
    }
}

private class CaseIgnorableRangesBuilder : RangesBuilder() {
    private val id = "CaseIgnorable"

    override fun categoryId(categoryCode: String): String = when (categoryCode) {
        CharCategory.NON_SPACING_MARK.code,
        CharCategory.ENCLOSING_MARK.code,
        CharCategory.FORMAT.code,
        CharCategory.MODIFIER_LETTER.code,
        CharCategory.MODIFIER_SYMBOL.code -> id
        else -> "Else"
    }

    override fun shouldSkip(categoryId: String): Boolean {
        return categoryId != id
    }
}
