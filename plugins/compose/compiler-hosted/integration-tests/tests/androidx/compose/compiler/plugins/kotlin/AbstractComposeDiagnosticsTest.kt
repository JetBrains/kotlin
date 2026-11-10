/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoRenderer
import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import java.io.File

abstract class AbstractComposeDiagnosticsTest : AbstractCompilerTest() {
    protected fun check(
        @Language("kotlin")
        expectedText: String,
        @Language("kotlin")
        commonText: String? = null,
        ignoreParseErrors: Boolean = false,
        additionalPaths: List<File> = emptyList(),
    ) {
        val clearText = clearTextFromDiagnosticMarkup(expectedText)
        val clearCommonText = commonText?.let(::clearTextFromDiagnosticMarkup)

        val diagnosticsByFileName = analyze(
            listOf(SourceFile("test.kt", clearText)),
            listOfNotNull(clearCommonText?.let { SourceFile("common.kt", it) }),
            additionalPaths
        ).reporter.diagnosticsByFile.mapKeys { it.key?.name }

        checkDiagnostics("test.kt", expectedText, clearText, diagnosticsByFileName, ignoreParseErrors)
        if (clearCommonText != null) {
            checkDiagnostics("common.kt", commonText, clearCommonText, diagnosticsByFileName, ignoreParseErrors)
        }
    }

    /**
     * Syntax errors fail the test unless [ignoreParseErrors] is set; either way they are not
     * part of the annotated text.
     */
    private fun checkDiagnostics(
        fileName: String,
        expectedText: String,
        clearText: String,
        diagnosticsByFileName: Map<String?, List<KtDiagnostic>>,
        ignoreParseErrors: Boolean,
    ) {
        val [syntaxErrors, allDiagnostics] = diagnosticsByFileName[fileName].orEmpty()
            .filterIsInstance<KtDiagnosticWithSource>()
            .partition { it.factoryName == FirSyntaxErrors.SYNTAX.name }
        if (!ignoreParseErrors && syntaxErrors.isNotEmpty()) {
            throw TestsCompilerError(
                IllegalStateException("Syntax errors in $fileName: " + syntaxErrors.joinToString { it.renderMessage() })
            )
        }

        val metaInfos = allDiagnostics.flatMap { diagnostic ->
            diagnostic.textRanges.map { range ->
                ParsedCodeMetaInfo(range.startOffset, range.endOffset, mutableListOf(), diagnostic.factoryName, description = null)
            }
        }
        val annotatedText = CodeMetaInfoRenderer.renderTagsToText(metaInfos, clearText).toString()

        assertEquals(expectedText, annotatedText)
    }

    protected fun checkFail(expectedText: String) {
        assertThrows(AssertionError::class.java) {
            check(expectedText)
        }
    }
}
