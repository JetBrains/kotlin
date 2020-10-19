package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSBinaryWithTypeRHSExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "binaryWithTypeRHSExpression"

    fun testAs() { doTest("'_ as '_") }

    fun testAsSafe() { doTest("'_ as? '_") }

}