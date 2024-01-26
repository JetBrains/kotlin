/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getAllClassOrObjectSymbols
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetAllClassOrObjectSymbolsTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - no classifiers in file`() {
        val file = inlineSourceCodeAnalysis.createKtFile("val foo = 42")
        analyze(file) {
            assertEquals(emptyList(), file.getFileSymbol().getAllClassOrObjectSymbols())
        }
    }

    @Test
    fun `test - single class in file`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(file) {
            assertEquals(listOf(file.getClassOrFail("Foo")), file.getFileSymbol().getAllClassOrObjectSymbols())
        }
    }

    @Test
    fun `test - multiple nested classes in file`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class A {
                    class B {
                        class C
                    }
                }
                    
                class D {
                    class E
                }
            """.trimIndent()
        )

        analyze(file) {
            assertEquals(
                listOf(
                    file.getClassOrFail("A"),
                    file.getClassOrFail("A").getMemberScope().getClassOrFail("B"),
                    file.getClassOrFail("A").getMemberScope().getClassOrFail("B").getMemberScope().getClassOrFail("C"),

                    file.getClassOrFail("D"),
                    file.getClassOrFail("D").getMemberScope().getClassOrFail("E")
                ),
                file.getFileSymbol().getAllClassOrObjectSymbols()
            )
        }
    }
}