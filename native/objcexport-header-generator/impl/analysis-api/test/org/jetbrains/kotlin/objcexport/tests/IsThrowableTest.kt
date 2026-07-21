/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.export.utilities.isThrowable
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getPropertyOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsThrowableTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - fake throwable`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Throwable
        """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            assertFalse(session.isThrowable(session.getClassOrFail(file, "Throwable")))
        }
    }

    @Test
    fun `test - true throwable`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            var foo: Throwable? = null
        """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            val isThrowable = session.isThrowable(session.getPropertyOrFail(file, "foo").returnType.expandedSymbol)
            assertTrue(isThrowable)
        }
    }
}
