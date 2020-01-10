/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.common.repl.IReplStageHistory
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmReplCompilerState
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.KJvmReplCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzer
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration

class KJvmReplCompilerImpl(val hostConfiguration: ScriptingHostConfiguration) : KJvmReplCompilerProxy {

    override fun createReplCompilationState(scriptCompilationConfiguration: ScriptCompilationConfiguration): JvmReplCompilerState.Compilation {
        val context = withMessageCollectorAndDisposable(disposeOnSuccess = false) { messageCollector, disposable ->
            createIsolatedCompilationContext(
                scriptCompilationConfiguration,
                hostConfiguration,
                messageCollector,
                disposable
            ).asSuccess()
        }.valueOr { throw IllegalStateException("Unable to initialize repl compiler:\n  ${it.reports.joinToString("\n  ")}") }
        return ReplCompilationState(context)
    }

    override fun checkSyntax(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        project: Project
    ): ResultWithDiagnostics<Boolean> =
        withMessageCollector(script) { messageCollector ->
            val ktFile = getScriptKtFile(
                script,
                scriptCompilationConfiguration,
                project,
                messageCollector
            )
                .valueOr { return it }
            val errorHolder = object : MessageCollectorBasedReporter {
                override val messageCollector = messageCollector
            }
            val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, errorHolder)
            when {
                syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof -> false.asSuccess(messageCollector.diagnostics)
                syntaxErrorReport.isHasErrors -> failure(messageCollector)
                else -> true.asSuccess()
            }
        }

    override fun compileReplSnippet(
        compilationState: JvmReplCompilerState.Compilation,
        snippet: SourceCode,
        snippetId: ReplSnippetId,
        // TODO: replace history with some interface based on CompiledScript
        history: IReplStageHistory<ScriptDescriptor>
    ): ResultWithDiagnostics<CompiledScript<*>> =
        withMessageCollector(snippet) { messageCollector ->

            val context = (compilationState as? ReplCompilationState)?.context
                ?: return failure(
                    snippet, messageCollector, "Internal error: unknown parameter passed as compilationState: $compilationState"
                )

            setIdeaIoUseFallback()

            // NOTE: converting between REPL entities from compiler and "new" scripting entities
            // TODO: (big) move REPL API from compiler to the new scripting infrastructure and streamline ops
            val codeLine = makeReplCodeLine(snippetId, snippet)

            val errorHolder = object : MessageCollectorBasedReporter {
                override val messageCollector = messageCollector
            }

            val snippetKtFile =
                getScriptKtFile(
                    snippet,
                    context.baseScriptCompilationConfiguration,
                    context.environment.project,
                    messageCollector
                )
                    .valueOr { return it }

            val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(snippetKtFile, errorHolder)
            if (syntaxErrorReport.isHasErrors) return failure(messageCollector)

            val (sourceFiles, sourceDependencies) = collectRefinedSourcesAndUpdateEnvironment(
                context,
                snippetKtFile,
                messageCollector
            )

            val firstFailure = sourceDependencies.firstOrNull { it.sourceDependencies is ResultWithDiagnostics.Failure }
                ?.let { it.sourceDependencies as ResultWithDiagnostics.Failure }

            if (firstFailure != null)
                return firstFailure

            if (history.isEmpty()) {
                val updatedConfiguration = ScriptDependenciesProvider.getInstance(context.environment.project)
                    ?.getScriptConfiguration(snippetKtFile)?.configuration
                    ?: context.baseScriptCompilationConfiguration
                registerPackageFragmetProvidersIfNeeded(updatedConfiguration, context.environment)
            }

            val analysisResult =
                compilationState.analyzerEngine.analyzeReplLineWithImportedScripts(snippetKtFile, sourceFiles.drop(1), codeLine)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

            val scriptDescriptor = when (analysisResult) {
                is ReplCodeAnalyzer.ReplLineAnalysisResult.WithErrors -> return failure(
                    messageCollector
                )
                is ReplCodeAnalyzer.ReplLineAnalysisResult.Successful -> {
                    (analysisResult.scriptDescriptor as? ScriptDescriptor)
                        ?: return failure(
                            snippet,
                            messageCollector,
                            "Unexpected script descriptor type ${analysisResult.scriptDescriptor::class}"
                        )
                }
                else -> return failure(
                    snippet,
                    messageCollector,
                    "Unexpected result ${analysisResult::class.java}"
                )
            }

            val generationState = GenerationState.Builder(
                snippetKtFile.project,
                ClassBuilderFactories.BINARIES,
                compilationState.analyzerEngine.module,
                compilationState.analyzerEngine.trace.bindingContext,
                sourceFiles,
                compilationState.environment.configuration
            ).build().apply {
                scriptSpecific.earlierScriptsForReplInterpreter = history.map { it.item }
                beforeCompile()
            }
            KotlinCodegenFacade.generatePackage(generationState, snippetKtFile.script!!.containingKtFile.packageFqName, sourceFiles)

            history.push(LineId(codeLine), scriptDescriptor)

            val dependenciesProvider = ScriptDependenciesProvider.getInstance(context.environment.project)
            val compiledScript =
                makeCompiledScript(
                    generationState,
                    snippet,
                    sourceFiles.first(),
                    sourceDependencies
                ) { ktFile ->
                    dependenciesProvider?.getScriptConfiguration(ktFile)?.configuration
                        ?: context.baseScriptCompilationConfiguration
                }

            compiledScript.asSuccess(messageCollector.diagnostics)
        }
}

internal class ReplCompilationState(val context: SharedScriptCompilationContext) : JvmReplCompilerState.Compilation {
    override val disposable: Disposable? get() = context.disposable
    override val baseScriptCompilationConfiguration: ScriptCompilationConfiguration get() = context.baseScriptCompilationConfiguration
    override val environment: KotlinCoreEnvironment get() = context.environment
    override val analyzerEngine: ReplCodeAnalyzer by lazy {
        ReplCodeAnalyzer(context.environment)
    }
}

internal fun makeReplCodeLine(id: ReplSnippetId, code: SourceCode): ReplCodeLine =
    ReplCodeLine(id.no, id.generation, code.text)

