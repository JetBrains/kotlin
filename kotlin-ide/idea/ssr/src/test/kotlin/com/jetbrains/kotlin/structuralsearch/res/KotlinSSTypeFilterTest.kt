package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTypeFilterTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "typeFilter"

    fun testShortNameTypeFilter() { doTest("val '_x:[exprtype(Int)]") }

    fun testFqNameTypeFilter() { doTest("val '_x:[exprtype(kotlin.Int)]") }

    fun testWithinHierarchyTypeFilter() { doTest("val '_x:[exprtype(*Number)]") }
}