/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl


import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmReplCompilerStageHistory
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmReplCompilerState
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.resolve.skipExtensionsResolutionForImplicits
import org.jetbrains.kotlin.scripting.resolve.skipExtensionsResolutionForImplicitsExceptInnermost
import org.jetbrains.kotlin.utils.addToStdlib.min
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

open class KJvmReplCompilerBase<AnalyzerT : ReplCodeAnalyzerBase> protected constructor(
    protected val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    val initAnalyzer: (SharedScriptCompilationContext, ImplicitsExtensionsResolutionFilter) -> AnalyzerT
) : ReplCompiler<KJvmCompiledScript>, ScriptCompiler {
    val state = JvmReplCompilerState({ createReplCompilationState(it, initAnalyzer) })
    val history = JvmReplCompilerStageHistory(state)
    protected val scriptPriority = AtomicInteger()
    private val resolutionFilter = ReplImplicitsExtensionsResolutionFilter()

    override var lastCompiledSnippet: LinkedSnippetImpl<KJvmCompiledScript>? = null
        protected set

    fun createReplCompilationState(
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        initAnalyzer: (SharedScriptCompilationContext, ImplicitsExtensionsResolutionFilter) -> AnalyzerT /* = { ReplCodeAnalyzer1(it.environment) } */
    ): ReplCompilationState<AnalyzerT> {
        val context = withMessageCollectorAndDisposable(disposeOnSuccess = false) { messageCollector, disposable ->
            createIsolatedCompilationContext(
                scriptCompilationConfiguration,
                hostConfiguration,
                messageCollector,
                disposable
            ).asSuccess()
        }.valueOr { throw IllegalStateException("Unable to initialize repl compiler:\n  ${it.reports.joinToString("\n  ")}") }

        updateResolutionFilter(scriptCompilationConfiguration)

        return ReplCompilationState(context, initAnalyzer, resolutionFilter)
    }

    override suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<KJvmCompiledScript>> =
        snippets.map { snippet ->
            withMessageCollector(snippet) { messageCollector ->
                updateResolutionFilter(configuration)

                val initialConfiguration = configuration.refineBeforeParsing(snippet).valueOr {
                    return it
                }

                val compilationState = state.getCompilationState(initialConfiguration)

                val (context, errorHolder, snippetKtFile) = prepareForAnalyze(
                    snippet,
                    messageCollector,
                    compilationState,
                    checkSyntaxErrors = true
                ).valueOr { return@withMessageCollector it }

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
                    registerPackageFragmentProvidersIfNeeded(
                        updatedConfiguration,
                        context.environment
                    )
                }

                val no = scriptPriority.getAndIncrement()

                val analysisResult =
                    compilationState.analyzerEngine.analyzeReplLineWithImportedScripts(snippetKtFile, sourceFiles.drop(1), snippet, no)
                AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

                val scriptDescriptor = when (analysisResult) {
                    is ReplCodeAnalyzerBase.ReplLineAnalysisResult.WithErrors -> return failure(
                        messageCollector
                    )
                    is ReplCodeAnalyzerBase.ReplLineAnalysisResult.Successful -> {
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

                history.push(LineId(no, 0, snippet.hashCode()), scriptDescriptor)

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

                lastCompiledSnippet = lastCompiledSnippet.add(compiledScript)

                lastCompiledSnippet?.asSuccess(messageCollector.diagnostics)
                    ?: failure(
                        snippet,
                        messageCollector,
                        "last compiled snippet should not be null"
                    )
            }
        }.last()

    override suspend fun invoke(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript> {
        return when (val res = compile(script, scriptCompilationConfiguration)) {
            is ResultWithDiagnostics.Success -> res.value.get().asSuccess(res.reports)
            is ResultWithDiagnostics.Failure -> res
        }
    }

    protected data class AnalyzePreparationResult(
        val context: SharedScriptCompilationContext,
        val errorHolder: MessageCollectorBasedReporter,
        val snippetKtFile: KtFile
    )

    protected fun prepareForAnalyze(
        snippet: SourceCode,
        parentMessageCollector: MessageCollector,
        compilationState: JvmReplCompilerState.Compilation,
        checkSyntaxErrors: Boolean
    ): ResultWithDiagnostics<AnalyzePreparationResult> =
        withMessageCollector(
            snippet,
            parentMessageCollector
        ) { messageCollector ->
            val context =
                (compilationState as? ReplCompilationState<*>)?.context
                    ?: return failure(
                        snippet, messageCollector, "Internal error: unknown parameter passed as compilationState: $compilationState"
                    )

            setIdeaIoUseFallback()

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

            if (checkSyntaxErrors) {
                val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(snippetKtFile, errorHolder)
                if (syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof) return failure(
                    messageCollector, ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code")
                )
                if (syntaxErrorReport.isHasErrors) return failure(
                    messageCollector
                )
            }

            return AnalyzePreparationResult(
                context,
                errorHolder,
                snippetKtFile
            ).asSuccess()
        }

    protected fun updateResolutionFilter(configuration: ScriptCompilationConfiguration) {
        val updatedConfiguration = updateConfigurationWithPreviousScripts(configuration)

        val classesToSkip =
            updatedConfiguration[ScriptCompilationConfiguration.skipExtensionsResolutionForImplicits]!!
        val classesToSkipAfterFirstTime =
            updatedConfiguration[ScriptCompilationConfiguration.skipExtensionsResolutionForImplicitsExceptInnermost]!!

        resolutionFilter.update(classesToSkip, classesToSkipAfterFirstTime)
    }

    private fun updateConfigurationWithPreviousScripts(
        configuration: ScriptCompilationConfiguration
    ): ScriptCompilationConfiguration {
        val allPreviousLines =
            generateSequence(lastCompiledSnippet) { it.previous }
                .map { KotlinType(it.get().scriptClassFQName) }
                .toList()

        val skipFirstTime = allPreviousLines.subList(0, min(1, allPreviousLines.size))
        val skipAlways =
            if (allPreviousLines.isEmpty()) emptyList()
            else allPreviousLines.subList(1, allPreviousLines.size)

        return ScriptCompilationConfiguration(configuration) {
            skipExtensionsResolutionForImplicits.update {
                it?.also { it.toMutableList().addAll(skipAlways) } ?: skipAlways
            }
            skipExtensionsResolutionForImplicitsExceptInnermost.update {
                it?.also { it.toMutableList().addAll(skipFirstTime) } ?: skipFirstTime
            }
        }
    }

    companion object {
        fun create(hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration) =
            KJvmReplCompilerBase(hostConfiguration) { context, resolutionFilter ->
                ReplCodeAnalyzerBase(context.environment, implicitsResolutionFilter = resolutionFilter)
            }
    }

}

class ReplCompilationState<AnalyzerT : ReplCodeAnalyzerBase>(
    val context: SharedScriptCompilationContext,
    val analyzerInit: (context: SharedScriptCompilationContext, implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter) -> AnalyzerT,
    override val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter
) : JvmReplCompilerState.Compilation {
    override val disposable: Disposable? get() = context.disposable
    override val baseScriptCompilationConfiguration: ScriptCompilationConfiguration get() = context.baseScriptCompilationConfiguration
    override val environment: KotlinCoreEnvironment get() = context.environment
    override val analyzerEngine: AnalyzerT by lazy {
        // ReplCodeAnalyzer1(context.environment)
        analyzerInit(context, implicitsResolutionFilter)
    }
}
