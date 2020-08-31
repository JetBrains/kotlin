package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSBinaryWithTypeRHSExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "binaryWithTypeRHSExpression"

    fun testAs() { doTest("'_ as '_") }

    fun testAsSafe() { doTest("'_ as? '_") }

}