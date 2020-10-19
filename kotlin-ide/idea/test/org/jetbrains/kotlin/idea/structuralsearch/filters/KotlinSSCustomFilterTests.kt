package org.jetbrains.kotlin.idea.structuralsearch.filters

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest
import org.jetbrains.kotlin.idea.structuralsearch.filters.OneStateFilter
import org.jetbrains.kotlin.idea.structuralsearch.filters.ValOnlyFilter
import org.jetbrains.kotlin.idea.structuralsearch.filters.VarOnlyFilter

class KotlinSSCustomFilterTests: KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "customFilter"

    private val enabled = OneStateFilter.ENABLED

    fun testVarOnlyFilter() { doTest("var '_:[_${VarOnlyFilter.CONSTRAINT_NAME}($enabled)]") }

    fun testValOnlyFilter() { doTest("val '_:[_${ValOnlyFilter.CONSTRAINT_NAME}($enabled)]") }

}