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
            val foo = file.getClassOrFail("Foo")
            assertNull(foo.getSuperClassSymbolNotAny())
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
            val fooSymbol = file.getClassOrFail("Foo")
            val barSymbol = fooSymbol.getSuperClassSymbolNotAny()
            assertEquals(barSymbol, file.getClassOrFail("Bar"))
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
            val aSymbol = file.getClassOrFail("A")
            val bSymbol = file.getClassOrFail("B")
            val cSymbol = file.getClassOrFail("C")

            assertEquals(cSymbol.getSuperClassSymbolNotAny(), bSymbol)
            assertEquals(bSymbol.getSuperClassSymbolNotAny(), aSymbol)
            assertNull(aSymbol.getSuperClassSymbolNotAny())
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
            val aSymbol = file.getClassOrFail("A")
            val i1Symbol = file.getClassOrFail("I1")
            val i2Symbol = file.getClassOrFail("I2")
            val fooSymbol = file.getClassOrFail("Foo")

            assertEquals(fooSymbol.getSuperClassSymbolNotAny(), aSymbol)
            assertNull(i1Symbol.getSuperClassSymbolNotAny())
            assertNull(i2Symbol.getSuperClassSymbolNotAny())
        }
    }
}