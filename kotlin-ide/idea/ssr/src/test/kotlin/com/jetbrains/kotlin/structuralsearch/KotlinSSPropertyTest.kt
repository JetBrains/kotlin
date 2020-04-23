package com.jetbrains.kotlin.structuralsearch

class KotlinSSPropertyTest : KotlinSSTest() {
    override fun getBasePath() = "property"

    fun testVar() { doTest("var '_") }

    fun testVal() { doTest("val '_") }

    fun testTypedVal() { doTest("val '_ : Int") }

    fun testValWithInitializer() { doTest("val '_ = 1") }

    fun testTypedValWithInitializer() { doTest("val '_ : Int = 1") }
}