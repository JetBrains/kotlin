/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

import kotlin.test.Test
import kotlin.test.assertEquals

interface ExplanationBuilder {
    fun value(subSource: String, display: Int, value: Any?)
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
    fun testComplexExpression() {
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
    fun testMultiLineExpression() {
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
}
