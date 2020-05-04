package com.jetbrains.kotlin.structuralsearch

class KotlinSSPrefixExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "prefixExpression"

    fun testUnaryPlus() { doTest("+'_") }

    fun testUnaryMinus() { doTest("-'_") }

    fun testNot() { doTest("!'_") }

    fun testPreIncrement() { doTest("++'_") }

    fun testPreDecrement() { doTest("--'_") }

}