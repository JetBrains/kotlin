/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.objCReceiverType
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getPropertyOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObjCReceiverTypeTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - inner constructor`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Outer {
                    inner class Inner
                }
            """.trimIndent()
        )

        analyze(file) {

            val outerClass = file.getClassOrFail("Outer")
            val innerClass = outerClass.getMemberScope().getClassOrFail("Inner")
            val innerClassConstructor = innerClass.getMemberScope().getConstructors().first()

            assertEquals(
                innerClassConstructor.objCReceiverType?.expandedClassSymbol?.classIdIfNonLocal,
                outerClass.classIdIfNonLocal
            )
        }
    }

    @Test
    fun `test - inner function`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Outer {
                    inner class Inner {
                        fun String.foo() = Unit
                    }
                }
            """.trimIndent()
        )

        analyze(file) {

            val outerClass = file.getClassOrFail("Outer")
            val innerClass = outerClass.getMemberScope().getClassOrFail("Inner")
            val foo = innerClass.getMemberScope().getFunctionOrFail("foo")

            assertEquals(
                ClassId.topLevel(StandardNames.FqNames.string.toSafe()),
                foo.objCReceiverType?.expandedClassSymbol?.classIdIfNonLocal
            )
        }
    }

    @Test
    fun `test - inner getter`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Outer {
                    inner class Inner {
                        val Boolean.prop: Boolean
                            get() = false
                    }
                }
            """.trimIndent()
        )

        analyze(file) {

            val outerClass = file.getClassOrFail("Outer")
            val innerClass = outerClass.getMemberScope().getClassOrFail("Inner")
            val getter = innerClass.getMemberScope().getPropertyOrFail("prop").getter

            assertEquals(
                ClassId.topLevel(StandardNames.FqNames._boolean.toSafe()),
                getter?.objCReceiverType?.expandedClassSymbol?.classIdIfNonLocal
            )
        }
    }

    @Test
    fun `test - inner setter`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Outer {
                    inner class Inner {
                        var String.prop: Int
                            get() = 42
                            set(value) {}
                    }
                }
            """.trimIndent()
        )

        analyze(file) {

            val outerClass = file.getClassOrFail("Outer")
            val innerClass = outerClass.getMemberScope().getClassOrFail("Inner")
            val setter = innerClass.getMemberScope().getPropertyOrFail("prop").setter

            assertEquals(
                ClassId.topLevel(StandardNames.FqNames.string.toSafe()),
                setter?.objCReceiverType?.expandedClassSymbol?.classIdIfNonLocal
            )
        }
    }

    @Test
    fun `test - regular extensions`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Foo {
                
                    fun Boolean.foo() = Unit
                
                    var String.prop: Int
                        get() = 42
                        set(value) {}
                }
            """.trimIndent()
        )

        analyze(file) {

            val fooClass = file.getClassOrFail("Foo")
            val foo = fooClass.getMemberScope().getFunctionOrFail("foo")
            val setter = fooClass.getMemberScope().getPropertyOrFail("prop").setter
            val getter = fooClass.getMemberScope().getPropertyOrFail("prop").getter

            assertNull(foo.objCReceiverType)
            assertNull(setter?.objCReceiverType)
            assertNull(getter?.objCReceiverType)
        }
    }
}