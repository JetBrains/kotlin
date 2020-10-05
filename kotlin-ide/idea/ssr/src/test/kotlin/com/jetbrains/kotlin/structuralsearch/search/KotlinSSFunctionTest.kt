package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSFunctionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "function"

    fun testFun() { doTest("fun a() { '_* }") }

    fun testFunAny() { doTest("fun '_( '_* )") }

    fun testFunLocal() { doTest("fun b() { '_* }") }

    fun testFunParam() { doTest("fun '_(b: Int, c: String) { '_* }") }

    fun testFunSameTypeParam() { doTest("fun '_('_ : '_A, '_ : '_A) { '_* }") }

    fun testFunSingleParam() { doTest("fun '_('_ : '_) { '_* }") }

    fun testFunTypeParam() { doTest("fun<T, R> '_(a: T, b: R, c: T) { '_* }") }

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

    fun testFunPublicModifier() { doTest("public fun '_('_*)") }

    fun testFunInternalModifier() { doTest("internal fun '_()") }

    fun testFunPrivateModifier() { doTest("private fun '_()") }

    fun testFunTypeVarRef() { doTest("fun '_(): '_") }

    fun testFunSimpleTypeReceiver() { doTest("fun<'_type> '_('_ : '_.('_type) -> '_)") }

    fun testFunReceiverType() {
        doTest(
            "fun <'_T, '_E, '_R> '_name('_f : '_T.('_E) -> '_R) : ('_T, '_E) -> '_R = { '_t, '_e -> '_t.'_f('_e) }"
        )
    }

    fun testFunTypeParamArgs() { doTest("fun <'_E, '_T> '_name(p1: '_E, p2: '_T)") }

    fun testMethod() { doTest("fun a()") }

    fun testMethodProtectedModifier() { doTest("protected fun '_()") }

    fun testFunExprBlock() { doTest("fun '_(): Int = 0") }

    fun testFunAnnotation() { doTest("@Foo fun '_('_*)") }

    fun testFunReceiverTypeReference() { doTest("fun '_.'_()") }

    fun testFunFqReceiverTypeReference() { doTest("fun kotlin.Int.'_()") }

    fun testFunVarargParam() { doTest("fun '_(vararg '_)") }

    fun testFunVarargAndNormalParam() { doTest("fun '_(vararg '_ : '_, '_ : '_)") }

    fun testFunVarargAndNormalReverseParam() { doTest("fun '_('_ : '_, vararg '_ : '_)") }

    fun testFunVarargFullMatchParam() { doTest("fun '_('_)") }
    
    fun testFunNoinlineParam() { doTest("fun '_(noinline '_)") }

    fun testFunEmptyBlock() { doTest("fun '_('_*) { '_{0,0} }") }

    fun testFun2ExprBlock() { doTest("fun '_('_*) { '_{2,2} }") }

    fun testFunBlockBodyExprAndVariable() { doTest(
        """
        fun '_('_*) {
            println()
            '_{0,1}
        }
        """) }

    fun testFunTypeProjection() { doTest("fun '_('_ : A<out '_>)") }

    fun testFunStarTypeProjection() { doTest("fun '_('_ : A<*>)") }
}