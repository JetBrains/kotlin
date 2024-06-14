/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.objcexport.resolveObjCNameAnnotation
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtResolvedObjCNameAnnotationTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - class - no ObjCName annotation`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(ktFile) {
            val fooSymbol = ktFile.symbol.fileScope.classifiers(Name.identifier("Foo")).single() as KaClassLikeSymbol
            assertNull(fooSymbol.resolveObjCNameAnnotation())
        }
    }

    @Test
    fun `test - class - with ObjCName annotation`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile(
            """
                @kotlin.native.ObjCName("FooObjC", "FooSwift", true)
                class Foo
            """.trimIndent()
        )
        analyze(ktFile) {
            val fooSymbol = ktFile.symbol.fileScope.classifiers(Name.identifier("Foo")).single() as KaClassLikeSymbol
            val resolvedObjCAnnotation = assertNotNull(fooSymbol.resolveObjCNameAnnotation())
            assertEquals("FooObjC", resolvedObjCAnnotation.objCName)
            assertEquals("FooSwift", resolvedObjCAnnotation.swiftName)
            assertTrue(resolvedObjCAnnotation.isExact)
        }
    }

    @Test
    fun `test - function - with ObjCName annotation`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile(
            """
                @kotlin.native.ObjCName("fooObjC", "fooSwift", true)
                fun foo() = Unit
            """.trimIndent()
        )
        analyze(ktFile) {
            val fooSymbol = ktFile.symbol.fileScope.callables(Name.identifier("foo")).single() as KaFunctionSymbol
            val resolvedObjCAnnotation = assertNotNull(fooSymbol.resolveObjCNameAnnotation())
            assertEquals("fooObjC", resolvedObjCAnnotation.objCName)
            assertEquals("fooSwift", resolvedObjCAnnotation.swiftName)
            assertTrue(resolvedObjCAnnotation.isExact)
        }
    }
}