package com.jetbrains.kotlin.structuralsearch

class KotlinSSBinaryWithTypeRHSExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "binaryWithTypeRHSExpression"

    fun testAs() { doTest("'_ as '_") }

    fun testAsFqType() { doTest("'_ as kotlin.Int") }

    fun testAsNonFqType() { doTest("'_ as Int") }
}