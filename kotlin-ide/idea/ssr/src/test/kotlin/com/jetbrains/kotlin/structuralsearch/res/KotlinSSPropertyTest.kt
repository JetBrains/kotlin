package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest
import com.jetbrains.kotlin.structuralsearch.KotlinStructuralSearchProfile

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

    fun testVarGetterModifier() {
        doTest("""
            var '_Field = '_ 
                @'_Ann get() = '_
        """, KotlinStructuralSearchProfile.PROPERTY_CONTEXT) }

    fun testVarSetterModifier() {
        doTest("""
            var '_Field = '_ 
                private set('_x) { '_* }
        """, KotlinStructuralSearchProfile.PROPERTY_CONTEXT) }

    fun testFunctionType() { doTest("val '_ : ('_{2,2}) -> Unit") }

    fun testFunctionTypeNamedParameter() { doTest("val '_ : ('_) -> '_") }

}