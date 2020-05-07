package com.jetbrains.kotlin.structuralsearch

class KotlinSSPropertyTest : KotlinSSTest() {
    override fun getBasePath() = "property"

    fun testVar() { doTest("var '_") }

    fun testVal() { doTest("val '_") }

    fun testTypedVal() { doTest("val '_ : Int") }

    fun testAmbiguousTypedVal() { doTest("val '_ : Int") }

    fun testFqTypedVal() { doTest("val '_ : Foo.Int") }

    fun testValWithInitializer() { doTest("val '_ = 1") }

    fun testTypedValWithInitializer() { doTest("val '_ : Int = 1") }

    fun testValReceiverType() { doTest("val '_ : ('_T) -> '_U = '_") }

    fun testVarTypeProjection() { doTest("var '_ : Comparable<'_T>") }
}