package com.jetbrains.kotlin.structuralsearch

class KotlinSSReferenceExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "referenceExpression"

    fun testMatchAny() { doTest("\$x\$") }
    fun testMatchWithFilter() { doTest("\$x\$") }
}