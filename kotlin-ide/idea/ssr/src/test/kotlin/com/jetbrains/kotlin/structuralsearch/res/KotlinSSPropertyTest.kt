package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSPropertyTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "property"

    fun testVar() { doTest("var '_") }

    fun testVal() { doTest("val '_") }

    fun testValType() { doTest("val '_ : Int") }

    fun testValFqType() { doTest("val '_ : Foo.Int") }

    fun testValComplexFqType() { doTest("val '_ : '_<'_<'_, (Foo.Int) -> Int>>") }

    fun testValInitializer() { doTest("val '_ = 1") }

    fun testValReceiverType() { doTest("val '_ : ('_T) -> '_U = '_") }

    fun testVarTypeProjection() { doTest("var '_ : Comparable<'_T>") }

    fun testVarStringAssign() { doTest("var '_  = \"Hello world\"") }

    fun testVarStringAssignPar() { doTest("var '_  = (\"Hello world\")") }

    fun testVarRefAssign() { doTest("var '_  = a") }

    fun testVarNoInitializer() { doTest("var '_ = '_{0,0}") }
}