/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCBaseCallable
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
            val session = useSiteSession
            val fooSymbol = session.getFunctionOrFail(file, "foo")
            assertTrue(session.isObjCBaseCallable(fooSymbol))
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
            val session = useSiteSession
            val fooSymbol = session.getClassOrFail(file, "Foo")
            val xSymbol = fooSymbol.memberScope.getFunctionOrFail("x")
            assertFalse(session.isObjCBaseCallable(xSymbol))
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
            val session = useSiteSession
            val fooSymbol = session.getClassOrFail(file, "Foo")
            val xSymbol = fooSymbol.memberScope.getFunctionOrFail("x")
            assertTrue(session.isObjCBaseCallable(xSymbol))
        }
    }
}
