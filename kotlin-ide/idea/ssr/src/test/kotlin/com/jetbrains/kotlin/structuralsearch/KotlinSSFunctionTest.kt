package com.jetbrains.kotlin.structuralsearch

class KotlinSSFunctionTest : KotlinSSTest() {
    override fun getBasePath() = "function"

    fun testFun() { doTest("fun a() { }") }

    fun testFunLocal() { doTest("fun b() { }") }

    fun testFunParam() { doTest("fun '_(b: Int, c: String) { }") }

    fun testFunTypeParam() { doTest("fun<T, R> '_(a: T, b: R, c: T) { }") }

    fun testFunReturnType() { doTest("fun '_(b: Int): Int { return b }") }

    fun testFunBlockBody() {
        doTest(
            """
            fun '_() {
                println()
            }
            """
        )
    }

    fun testFunPublicModifier() { doTest("public fun '_() { }") }

    fun testFunInternalModifier() { doTest("internal fun '_() { }") }

    fun testFunPrivateModifier() { doTest("private fun '_() { }") }

    fun testMethod() { doTest("fun a() { }") }

    fun testMethodProtectedModifier() { doTest("protected fun '_() { }") }
}