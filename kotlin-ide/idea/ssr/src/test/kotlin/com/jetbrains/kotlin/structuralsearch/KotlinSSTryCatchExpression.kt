package com.jetbrains.kotlin.structuralsearch

class KotlinSSTryCatchExpression : KotlinSSTest() {
    override fun getBasePath() = "tryCatchExpression"

    fun testTryCatch() {
        doTest(
            """
            try {
                println(0)
            } catch (e: Exception) {
                println(1)
            }
            """
        )
    }

    fun testTryFinally() {
        doTest(
            """
            try {
                println(0)
            } finally {
                println(1)
            }
            """
        )
    }
}