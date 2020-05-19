package com.jetbrains.kotlin.structuralsearch

class KotlinSSQualifiedExpressionTest : KotlinSSTest() {
    override fun getBasePath(): String = "qualifiedExpression"

    fun testDotRegular() { doTest("'_.'_") }

    fun testDotOptionalReference() { doTest("'_?.'_") }

    fun testSafeAccess() { doTest("\$e1\$?.'_") }
}