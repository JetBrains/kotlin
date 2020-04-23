package com.jetbrains.kotlin.structuralsearch

class KotlinSSBinaryExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "binaryExpression"

    fun testBinaryExpression() { doTest("1 + 2 - 3") }

    fun testTwoBinaryExpressions() {
        doTest("""
            a = 1
            b = 2
        """.trimIndent())
    }
}