package com.jetbrains.kotlin.structuralsearch

class KotlinSSBinaryWithTypeRHSExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "binaryWithTypeRHSExpression"

    fun testAs() { doTest("'_ as '_") }

    fun testAsSafe() { doTest("'_ as? '_") }

}