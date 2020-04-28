package com.jetbrains.kotlin.structuralsearch

class KotlinSSIsExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "isExpression"

    fun testIs() { doTest("'_ is '_") }

    fun testNegatedIs() { doTest("'_ !is '_") }
}