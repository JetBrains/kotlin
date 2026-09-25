/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.*
import templates.Family.*
import java.io.BufferedWriter

object AllEqualSampleGenerator {
    fun generate() {
        forEachCollectionFamily { family, primitive -> generate(family, primitive) }
        generate(CharSequences, PrimitiveType.Char)
    }

    private fun generate(family: Family, primitive: PrimitiveType? = null) {
        val collectionClass = collectionClassName(family, primitive)
        val receiverFactory = ReceiverFactory(family, primitive)
        val config = allEqualConfigFor(primitive)
        val inlineReceivers = family == Sequences || family == CharSequences

        val className = "AllEqual${collectionClass}Samples"
        writeGeneratedFile("libraries/stdlib/samples/test/samples/generated/allequal/$className.kt") {
            writeSampleHeader("samples.generated.allequal", "AllEqualSampleGenerator.kt", className, config.sampleNeedsAbsImport)
            writeAllEqualSample(receiverFactory, config, inlineReceivers, family)
            writeAllEqualBySample(receiverFactory, config, inlineReceivers)
            appendLine("}")
        }
    }

    private fun valOrInline(inline: Boolean, name: String, value: String): Pair<String, String> {
        return if (inline) "" to value else "\n        val $name = $value\n" to name
    }

    private fun charSequenceShowcase(family: Family): String {
        if (family != CharSequences) return ""
        return "\n" +
                "        // 😲 is represented by a pair of different UTF-16 characters, thus the characters of \"😲😲\" are not all equal\n" +
                "        assertPrints(\"😲😲\".allEqual(), \"false\")"
    }

    private fun BufferedWriter.writeAllEqualSample(
        receiverFactory: ReceiverFactory,
        config: AllEqualTypeConfig,
        inlineReceivers: Boolean,
        family: Family,
    ) {
        val equal = config.sampleEqualValue
        val other = config.sampleOtherValue
        val [sameDecl, sameRef] = valOrInline(inlineReceivers, "sameValues", receiverFactory(equal, equal, equal))
        val [mixedDecl, mixedRef] = valOrInline(inlineReceivers, "mixedValues", receiverFactory(equal, equal, other))
        appendLine(
            """
    @Sample
    fun allEqual() {
        assertPrints(${receiverFactory()}.allEqual(), "true")
        assertPrints(${receiverFactory(equal)}.allEqual(), "true")
$sameDecl        assertPrints($sameRef.allEqual(), "true")
$mixedDecl        assertPrints($mixedRef.allEqual(), "false")${charSequenceShowcase(family)}
    }"""
        )
    }

    private fun BufferedWriter.writeAllEqualBySample(
        receiverFactory: ReceiverFactory,
        config: AllEqualTypeConfig,
        inlineReceivers: Boolean,
    ) {
        val selectorValues = config.sampleSelectorValues
        val singleElement = selectorValues.first()
        val firstSelector = config.sampleSelectorAssertions.first().first
        val [valuesDecl, valuesRef] = valOrInline(inlineReceivers, "values", receiverFactory(selectorValues))
        val assertionLines = config.sampleSelectorAssertions.joinToString("\n") { [selector, expected] ->
            "        assertPrints($valuesRef.allEqualBy { $selector }, \"$expected\")"
        }
        appendLine(
            """
    @Sample
    fun allEqualBy() {
        assertPrints(${receiverFactory()}.allEqualBy { $firstSelector }, "true")
        assertPrints(${receiverFactory(singleElement)}.allEqualBy { $firstSelector }, "true")
$valuesDecl$assertionLines
    }"""
        )
    }

}
