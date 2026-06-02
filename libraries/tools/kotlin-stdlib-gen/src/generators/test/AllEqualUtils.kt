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
    sampleSelectorAssertions = listOf("it * it" to true, absExpr to true, "it * it * it" to false),
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
    sampleSelectorAssertions = listOf("it * it" to true, "abs(it)" to true, "it * it * it" to false),
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
        sampleSelectorAssertions = listOf("0" to true, "if (it) 1 else 0" to false),
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

