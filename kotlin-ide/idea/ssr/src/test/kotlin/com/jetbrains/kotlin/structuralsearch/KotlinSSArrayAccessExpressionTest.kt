package com.jetbrains.kotlin.structuralsearch

class KotlinSSArrayAccessExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "arrayAccessExpression"

    fun testConstantExpressionAccess() { doTest("a[0]") }

}