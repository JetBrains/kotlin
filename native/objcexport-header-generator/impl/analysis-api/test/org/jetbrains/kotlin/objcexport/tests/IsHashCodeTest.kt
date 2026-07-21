/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.export.utilities.isHashCode
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
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
            val session = useSiteSession
            val anySymbol = findClass(StandardClassIds.Any) ?: error("Missing kotlin.Any")
            val hashCodeSymbol = anySymbol.getFunctionOrFail("hashCode", session)
            assertTrue(session.isHashCode(hashCodeSymbol))
        }
    }

    @Test
    fun `test - data class hashCode`() {
        val file = inlineSourceCodeAnalysis.createKtFile("data class Foo(val x: Int)")
        analyze(file) {
            val session = useSiteSession
            val fooSymbol = session.getClassOrFail(file, "Foo")
            val hashCodeSymbol = fooSymbol.getFunctionOrFail("hashCode", session)
            assertTrue(session.isHashCode(hashCodeSymbol))
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
            val session = useSiteSession
            val hashCodeSymbol = session.getClassOrFail(file, "Foo").getFunctionOrFail("hashCode", session)
            assertTrue(session.isHashCode(hashCodeSymbol))
        }
    }

    @Test
    fun `test - Any equals method is returning false for isHashCode`() {
        val file = inlineSourceCodeAnalysis.createKtFile("")
        analyze(file) {
            val session = useSiteSession
            val anySymbol = findClass(StandardClassIds.Any) ?: error("Missing kotlin.Any")
            val equalsSymbol = anySymbol.getFunctionOrFail("equals", session)
            assertFalse(session.isHashCode(equalsSymbol))
        }
    }
}
