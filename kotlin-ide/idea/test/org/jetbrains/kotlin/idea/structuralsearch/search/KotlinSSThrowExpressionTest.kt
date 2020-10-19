package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSThrowExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "throwExpression"

    fun testAnyException() { doTest("throw '_") }
}