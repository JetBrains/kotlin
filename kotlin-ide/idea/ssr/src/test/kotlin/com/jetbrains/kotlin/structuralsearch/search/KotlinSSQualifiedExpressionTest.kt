package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSQualifiedExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "qualifiedExpression"

    fun testDotRegular() { doTest("'_.'_") }

    fun testSafeAccess() { doTest("\$e1\$?.'_") }

    fun testDotNoReceiver() { doTest("'_{0,0}.'_()") }
}