/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.classNamed
import org.jetbrains.kotlin.sir.providers.support.functionsNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.util.name
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OperatorFunctionsTranslationTests : SirTranslationTest() {
    @Test
    fun `binary operator functions translation`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Vector(val x: Int, val y: Int) {
                    operator fun plus(other: Vector): Vector = Vector(x + other.x, y + other.y)
                    operator fun minus(other: Vector): Vector = Vector(x - other.x, y - other.y)
                    operator fun times(scalar: Int): Vector = Vector(x * scalar, y * scalar)
                    operator fun div(scalar: Int): Vector = Vector(x / scalar, y / scalar)
                }
            """.trimIndent()
        )
        translate(file) { declarations ->
            val vectorClass = declarations.classNamed("Vector")

            // Check plus operator
            val plusFunction = vectorClass.declarations.functionsNamed("plus").singleOrNull()
            assertNotNull(plusFunction, "plus function should be exported")
            assertEquals(1, plusFunction.parameters.size)
            assertEquals("other", plusFunction.parameters[0].name)

            // Check minus operator
            val minusFunction = vectorClass.declarations.functionsNamed("minus").singleOrNull()
            assertNotNull(minusFunction, "minus function should be exported")
            assertEquals(1, minusFunction.parameters.size)
            assertEquals("other", minusFunction.parameters[0].name)

            // Check times operator
            val timesFunction = vectorClass.declarations.functionsNamed("times").singleOrNull()
            assertNotNull(timesFunction, "times function should be exported")
            assertEquals(1, timesFunction.parameters.size)
            assertEquals("scalar", timesFunction.parameters[0].name)

            // Check div operator
            val divFunction = vectorClass.declarations.functionsNamed("div").singleOrNull()
            assertNotNull(divFunction, "div function should be exported")
            assertEquals(1, divFunction.parameters.size)
            assertEquals("scalar", divFunction.parameters[0].name)
        }
    }

    @Test
    fun `unary operator functions translation`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Vector(val x: Int, val y: Int) {
                    operator fun unaryMinus(): Vector = Vector(-x, -y)
                }
            """.trimIndent()
        )
        translate(file) { declarations ->
            val vectorClass = declarations.classNamed("Vector")

            // Check unaryMinus operator
            val unaryMinusFunction = vectorClass.declarations.functionsNamed("unaryMinus").singleOrNull()
            assertNotNull(unaryMinusFunction, "unaryMinus function should be exported")
            assertEquals(0, unaryMinusFunction.parameters.size)
        }
    }

    @Test
    fun `comparison operator functions translation`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Vector(val x: Int, val y: Int) {
                    operator fun compareTo(other: Vector): Int = (x * x + y * y).compareTo(other.x * other.x + other.y * other.y)
                }
            """.trimIndent()
        )
        translate(file) { declarations ->
            val vectorClass = declarations.classNamed("Vector")

            // Check compareTo operator
            val compareToFunction = vectorClass.declarations.functionsNamed("compareTo").singleOrNull()
            assertNotNull(compareToFunction, "compareTo function should be exported")
            assertEquals(1, compareToFunction.parameters.size)
            assertEquals("other", compareToFunction.parameters[0].name)
        }
    }

    @Test
    fun `indexing operator functions translation`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Vector(val x: Int, val y: Int) {
                    operator fun get(index: Int): Int = when(index) {
                        0 -> x
                        1 -> y
                        else -> throw IndexOutOfBoundsException("Invalid index")
                    }
                }
            """.trimIndent()
        )
        translate(file) { declarations ->
            val vectorClass = declarations.classNamed("Vector")

            // Check get operator
            val getFunction = vectorClass.declarations.functionsNamed("get").singleOrNull()
            assertNotNull(getFunction, "get function should be exported")
            assertEquals(1, getFunction.parameters.size)
            assertEquals("index", getFunction.parameters[0].name)
        }
    }
}
