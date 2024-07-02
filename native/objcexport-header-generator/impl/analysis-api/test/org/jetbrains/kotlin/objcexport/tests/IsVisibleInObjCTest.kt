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
            val fooSymbol = getFunctionOrFail(file, "foo")
            assertTrue(isVisibleInObjC(fooSymbol))
        }
    }

    @Test
    fun `test - internal function`() {
        val file = inlineSourceCodeAnalysis.createKtFile("internal fun foo() = Unit")
        analyze(file) {
            val fooSymbol = getFunctionOrFail(file, "foo")
            assertFalse(isVisibleInObjC(fooSymbol))
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
            val fooSymbol = getFunctionOrFail(file, "foo")
            assertFalse(isVisibleInObjC(fooSymbol))
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
            val fooSymbol = getFunctionOrFail(file, "foo")
            assertFalse(isVisibleInObjC(fooSymbol))
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
            val fooSymbol = getFunctionOrFail(file, "foo")
            assertTrue(isVisibleInObjC(fooSymbol))
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
            val fooSymbol = getFunctionOrFail(file, "foo")
            assertFalse(isVisibleInObjC(fooSymbol))
        }
    }

    @Test
    fun `test - public class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            assertTrue(isVisibleInObjC(fooSymbol))
        }
    }

    @Test
    fun `test - internal class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("internal class Foo")
        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            assertFalse(isVisibleInObjC(fooSymbol))
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
            val fooSymbol = getClassOrFail(file, "Foo")
            assertTrue(isVisibleInObjC(fooSymbol))
        }
    }

    @Test
    fun `test - inline class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("inline class Foo(val x: Int)")
        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            assertFalse(isVisibleInObjC(fooSymbol))
        }
    }

    @Test
    fun `test - expect class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("expect class Foo")
        analyze(file) {
            val fooSymbol = getClassOrFail(file, "Foo")
            assertFalse(isVisibleInObjC(fooSymbol))
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
            val fooSymbol = getClassOrFail(file, "Foo")
            assertFalse(isVisibleInObjC(fooSymbol))
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
            val fooSymbol = getClassOrFail(file, "Foo")
            assertFalse(isVisibleInObjC(fooSymbol))
        }
    }

    @Test
    fun `test - containing symbol visible`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class PublicClass {
                    fun foo()
                }
            """.trimIndent()
        )

        analyze(file) {
            assertTrue(isVisibleInObjC(getClassOrFail(file, "PublicClass").getFunctionOrFail("foo", this)))
        }
    }

    @Test
    fun `test - invisible symbol inside private class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                private class PrivateClass {
                    fun foo()
                }
            """.trimIndent()
        )

        analyze(file) {
            assertFalse(isVisibleInObjC(getClassOrFail(file, "PrivateClass").getFunctionOrFail("foo", this)))
        }
    }

    @Test
    fun `test - nested visible function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class PublicA {
                class PublicB {
                    class PublicC {
                        fun foo() {}
                    }   
                }
            }
            """.trimIndent()
        )

        analyze(file) {
            val foo = file
                .getClassOrFail("PublicA", this).memberScope
                .getClassOrFail("PublicB").memberScope
                .getClassOrFail("PublicC")
                .getFunctionOrFail("foo", this)
            assertTrue(isVisibleInObjC(foo))
        }
    }

    @Test
    fun `test - nested invisible function inside private class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class PublicA {
                private class PrivateB {
                    class PublicC {
                        fun foo() {}
                    }   
                }
            }
            """.trimIndent()
        )

        analyze(file) {
            val foo = file
                .getClassOrFail("PublicA", this).memberScope
                .getClassOrFail("PrivateB").memberScope
                .getClassOrFail("PublicC")
                .getFunctionOrFail("foo", this)
            assertFalse(isVisibleInObjC(foo))
        }
    }

    @Test
    fun `test - invisible classes with @HidesFromObjC and visible members`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            @kotlin.native.HidesFromObjC
            annotation class HideIt                


            class PublicA {
                fun publicA() = Unit
            
                @HideIt
                class HiddenB {
                    fun publicB() = Unit

                    class HiddenC {
                        fun publicC() = Unit
                    }   
                }
            }
            """.trimIndent()
        )

        analyze(file) {
            val publicA = getClassOrFail(file, "PublicA")
            val hiddenB = publicA.memberScope.getClassOrFail("HiddenB")
            val hiddenC = hiddenB.memberScope.getClassOrFail("HiddenC")

            assertFalse(isVisibleInObjC(hiddenB))
            assertFalse(isVisibleInObjC(hiddenC))

            assertTrue(isVisibleInObjC(publicA.getFunctionOrFail("publicA", this)))
            assertTrue(isVisibleInObjC(hiddenB.getFunctionOrFail("publicB", this)))
            assertTrue(isVisibleInObjC(hiddenC.getFunctionOrFail("publicC", this)))
        }
    }
}
