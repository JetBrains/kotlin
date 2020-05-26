package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSLambdaExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "lambdaExpression"

    fun testEmpty() { doTest("{}") }

    fun testBody() { doTest("{ '_Expr+ }") }

    fun testExplicitIt() { doTest("{ it -> '_Expr+ }") }

}