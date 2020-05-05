package com.jetbrains.kotlin.structuralsearch

class KotlinSSLambdaExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "lambdaExpression"

    fun testEmpty() { doTest("{}") }

    fun testBody() { doTest("{ '_Expr+ }") }

    fun testExplicitIt() { doTest("{ it -> '_Expr+ }") }

}