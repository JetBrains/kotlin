/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.getCallableSymbolsForObjCMemberTranslation
import org.jetbrains.kotlin.objcexport.getStableCallableOrder
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
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
            val fooSymbol = getClassOrFail(file, "Foo")
            assertEquals(
                listOf("bar", "abstractFun"),
                getCallableSymbolsForObjCMemberTranslation(fooSymbol)
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
            val foo = getClassOrFail(file, "Foo")
            assertEquals(
                listOf("component1", "copy", "equals", "hashCode", "toString", "a"),
                getCallableSymbolsForObjCMemberTranslation(foo)
                    .sortedWith(getStableCallableOrder())
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
            val foo = getClassOrFail(file, "Foo")
            assertEquals(
                emptyList(),
                getCallableSymbolsForObjCMemberTranslation(foo)
                    .sortedWith(getStableCallableOrder())
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
            val foo = getClassOrFail(file, "Foo")
            assertEquals(
                listOf("a", "b"),
                getCallableSymbolsForObjCMemberTranslation(foo)
                    .sortedWith(getStableCallableOrder())
                    .map { it as KaNamedSymbol }
                    .map { it.name.asString() }
            )
        }
    }
}