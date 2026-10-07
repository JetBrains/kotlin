/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplCompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.*
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.collectScriptsCompilationDependenciesRecursively
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.impl._isSyntheticSnippet
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.add

class K2ReplCompiler(
    private val state: ReplCompilerState,
    private val convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile = SourceCode::convertToFirViaLightTree,
) : ReplCompiler<CompiledSnippet> {

    override val lastCompiledSnippet: LinkedSnippet<CompiledSnippet>?
        get() = state.lastCompiledSnippet

    override suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        configuration[ScriptCompilationConfiguration.repl.currentSnippetNo]?.let { state.nextSnippetNo = it }
        snippets.forEach { mainSnippet ->
            val [updatedConfiguration, syntheticSnippets] = configuration.prependSyntheticSnippets(mainSnippet).valueOr { return it }
            val snippetsWithSynthetics = syntheticSnippets + mainSnippet
            snippetsWithSynthetics.forEach { snippet ->
                // Messages from earlier snippets should not leak into the next snippet
                state.messageCollector.clear()
                val historyCheckpoint = state.makeHistoryCheckpoint()
                val res = try {
                    compileImpl(
                        state, snippet,
                        if (snippet == mainSnippet) updatedConfiguration.with { reset(repl._isSyntheticSnippet) }
                        else updatedConfiguration.with {
                            resultField("")
                            repl.resultFieldPrefix("")
                            repl._isSyntheticSnippet(true)
                        },
                        convertToFir,
                    )
                } catch (e: Throwable) {
                    state.rollbackHistory(historyCheckpoint)
                    throw e
                }
                state.nextSnippetNo++
                when (res) {
                    is ResultWithDiagnostics.Success -> {
                        state.lastCompiledSnippet = state.lastCompiledSnippet.add(res.value)
                    }
                    is ResultWithDiagnostics.Failure -> {
                        state.rollbackHistory(historyCheckpoint)
                        return res
                    }
                }
            }
        }
        return state.lastCompiledSnippet?.asSuccess() ?: ResultWithDiagnostics.Failure("No snippets provided".asErrorDiagnostics())
    }

    suspend fun compile(snippet: SourceCode): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> =
        compile(snippet, state.baseScriptCompilationConfiguration)

    companion object {

        fun createCompilationState(
            messageCollector: ScriptDiagnosticsMessageCollector,
            rootDisposable: Disposable,
            scriptCompilationConfiguration: ScriptCompilationConfiguration,
            hostConfiguration: ScriptingHostConfiguration =
                ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                    repl {
                        firReplHistoryProvider(FirReplHistoryProviderImpl())
                        isReplSnippetSource { _, _ -> true }
                    }
                }
        ): ReplCompilerState {

            val effectiveHostConfig =
                effectiveHostConfiguration(hostConfiguration, scriptCompilationConfiguration.withoutReplSessionHostProperties())
            return createIsolatedScriptingState(
                scriptCompilationConfiguration,
                effectiveHostConfig,
                messageCollector,
                rootDisposable,
                { add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, ReplCompilerPluginRegistrar(effectiveHostConfig)) },
            ) { _, compilerConfiguration, projectEnvironment, ignoredOptionsReportingState ->
                ReplCompilerState(
                    scriptCompilationConfiguration,
                    compilerConfiguration,
                    projectEnvironment,
                    ignoredOptionsReportingState,
                    messageCollector,
                    effectiveHostConfig,
                )
            }
        }
    }
}

@OptIn(LegacyK2CliPipeline::class, DirectDeclarationsAccess::class)
private fun compileImpl(
    state: ReplCompilerState,
    snippet: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile,
): ResultWithDiagnostics<CompiledSnippet> {
    val initialScriptCompilationConfiguration = scriptCompilationConfiguration.with { repl.currentSnippetNo(state.nextSnippetNo) }
    val project = state.projectEnvironment.project
    val messageCollector = state.messageCollector
    val compilerConfiguration = state.compilerConfiguration.copy().apply {
        jvmTarget = selectJvmTarget(scriptCompilationConfiguration, messageCollector)
    }
    val diagnosticsReporter = DiagnosticsCollectorImpl()
    val renderDiagnosticName = compilerConfiguration.renderDiagnosticInternalName
    val compilerEnvironment = ModuleCompilerEnvironment(state.projectEnvironment, diagnosticsReporter)
    val targetId = TargetId(snippet.name!!, "java-production")

    fun ScriptCompilationConfiguration.refineAll(source: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        refineAllWithFirAnnotationSession(source, state.hostConfiguration, { _, _ -> state.sessionForAnnotationResolution }, convertToFir)

    // The annotation-based refinement converts the snippet and fails on its syntax errors before the check below,
    // so the incomplete snippet should be recognized here as well.
    var snippetRefinementRawFir: Pair<BaseDiagnosticsCollector, KtSourceFile?>? = null
    val refinedSnippetConfiguration = initialScriptCompilationConfiguration.refineAllWithFirAnnotationSession(
        snippet, state.hostConfiguration, { _, _ -> state.sessionForAnnotationResolution },
        convertToFir = { session, collector ->
            convertToFir(session, collector).also { snippetRefinementRawFir = collector to it.sourceFile }
        }
    ).valueOr { failure ->
        val [collector, snippetFile] = snippetRefinementRawFir ?: return failure
        if (!collector.isIncompleteSnippet(snippet, snippetFile)) return failure
        return ResultWithDiagnostics.Failure(listOf(ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code")) + failure.reports)
    }

    val refinedConfigurationsCache = state.hostConfiguration[ScriptingHostConfiguration.scriptRefinedCompilationConfigurationsCache]
        ?: error("ScriptRefinedCompilationConfigurationCache is not configured in the REPL host configuration")
    refinedConfigurationsCache.storeRefinedCompilationConfiguration(snippet, refinedSnippetConfiguration.asSuccess())

    fun getRefinedConfiguration(source: SourceCode): ScriptCompilationConfiguration =
        refinedConfigurationsCache.getRefinedCompilationConfiguration(source)?.valueOrNull() ?: refinedSnippetConfiguration

    // configuration refinement with the additional sources collection
    val allSourceFiles = mutableListOf(snippet)
    (
        val classpath, val newSources = sources, val sourceDependencies
    ) =
        collectScriptsCompilationDependenciesRecursively(allSourceFiles) { source ->
            state.hostConfiguration.getOrStoreRefinedCompilationConfiguration(source) { importedScript, baseConfiguration ->
                baseConfiguration.refineAll(importedScript)
            }
        }.valueOr { return it }
    allSourceFiles.addAll(0, newSources)

    state.applyRefinedCompilerOptions(compilerConfiguration, allSourceFiles, messageCollector, ::getRefinedConfiguration)

    val [libModuleData, newClassPath] = state.moduleDataProvider.addNewLibraryModuleDataIfNeeded(classpath.map(File::toPath))

    if (newClassPath.isNotEmpty()) {
        state.projectEnvironment.updateClasspath(newClassPath.map { it.toFile() })
    }

    val extensionRegistrars = compilerConfiguration.getCompilerExtensions(FirExtensionRegistrar)
    if (libModuleData != null) {
        createScriptingAdditionalLibrariesSession(
            libModuleData,
            state.sessionFactoryContext,
            state.moduleDataProvider,
            state.sharedLibrarySession,
            extensionRegistrars,
            compilerConfiguration,
        )
        KotlinJavaPsiFacade.getInstance(project).clearPackageCaches()
    }

    val moduleData = state.moduleDataProvider.addNewSnippetModuleData(Name.special("<REPL-snippet-${snippet.name!!}>"))

    val session = createScriptSourceSession(
        moduleData, extensionRegistrars, compilerConfiguration, state.sessionFactoryContext, state.hostConfiguration
    )

    val sourcesToFir = allSourceFiles.associateWith { it.convertToFir(session, diagnosticsReporter) }
    val rawFir = sourcesToFir.values.toList()

    // syntax errors reporting
    if (diagnosticsReporter.hasErrors) {
        if (diagnosticsReporter.isIncompleteSnippet(snippet, sourcesToFir[snippet]?.sourceFile)) {
            messageCollector.report(ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code"))
        }
        diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
        return failure(messageCollector)
    }

    state.hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]?.let { historyProvider ->
        for (importedSource in newSources) {
            val importedSnippet = sourcesToFir[importedSource]?.declarations?.firstIsInstanceOrNull<FirReplSnippet>() ?: continue
            historyProvider.putImportedSnippet(importedSnippet.symbol)
        }
    }

    val outputs = listOf(resolveAndCheckFir(session, rawFir, diagnosticsReporter)).also {
        it.runPlatformCheckers(diagnosticsReporter)
    }
    val frontendOutput = AllModulesFrontendOutput(outputs)

    val [irInput, generationState] = generateCodeIfNoErrors(
        compilerConfiguration, targetId, frontendOutput, compilerEnvironment, messageCollector, renderDiagnosticName
    ).valueOr { return it }

    return makeCompiledScript(
        generationState,
        snippet,
        { source ->
            sourcesToFir[source]?.declarations?.firstIsInstanceOrNull<FirReplSnippet>()?.snippetClass?.symbol?.classId?.asSingleFqName()
        },
        sourceDependencies,
        ::getRefinedConfiguration,
        extractResultFields(irInput.irModuleFragment)
    ).onSuccess { compiledScript ->
        ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
    }
}

private fun BaseDiagnosticsCollector.isIncompleteSnippet(snippet: SourceCode, snippetFile: KtSourceFile?): Boolean {
    if (snippetFile == null) return false
    val snippetDiagnostics = diagnosticsByFile[snippetFile].orEmpty().filter { it.severity == Severity.ERROR }
    if (snippetDiagnostics.isEmpty()) return false
    val textEnd = snippet.text.trimEnd().length
    return snippetDiagnostics.all { diagnostic ->
        diagnostic.factory == FirSyntaxErrors.SYNTAX &&
                diagnostic is KtDiagnosticWithSource && diagnostic.textRanges.all { range -> range.endOffset >= textEnd }
    }
}

// Find the appropriate jvm target for the compiler from the ScriptCompilationConfiguration.
// Since this can be configured in two places, we check if both places agree on the same value (if configured twice).
// If not, CompilerOptions takes precedence and a warning is reported. We treat CompilerOptions with a higher priority
// as we assume they are more likely to be under the user's control.
internal fun selectJvmTarget(configuration: ScriptCompilationConfiguration, messageCollector: MessageCollector): JvmTarget {
    val jvmTargetFromBlock = configuration[ScriptCompilationConfiguration.jvm.jvmTarget]?.let { JvmTarget.fromString(it) }
    val jvmTargetFromOptions = configuration[ScriptCompilationConfiguration.compilerOptions]
        ?.zipWithNext()
        ?.firstOrNull { it.first == "-jvm-target" }
        ?.second
        ?.let { JvmTarget.fromString(it) }

    if (jvmTargetFromBlock != null && jvmTargetFromOptions != null && jvmTargetFromBlock != jvmTargetFromOptions) {
        val message =
            "JVM target in ScriptCompilationConfiguration is defined differently in `jvm.jvmTarget` (${jvmTargetFromBlock}) vs. in `compilerOptions` (${jvmTargetFromOptions}). Using $jvmTargetFromOptions."
        messageCollector.report(
            severity = CompilerMessageSeverity.STRONG_WARNING,
            message = message
        )
    }
    return jvmTargetFromOptions ?: jvmTargetFromBlock ?: JvmTarget.DEFAULT
}

/** The configuration of a snippet from another REPL may carry the session-bound properties of its host. */
private fun ScriptCompilationConfiguration.withoutReplSessionHostProperties(): ScriptCompilationConfiguration =
    with {
        hostConfiguration.update {
            it?.let { embeddedHostConfiguration ->
                ScriptingHostConfiguration(embeddedHostConfiguration) {
                    reset(ScriptingHostConfiguration.repl.firReplHistoryProvider)
                    reset(ScriptingHostConfiguration.repl.isReplSnippetSource)
                }
            }
        }
    }
