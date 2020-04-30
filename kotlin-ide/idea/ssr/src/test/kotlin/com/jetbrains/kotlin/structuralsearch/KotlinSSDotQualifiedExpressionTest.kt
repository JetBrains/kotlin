package com.jetbrains.kotlin.structuralsearch

class KotlinSSDotQualifiedExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "dotQualifiedExpression"

    fun testRegular() { doTest("'_.'_") }

    fun testOptionalReference() { doTest("'_?.'_") }

}