/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDeclaredSuperInterfaceSymbols
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
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
            val foo = file.getClassOrFail("Foo")

            assertEquals(
                listOf(file.getClassOrFail("X"), file.getClassOrFail("Y")),
                foo.getDeclaredSuperInterfaceSymbols()
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
            assertEquals(
                listOf(file.getClassOrFail("A"), file.getClassOrFail("B")),
                file.getClassOrFail("Foo").getDeclaredSuperInterfaceSymbols()
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
            assertEquals(
                listOf(file.getClassOrFail("A"), file.getClassOrFail("B")),
                file.getClassOrFail("Foo").getDeclaredSuperInterfaceSymbols()
            )
        }
    }
}