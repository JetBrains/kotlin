/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.export.utilities.hasExportForCompilerAnnotation
import org.jetbrains.kotlin.analysis.api.scopes.scope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getPropertyOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class HasExportForCompilerAnnotationTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - has ExportForCompiler annotation`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile(
            """
            class Foo
            val array: Array<Int>
            val iterator: Iterator<Int>
            val foo: Foo
        """.trimIndent()
        )

        analyze(ktFile) {
            assertTrue(
                verifyHasExportForCompilerAnnotation(getPropertyOrFail(ktFile, "array"))
            )
            assertFalse(
                verifyHasExportForCompilerAnnotation(getPropertyOrFail(ktFile, "iterator"))
            )
            assertFalse(
                verifyHasExportForCompilerAnnotation(getPropertyOrFail(ktFile, "foo"))
            )
        }
    }
}

@OptIn(KaExperimentalApi::class)
context(session: KaSession)
private fun verifyHasExportForCompilerAnnotation(property: KaPropertySymbol): Boolean {
    return property
        .returnType
        .scope?.getConstructors()?.toList()?.any { it.hasExportForCompilerAnnotation }
        ?: fail("Property return type has no constructors: ${property.returnType}")
}
