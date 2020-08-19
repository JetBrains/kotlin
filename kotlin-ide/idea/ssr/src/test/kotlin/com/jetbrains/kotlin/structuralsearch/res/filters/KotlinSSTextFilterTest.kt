package com.jetbrains.kotlin.structuralsearch.res.filters

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTextFilterTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "textFilter"

    fun testHierarchyClassName() { doTest("class '_:*[regex(Foo)]") }

    fun testHierarchyClassDeclaration() { doTest("class Foo { val '_:*[regex(.*)] }") }

    fun testHierarchyClassSuperType() { doTest("class '_ : '_:*[regex(Foo)]()") }
}