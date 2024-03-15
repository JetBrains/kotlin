/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.objcexport.StableCallableOrder
import org.jetbrains.kotlin.objcexport.getCallableSymbolsForObjCMemberTranslation
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
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
            val fooSymbol = file.getClassOrFail("Foo")
            assertEquals(
                listOf("bar", "abstractFun"),
                fooSymbol.getCallableSymbolsForObjCMemberTranslation()
                    .map { it as KtFunctionSymbol }
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
            val foo = file.getClassOrFail("Foo")
            assertEquals(
                listOf("component1", "copy", "equals", "hashCode", "toString", "a"),
                foo.getCallableSymbolsForObjCMemberTranslation()
                    .sortedWith(StableCallableOrder)
                    .map { it as KtNamedSymbol }
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
            val foo = file.getClassOrFail("Foo")
            assertEquals(
                emptyList(),
                foo.getCallableSymbolsForObjCMemberTranslation()
                    .sortedWith(StableCallableOrder)
                    .map { it as KtNamedSymbol }
                    .map { it.name.asString() }
            )
        }
    }
}