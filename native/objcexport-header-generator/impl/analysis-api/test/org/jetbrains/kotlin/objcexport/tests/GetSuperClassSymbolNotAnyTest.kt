/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.export.utilities.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
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
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo")
            assertNull(session.getSuperClassSymbolNotAny(foo))
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
            val session = useSiteSession
            val fooSymbol = session.getClassOrFail(file, "Foo")
            val barSymbol = session.getSuperClassSymbolNotAny(fooSymbol)
            assertEquals(barSymbol, session.getClassOrFail(file, "Bar"))
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
            val session = useSiteSession
            val aSymbol = session.getClassOrFail(file, "A")
            val bSymbol = session.getClassOrFail(file, "B")
            val cSymbol = session.getClassOrFail(file, "C")

            assertEquals(session.getSuperClassSymbolNotAny(cSymbol), bSymbol)
            assertEquals(session.getSuperClassSymbolNotAny(bSymbol), aSymbol)
            assertNull(session.getSuperClassSymbolNotAny(aSymbol))
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
            val session = useSiteSession
            val aSymbol = session.getClassOrFail(file, "A")
            val i1Symbol = session.getClassOrFail(file, "I1")
            val i2Symbol = session.getClassOrFail(file, "I2")
            val fooSymbol = session.getClassOrFail(file, "Foo")

            assertEquals(session.getSuperClassSymbolNotAny(fooSymbol), aSymbol)
            assertNull(session.getSuperClassSymbolNotAny(i1Symbol))
            assertNull(session.getSuperClassSymbolNotAny(i2Symbol))
        }
    }
}
