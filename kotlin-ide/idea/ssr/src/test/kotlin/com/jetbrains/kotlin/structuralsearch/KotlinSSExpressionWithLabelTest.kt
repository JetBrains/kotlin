package com.jetbrains.kotlin.structuralsearch

class KotlinSSExpressionWithLabelTest : KotlinSSTest() {
    override fun getBasePath() = "expressionWithLabel"

    fun testBreak() { doTest("break") }

    fun testContinue() { doTest("continue") }

    fun testReturn() { doTest("return 1") }

    fun testSuper() { doTest("super") }

    fun testThis() { doTest("this") }

    fun testBreakLabel() { doTest("break@loop") }

    fun testContinueLabel() { doTest("continue@loop") }

    fun testReturnLabel() { doTest("return@lit") }

    // TODO: Write tests for super and this with labels
}