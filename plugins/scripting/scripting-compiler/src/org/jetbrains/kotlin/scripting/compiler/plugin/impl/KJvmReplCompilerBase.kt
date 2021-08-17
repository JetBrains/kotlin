/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl


import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.*
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.resolve.skipExtensionsResolutionForImplicits
import org.jetbrains.kotlin.scripting.resolve.skipExtensionsResolutionForImplicitsExceptInnermost
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

                if (messageCollector.hasErrors()) return failure(messageCollector)

                if (history.isEmpty()) {
                    val updatedConfiguration = ScriptDependenciesProvider.getInstance(context.environment.project)
                        ?.getScriptConfiguration(snippetKtFile)?.configuration
                        ?: context.baseScriptCompilationConfiguration
                    registerPackageFragmentProvidersIfNeeded(
                        updatedConfiguration,
                        context.environment
                    )
                }

                val snippetNo = scriptPriority.getAndIncrement()

                val analysisResult =
                    compilationState.analyzerEngine.analyzeReplLineWithImportedScripts(
                        snippetKtFile,
                        sourceFiles.drop(1),
                        snippet,
                        snippetNo
                    )
                AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

                val scriptDescriptor = when (analysisResult) {
                    is ReplCodeAnalyzerBase.ReplLineAnalysisResult.WithErrors -> return failure(messageCollector)
                    is ReplCodeAnalyzerBase.ReplLineAnalysisResult.Successful -> {
                        (analysisResult.scriptDescriptor as? ScriptDescriptor)
                            ?: throw AssertionError("Unexpected script descriptor type ${analysisResult.scriptDescriptor::class}")
                    }
                    else -> throw AssertionError("Unexpected result ${analysisResult::class.java}")
                }

                val isIr = context.environment.configuration.getBoolean(JVMConfigurationKeys.IR)

                val generationState = if (isIr) {
                    generateWithBackendIr(compilationState, snippetKtFile, sourceFiles)
                } else {
                    generateWithOldBackend(compilationState, snippetKtFile, sourceFiles)
                }

                history.push(LineId(snippetNo, 0, snippet.hashCode()), scriptDescriptor)

                val dependenciesProvider = ScriptDependenciesProvider.getInstance(context.environment.project)
                makeCompiledScript(
                    generationState,
                    snippet,
                    sourceFiles.first(),
                    sourceDependencies
                ) { ktFile ->
                    dependenciesProvider?.getScriptConfiguration(ktFile)?.configuration
                        ?: context.baseScriptCompilationConfiguration
                }.onSuccess { compiledScript ->

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

    private fun generateWithOldBackend(
        compilationState: ReplCompilationState<AnalyzerT>,
        snippetKtFile: KtFile,
        sourceFiles: List<KtFile>
    ): GenerationState {
        val generationState = GenerationState.Builder(
            snippetKtFile.project,
            ClassBuilderFactories.BINARIES,
            compilationState.analyzerEngine.module,
            compilationState.analyzerEngine.trace.bindingContext,
            sourceFiles,
            compilationState.environment.configuration
        ).build().also { generationState ->
            generationState.scriptSpecific.earlierScriptsForReplInterpreter = history.map { it.item }
            generationState.beforeCompile()
        }
        KotlinCodegenFacade.generatePackage(generationState, snippetKtFile.script!!.containingKtFile.packageFqName, sourceFiles)

        return generationState
    }

    private fun generateWithBackendIr(
        compilationState: ReplCompilationState<AnalyzerT>,
        snippetKtFile: KtFile,
        sourceFiles: List<KtFile>
    ): GenerationState {
        val generatorExtensions = object : JvmGeneratorExtensionsImpl(compilationState.environment.configuration) {
            override fun getPreviousScripts() = history.map { compilationState.symbolTable.referenceScript(it.item) }
        }
        val codegenFactory = JvmIrCodegenFactory(
            compilationState.environment.configuration,
            compilationState.environment.configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases),
            compilationState.mangler, compilationState.symbolTable, generatorExtensions
        )
        val generationState = GenerationState.Builder(
            snippetKtFile.project,
            ClassBuilderFactories.BINARIES,
            compilationState.analyzerEngine.module,
            compilationState.analyzerEngine.trace.bindingContext,
            sourceFiles,
            compilationState.environment.configuration
        )
            .codegenFactory(codegenFactory)
            .build()

        generationState.codegenFactory.generateModule(generationState, generationState.files)

        return generationState
    }

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
        val analyzer = analyzerInit(context, implicitsResolutionFilter)
        val psiFacade = KotlinJavaPsiFacade.getInstance(environment.project)
        psiFacade.setNotFoundPackagesCachingStrategy(ReplNotFoundPackagesCachingStrategy)
        analyzer
    }

    private val manglerAndSymbolTable by lazy {
        val mangler = JvmDescriptorMangler(
            MainFunctionDetector(analyzerEngine.trace.bindingContext, environment.configuration.languageVersionSettings)
        )
        val symbolTable = SymbolTable(JvmIdSignatureDescriptor(mangler), IrFactoryImpl, JvmNameProvider)
        mangler to symbolTable
    }

    override val mangler: JvmDescriptorMangler get() = manglerAndSymbolTable.first
    override val symbolTable: SymbolTable get() = manglerAndSymbolTable.second
}
