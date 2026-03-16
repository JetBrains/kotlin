/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.Family
import templates.Family.*
import templates.PrimitiveType
import java.io.BufferedWriter
import java.io.File

fun collectionClassName(family: Family, primitive: PrimitiveType?): String = when (family) {
    Iterables, Sequences -> family.toString()
    ArraysOfObjects -> "Array"
    ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!}Array"
    else -> error(family)
}

fun constructorName(family: Family, primitive: PrimitiveType?): String = when (family) {
    Iterables -> "listOf"
    Sequences -> "sequenceOf"
    ArraysOfObjects -> "arrayOf"
    ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!.name.lowercase()}ArrayOf"
    else -> error(family)
}

fun forEachIsSortedFamily(action: (Family, PrimitiveType?) -> Unit) {
    action(Iterables, null)
    action(Sequences, null)
    action(ArraysOfObjects, null)
    for (primitive in PrimitiveType.defaultPrimitives) {
        action(ArraysOfPrimitives, primitive)
    }
    for (primitive in PrimitiveType.unsignedPrimitives) {
        action(ArraysOfUnsigned, primitive)
    }
}

fun writeGeneratedFile(path: String, block: BufferedWriter.() -> Unit) {
    val file = File(path)
    file.parentFile.mkdirs()
    file.bufferedWriter().use { it.block() }
}

fun emptyCollectionExpr(ctor: String, primitive: PrimitiveType?): String =
    if (primitive == null) "$ctor<String>()" else "$ctor()"

fun swapAdjacentPair(values: List<String>): List<String> {
    val i = values.zipWithNext().indexOfFirst { (a, b) -> a != b }
    check(i >= 0) { "Cannot swap: all elements are equal" }
    return values.toMutableList().apply { this[i] = this[i + 1].also { this[i + 1] = this[i] } }
}

data class CaseInsensitiveTestData(val sorted: List<String>, val unsorted: List<String>)

class IsSortedTypeConfig(
    val sortedValues: List<String>,
    val caseInsensitiveValues: CaseInsensitiveTestData? = null,
    val selectorExpr: String = "it",
    val selectorSortedValues: List<String> = sortedValues,
    val sampleNeedsAbsImport: Boolean = false,
    val sampleSortedValues: List<String> = sortedValues,
    val sampleSelectorValues: List<String>,
    val sampleSelectorAssertions: List<Pair<String, Boolean>>,
)

private fun signedIntConfig(suffix: String, absExpr: String): IsSortedTypeConfig = IsSortedTypeConfig(
    sortedValues = (1..5).map { "$it$suffix" },
    selectorExpr = "it * it",
    selectorSortedValues = listOf("1", "-2", "3").map { "$it$suffix" },
    sampleNeedsAbsImport = true,
    sampleSelectorValues = listOf("1", "-2", "3", "-4", "5").map { "$it$suffix" },
    sampleSelectorAssertions = listOf("it * it" to true, absExpr to true, "it" to false)
)

private fun unsignedIntConfig(suffix: String, modExpr: String): IsSortedTypeConfig = IsSortedTypeConfig(
    sortedValues = (1..5).map { "$it$suffix" },
    selectorExpr = modExpr,
    selectorSortedValues = listOf("3", "1", "2").map { "$it$suffix" },
    sampleSelectorValues = listOf("3", "1", "4", "2").map { "$it$suffix" },
    sampleSelectorAssertions = listOf(modExpr to true, "it" to false)
)

private fun floatingPointConfig(suffix: String): IsSortedTypeConfig = IsSortedTypeConfig(
    sortedValues = listOf("1.0", "2.5", "3.14").map { "$it$suffix" },
    selectorExpr = "it * it",
    selectorSortedValues = listOf("-0.5", "1.0", "-1.5").map { "$it$suffix" },
    sampleNeedsAbsImport = true,
    sampleSelectorValues = listOf("-0.5", "1.0", "-1.5", "2.0").map { "$it$suffix" },
    sampleSelectorAssertions = listOf("it * it" to true, "abs(it)" to true, "it" to false)
)

fun nullSelectorCondition(primitive: PrimitiveType?, value: String): String = when (primitive) {
    PrimitiveType.Byte, PrimitiveType.Short -> "it.toInt() == $value"
    PrimitiveType.UByte, PrimitiveType.UShort -> "it.toUInt() == $value"
    else -> "it == $value"
}

fun nullSelectorTypeArgs(family: Family, primitive: PrimitiveType?): String {
    val typeName = primitive?.name ?: "String"
    return when (family) {
        Iterables, Sequences, ArraysOfObjects -> "<$typeName, $typeName>"
        else -> "<$typeName>"
    }
}

fun isSortedConfigFor(primitive: PrimitiveType?): IsSortedTypeConfig = when (primitive) {
    PrimitiveType.Char -> IsSortedTypeConfig(
        sortedValues = listOf("'a'", "'b'", "'c'"),
        selectorExpr = "it.uppercaseChar()",
        selectorSortedValues = listOf("'A'", "'b'", "'C'"),
        sampleSelectorValues = listOf("'A'", "'b'", "'C'"),
        sampleSelectorAssertions = listOf("it.uppercaseChar()" to true, "it.lowercaseChar()" to true, "it" to false)
    )
    PrimitiveType.Boolean -> IsSortedTypeConfig(
        sortedValues = listOf("false", "true", "true"),
        selectorExpr = "it.compareTo(false)",
        sampleSortedValues = listOf("false", "false", "true"),
        sampleSelectorValues = listOf("false", "false", "true"),
        sampleSelectorAssertions = listOf("it.compareTo(false)" to true, "it" to true, "!it" to false)
    )
    null -> IsSortedTypeConfig(
        sortedValues = listOf("\"a\"", "\"b\"", "\"c\""),
        selectorExpr = "it.length",
        selectorSortedValues = listOf("\"a\"", "\"bb\"", "\"ccc\""),
        caseInsensitiveValues = CaseInsensitiveTestData(
            sorted = listOf("\"Apple\"", "\"banana\"", "\"Cherry\""),
            unsorted = listOf("\"banana\"", "\"Apple\"", "\"Cherry\"")
        ),
        sampleSortedValues = listOf("\"apple\"", "\"banana\"", "\"cherry\""),
        sampleSelectorValues = listOf("\"c\"", "\"bb\"", "\"aaa\""),
        sampleSelectorAssertions = listOf("it.length" to true, "it" to false)
    )
    PrimitiveType.Byte, PrimitiveType.Short -> signedIntConfig(suffix = "", absExpr = "abs(it.toInt())")
    PrimitiveType.Int -> signedIntConfig(suffix = "", absExpr = "abs(it)")
    PrimitiveType.Long -> signedIntConfig(suffix = "L", absExpr = "abs(it)")
    PrimitiveType.Float -> floatingPointConfig(suffix = "f")
    PrimitiveType.Double -> floatingPointConfig(suffix = "")
    PrimitiveType.UByte, PrimitiveType.UShort -> unsignedIntConfig(suffix = "u", modExpr = "it.toUInt() % 3u")
    PrimitiveType.UInt -> unsignedIntConfig(suffix = "u", modExpr = "it % 3u")
    PrimitiveType.ULong -> unsignedIntConfig(suffix = "uL", modExpr = "it % 3uL")
}
