/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.*
import templates.Family.*
import java.io.BufferedWriter

object AllDistinctSampleGenerator {
    fun generate() {
        forEachCollectionFamily { family, primitive -> generate(family, primitive) }
        generate(CharSequences, PrimitiveType.Char)
    }

    private fun generate(family: Family, primitive: PrimitiveType? = null) {
        val collectionClass = collectionClassName(family, primitive)
        val receiverFactory = ReceiverFactory(family, primitive)
        val config = allDistinctConfigFor(primitive)
        val inlineReceivers = family == Sequences || family == CharSequences

        val className = "AllDistinct${collectionClass}Samples"
        writeGeneratedFile("libraries/stdlib/samples/test/samples/generated/alldistinct/$className.kt") {
            writeSampleHeader("samples.generated.alldistinct", "AllDistinctSampleGenerator.kt", className, config.sampleNeedsAbsImport)
            writeAllDistinctSample(receiverFactory, config, inlineReceivers, family, primitive)
            writeAllDistinctBySample(receiverFactory, config, inlineReceivers)
            appendLine("}")
        }
    }

    private fun valOrInline(inline: Boolean, name: String, value: String): Pair<String, String> {
        return if (inline) "" to value else "\n        val $name = $value\n" to name
    }

    private fun floatingPointShowcase(receiverFactory: ReceiverFactory, primitive: PrimitiveType?): String {
        if (primitive != PrimitiveType.Float && primitive != PrimitiveType.Double) return ""
        val typeName = primitive.name
        val suffix = if (primitive == PrimitiveType.Float) "f" else ""
        return "\n\n" +
                "        assertFalse(${receiverFactory("$typeName.NaN", "$typeName.NaN")}.allDistinct())\n" +
                "        assertTrue(${receiverFactory("0.0$suffix", "-0.0$suffix")}.allDistinct())"
    }

    private fun charSequenceShowcase(family: Family): String {
        if (family != CharSequences) return ""
        return "\n" +
                "        // 😱 and 😲 are represented by pairs of UTF-16 characters with the same first character, thus they are not all distinct\n" +
                "        assertFalse(\"😱😲\".allDistinct())"
    }

    private fun BufferedWriter.writeAllDistinctSample(
        receiverFactory: ReceiverFactory,
        config: AllDistinctTypeConfig,
        inlineReceivers: Boolean,
        family: Family,
        primitive: PrimitiveType?,
    ) {
        val single = config.sampleDistinctValues.first()
        val [distinctDecl, distinctRef] = valOrInline(inlineReceivers, "distinctValues", receiverFactory(config.sampleDistinctValues))
        val [duplicateDecl, duplicateRef] = valOrInline(inlineReceivers, "duplicateValues", receiverFactory(config.sampleDuplicateValues))
        val showcase = floatingPointShowcase(receiverFactory, primitive) + charSequenceShowcase(family)
        appendLine(
            """
    @Sample
    fun allDistinct() {
        assertTrue(${receiverFactory()}.allDistinct())
        assertTrue(${receiverFactory(single)}.allDistinct())
$distinctDecl        assertTrue($distinctRef.allDistinct())
$duplicateDecl        assertFalse($duplicateRef.allDistinct())$showcase
    }"""
        )
    }

    private fun BufferedWriter.writeAllDistinctBySample(
        receiverFactory: ReceiverFactory,
        config: AllDistinctTypeConfig,
        inlineReceivers: Boolean,
    ) {
        val selectorValues = config.sampleSelectorValues
        val singleElement = selectorValues.first()
        val firstSelector = config.sampleSelectorAssertions.first().first
        val [valuesDecl, valuesRef] = valOrInline(inlineReceivers, "values", receiverFactory(selectorValues))
        val assertionLines = config.sampleSelectorAssertions.joinToString("\n") { [selector, expected] ->
            val assertion = if (expected) "assertTrue" else "assertFalse"
            "        $assertion($valuesRef.allDistinctBy { $selector })"
        }
        appendLine(
            """
    @Sample
    fun allDistinctBy() {
        assertTrue(${receiverFactory()}.allDistinctBy { $firstSelector })
        assertTrue(${receiverFactory(singleElement)}.allDistinctBy { $firstSelector })
$valuesDecl$assertionLines
    }"""
        )
    }
}
