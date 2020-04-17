package com.jetbrains.kotlin.structuralsearch

class KotlinSSExpressionWithLabelTest : KotlinSSTest() {
    override fun getBasePath() = "expressionWithLabel"

    fun testBreak() { doTest("break") }
    fun testBreakLabel() { doTest("break@loop") }

    fun testContinue() { doTest("continue") }
    fun testContinueLabel() { doTest("continue@loop") }

    fun testReturn() { doTest("return 1") }
    fun testReturnLabel() { doTest("return@lit") }

    fun testSuper() { doTest("super") }
    fun testSuperTypeQualifier() { doTest("super<B>") }

    fun testThis() { doTest("this") }
    fun testThisLabel() { doTest("this@A") }

}