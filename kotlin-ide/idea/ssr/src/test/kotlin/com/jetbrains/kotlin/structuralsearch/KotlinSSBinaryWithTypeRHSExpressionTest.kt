package com.jetbrains.kotlin.structuralsearch

class KotlinSSBinaryWithTypeRHSExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "binaryWithTypeRHSExpression"

    fun testFqType() { doTest("'_ as kotlin.Int") }

    fun testNonFqType() { doTest("'_ as Int") }
}