package com.jetbrains.kotlin.structuralsearch

class KotlinSSIfExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "ifExpression"

    fun testIf() {
        doTest("if(true) b = true")
    }

    fun testIfElse() {
        doTest("if(true) b = 1 else b = 2")
    }

    fun testIfBlock() {
        doTest("""if(true) {
|           a = 1
|       }""".trimMargin())
    }
}