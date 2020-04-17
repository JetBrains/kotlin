package com.jetbrains.kotlin.structuralsearch

class KotlinSSBinaryExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "binaryExpression"

    fun testBinaryExpression() { doTest("1 + 2 - 3") }
}