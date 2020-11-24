package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSDoubleColonExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "doubleColonExpression"

    fun testClassLiteralExpression() { doTest("Int::class") }

    fun testFqClassLiteralExpression() { doTest("kotlin.Int::class") }

    fun testDotQualifiedExpression() { doTest("Int::class.java") }
}