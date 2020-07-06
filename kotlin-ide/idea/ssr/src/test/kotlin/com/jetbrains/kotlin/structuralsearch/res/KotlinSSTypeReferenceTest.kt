package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTypeReferenceTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "typeReference"

    fun testAny() { doTest("fun '_('_ : '_) { '_* }") }

    fun testFqType() { doTest("fun '_('_ : kotlin.Int) { '_* }") }

    fun testFunctionType() { doTest("fun '_('_ : ('_) -> '_) { '_* }") }

    fun testNullableType() { doTest("fun '_('_ : '_ ?) { '_* }") }
}