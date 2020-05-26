package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTypeAliasTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "typeAlias"

    fun testTypeAlias() { doTest("typealias '_ = Int") }
}