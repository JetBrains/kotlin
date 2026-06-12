/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.PrimitiveType

class AllDistinctTypeConfig(
    val value1: String,
    val value2: String,
    val valuesWithDistinctKeys: List<String>,
    val valuesWithDuplicateKeys: List<String>,
    val sampleDistinctValues: List<String>,
    val sampleDuplicateValues: List<String>,
    val sampleSelectorValues: List<String>,
    val sampleSelectorAssertions: List<Pair<String, Boolean>>,
    val selectorExpr: String = "it",
    val sampleNeedsAbsImport: Boolean = false,
)

private fun signedIntConfig(suffix: String): AllDistinctTypeConfig = AllDistinctTypeConfig(
    value1 = "1$suffix",
    value2 = "2$suffix",
    selectorExpr = "it * it",
    valuesWithDistinctKeys = listOf("1", "2").map { "$it$suffix" },
    valuesWithDuplicateKeys = listOf("1", "-1", "1").map { "$it$suffix" },
    sampleDistinctValues = listOf("1", "2", "3").map { "$it$suffix" },
    sampleDuplicateValues = listOf("1", "2", "1").map { "$it$suffix" },
    sampleSelectorValues = listOf("1", "-1", "2").map { "$it$suffix" },
    sampleSelectorAssertions = listOf("it * it" to false, "it" to true),
)

private fun unsignedIntConfig(suffix: String, modExpr: String): AllDistinctTypeConfig = AllDistinctTypeConfig(
    value1 = "1$suffix",
    value2 = "2$suffix",
    selectorExpr = modExpr,
    valuesWithDistinctKeys = listOf("1", "2").map { "$it$suffix" },
    valuesWithDuplicateKeys = listOf("1", "3", "5").map { "$it$suffix" },
    sampleDistinctValues = listOf("1", "2", "3").map { "$it$suffix" },
    sampleDuplicateValues = listOf("1", "2", "1").map { "$it$suffix" },
    sampleSelectorValues = listOf("1", "3", "2").map { "$it$suffix" },
    sampleSelectorAssertions = listOf(modExpr to false, "it" to true),
)

private fun floatingPointConfig(suffix: String): AllDistinctTypeConfig = AllDistinctTypeConfig(
    value1 = "1.0$suffix",
    value2 = "2.0$suffix",
    selectorExpr = "it * it",
    valuesWithDistinctKeys = listOf("1.0", "2.0").map { "$it$suffix" },
    valuesWithDuplicateKeys = listOf("1.0", "-1.0", "1.0").map { "$it$suffix" },
    sampleDistinctValues = listOf("1.0", "2.0", "3.0").map { "$it$suffix" },
    sampleDuplicateValues = listOf("1.0", "2.0", "1.0").map { "$it$suffix" },
    sampleSelectorValues = listOf("1.0", "-1.0", "2.0").map { "$it$suffix" },
    sampleSelectorAssertions = listOf("it * it" to false, "it" to true),
)

fun allDistinctConfigFor(primitive: PrimitiveType?): AllDistinctTypeConfig = when (primitive) {
    PrimitiveType.Char -> AllDistinctTypeConfig(
        value1 = "'a'",
        value2 = "'b'",
        selectorExpr = "it.uppercaseChar()",
        valuesWithDistinctKeys = listOf("'a'", "'B'"),
        valuesWithDuplicateKeys = listOf("'a'", "'A'", "'a'"),
        sampleDistinctValues = listOf("'a'", "'b'", "'c'"),
        sampleDuplicateValues = listOf("'a'", "'b'", "'a'"),
        sampleSelectorValues = listOf("'a'", "'A'", "'b'"),
        sampleSelectorAssertions = listOf("it.uppercaseChar()" to false, "it" to true),
    )
    PrimitiveType.Boolean -> AllDistinctTypeConfig(
        value1 = "true",
        value2 = "false",
        valuesWithDistinctKeys = listOf("true", "false"),
        valuesWithDuplicateKeys = listOf("true", "true", "true"),
        sampleDistinctValues = listOf("true", "false"),
        sampleDuplicateValues = listOf("true", "false", "true"),
        sampleSelectorValues = listOf("true", "false"),
        sampleSelectorAssertions = listOf("if (it) 1 else 0" to true, "0" to false),
    )
    null -> AllDistinctTypeConfig(
        value1 = "\"a\"",
        value2 = "\"b\"",
        selectorExpr = "it.length",
        valuesWithDistinctKeys = listOf("\"a\"", "\"bb\""),
        valuesWithDuplicateKeys = listOf("\"a\"", "\"b\"", "\"c\""),
        sampleDistinctValues = listOf("\"apple\"", "\"mango\"", "\"peach\""),
        sampleDuplicateValues = listOf("\"apple\"", "\"mango\"", "\"apple\""),
        sampleSelectorValues = listOf("\"apple\"", "\"mango\"", "\"peach\""),
        sampleSelectorAssertions = listOf("it.length" to false, "it" to true),
    )
    PrimitiveType.Byte, PrimitiveType.Short -> signedIntConfig(suffix = "")
    PrimitiveType.Int -> signedIntConfig(suffix = "")
    PrimitiveType.Long -> signedIntConfig(suffix = "L")
    PrimitiveType.Float -> floatingPointConfig(suffix = "f")
    PrimitiveType.Double -> floatingPointConfig(suffix = "")
    PrimitiveType.UByte, PrimitiveType.UShort -> unsignedIntConfig(suffix = "u", modExpr = "it.toUInt() % 2u")
    PrimitiveType.UInt -> unsignedIntConfig(suffix = "u", modExpr = "it % 2u")
    PrimitiveType.ULong -> unsignedIntConfig(suffix = "uL", modExpr = "it % 2uL")
}
