package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSThrowExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "throwExpression"

    fun testAnyException() { doTest("throw '_") }
}