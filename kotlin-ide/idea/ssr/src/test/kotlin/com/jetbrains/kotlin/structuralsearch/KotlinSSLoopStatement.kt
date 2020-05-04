package com.jetbrains.kotlin.structuralsearch

class KotlinSSLoopStatement : KotlinSSTest() {
    override fun getBasePath() = "loopStatement"

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
}