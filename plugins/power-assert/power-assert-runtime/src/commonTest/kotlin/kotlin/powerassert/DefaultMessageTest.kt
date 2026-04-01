/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.powerassert

import kotlin.test.Test
import kotlin.test.assertEquals

interface ExplanationBuilder {
    fun value(subSource: String, display: Int = 0, value: Any?)
}

@ExperimentalPowerAssert
fun explanation(source: String, builder: ExplanationBuilder.() -> Unit): Explanation {
    val expressions = mutableListOf<Expression>()
    object : ExplanationBuilder {
        override fun value(subSource: String, display: Int, value: Any?) {
            val startOffset = source.indexOf(subSource)
            expressions.add(ValueExpression(startOffset, startOffset + subSource.length, startOffset + display, value))
        }
    }.builder()
    return SimpleExplanation(0, source, expressions)
}

@ExperimentalPowerAssert
class DefaultMessageTest {
    @Test
    fun testSimpleExplanation() {
        val explanation = explanation(
            source = "assert(1 == 2)"
        ) {
            value("1 == 2", display = 2, value = false)
        }
        assertEquals(
            actual = explanation.toDefaultMessage().trim(),
            expected = """
                assert(1 == 2)
                         |
                         false
            """.trimIndent(),
        )
    }

    @Test
    fun testComplexExplanation() {
        val explanation = explanation(
            source = "assert(1 == 1 && 2 == 3)"
        ) {
            value("1 == 1", display = 2, value = true)
            value("2 == 3", display = 2, value = false)
        }
        assertEquals(
            actual = explanation.toDefaultMessage().trim(),
            expected = """
                assert(1 == 1 && 2 == 3)
                         |         |
                         true      false
            """.trimIndent(),
        )
    }

    @Test
    fun testMultiLineExplanation() {
        val explanation = explanation(
            source = """
                assert(
                    1 == 1 &&
                            2 == 3
                )
            """.trimIndent()
        ) {
            value("1 == 1", display = 2, value = true)
            value("2 == 3", display = 2, value = false)
        }
        assertEquals(
            actual = explanation.toDefaultMessage().trim(),
            expected = """
                assert(
                    1 == 1 &&
                      |
                      true

                            2 == 3
                              |
                              false

                )
            """.trimIndent(),
        )
    }

    @Test
    fun testMultiLineExpressionValue() {
        val explanation = explanation(
            source = """
                assert(str.substring(0, 8).length == 0)
            """.trimIndent()
        ) {
            val str = """
                This
                 Is
                  A
                   Long
                  Multiple
                 Line
                String
            """.trimIndent()
            value("str", value = str)
            value("str.substring(0, 8)", display = 4, value = str.substring(0, 8))
            value("str.substring(0, 8).length", display = 20, value = str.substring(0, 8).length)
            value("str.substring(0, 8).length == 0", display = 27, value = false)
        }
        assertEquals(
            actual = explanation.toDefaultMessage().trim(),
            expected = """
                assert(str.substring(0, 8).length == 0)
                       |   |               |      |
                       |   |               8      false
                       |   ""${'"'}
                       |   This
                       |    Is
                       |   ""${'"'}
                       ""${'"'}
                       This
                        Is
                         A
                          Long
                         Multiple
                        Line
                       String
                       ""${'"'}
            """.trimIndent(),
        )
    }

    @Test
    fun testCharExpressionValue() {
        val explanation = explanation(
            source = """
                assert(c == 'x')
            """.trimIndent()
        ) {
            value("c", value = 'a')
        }
        assertEquals(
            actual = explanation.toDefaultMessage().trim(),
            expected = """
                assert(c == 'x')
                       |
                       'a'
            """.trimIndent(),
        )
    }

    @Test
    fun testStringExpressionValue() {
        val explanation = explanation(
            source = """
                assert(str == "world")
            """.trimIndent()
        ) {
            value("str", value = "hello")
        }
        assertEquals(
            actual = explanation.toDefaultMessage().trim(),
            expected = """
                assert(str == "world")
                       |
                       "hello"
            """.trimIndent(),
        )
    }
}
