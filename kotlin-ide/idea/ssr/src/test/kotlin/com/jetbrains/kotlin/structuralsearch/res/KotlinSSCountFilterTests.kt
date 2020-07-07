package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSCountFilterTests : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "countFilter"

    // isApplicableMinCount

    fun testPropertyAssignment() { doTest("var '_ = '_?") }

    fun testDotQualifiedFacultativeReceiver() {}

    fun testNoQualifier() { doTest("'_{0,0}::'_") }

    fun testWhenFacultativeArgument() { doTest("when ('_?) {}") }

    fun testFunNoReceiverTypeReference() { doTest("fun '_{0,0}.'_()") }

    fun testDotOptionalReference() { doTest("'_?.'_") }

    // isApplicableMaxCount

    fun testDestructuringDeclarationCount() { doTest("for (('_{3,3}) in '_) { '_* }") }

    fun testWhenTwoEntries() { doTest("when ('_?) { '_{2,2} -> '_ }") }
}