package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSArrayAccessExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "arrayAccessExpression"

    fun testConstantExpressionAccess() { doTest("a[0]") }
}