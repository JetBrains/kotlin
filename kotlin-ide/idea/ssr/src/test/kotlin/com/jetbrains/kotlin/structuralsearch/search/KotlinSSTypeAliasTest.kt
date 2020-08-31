package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTypeAliasTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "typeAlias"

    fun testTypeAlias() { doTest("typealias '_ = Int") }

    fun testAnnotated() { doTest("@Ann typealias '_ = '_") }
}