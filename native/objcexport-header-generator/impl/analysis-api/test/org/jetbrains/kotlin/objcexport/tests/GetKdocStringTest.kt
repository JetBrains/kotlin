/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getKDocString
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetKdocStringTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - simple class`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile(
            """
            /**
            * Kdoc for 'Foo'
            */
            class Foo
        """.trimIndent()
        )

        analyze(ktFile) {
            val foo = ktFile.getClassOrFail("Foo")
            assertEquals(
                """
                    /**
                    * Kdoc for 'Foo'
                    */
                """.trimIndent(),
                foo.getKDocString()
            )
        }
    }

    @Test
    fun `test - simple function`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile(
            """
            /**
            * Kdoc for 'foo'
            */
            fun foo() = Unit
        """.trimIndent()
        )

        analyze(ktFile) {
            val foo = ktFile.getFunctionOrFail("foo")
            assertEquals(
                """
                    /**
                    * Kdoc for 'foo'
                    */
                """.trimIndent(),
                foo.getKDocString()
            )
        }
    }
}