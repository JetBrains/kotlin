package com.jetbrains.kotlin.structuralsearch

class KotlinSSConstantExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "constantExpression"

    fun testInteger() { doTest("1") }

    fun testBoolean() { doTest("true") }

    fun testString() { doTest("\"1\"") }
}