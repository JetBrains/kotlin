package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTypeAliasTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "typeAlias"

    fun testTypeAlias() { doTest("typealias '_ = Int") }

    fun testAnnotated() { doTest("@Ann typealias '_ = '_") }
}