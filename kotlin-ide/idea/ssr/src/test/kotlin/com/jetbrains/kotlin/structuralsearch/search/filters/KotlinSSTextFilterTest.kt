package com.jetbrains.kotlin.structuralsearch.search.filters

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTextFilterTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "textFilter"

    fun testHierarchyClassName() { doTest("class '_:*[regex(Foo2)]") }

    fun testHierarchyClassDeclaration() { doTest("class Foo2 { val '_:*[regex(.*)] }") }

    fun testHierarchyClassSuperType() { doTest("class '_ : '_:*[regex(Foo2)]()") }

    fun testFqSuperType() { doTest("class '_ : '_:[regex(test\\.Foo)]()") }

    fun testFqTypeAlias() { doTest("fun '_('_ : '_:[regex(test\\.OtherInt)])") }
}