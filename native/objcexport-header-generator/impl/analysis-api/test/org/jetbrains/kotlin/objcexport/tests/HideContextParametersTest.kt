/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertFalse
import org.junit.jupiter.api.Test

class HideContextParametersTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - top level function isn't visible`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Context

                context(Context)
                fun foo() = Unit
            """.trimMargin()
        )
        analyze(file) {
            assertFalse(isVisibleInObjC(file.getFunctionOrFail("foo", this)))
        }
    }

    @Test
    fun `test - member function isn't visible`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Context
                
                class Foo {
                    context(Context)
                    fun bar() = Unit
                }
                
            """.trimMargin()
        )
        analyze(file) {
            assertFalse(
                isVisibleInObjC(
                    file.getClassOrFail("Foo", this).getFunctionOrFail("bar", this)
                )
            )
        }
    }
}