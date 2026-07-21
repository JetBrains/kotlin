/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.export.utilities.getDeclaredSuperInterfaceSymbols
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetSuperInterfacesTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - transitive super interfaces`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            interface A
            interface B 
            interface C

            interface X: A, B
            interface Y : C

            class Foo: X, Y
        """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo")

            assertEquals(
                listOf(session.getClassOrFail(file, "X"), session.getClassOrFail(file, "Y")),
                session.getDeclaredSuperInterfaceSymbols(foo)
            )
        }
    }

    @Test
    fun `test - super interface and super class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                interface A
                interface B
                abstract class X
                class Foo: X(), A, B
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            assertEquals(
                listOf(session.getClassOrFail(file, "A"), session.getClassOrFail(file, "B")),
                session.getDeclaredSuperInterfaceSymbols(session.getClassOrFail(file, "Foo"))
            )
        }
    }

    @Test
    fun `test - subclassing Any explicitly`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                interface A
                interface B
                class Foo: Any(), A, B
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            assertEquals(
                listOf(session.getClassOrFail(file, "A"), session.getClassOrFail(file, "B")),
                session.getDeclaredSuperInterfaceSymbols(session.getClassOrFail(file, "Foo"))
            )
        }
    }
}
