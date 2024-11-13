/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getAllContainingDeclarations
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAllContainingDeclarationsTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - containing declarations`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            interface A {
                interface B {
                    interface C
                }
            }
        """.trimIndent()
        )

        analyze(file) {
            val a = getClassOrFail(file, "A")
            val b = getClassOrFail(a.staticMemberScope, "B")
            val c = getClassOrFail(b.staticMemberScope, "C")
            assertTrue(getAllContainingDeclarations(a).isEmpty())
            assertEquals(listOf("A"), getAllContainingDeclarations(b).map { it.name?.asString() })
            assertEquals(listOf("B", "A"), getAllContainingDeclarations(c).map { it.name?.asString() })
        }
    }
}