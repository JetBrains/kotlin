package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSWhenExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "whenExpression"

    fun testWhenVariableSubject() {
        doTest(
            """
            when(b) {
                true -> b = false
                false -> b = true
            }
            """
        )
    }

    fun testWhenExpressionSubject() {
        doTest(
            """
            when(val b = false) {
                true -> println(b)
                false ->  println(b)
            }
            """
        )
    }

    fun testWhenRangeEntry() {
        doTest(
            """
            when(10) {
                in 3..10 -> '_
                else -> '_
            }
            """
        )
    }

    fun testWhenExpressionEntry() {
        doTest(
            """
            when {
                a == b -> return true
                else ->  return false
            }
            """
        )
    }

    fun testWhenIsPatternEntry() {
        doTest(
            """
            when(a) {
                is Int -> return true
                is String -> return true
                else ->  return false
            }
            """
        )
    }

    fun testWhenVarRefEntry() {
        doTest(
            """
            when (i) {
                '_ -> '_
             }
            """.trimIndent()
        )
    }

    fun testWhenAnyEntry() {
        doTest(
            """
            when ('_) {
                '_ -> '_
             }
            """.trimIndent()
        )
    }

    fun testWhenElseEntry() {
        doTest(
            """
            when ('_?) {
                else -> println()
            }
            """
        )
    }
}