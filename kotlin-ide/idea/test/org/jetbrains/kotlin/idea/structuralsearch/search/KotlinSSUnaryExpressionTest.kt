package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSUnaryExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "unaryExpression"

    fun testUnaryPlus() { doTest("+3") }

    fun testUnaryMinus() { doTest("-3") }

    fun testNot() { doTest("!'_") }

    fun testPreIncrement() { doTest("++'_ ") }

    fun testPostIncrement() { doTest("'_ ++") }

    fun testPreDecrement() { doTest("--'_") }

    fun testPostDecrement() { doTest("'_--") }

    fun testAssertNotNull() { doTest("'_!!") }
}