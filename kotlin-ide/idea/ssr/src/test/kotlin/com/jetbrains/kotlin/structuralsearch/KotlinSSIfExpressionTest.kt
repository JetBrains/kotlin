package com.jetbrains.kotlin.structuralsearch

class KotlinSSIfExpressionTest : KotlinSSTest() {
    override fun getBasePath() = "ifExpression"

    fun testIf() { doTest("if(true) b = true") }

    fun testIfElse() { doTest("if(true) 1 else 2") }

    fun testIfBlock() {
        doTest(
            """
            if(true) {
|               a = 1
|           }""".trimMargin()
        )
    }

    fun testIfElseBlock() {
        doTest(
            """
            if (a == 1) {
                a = 2
            } else {
                a = 3
            }""".trimMargin()
        )
    }

    fun testIfElseCondition() {
        doTest(
            """
            if (a == 1) {
                a = 2
            } else {
                a = 3
            }""".trimMargin()
        )
    }
}