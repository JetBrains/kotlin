package com.jetbrains.kotlin.structuralsearch

class KotlinSSUnaryExpressionTest : KotlinSSTest() {
    override fun getBasePath(): String = "unaryExpression"

    fun testPreIncrement() { doTest("++'_ ") }

    fun testPostIncrement() { doTest("'_ ++") }

    fun testPreDecrement() { doTest("--'_") }

    fun testPostDecrement() { doTest("'_--") }

    fun testAssertNotNull() { doTest("'_!!") }

    fun testNot() { doTest("!'_") }

}