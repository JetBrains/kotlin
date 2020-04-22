package com.jetbrains.kotlin.structuralsearch

class KotlinSSCallTest : KotlinSSTest() {
    override fun getBasePath() = "call"

    fun testFunctionCall() {
        doTest("""
            a()
        """.trimIndent())
    }
}