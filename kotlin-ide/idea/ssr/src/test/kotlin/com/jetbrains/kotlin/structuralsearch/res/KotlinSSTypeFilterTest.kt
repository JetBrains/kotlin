package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTypeFilterTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "typeFilter"

    // Behavior

    fun testShortNameTypeFilter() { doTest("val '_x:[exprtype(Int)]") }

    fun testFqNameTypeFilter() { doTest("val '_x:[exprtype(kotlin.Int)]") }

    fun testWithinHierarchyTypeFilter() { doTest("val '_x:[exprtype(*Number)]") }

    fun testNullableType() { doTest("'_('_:[exprtype(Int?)])") }

    fun testNullableTypeHierarchy() { doTest("val '_:[exprtype(*A)]") }

    // Elements where type filter is enabled

    fun testTypeDotQualifiedExpression() { doTest("'_:[exprtype(String)].'_") }

    fun testTypeArrayAccessArrayExpression() { doTest("'_:[exprtype(Array)]['_]") }

    fun testTypeArrayAccessIndicesNode() { doTest("'_['_:[exprtype(String)]]") }
}