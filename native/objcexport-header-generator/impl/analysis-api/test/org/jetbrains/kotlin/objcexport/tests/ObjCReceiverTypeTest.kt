/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.export.test.getPropertyOrFail
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.getObjCReceiverType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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

            val session = useSiteSession
            val outerClass = session.getClassOrFail(file, "Outer")
            val innerClass = outerClass.memberScope.getClassOrFail("Inner")
            val innerClassConstructor = innerClass.memberScope.constructors.first()

            assertEquals(
                session.getObjCReceiverType(innerClassConstructor)?.expandedSymbol?.classId,
                outerClass.classId
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

            val session = useSiteSession
            val outerClass = session.getClassOrFail(file, "Outer")
            val innerClass = outerClass.memberScope.getClassOrFail("Inner")
            val foo = innerClass.memberScope.getFunctionOrFail("foo")

            assertEquals(
                ClassId.topLevel(StandardNames.FqNames.string.toSafe()),
                session.getObjCReceiverType(foo)?.expandedSymbol?.classId
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

            val session = useSiteSession
            val outerClass = session.getClassOrFail(file, "Outer")
            val innerClass = outerClass.memberScope.getClassOrFail("Inner")
            val getter = innerClass.memberScope.getPropertyOrFail("prop").getter

            assertEquals(
                ClassId.topLevel(StandardNames.FqNames._boolean.toSafe()),
                session.getObjCReceiverType(getter)?.expandedSymbol?.classId
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

            val session = useSiteSession
            val outerClass = session.getClassOrFail(file, "Outer")
            val innerClass = outerClass.memberScope.getClassOrFail("Inner")
            val setter = innerClass.memberScope.getPropertyOrFail("prop").setter

            assertEquals(
                ClassId.topLevel(StandardNames.FqNames.string.toSafe()),
                session.getObjCReceiverType(setter)?.expandedSymbol?.classId
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

            val session = useSiteSession
            val fooClass = session.getClassOrFail(file, "Foo")
            val foo = fooClass.memberScope.getFunctionOrFail("foo")
            val setter = fooClass.memberScope.getPropertyOrFail("prop").setter
            val getter = fooClass.memberScope.getPropertyOrFail("prop").getter

            assertEquals(buildClassType(StandardClassIds.Boolean), session.getObjCReceiverType(foo))
            assertEquals(buildClassType(StandardClassIds.String), session.getObjCReceiverType(setter))
            assertEquals(buildClassType(StandardClassIds.String), session.getObjCReceiverType(getter))
        }
    }
}
