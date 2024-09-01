/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isHashCode
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
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
            val hashCodeSymbol = anySymbol.getFunctionOrFail("hashCode", this)
            assertTrue(isHashCode(hashCodeSymbol))
        }
    }

    @Test
    fun `test - data class hashCode`() {
        val file = inlineSourceCodeAnalysis.createKtFile("data class Foo(val x: Int)")
        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            val hashCodeSymbol = fooSymbol.getFunctionOrFail("hashCode", this)
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
            val hashCodeSymbol = getClassOrFail(file, "Foo").getFunctionOrFail("hashCode", this)
            assertTrue(isHashCode(hashCodeSymbol))
        }
    }

    @Test
    fun `test - Any equals method is returning false for isHashCode`() {
        val file = inlineSourceCodeAnalysis.createKtFile("")
        analyze(file) {
            val anySymbol = findClass(StandardClassIds.Any) ?: error("Missing kotlin.Any")
            val equalsSymbol = anySymbol.getFunctionOrFail("equals", this)
            assertFalse(isHashCode(equalsSymbol))
        }
    }
}