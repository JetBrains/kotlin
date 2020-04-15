package com.jetbrains.kotlin.structuralsearch

class KotlinSSConstantExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "constantExpression"

    fun testNull() { doTest("null") }

    fun testBoolean() { doTest("true") }

    fun testInteger() { doTest("1") }

    fun testFloat() { doTest("1.0f") }

    fun testCharacter() { doTest("'a'") }
}