/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCBaseCallable
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsObjCBaseCallableTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - top level function`() {
        val file = inlineSourceCodeAnalysis.createKtFile("fun foo() = Unit")
        analyze(file) {
            val fooSymbol = getFunctionOrFail(file, "foo")
            assertTrue(isObjCBaseCallable(fooSymbol))
        }
    }

    @Test
    fun `test - function overriding abstract function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                abstract class Bar {
                    abstract fun x()
                }
                
                class Foo : Bar() {
                    override fun x() = Unit
                }

            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            val xSymbol = fooSymbol.memberScope.getFunctionOrFail("x")
            assertFalse(isObjCBaseCallable(xSymbol))
        }
    }

    @Test
    fun `test - function overriding private interface function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                private interface I {
                    fun x()
                }
                class Foo: I {
                    override fun x() = Unit
                }
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            val xSymbol = fooSymbol.memberScope.getFunctionOrFail("x")
            assertTrue(isObjCBaseCallable(xSymbol))
        }
    }
}
