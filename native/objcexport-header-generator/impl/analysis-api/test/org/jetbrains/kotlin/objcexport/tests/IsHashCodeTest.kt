/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.export.utilities.isHashCode
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.name.StandardClassIds
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsHashCodeTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - Any - hashCode`() {
        val file = inlineSourceCodeAnalysis.createKtFile("")
        analyze(file) {
            val anySymbol = findClass(StandardClassIds.Any) ?: error("Missing kotlin.Any")
            val hashCodeSymbol = getFunctionOrFail(anySymbol, "hashCode")
            assertTrue(isHashCode(hashCodeSymbol))
        }
    }

    @Test
    fun `test - data class hashCode`() {
        val file = inlineSourceCodeAnalysis.createKtFile("data class Foo(val x: Int)")
        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            val hashCodeSymbol = getFunctionOrFail(fooSymbol, "hashCode")
            assertTrue(isHashCode(hashCodeSymbol))
        }
    }

    @Test
    fun `test - overridden hashCode`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Foo {
                override fun hashCode() = 42
            }
            """.trimIndent()
        )

        analyze(file) {
            val hashCodeSymbol = getFunctionOrFail(getClassOrFail(file, "Foo"), "hashCode")
            assertTrue(isHashCode(hashCodeSymbol))
        }
    }

    @Test
    fun `test - Any equals method is returning false for isHashCode`() {
        val file = inlineSourceCodeAnalysis.createKtFile("")
        analyze(file) {
            val anySymbol = findClass(StandardClassIds.Any) ?: error("Missing kotlin.Any")
            val equalsSymbol = getFunctionOrFail(anySymbol, "equals")
            assertFalse(isHashCode(equalsSymbol))
        }
    }
}
