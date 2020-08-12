package com.jetbrains.kotlin.structuralsearch.res.filters

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest
import com.jetbrains.kotlin.structuralsearch.filters.OneStateFilter
import com.jetbrains.kotlin.structuralsearch.filters.ValOnlyFilter
import com.jetbrains.kotlin.structuralsearch.filters.VarOnlyFilter

class KotlinSSCustomFilterTests: KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "customFilter"
    
    private val enabled = OneStateFilter.ENABLED

    fun testVarOnlyFilter() { doTest("var '_:[_${VarOnlyFilter.CONSTRAINT_NAME}($enabled)]") }

    fun testValOnlyFilter() { doTest("val '_:[_${ValOnlyFilter.CONSTRAINT_NAME}($enabled)]") }

}