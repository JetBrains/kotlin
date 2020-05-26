package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSBinaryExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "binaryExpression"

    fun testBinaryExpression() { doTest("1 + 2 - 3") }

    fun testBinaryParExpression() { doTest("3 * (2 - 3)") }

    fun testTwoBinaryExpressions() { doTest("a = 1 \n b = 2") }

    fun testElvis() { doTest("'_ ?: '_") }
}