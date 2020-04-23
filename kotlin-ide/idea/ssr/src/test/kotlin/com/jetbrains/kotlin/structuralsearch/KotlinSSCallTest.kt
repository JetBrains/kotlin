package com.jetbrains.kotlin.structuralsearch

class KotlinSSCallTest : KotlinSSTest() {
    override fun getBasePath() = "procedureCall"

    fun testFunctionCall() { doTest("a()") }

    fun testFunctionArgumentsCall() { doTest("a(true, 0)") }

    fun testFunctionNamedArgumentsCall() { doTest("a(b = true, c = 0)") }

    fun testFunctionScrambledArgumentsCall() { doTest("a(c = 0, b = true)") }
}