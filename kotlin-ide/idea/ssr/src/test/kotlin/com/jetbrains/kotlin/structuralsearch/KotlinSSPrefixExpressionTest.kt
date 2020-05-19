package com.jetbrains.kotlin.structuralsearch

class KotlinSSPrefixExpressionTest : KotlinSSTest() {
    override fun getBasePath(): String = "prefixExpression"

    fun testUnaryPlus() { doTest("+'_") }

    fun testUnaryMinus() { doTest("-'_") }

    fun testNot() { doTest("!'_") }

    fun testIncrement() { doTest("++'_") }

    fun testIncrementParentheses() { doTest("++('_)") }

    fun testIncrementMultParentheses() { doTest("++(('_))") }

    fun testDecrement() { doTest("--'_") }

}