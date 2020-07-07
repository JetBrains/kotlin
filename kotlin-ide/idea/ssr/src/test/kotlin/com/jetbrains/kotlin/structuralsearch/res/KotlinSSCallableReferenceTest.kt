package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSCallableReferenceTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "callableReference"

    fun testCallableReference() { doTest("::'_") }

    fun testExtensionFun() { doTest("List<Int>::'_") }

    fun testPropertyReference() { doTest("::'_.name") }
}