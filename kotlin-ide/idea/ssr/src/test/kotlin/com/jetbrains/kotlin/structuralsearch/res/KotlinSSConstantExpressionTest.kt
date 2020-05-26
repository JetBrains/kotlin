package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSConstantExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "constantExpression"

    fun testNull() { doTest("null") }

    fun testBoolean() { doTest("true") }

    fun testInteger() { doTest("1") }

    fun testFloat() { doTest("1.0f") }

    fun testCharacter() { doTest("'a'") }
}