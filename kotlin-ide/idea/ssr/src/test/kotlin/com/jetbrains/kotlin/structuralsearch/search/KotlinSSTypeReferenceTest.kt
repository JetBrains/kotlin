package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTypeReferenceTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "typeReference"

    fun testAny() { doTest("fun '_('_ : '_) { '_* }") }

    fun testFqType() { doTest("fun '_('_ : kotlin.Int) { '_* }") }

    fun testFunctionType() { doTest("fun '_('_ : ('_) -> '_) { '_* }") }

    fun testNullableType() { doTest("fun '_('_ : '_ ?) { '_* }") }
    
    fun testFqTextFilter() { doTest("""fun '_('_ : '_:[regex( kotlin\.Int )])""") }

    fun testStandaloneNullable() { doTest("Int?") }
}