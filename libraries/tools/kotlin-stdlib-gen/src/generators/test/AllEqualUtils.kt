/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.PrimitiveType

class AllEqualTypeConfig(
    val equalValue: String,
    val otherValue: String,
    val selectorExpr: String = "it",
    val valuesWithEqualSelector: List<String> = listOf(equalValue, equalValue, equalValue),
    val valuesWithDiffSelector: List<String> = listOf(equalValue, otherValue),
    val sampleEqualValue: String = equalValue,
    val sampleOtherValue: String = otherValue,
    val sampleSelectorValues: List<String> = valuesWithEqualSelector,
    val sampleSelectorAssertions: List<Pair<String, Boolean>> = listOf(selectorExpr to true, "it" to false),
    val sampleNeedsAbsImport: Boolean = false,
)

private fun signedIntConfig(suffix: String, absExpr: String): AllEqualTypeConfig = AllEqualTypeConfig(
    equalValue = "1$suffix",
    otherValue = "2$suffix",
    selectorExpr = "it * it",
    valuesWithEqualSelector = listOf("1", "-1", "1").map { "$it$suffix" },
    valuesWithDiffSelector = listOf("1", "2").map { "$it$suffix" },
    sampleNeedsAbsImport = true,
    sampleSelectorValues = listOf("1", "-1", "1").map { "$it$suffix" },
    sampleSelectorAssertions = listOf("it * it" to true, absExpr to true, "it" to false),
)

private fun unsignedIntConfig(suffix: String, modExpr: String): AllEqualTypeConfig = AllEqualTypeConfig(
    equalValue = "1$suffix",
    otherValue = "2$suffix",
    selectorExpr = modExpr,
    valuesWithEqualSelector = listOf("1", "3", "5").map { "$it$suffix" },
    valuesWithDiffSelector = listOf("1", "2").map { "$it$suffix" },
    sampleSelectorValues = listOf("1", "3", "5").map { "$it$suffix" },
    sampleSelectorAssertions = listOf(modExpr to true, "it" to false),
)

private fun floatingPointConfig(suffix: String): AllEqualTypeConfig = AllEqualTypeConfig(
    equalValue = "1.0$suffix",
    otherValue = "2.0$suffix",
    selectorExpr = "it * it",
    valuesWithEqualSelector = listOf("1.0", "-1.0", "1.0").map { "$it$suffix" },
    valuesWithDiffSelector = listOf("1.0", "2.0").map { "$it$suffix" },
    sampleNeedsAbsImport = true,
    sampleSelectorValues = listOf("1.0", "-1.0", "1.0").map { "$it$suffix" },
    sampleSelectorAssertions = listOf("it * it" to true, "abs(it)" to true, "it" to false),
)

fun allEqualConfigFor(primitive: PrimitiveType?): AllEqualTypeConfig = when (primitive) {
    PrimitiveType.Char -> AllEqualTypeConfig(
        equalValue = "'a'",
        otherValue = "'b'",
        selectorExpr = "it.uppercaseChar()",
        valuesWithEqualSelector = listOf("'a'", "'A'", "'a'"),
        valuesWithDiffSelector = listOf("'a'", "'B'"),
        sampleSelectorValues = listOf("'a'", "'A'", "'a'"),
        sampleSelectorAssertions = listOf("it.uppercaseChar()" to true, "it.lowercaseChar()" to true, "it" to false),
    )
    PrimitiveType.Boolean -> AllEqualTypeConfig(
        equalValue = "true",
        otherValue = "false",
        sampleSelectorValues = listOf("true", "false", "true"),
        sampleSelectorAssertions = listOf("true" to true, "it" to false, "!it" to false),
    )
    null -> AllEqualTypeConfig(
        equalValue = "\"a\"",
        otherValue = "\"b\"",
        selectorExpr = "it.length",
        valuesWithEqualSelector = listOf("\"a\"", "\"b\"", "\"c\""),
        valuesWithDiffSelector = listOf("\"a\"", "\"bb\""),
        sampleEqualValue = "\"apple\"",
        sampleOtherValue = "\"orange\"",
        sampleSelectorValues = listOf("\"apple\"", "\"mango\"", "\"peach\""),
        sampleSelectorAssertions = listOf("it.length" to true, "it" to false),
    )
    PrimitiveType.Byte, PrimitiveType.Short -> signedIntConfig(suffix = "", absExpr = "abs(it.toInt())")
    PrimitiveType.Int -> signedIntConfig(suffix = "", absExpr = "abs(it)")
    PrimitiveType.Long -> signedIntConfig(suffix = "L", absExpr = "abs(it)")
    PrimitiveType.Float -> floatingPointConfig(suffix = "f")
    PrimitiveType.Double -> floatingPointConfig(suffix = "")
    PrimitiveType.UByte, PrimitiveType.UShort -> unsignedIntConfig(suffix = "u", modExpr = "it.toUInt() % 2u")
    PrimitiveType.UInt -> unsignedIntConfig(suffix = "u", modExpr = "it % 2u")
    PrimitiveType.ULong -> unsignedIntConfig(suffix = "uL", modExpr = "it % 2uL")
}

fun allEqualWithPredicateFor(primitive: PrimitiveType?): PredicateSample = when (primitive) {
    PrimitiveType.Char -> PredicateSample(
        predicateExpr = "{ a, b -> a.equals(b, ignoreCase = true) }",
        positiveValues = listOf("'a'", "'A'", "'a'"),
        negativeValues = listOf("'a'", "'A'", "'b'"),
    )
    PrimitiveType.Boolean -> PredicateSample(
        predicateExpr = "{ a, b -> a == b }",
        positiveValues = listOf("true", "true", "true"),
        negativeValues = listOf("true", "true", "false"),
    )
    null -> PredicateSample(
        predicateExpr = "{ a, b -> a.equals(b, ignoreCase = true) }",
        positiveValues = listOf("\"Apple\"", "\"APPLE\"", "\"apple\""),
        negativeValues = listOf("\"apple\"", "\"apple\"", "\"orange\""),
    )
    PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.Int -> PredicateSample(
        predicateExpr = "{ a, b -> a / 10 == b / 10 }",
        positiveValues = listOf("10", "12", "15"),
        negativeValues = listOf("10", "12", "25"),
    )
    PrimitiveType.Long -> PredicateSample(
        predicateExpr = "{ a, b -> a / 10L == b / 10L }",
        positiveValues = listOf("10L", "12L", "15L"),
        negativeValues = listOf("10L", "12L", "25L"),
    )
    PrimitiveType.Float -> PredicateSample(
        predicateExpr = "{ a, b -> a.toInt() == b.toInt() }",
        positiveValues = listOf("1.0f", "1.25f", "1.5f"),
        negativeValues = listOf("1.0f", "1.25f", "2.0f"),
    )
    PrimitiveType.Double -> PredicateSample(
        predicateExpr = "{ a, b -> a.toInt() == b.toInt() }",
        positiveValues = listOf("1.0", "1.25", "1.5"),
        negativeValues = listOf("1.0", "1.25", "2.0"),
    )
    PrimitiveType.UByte, PrimitiveType.UShort -> PredicateSample(
        predicateExpr = "{ a, b -> a.toUInt() / 10u == b.toUInt() / 10u }",
        positiveValues = listOf("10u", "12u", "15u"),
        negativeValues = listOf("10u", "12u", "25u"),
    )
    PrimitiveType.UInt -> PredicateSample(
        predicateExpr = "{ a, b -> a / 10u == b / 10u }",
        positiveValues = listOf("10u", "12u", "15u"),
        negativeValues = listOf("10u", "12u", "25u"),
    )
    PrimitiveType.ULong -> PredicateSample(
        predicateExpr = "{ a, b -> a / 10uL == b / 10uL }",
        positiveValues = listOf("10uL", "12uL", "15uL"),
        negativeValues = listOf("10uL", "12uL", "25uL"),
    )
}

data class PredicateSample(
    val predicateExpr: String,
    val positiveValues: List<String>,
    val negativeValues: List<String>,
    val needsAbsImport: Boolean = false,
)

/**
 * A triple of values and a predicate chosen so that `allEqualWith` returns `true`
 * under the specified compare-with-first semantics, but would return `false` under
 * a mistaken adjacent-pair semantics.
 */
data class FirstVsEachCase(
    val values: List<String>,
    val predicateExpr: String,
)

fun firstVsEachCaseFor(primitive: PrimitiveType?): FirstVsEachCase = when (primitive) {
    PrimitiveType.Boolean -> FirstVsEachCase(
        values = listOf("false", "true", "false"),
        predicateExpr = "{ a, b -> !a || b }",
    )
    PrimitiveType.Char -> FirstVsEachCase(
        values = listOf("'a'", "'b'", "'a'"),
        predicateExpr = "{ a, b -> a <= b }",
    )
    null -> FirstVsEachCase(
        values = listOf("\"a\"", "\"bb\"", "\"a\""),
        predicateExpr = "{ a, b -> a.length <= b.length }",
    )
    PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.Int -> FirstVsEachCase(
        values = listOf("1", "2", "1"),
        predicateExpr = "{ a, b -> a <= b }",
    )
    PrimitiveType.Long -> FirstVsEachCase(
        values = listOf("1L", "2L", "1L"),
        predicateExpr = "{ a, b -> a <= b }",
    )
    PrimitiveType.Float -> FirstVsEachCase(
        values = listOf("1.0f", "2.0f", "1.0f"),
        predicateExpr = "{ a, b -> a <= b }",
    )
    PrimitiveType.Double -> FirstVsEachCase(
        values = listOf("1.0", "2.0", "1.0"),
        predicateExpr = "{ a, b -> a <= b }",
    )
    PrimitiveType.UByte, PrimitiveType.UShort, PrimitiveType.UInt -> FirstVsEachCase(
        values = listOf("1u", "2u", "1u"),
        predicateExpr = "{ a, b -> a <= b }",
    )
    PrimitiveType.ULong -> FirstVsEachCase(
        values = listOf("1uL", "2uL", "1uL"),
        predicateExpr = "{ a, b -> a <= b }",
    )
}
