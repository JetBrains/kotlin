package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSIsExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "isExpression"

    fun testIs() { doTest("'_ is '_") }

    fun testNegatedIs() { doTest("'_ !is '_") }
}