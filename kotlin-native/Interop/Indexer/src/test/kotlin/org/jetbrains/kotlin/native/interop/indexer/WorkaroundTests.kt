/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import clang.CXTranslationUnit_DetailedPreprocessingRecord
import clang.clang_disposeTranslationUnit
import kotlin.test.Test

class WorkaroundTests : IndexerTests() {
    @Test
    fun `KT-48807`() {
        if (System.getProperty("os.name") != "Mac OS X") {
            return
        }
        val code = """
            #if !defined(NS_FORMAT_ARGUMENT)
                #if defined(__clang__)
                #define NS_FORMAT_ARGUMENT(A) __attribute__ ((format_arg(A)))
                #else
                #define NS_FORMAT_ARGUMENT(A)
                #endif
            #endif
            
            NS_FORMAT_ARGUMENT(1) int f(const char* c);
        """.trimIndent()
        val language = Language.OBJECTIVE_C
        val compilation = CompilationImpl(
                includes = emptyList(),
                additionalPreambleLines = code.split("\n"),
                compilerArgs = defaultCompilerArgs(language),
                language = language
        )
        withIndex { index ->
            val translationUnit = compilation.parse(
                    index,
                    options = CXTranslationUnit_DetailedPreprocessingRecord,
            )
            try {
                // Will throw `error: function does not return string` if cinterop
                // doesn't have a workaround for the `NS_FORMAT_ARGUMENT(A)` macro.
                translationUnit.ensureNoCompileErrors()
            } finally {
                clang_disposeTranslationUnit(translationUnit)
            }
        }
    }
}