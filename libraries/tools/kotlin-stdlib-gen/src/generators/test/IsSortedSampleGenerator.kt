/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.*
import templates.Family.*
import java.io.BufferedWriter

object IsSortedSampleGenerator {
    fun generate() {
        forEachCollectionFamily { family, primitive -> generate(family, primitive) }
    }

    private fun generate(family: Family, primitive: PrimitiveType? = null) {
        val collectionClass = collectionClassName(family, primitive)
        val ctor = constructorName(family, primitive)
        val config = isSortedConfigFor(primitive)
        val sorted = config.sampleSortedValues
        val unsorted = swapAdjacentPair(sorted)
        val emptyCollection = emptyCollectionExpr(ctor, primitive)
        val isSequence = family == Sequences
        val className = "IsSorted${collectionClass}Samples"
        writeGeneratedFile("libraries/stdlib/samples/test/samples/generated/issorted/$className.kt") {
            writeSampleHeader("samples.generated.issorted", "IsSortedSampleGenerator.kt", className, config.sampleNeedsAbsImport)
            writeSimpleSample("isSorted", ctor, sorted, unsorted, emptyCollection)
            val descSorted = sorted.reversed()
            val descUnsorted = swapAdjacentPair(descSorted)
            writeSimpleSample("isSortedDescending", ctor, descSorted, descUnsorted, emptyCollection)
            writeComparatorSample(ctor, sorted, config.caseInsensitiveValues?.sorted, isSequence, emptyCollection, primitive)
            writeSelectorSample(descending = false, ctor, isSequence, primitive, config, emptyCollection)
            writeSelectorSample(descending = true, ctor, isSequence, primitive, config, emptyCollection)
            appendLine("}")
        }
    }

    private fun BufferedWriter.writeSimpleSample(
        name: String,
        ctor: String,
        sortedValues: List<String>,
        unsortedValues: List<String>,
        emptyCollection: String,
    ) {
        val sortedArgs = sortedValues.joinToString(", ")
        val unsortedArgs = unsortedValues.joinToString(", ")
        val singleElement = sortedValues.first()
        appendLine(
            """
    @Sample
    fun $name() {
        assertTrue($emptyCollection.$name())
        assertTrue($ctor($singleElement).$name())

        val sorted = $ctor($sortedArgs)
        assertTrue(sorted.$name())

        val unsorted = $ctor($unsortedArgs)
        assertFalse(unsorted.$name())
    }"""
        )
    }

    private fun valOrInline(isSequence: Boolean, ctor: String, name: String, args: String): Pair<String, String> {
        val ctorCall = "$ctor($args)"
        return if (isSequence) "" to ctorCall else "        val $name = $ctorCall\n" to name
    }

    private fun BufferedWriter.writeComparatorSample(
        ctor: String,
        sortedValues: List<String>,
        caseInsensitiveSortedValues: List<String>?,
        isSequence: Boolean,
        emptyCollection: String,
        primitive: PrimitiveType?,
    ) {
        val singleElement = sortedValues.first()
        val sortedArgs = sortedValues.joinToString(", ")
        val reversedArgs = sortedValues.reversed().joinToString(", ")
        val [sortedDecl, sortedRef] = valOrInline(isSequence, ctor, "sorted", sortedArgs)
        val [reversedDecl, reversedRef] = valOrInline(isSequence, ctor, "reversed", reversedArgs)
        val caseInsensitiveBlock = if (caseInsensitiveSortedValues != null) {
            val [caseDecl, caseRef] = valOrInline(isSequence, ctor, "caseInsensitive", caseInsensitiveSortedValues.joinToString(", "))
            "\n${caseDecl}        assertTrue($caseRef.isSortedWith(String.CASE_INSENSITIVE_ORDER))\n"
        } else ""
        val nullsBlock = if (primitive == null) {
            val nullArgs = "null, ${sortedValues[0]}, ${sortedValues[1]}"
            val [nullsDecl, nullsRef] = valOrInline(isSequence, ctor, "withNulls", nullArgs)
            "\n${nullsDecl}        assertTrue($nullsRef.isSortedWith(nullsFirst(naturalOrder())))\n        assertFalse($nullsRef.isSortedWith(nullsLast(naturalOrder())))\n"
        } else ""

        appendLine(
            """
    @Sample
    fun isSortedWith() {
        assertTrue($emptyCollection.isSortedWith(naturalOrder()))
        assertTrue($ctor($singleElement).isSortedWith(naturalOrder()))

$sortedDecl        assertTrue($sortedRef.isSortedWith(naturalOrder()))
        assertFalse($sortedRef.isSortedWith(reverseOrder()))

$reversedDecl        assertTrue($reversedRef.isSortedWith(reverseOrder()))
$caseInsensitiveBlock$nullsBlock    }"""
        )
    }

    private fun BufferedWriter.writeSelectorSample(
        descending: Boolean,
        ctor: String,
        isSequence: Boolean,
        primitive: PrimitiveType?,
        config: IsSortedTypeConfig,
        emptyCollection: String,
    ) {
        val name = if (descending) "isSortedByDescending" else "isSortedBy"
        val selectorValues = if (descending) config.sampleSelectorValues.reversed() else config.sampleSelectorValues
        val assertions = config.sampleSelectorAssertions
        val args = selectorValues.joinToString(", ")
        val singleElement = selectorValues.first()
        val firstSelector = assertions.first().first
        val a = config.sortedValues[0]
        val b = config.sortedValues[1]
        val condition = nullSelectorCondition(primitive, a)
        val nullSelectorArgs = if (descending) "$b, $a" else "$a, $b"
        val [valuesDecl, valuesRef] = valOrInline(isSequence, ctor, "values", args)
        val assertionLines = assertions.joinToString("\n") { [selector, expected] ->
            val assertion = if (expected) "assertTrue" else "assertFalse"
            "        $assertion($valuesRef.$name { $selector })"
        }
        val identityNullElementLine = if (primitive == null && !descending) {
            "\n        assertTrue($ctor(null, $a, $b).isSortedBy { it })"
        } else ""
        val nullElementLine = if (primitive == null) {
            val nullElementArgs = if (descending) {
                "${selectorValues[0]}, ${selectorValues[1]}, null"
            } else {
                "null, ${selectorValues[0]}, ${selectorValues[1]}"
            }
            "\n        assertTrue($ctor($nullElementArgs).$name { it?.length })"
        } else ""

        appendLine(
            """
    @Sample
    fun $name() {
        assertTrue($emptyCollection.$name { $firstSelector })
        assertTrue($ctor($singleElement).$name { $firstSelector })

${valuesDecl}$assertionLines
        assertTrue($ctor($nullSelectorArgs).$name { if ($condition) null else it })$identityNullElementLine$nullElementLine
    }"""
        )
    }
}
