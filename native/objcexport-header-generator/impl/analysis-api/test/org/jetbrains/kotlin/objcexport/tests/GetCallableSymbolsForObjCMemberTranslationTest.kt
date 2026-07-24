/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.objcexport.getCallableSymbolsForObjCMemberTranslation
import org.jetbrains.kotlin.objcexport.getStableCallableOrder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetCallableSymbolsForObjCMemberTranslationTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - regular class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                abstract class Base {
                    open fun base() = Unit
                    abstract fun abstractFun()
                }
                class Foo: Base() {
                    fun bar() {}
                    override fun abstractFun(): Unit = error("stub")
                }
            """.trimIndent()
        )
        analyze(file) {
            val session = useSiteSession
            val fooSymbol = session.getClassOrFail(file, "Foo")
            assertEquals(
                listOf("bar", "abstractFun"),
                session.getCallableSymbolsForObjCMemberTranslation(fooSymbol)
                    .map { it as KaNamedFunctionSymbol }
                    .map { it.name.asString() }
            )
        }
    }

    @Test
    fun `test - data class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                data class Foo(val a: Int)
            """.trimIndent()
        )
        analyze(file) {
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo")
            assertEquals(
                listOf("component1", "copy", "equals", "hashCode", "toString", "a"),
                session.getCallableSymbolsForObjCMemberTranslation(foo)
                    .sortedWith(session.getStableCallableOrder())
                    .map { it as KaNamedSymbol }
                    .map { it.name.asString() }
            )
        }
    }

    @Test
    fun `test - enum class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                enum class Foo {
                    A, B, C
                }
            """.trimIndent()
        )
        analyze(file) {
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo")
            assertEquals(
                emptyList(),
                session.getCallableSymbolsForObjCMemberTranslation(foo)
                    .sortedWith(session.getStableCallableOrder())
                    .map { it as KaNamedSymbol }
                    .map { it.name.asString() }
            )
        }
    }

    @Test
    fun `test - extension properties ordered as functions`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Foo { 
                  val Foo.a: Int get() = 42
                  fun b(): Int = 42
                }
            """.trimIndent()
        )
        analyze(file) {
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo")
            assertEquals(
                listOf("a", "b"),
                session.getCallableSymbolsForObjCMemberTranslation(foo)
                    .sortedWith(session.getStableCallableOrder())
                    .map { it as KaNamedSymbol }
                    .map { it.name.asString() }
            )
        }
    }
}
