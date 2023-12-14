/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsVisibleInObjCTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - simple public function`() {
        val file = inlineSourceCodeAnalysis.createKtFile("fun foo() = Unit")
        analyze(file) {
            val fooSymbol = file.getFunctionOrFail("foo")
            assertTrue(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - internal function`() {
        val file = inlineSourceCodeAnalysis.createKtFile("internal fun foo() = Unit")
        analyze(file) {
            val fooSymbol = file.getFunctionOrFail("foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - HiddenFromObjC function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @kotlin.native.HiddenFromObjC
                fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = file.getFunctionOrFail("foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - custom HidesFromObjC annotation function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @kotlin.native.HidesFromObjC
                annotation class MyInternalApi
                
                @MyInternalApi
                fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = file.getFunctionOrFail("foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - deprecation warning function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @Deprecated(level = DeprecationLevel.WARNING)
                fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = file.getFunctionOrFail("foo")
            assertTrue(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - deprecation hidden function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @Deprecated(level = DeprecationLevel.HIDDEN)
                fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = file.getFunctionOrFail("foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - public class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(file) {
            val fooSymbol = file.getClassOrFail("Foo")
            assertTrue(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - internal class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("internal class Foo")
        analyze(file) {
            val fooSymbol = file.getClassOrFail("Foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - PublishedApi class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @PublishedApi
                internal class Foo
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = file.getClassOrFail("Foo")
            assertTrue(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - inline class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("inline class Foo(val x: Int)")
        analyze(file) {
            val fooSymbol = file.getClassOrFail("Foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - expect class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("expect class Foo")
        analyze(file) {
            val fooSymbol = file.getClassOrFail("Foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - deprecation hidden class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @Deprecated(level = DeprecationLevel.HIDDEN)
                class Foo
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = file.getClassOrFail("Foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }

    @Test
    fun `test - custom HidesFromObjC annotation class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @kotlin.native.HidesFromObjC
                annotation class MyInternalApi
                
                @MyInternalApi
                class Foo
            """.trimIndent()
        )

        analyze(file) {
            val fooSymbol = file.getClassOrFail("Foo")
            assertFalse(fooSymbol.isVisibleInObjC())
        }
    }
}