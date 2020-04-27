package com.jetbrains.kotlin.structuralsearch

class KotlinSSProcedureCallTest : KotlinSSTest() {
    override fun getBasePath() = "procedureCall"

    fun testFunCall() { doTest("a()") }

    fun testFunArgCall() { doTest("a(true, 0)") }

    fun testFunNamedArgsCall() { doTest("a(b = true, c = 0)") }

    fun testFunMixedArgsCall() { doTest("a(c = 0, b = true)") }

    /** Reverse of [testFunSpreadVarargCall] */
    fun testFunVarargCall() { doTest("a(*intArrayOf(1, 2, 3))") }

    /** Reverse of [testFunVarargCall] */
    fun testFunSpreadVarargCall() { doTest("a(1, 2, 3)") }

    /** Reverse of [testFunMixedSpreadVarargCall] */
    fun testFunMixedVarargCall() { doTest("a(0, *intArrayOf(1, 2, 3), 4)") }

    /** Reverse of [testFunMixedVarargCall] */
    fun testFunMixedSpreadVarargCall() { doTest("a(0, 1, 2, 3, 4)") }

    fun testFunTypeArgCall() { doTest("a<Int, String>(0, \"a\")") }
}