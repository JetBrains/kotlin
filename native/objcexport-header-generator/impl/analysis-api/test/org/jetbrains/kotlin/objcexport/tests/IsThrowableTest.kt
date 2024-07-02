/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isThrowable
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getPropertyOrFail
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
            assertFalse(isThrowable(getClassOrFail(file, "Throwable")))
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
            val isThrowable = isThrowable(getPropertyOrFail(file, "foo").returnType.expandedSymbol)
            assertTrue(isThrowable)
        }
    }
}