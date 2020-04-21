package com.jetbrains.kotlin.structuralsearch

class KotlinSSReferenceExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "referenceExpression"

    fun testMatchAny() { doTest("'_") }

    fun testRegexTextFilter() { doTest("'_:[regex( foo.+ )]") }
}