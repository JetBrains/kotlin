/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetSuperClassSymbolNotAnyTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - no declared superclass - returns null`() {
        val file = inlineSourceCodeAnalysis.createKtFile("""class Foo""")
        analyze(file) {
            val foo = getClassOrFail(file, "Foo")
            assertNull(getSuperClassSymbolNotAny(foo))
        }
    }

    @Test
    fun `test - single abstract super class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                abstract class Bar
                class Foo: Bar()
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            val barSymbol = getSuperClassSymbolNotAny(fooSymbol)
            assertEquals(barSymbol, getClassOrFail(file, "Bar"))
        }
    }

    @Test
    fun `test - multiple abstract classes`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                abstract class A
                abstract class B: A()
                class C: B()
            """.trimIndent()
        )

        analyze(file) {
            val aSymbol = getClassOrFail(file, "A")
            val bSymbol = getClassOrFail(file, "B")
            val cSymbol = getClassOrFail(file, "C")

            assertEquals(getSuperClassSymbolNotAny(cSymbol), bSymbol)
            assertEquals(getSuperClassSymbolNotAny(bSymbol), aSymbol)
            assertNull(getSuperClassSymbolNotAny(aSymbol))
        }
    }

    @Test
    fun `test - abstract class and interfaces`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                abstract class A
                interface I1
                interface I2
                class Foo: I1, I2, A()
            """.trimIndent()
        )

        analyze(file) {
            val aSymbol = getClassOrFail(file, "A")
            val i1Symbol = getClassOrFail(file, "I1")
            val i2Symbol = getClassOrFail(file, "I2")
            val fooSymbol = getClassOrFail(file, "Foo")

            assertEquals(getSuperClassSymbolNotAny(fooSymbol), aSymbol)
            assertNull(getSuperClassSymbolNotAny(i1Symbol))
            assertNull(getSuperClassSymbolNotAny(i2Symbol))
        }
    }
}