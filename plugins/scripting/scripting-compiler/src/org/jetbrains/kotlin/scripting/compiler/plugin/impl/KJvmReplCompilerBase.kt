/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl


import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmReplCompilerState
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import org.jetbrains.kotlin.scripting.resolve.skipExtensionsResolutionForImplicits
import org.jetbrains.kotlin.scripting.resolve.skipExtensionsResolutionForImplicitsExceptInnermost
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.PropertiesCollection
import kotlin.script.experimental.util.add

// NOTE: this implementation, as it is used in the REPL infrastructure, may be created for every snippet and provided with the state
// so it should not keep any compilation state outside the state field
open class KJvmReplCompilerBase<AnalyzerT : ReplCodeAnalyzerBase>(
    protected val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    val state: JvmReplCompilerState<*> = JvmReplCompilerState({ createCompilationState(it, hostConfiguration) })
) : ReplCompiler<KJvmCompiledScript>, ScriptCompiler {

    override var lastCompiledSnippet: LinkedSnippetImpl<KJvmCompiledScript>? = null
        protected set

    @OptIn(UnsafeDuringIrConstructionAPI::class, ObsoleteDescriptorBasedAPI::class)
    override suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<KJvmCompiledScript>> =
        snippets.map { snippet ->
            // TODO: get rid of messageCollector to avoid creation of additional entities
            withMessageCollector(snippet) { messageCollector ->
                val initialConfiguration = configuration.refineBeforeParsing(snippet).valueOr {
                    return it
                }

                @Suppress("UNCHECKED_CAST")
                val compilationState = state.getCompilationState(initialConfiguration) as ReplCompilationState<AnalyzerT>

                updateResolutionFilterWithHistory(configuration)

                val (context, errorHolder, snippetKtFile) = prepareForAnalyze(
                    snippet,
                    messageCollector,
                    compilationState,
                    failOnSyntaxErrors = true
                ).valueOr { return@withMessageCollector it }

                val (sourceFiles, sourceDependencies) = collectRefinedSourcesAndUpdateEnvironment(
                    context,
                    snippetKtFile,
                    initialConfiguration,
                    messageCollector
                )

                val firstFailure = sourceDependencies.firstOrNull { it.sourceDependencies is ResultWithDiagnostics.Failure }
                    ?.let { it.sourceDependencies as ResultWithDiagnostics.Failure }

                if (firstFailure != null)
                    return firstFailure

                checkKotlinPackageUsageForPsi(context.environment.configuration, sourceFiles, messageCollector)

                if (messageCollector.hasErrors()) return failure(messageCollector)

                // TODO: support case then JvmDependencyFromClassLoader is registered in non-first line
                // registerPackageFragmentProvidersIfNeeded already tries to avoid duplicated registering, but impact on
                // executing it on every snippet needs to be evaluated first
                if (state.history.isEmpty()) {
                    val updatedConfiguration = ScriptConfigurationsProvider.getInstance(context.environment.project)
                        ?.getScriptConfigurationResult(snippetKtFile, context.baseScriptCompilationConfiguration)?.valueOrNull()?.configuration
                        ?: context.baseScriptCompilationConfiguration
                    registerPackageFragmentProvidersIfNeeded(
                        updatedConfiguration,
                        context.environment
                    )
                }

                // TODO: ensure that currentLineId passing is only used for single snippet compilation
                val lineId = configuration[ScriptCompilationConfiguration.repl.currentLineId]
                    ?: LineId(state.getNextLineNo(), state.currentGeneration, snippet.hashCode())

                val analysisResult =
                    compilationState.analyzerEngine.analyzeReplLineWithImportedScripts(
                        snippetKtFile,
                        sourceFiles.drop(1),
                        snippet,
                        lineId.no
                    )
                AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder, renderDiagnosticName = false)

                val scriptDescriptor = when (analysisResult) {
                    is ReplCodeAnalyzerBase.ReplLineAnalysisResult.WithErrors -> return failure(messageCollector)
                    is ReplCodeAnalyzerBase.ReplLineAnalysisResult.Successful -> {
                        (analysisResult.scriptDescriptor as? ScriptDescriptor)
                            ?: throw AssertionError("Unexpected script descriptor type ${analysisResult.scriptDescriptor::class}")
                    }
                    else -> throw AssertionError("Unexpected result ${analysisResult::class.java}")
                }

                val codegenDiagnosticsCollector = SimpleDiagnosticsCollector { message, severity ->
                    messageCollector.report(severity, message)
                }

                val generationState = GenerationState(
                    snippetKtFile.project,
                    compilationState.analyzerEngine.module,
                    compilationState.environment.configuration,
                    diagnosticReporter = codegenDiagnosticsCollector,
                )

                val generatorExtensions = object : JvmGeneratorExtensionsImpl(compilationState.environment.configuration) {
                    override fun getPreviousScripts() = state.history.map { compilationState.symbolTable.descriptorExtension.referenceScript(it.item) }
                }
                val codegenFactory = JvmIrCodegenFactory(
                    compilationState.environment.configuration,
                    compilationState.mangler, compilationState.symbolTable, generatorExtensions
                )
                val irBackendInput = codegenFactory.convertToIr(
                    generationState, sourceFiles, compilationState.analyzerEngine.trace.bindingContext
                )

                if (codegenDiagnosticsCollector.hasErrors) {
                    return failure(messageCollector, *codegenDiagnosticsCollector.scriptDiagnostics(snippet).toTypedArray())
                }

                codegenFactory.generateModule(generationState, irBackendInput)

                if (codegenDiagnosticsCollector.hasErrors) {
                    return failure(messageCollector, *codegenDiagnosticsCollector.scriptDiagnostics(snippet).toTypedArray())
                }

                state.history.push(lineId, scriptDescriptor)

                val configurationsProvider = ScriptConfigurationsProvider.getInstance(context.environment.project)
                makeCompiledScript(
                    generationState,
                    snippet,
                    sourceFiles.first(),
                    sourceDependencies,
                    { ktFile ->
                        configurationsProvider?.getScriptConfigurationResult(ktFile, context.baseScriptCompilationConfiguration)?.valueOrNull()?.configuration
                            ?: context.baseScriptCompilationConfiguration
                    },
                    extractResultFields(irBackendInput.irModuleFragment)
                ).onSuccess { compiledScript ->

                    lastCompiledSnippet = lastCompiledSnippet.add(compiledScript)

                    lastCompiledSnippet?.asSuccess(messageCollector.diagnostics)
                        ?: failure(
                            snippet,
                            messageCollector,
                            "last compiled snippet should not be null"
                        )
                }
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
        failOnSyntaxErrors: Boolean
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

            val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(snippetKtFile, errorHolder)
            if (syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof) {
                messageCollector.report(ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code"))
            }
            if (failOnSyntaxErrors && syntaxErrorReport.isHasErrors) return failure(messageCollector)

            return AnalyzePreparationResult(
                context,
                errorHolder,
                snippetKtFile
            ).asSuccess()
        }

    protected fun updateResolutionFilterWithHistory(configuration: ScriptCompilationConfiguration) {
        val updatedConfiguration = updateConfigurationWithPreviousScripts(configuration)

        val classesToSkip =
            updatedConfiguration[ScriptCompilationConfiguration.skipExtensionsResolutionForImplicits]!!
        val classesToSkipAfterFirstTime =
            updatedConfiguration[ScriptCompilationConfiguration.skipExtensionsResolutionForImplicitsExceptInnermost]!!

        (state.compilation as ReplCompilationState<*>).implicitsResolutionFilter.update(classesToSkip, classesToSkipAfterFirstTime)
    }


    private fun updateConfigurationWithPreviousScripts(
        configuration: ScriptCompilationConfiguration
    ): ScriptCompilationConfiguration {
        val allPreviousLines =
            generateSequence(lastCompiledSnippet) { it.previous }
                .map { KotlinType(it.get().scriptClassFQName) }
                .toList()

        val skipFirstTime = allPreviousLines.subList(0, minOf(1, allPreviousLines.size))
        val skipAlways =
            if (allPreviousLines.isEmpty()) emptyList()
            else allPreviousLines.subList(1, allPreviousLines.size)

        return ScriptCompilationConfiguration(configuration) {
            skipExtensionsResolutionForImplicits(*skipAlways.toTypedArray())
            skipExtensionsResolutionForImplicitsExceptInnermost(*skipFirstTime.toTypedArray())
        }
    }

    companion object {

        fun createCompilationState(
            scriptCompilationConfiguration: ScriptCompilationConfiguration,
            hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration
        ): ReplCompilationState<ReplCodeAnalyzerBase> =
            createCompilationState(scriptCompilationConfiguration, hostConfiguration) { context1, resolutionFilter ->
                ReplCodeAnalyzerBase(context1.environment, implicitsResolutionFilter = resolutionFilter)
            }

        fun <AnalyzerT : ReplCodeAnalyzerBase> createCompilationState(
            scriptCompilationConfiguration: ScriptCompilationConfiguration,
            hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
            initAnalyzer: (SharedScriptCompilationContext, ImplicitsExtensionsResolutionFilter) -> AnalyzerT
        ): ReplCompilationState<AnalyzerT> {
            val context = withMessageCollectorAndDisposable(disposeOnSuccess = false) { messageCollector, disposable ->
                createIsolatedCompilationContext(
                    scriptCompilationConfiguration,
                    hostConfiguration,
                    messageCollector,
                    disposable
                ).asSuccess()
            }.valueOr { throw IllegalStateException("Unable to initialize repl compiler:\n  ${it.reports.joinToString("\n  ")}") }

            return ReplCompilationState(
                context,
                initAnalyzer,
                ReplImplicitsExtensionsResolutionFilter(
                    scriptCompilationConfiguration[ScriptCompilationConfiguration.skipExtensionsResolutionForImplicits].orEmpty(),
                    scriptCompilationConfiguration[ScriptCompilationConfiguration.skipExtensionsResolutionForImplicitsExceptInnermost].orEmpty()
                )
            )
        }
    }
}

class ReplCompilationState<AnalyzerT : ReplCodeAnalyzerBase>(
    val context: SharedScriptCompilationContext,
    val analyzerInit: (context: SharedScriptCompilationContext, implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter) -> AnalyzerT,
    val implicitsResolutionFilter: ReplImplicitsExtensionsResolutionFilter
) : JvmReplCompilerState.Compilation {
    override val disposable: Disposable? get() = context.disposable
    override val baseScriptCompilationConfiguration: ScriptCompilationConfiguration get() = context.baseScriptCompilationConfiguration
    override val environment: KotlinCoreEnvironment get() = context.environment

    val analyzerEngine: AnalyzerT by lazy {
        val analyzer = analyzerInit(context, implicitsResolutionFilter)
        val psiFacade = KotlinJavaPsiFacade.getInstance(environment.project)
        psiFacade.setNotFoundPackagesCachingStrategy(ReplNotFoundPackagesCachingStrategy)
        analyzer
    }

    private val manglerAndSymbolTable by lazy {
        val mangler = JvmDescriptorMangler(
            MainFunctionDetector(analyzerEngine.trace.bindingContext, environment.configuration.languageVersionSettings)
        )
        val symbolTable = SymbolTable(JvmIdSignatureDescriptor(mangler), IrFactoryImpl)
        mangler to symbolTable
    }

    val mangler: JvmDescriptorMangler get() = manglerAndSymbolTable.first
    val symbolTable: SymbolTable get() = manglerAndSymbolTable.second
}

/**
 * Internal property for transferring line id information when using new repl infrastructure with legacy one
 */
val ReplScriptCompilationConfigurationKeys.currentLineId by PropertiesCollection.key<LineId>(isTransient = true)
