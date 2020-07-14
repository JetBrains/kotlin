package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest
import com.jetbrains.kotlin.structuralsearch.KotlinStructuralSearchProfile

class KotlinSSTypeFilterTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "typeFilter"

    // Behavior

    fun testShortNameTypeFilter() { doTest("val '_x:[exprtype(Int)]") }

    fun testFqNameTypeFilter() { doTest("val '_x:[exprtype(kotlin.Int)]") }

    fun testWithinHierarchyTypeFilter() { doTest("val '_x:[exprtype(*Number)]") }

    fun testNullableType() { doTest("'_('_:[exprtype(Int?)])") }

    fun testNullableTypeHierarchy() { doTest("val '_:[exprtype(*A)]") }

    // Elements where type filter is enabled

    fun testTypeValueArgument() { doTest("'_('_:[exprtype(String)])") }

    fun testTypeBinaryExpression() { doTest("'_:[exprtype(Int)] + '_:[exprtype(Float)]") }

    fun testTypeBinaryExpressionWithTypeRHS() { doTest("'_:[exprtype(Any)] as '_") }

    fun testTypeIsExpression() { doTest("'_:[exprtype(Any)] is '_") }

    fun testTypeBlockExpression() { doTest("{'_ -> '_:[exprtype(Int)]}") }

    fun testTypeArrayAccessArrayExpression() { doTest("'_:[exprtype(Array)]['_]") }

    fun testTypeArrayAccessIndicesNode() { doTest("'_['_:[exprtype(String)]]") }

    fun testTypePostfixExpression() { doTest("'_:[exprtype(Int)]++") }

    fun testTypeDotQualifiedExpression() { doTest("'_:[exprtype(String)].'_") }

    fun testTypeSafeQualifiedExpression() { doTest("'_:[exprtype(A?)]?.'_") }

    fun testTypeCallableReferenceExpression() { doTest("'_:[exprtype(A)]::'_") }

    fun testTypeSimpleNameStringTemplateEntry() { doTest(""" "$$'_:[exprtype(Int)]" """) }

    fun testTypeBlockStringTemplateEntry() { doTest(""" "${'$'}{ '_:[exprtype(Int)] }" """) }

    fun testTypePropertyAccessor() { doTest("val '_ get() = '_:[exprtype(Int)]", KotlinStructuralSearchProfile.PROPERTY_CONTEXT) }

    fun testTypeWhenEntry() { doTest("when { '_ -> '_:[exprtype(Int)] }") }
}