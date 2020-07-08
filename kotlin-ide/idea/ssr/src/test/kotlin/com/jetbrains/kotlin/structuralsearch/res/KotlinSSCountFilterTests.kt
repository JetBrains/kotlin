package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSCountFilterTests : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "countFilter"

    // isApplicableMinCount

    fun testMinProperty() { doTest("var '_ = '_?") }

    fun testMinDotQualifierExpression() { doTest("'_?.'_") }

    fun testMinFunctionTypeReference() { doTest("fun '_{0,0}.'_()") }

    fun testMinCallableReferenceExpression() { doTest("'_{0,0}::'_") }

    fun testMinWhenExpression() { doTest("when ('_?) {}") }

    // isApplicableMaxCount

    fun testMaxDestructuringDeclarationEntry() { doTest("for (('_{3,3}) in '_) { '_* }") }

    fun testMaxWhenConditionWithExpression() { doTest("when ('_?) { '_{2,2} -> '_ }") }
}