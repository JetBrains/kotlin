package com.jetbrains.kotlin.structuralsearch

class KotlinSSUnaryExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "unaryExpression"

    fun testUnaryPlus() { doTest("+a") }

    fun testUnaryMinus() { doTest("-a") }

    fun testNot() { doTest("!a") }

    fun testPreIncrement() { doTest("++b") }

    fun testPreDecrement() { doTest("--b") }

    fun testPostIncrement() { doTest("b++") }

    fun testPostDecrement() { doTest("b--") }
}