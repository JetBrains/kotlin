package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSObjectExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "objectExpression"

    fun testObject() {
        doTest(
            """
                fun '_() = object {
                    val c = 1
                }
            """
        )
    }

    fun testObjectAnyReturn() {
        doTest(
            """
                fun '_(): Any = object {
                    val c = 1
                }
            """
        )
    }

    fun testObjectAnonymous() {
        doTest(
            """
                private fun '_() = object {
                    val c = 1
                }
            """
        )
    }

    fun testObjectSuperType() {
        doTest(
            """
                fun '_() = object : MouseAdapter() { }
            """
        )
    }
}