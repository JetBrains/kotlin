package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTryCatchExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "tryCatchExpression"

    fun testTryCatch() {
        doTest(
            """
            try {
                println(0)
            } catch (e: Exception) {
                println(1)
            }
            """
        )
    }

    fun testTryFinally() {
        doTest(
            """
            try {
                println(0)
            } finally {
                println(1)
            }
            """
        )
    }
}