package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

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
                in 3..10 -> println("In range")
                else ->  println("Not in Range.")
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
                '_ -> println("one")
             }
            """.trimIndent()
        )
    }

    fun testWhenAnyEntry() {
        doTest(
            """
            when ('_) {
                '_ -> println("ok")
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