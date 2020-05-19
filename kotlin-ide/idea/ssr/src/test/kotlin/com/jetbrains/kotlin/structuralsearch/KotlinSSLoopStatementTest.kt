package com.jetbrains.kotlin.structuralsearch

class KotlinSSLoopStatementTest : KotlinSSTest() {
    override fun getBasePath(): String = "loopStatement"

    fun testForLoop() {
        doTest(
            """
            for(i in 0..10) {
                println(i)
            }
            """
        )
    }

    fun testWhileLoop() {
        doTest(
            """
            while(true) {
                println(0)
            }
            """
        )
    }

    fun testDoWhileLoop() {
        doTest(
            """
            do {
                println(0)
            } while(true)
            """
        )
    }
}