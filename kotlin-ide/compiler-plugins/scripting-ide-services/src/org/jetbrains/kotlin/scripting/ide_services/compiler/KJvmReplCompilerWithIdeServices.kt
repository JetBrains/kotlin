/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerBase
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.failure
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.withMessageCollector
import org.jetbrains.kotlin.scripting.ide_services.compiler.impl.IdeLikeReplCodeAnalyzer
import org.jetbrains.kotlin.scripting.ide_services.compiler.impl.getKJvmCompletion
import org.jetbrains.kotlin.scripting.ide_services.compiler.impl.prepareCodeForCompletion
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.calcAbsolute

class KJvmReplCompilerWithIdeServices(hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration) :
    KJvmReplCompilerBase<IdeLikeReplCodeAnalyzer>(hostConfiguration, {
        IdeLikeReplCodeAnalyzer(it.environment)
    }),
    ReplCompleter, ReplCodeAnalyzer {

    override suspend fun complete(
        snippet: SourceCode,
        cursor: SourceCode.Position,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<ReplCompletionResult> =
        withMessageCollector(snippet) { messageCollector ->
            val initialConfiguration = configuration.refineBeforeParsing(snippet).valueOr {
                return it
            }

            val cursorAbs = cursor.calcAbsolute(snippet)

            val newText =
                prepareCodeForCompletion(snippet.text, cursorAbs)
            val newSnippet = object : SourceCode {
                override val text: String
                    get() = newText
                override val name: String?
                    get() = snippet.name
                override val locationId: String?
                    get() = snippet.locationId

            }

            val compilationState = state.getCompilationState(initialConfiguration)

            val (_, errorHolder, snippetKtFile) = prepareForAnalyze(
                newSnippet,
                messageCollector,
                compilationState,
                checkSyntaxErrors = false
            ).valueOr { return@withMessageCollector it }

            val analysisResult =
                compilationState.analyzerEngine.statelessAnalyzeWithImportedScripts(snippetKtFile, emptyList(), scriptPriority.get() + 1)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

            val (_, bindingContext, resolutionFacade, moduleDescriptor) = when (analysisResult) {
                is IdeLikeReplCodeAnalyzer.ReplLineAnalysisResultWithStateless.Stateless -> {
                    analysisResult
                }
                else -> return failure(
                    newSnippet,
                    messageCollector,
                    "Unexpected result ${analysisResult::class.java}"
                )
            }

            return getKJvmCompletion(
                snippetKtFile,
                bindingContext,
                resolutionFacade,
                moduleDescriptor,
                cursorAbs
            ).asSuccess(messageCollector.diagnostics)
        }

    private fun List<ScriptDiagnostic>.toAnalyzeResult() = (filter {
        when (it.severity) {
            ScriptDiagnostic.Severity.FATAL,
            ScriptDiagnostic.Severity.ERROR,
            ScriptDiagnostic.Severity.WARNING
            -> true
            else -> false
        }
    }).asSequence()

    override suspend fun analyze(
        snippet: SourceCode,
        cursor: SourceCode.Position,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<ReplAnalyzerResult> {
        return withMessageCollector(snippet) { messageCollector ->
            val initialConfiguration = configuration.refineBeforeParsing(snippet).valueOr {
                return it
            }

            val compilationState = state.getCompilationState(initialConfiguration)

            val (_, errorHolder, snippetKtFile) = prepareForAnalyze(
                snippet,
                messageCollector,
                compilationState,
                checkSyntaxErrors = true
            ).valueOr { return@withMessageCollector messageCollector.diagnostics.toAnalyzeResult().asSuccess() }

            val analysisResult =
                compilationState.analyzerEngine.statelessAnalyzeWithImportedScripts(snippetKtFile, emptyList(), scriptPriority.get() + 1)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

            messageCollector.diagnostics.toAnalyzeResult().asSuccess()
        }
    }
}
