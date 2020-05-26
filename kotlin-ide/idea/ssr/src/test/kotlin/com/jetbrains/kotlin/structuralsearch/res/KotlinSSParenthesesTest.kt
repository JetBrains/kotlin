package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSParenthesesTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "parentheses"

    fun testPostIncr() { doTest("++(('_))") }

    fun testStringLiteral() { doTest("(((\"Hello World!\")))") }

    fun testVariableRef() { doTest("(('_))") }

    fun testBinaryExpr() { doTest("1 + 2 + 3") }
}