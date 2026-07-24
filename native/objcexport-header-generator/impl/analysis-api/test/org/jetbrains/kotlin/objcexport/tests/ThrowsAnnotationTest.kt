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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefinedThrows
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getEffectiveThrows
import org.jetbrains.kotlin.objcexport.analysisApiUtils.hasThrowsAnnotation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThrowsAnnotationTest(private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {

    @Test
    fun `test - has throws annotation`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @Throws
                fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            assertTrue(session.getFunctionOrFail(file, "foo").hasThrowsAnnotation)
        }
    }

    @Test
    fun `test - override has no throws annotation`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                open class A {
                  @Throws
                  open fun foo() = Unit
                }
                class B: A() {
                  override fun foo() = Unit
                }
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            val classA = session.getClassOrFail(file, "A")
            val classB = session.getClassOrFail(file, "B")

            assertTrue(session.getFunctionOrFail(classA, "foo").hasThrowsAnnotation)
            assertFalse(session.getFunctionOrFail(classB, "foo").hasThrowsAnnotation)
        }
    }

    @Test
    fun `test - defined function throws`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @Throws(IllegalStateException::class, RuntimeException::class)
                fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            val foo = session.getFunctionOrFail(file, "foo")
            assertEquals(listOf("IllegalStateException", "RuntimeException"), session.getDefinedThrows(foo).mapName())
        }
    }

    @Test
    fun `test - effective and defined classes throws`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            abstract class A {
                @Throws(IllegalStateException::class)
                abstract fun foo()
            }
            
            class B : A() {
                @Throws(RuntimeException::class)
                override fun foo() = Unit
            }
            class C : B() {
                @Throws(IndexOutOfBoundsException::class)
                override fun foo() = Unit
            }
            """.trimIndent()
        )

        analyze(file) {

            val session = useSiteSession
            val classA = session.getClassOrFail(file, "A")
            val fooA = getFunctionOrFail(classA.memberScope, "foo")
            assertEquals(listOf("IllegalStateException"), session.getEffectiveThrows(fooA).mapName())
            assertEquals(listOf("IllegalStateException"), session.getDefinedThrows(fooA).mapName())

            val classB = session.getClassOrFail(file, "B")
            val fooB = getFunctionOrFail(classB.memberScope, "foo")
            assertEquals(listOf("IllegalStateException"), session.getEffectiveThrows(fooB).mapName())
            assertEquals(listOf("RuntimeException"), session.getDefinedThrows(fooB).mapName())

            val classC = session.getClassOrFail(file, "C")
            val fooC = getFunctionOrFail(classC.memberScope, "foo")
            assertEquals(listOf("IllegalStateException"), session.getEffectiveThrows(fooC).mapName())
            assertEquals(listOf("IndexOutOfBoundsException"), session.getDefinedThrows(fooC).mapName())
        }
    }

    @Test
    fun `test - constructor throws`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class Foo @Throws(IllegalStateException::class) constructor()
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo").memberScope.constructors.first()
            assertEquals(listOf("IllegalStateException"), session.getEffectiveThrows(foo).mapName())
            assertEquals(listOf("IllegalStateException"), session.getDefinedThrows(foo).mapName())
        }
    }

    @Test
    fun `test - non throws annotation`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            annotation class NonThrows(vararg val exceptionClasses: KClass<out Throwable>)

            @Throws(RuntimeException::class)
            @NonThrows(IllegalStateException::class)
            fun foo() = Unit
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            val foo = session.getFunctionOrFail(file, "foo")
            assertEquals(listOf("RuntimeException"), session.getDefinedThrows(foo).mapName())
        }
    }
}

private fun List<ClassId>.mapName(): List<String> {
    return map { it.shortClassName.asString() }
}
