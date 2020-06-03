package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSArrayAccessExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "arrayAccessExpression"

    fun testConstAccess() { doTest("a[0]") }

    fun testConstAccessGet() { doTest("a[0]") }
}