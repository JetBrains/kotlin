package com.jetbrains.kotlin.structuralsearch

class KotlinSSPostfixExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "postfixExpression"

    fun testIncrement() { doTest("'_ ++") }

    fun testDecrement() { doTest("'_--") }

    fun testAssertNotNull() { doTest("'_!!") }
}