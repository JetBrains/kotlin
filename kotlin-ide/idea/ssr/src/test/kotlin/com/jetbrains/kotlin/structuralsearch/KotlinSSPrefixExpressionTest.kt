package com.jetbrains.kotlin.structuralsearch

class KotlinSSPrefixExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "prefixExpression"

    fun testUnaryPlus() { doTest("+'_") }

    fun testUnaryMinus() { doTest("-'_") }

    fun testNot() { doTest("!'_") }

    fun testIncrement() { doTest("++'_") }

    fun testIncrementParenthesis() { doTest("++('_)") }

    fun testIncrementMultParenthesis() { doTest("++(('_))") }

    fun testDecrement() { doTest("--'_") }

}