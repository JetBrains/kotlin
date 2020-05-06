/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerBase
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.failure
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.withMessageCollector
import org.jetbrains.kotlin.scripting.ide_services.compiler.api.DetailsLevel
import org.jetbrains.kotlin.scripting.ide_services.compiler.api.ReplInspectorResult
import org.jetbrains.kotlin.scripting.ide_services.compiler.api.ReplSymbolInspector
import org.jetbrains.kotlin.scripting.ide_services.compiler.impl.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.calcAbsolute
import kotlin.script.experimental.util.PropertiesCollection

class KJvmReplCompilerWithIdeServices(hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration) :
    KJvmReplCompilerBase<IdeLikeReplCodeAnalyzer>(hostConfiguration, {
        IdeLikeReplCodeAnalyzer(it.environment)
    }),
    ReplCompleter, ReplCodeAnalyzer, ReplSymbolInspector {

    override suspend fun complete(
        snippet: SourceCode,
        cursor: SourceCode.Position,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<ReplCompletionResult> =
        withMessageCollector(snippet) { messageCollector ->
            val analyzeResult = analyzeWithCursor(
                messageCollector, snippet, configuration, cursor
            ) { snippet, cursorAbs ->
                val newText =
                    prepareCodeForCompletion(snippet.text, cursorAbs)
                object : SourceCode {
                    override val text: String
                        get() = newText
                    override val name: String?
                        get() = snippet.name
                    override val locationId: String?
                        get() = snippet.locationId
                }
            }

            val completionOptions = configuration[PropertiesCollection.Key("completionOptions", CompletionOptions())]!!

            with(analyzeResult.valueOr { return it }) {
                return getKJvmCompletion(
                    ktScript,
                    bindingContext,
                    resolutionFacade,
                    moduleDescriptor,
                    cursorAbs,
                    completionOptions.filterOutShadowedDescriptors,
                    completionOptions.nameFilter,
                ).asSuccess(messageCollector.diagnostics)
            }
        }

    private fun List<ScriptDiagnostic>.toAnalyzeResultSequence() = (filter {
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
            val analyzeResult = analyzeWithCursor(
                messageCollector, snippet, configuration
            )

            with(analyzeResult.valueOr { return it }) {
                val resultRenderedType = resultProperty?.let {
                    DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it.type)
                }
                return ReplAnalyzerResult(
                    messageCollector.diagnostics.toAnalyzeResultSequence(),
                    resultRenderedType,
                ).asSuccess()
            }
        }
    }

    override suspend fun inspect(
        snippet: SourceCode,
        cursor: SourceCode.Position,
        configuration: ScriptCompilationConfiguration,
        detailsLevel: DetailsLevel
    ): ResultWithDiagnostics<ReplInspectorResult> =
        withMessageCollector(snippet) { messageCollector ->
            val analyzeResult = analyzeWithCursor(
                messageCollector, snippet, configuration, cursor
            )

            with(analyzeResult.valueOr { return it }) {
                return getKJvmInspections(
                    snippet.text,
                    ktScript,
                    bindingContext,
                    resolutionFacade,
                    moduleDescriptor,
                    cursorAbs,
                    detailsLevel
                ).asSuccess(
                    messageCollector.diagnostics
                )
            }
        }

    private fun analyzeWithCursor(
        messageCollector: ScriptDiagnosticsMessageCollector,
        snippet: SourceCode,
        configuration: ScriptCompilationConfiguration,
        cursor: SourceCode.Position? = null,
        getNewSnippet: (SourceCode, Int) -> SourceCode = { code, _ -> code }
    ): ResultWithDiagnostics<AnalyzeWithCursorResult> {
        val initialConfiguration = configuration.refineBeforeParsing(snippet).valueOr {
            return it
        }

        val cursorAbs = cursor?.calcAbsolute(snippet) ?: -1
        val newSnippet = if (cursorAbs == -1) snippet else getNewSnippet(snippet, cursorAbs)

        val compilationState = state.getCompilationState(initialConfiguration)

        val (_, errorHolder, snippetKtFile) = prepareForAnalyze(
            newSnippet,
            messageCollector,
            compilationState,
            checkSyntaxErrors = false
        ).valueOr { return it }

        val analysisResult =
            compilationState.analyzerEngine.statelessAnalyzeWithImportedScripts(snippetKtFile, emptyList(), scriptPriority.get() + 1)
        AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

        val (_, bindingContext, resolutionFacade, moduleDescriptor, resultProperty) = when (analysisResult) {
            is IdeLikeReplCodeAnalyzer.ReplLineAnalysisResultWithStateless.Stateless -> {
                analysisResult
            }
            else -> return failure(
                newSnippet,
                messageCollector,
                "Unexpected result ${analysisResult::class.java}"
            )
        }

        return AnalyzeWithCursorResult(
            snippetKtFile, bindingContext, resolutionFacade, moduleDescriptor, cursorAbs, resultProperty
        ).asSuccess()
    }

    companion object {
        data class AnalyzeWithCursorResult(
            val ktScript: KtFile,
            val bindingContext: BindingContext,
            val resolutionFacade: KotlinResolutionFacadeForRepl,
            val moduleDescriptor: ModuleDescriptor,
            val cursorAbs: Int,
            val resultProperty: PropertyDescriptor?,
        )
    }
}
