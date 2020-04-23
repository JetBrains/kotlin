package com.jetbrains.kotlin.structuralsearch

class KotlinSSProcedureCallTest : KotlinSSTest() {
    override fun getBasePath() = "procedureCall"

    fun testFunctionCall() { doTest("a()") }

    fun testFunctionArgumentsCall() { doTest("a(true, 0)") }

    fun testFunctionNamedValueArgumentsCall() { doTest("a(b = true, c = 0)") }

    fun testFunctionScrambledValueArgumentsCall() { doTest("a(c = 0, b = true)") }
}