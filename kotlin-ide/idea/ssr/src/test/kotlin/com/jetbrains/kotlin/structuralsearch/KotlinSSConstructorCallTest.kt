package com.jetbrains.kotlin.structuralsearch

class KotlinSSConstructorCallTest : KotlinSSTest() {
    override fun getBasePath() = "constructorCall"

    fun testConstrArgCall() { doTest("A(true, 0)") }

    fun testConstrCall() { doTest("A()") }

    fun testConstrLambdaArgCall() { doTest("A { println() }") }

    fun testConstrMixedArgsCall() { doTest("A(c = 0, b = true)") }

    fun testConstrMixedSpreadVarargCall() { doTest("A(0, 1, 2, 3, 4)") }

    fun testConstrMixedVarargCall() { doTest("A(0, *intArrayOf(1, 2, 3), 4)") }

    fun testConstrNamedArgsCall() { doTest("A(b = true, c = 0)") }

    fun testConstrSpreadVarargCall() { doTest("A(1, 2, 3)") }

    fun testConstrTypeArgCall() { doTest("A<Int, String>(0, \"a\")") }

    fun testConstrVarargCall() { doTest("A(*intArrayOf(1, 2, 3))") }
}