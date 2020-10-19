package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSArrayAccessExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "arrayAccessExpression"

    fun testConstAccess() { doTest("a[0]") }

    fun testConstAccessGet() { doTest("a[0]") }
}