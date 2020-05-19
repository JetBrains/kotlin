package com.jetbrains.kotlin.structuralsearch

class KotlinSSPostfixExpressionTest : KotlinSSTest() {
    override fun getBasePath(): String = "postfixExpression"

    fun testIncrement() { doTest("'_ ++") }

    fun testIncrementParentheses() { doTest("('_)++") }

    fun testIncrementMultParentheses() { doTest("(('_))++") }

    fun testDecrement() { doTest("'_--") }

    fun testAssertNotNull() { doTest("'_!!") }
}