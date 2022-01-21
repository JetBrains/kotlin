/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.ScriptsCompilationDependencies
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.JvmDependencyFromClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm

class ScriptJvmCompilerIsolated(val hostConfiguration: ScriptingHostConfiguration) : ScriptCompilerProxy {

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript> =
        withMessageCollectorAndDisposable(script = script) { messageCollector, disposable ->
            withScriptCompilationCache(script, scriptCompilationConfiguration, messageCollector) {
                val initialConfiguration = scriptCompilationConfiguration.refineBeforeParsing(script).valueOr {
                    return@withScriptCompilationCache it
                }

                val context = createIsolatedCompilationContext(
                    initialConfiguration, hostConfiguration, messageCollector, disposable
                )

                compileImpl(script, context, initialConfiguration, messageCollector)
            }
        }
}

class ScriptJvmCompilerFromEnvironment(val environment: KotlinCoreEnvironment) : ScriptCompilerProxy {

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript> {
        val parentMessageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]
        return withMessageCollector(script = script, parentMessageCollector = parentMessageCollector) { messageCollector ->
            withScriptCompilationCache(script, scriptCompilationConfiguration, messageCollector) {

                val initialConfiguration = scriptCompilationConfiguration.refineBeforeParsing(script).valueOr {
                    return@withScriptCompilationCache it
                }

                val context = createCompilationContextFromEnvironment(initialConfiguration, environment, messageCollector)

                try {
                    environment.configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

                    compileImpl(script, context, initialConfiguration, messageCollector)
                } finally {
                    if (parentMessageCollector != null)
                        environment.configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, parentMessageCollector)
                }
            }
        }
    }
}

private fun withScriptCompilationCache(
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    messageCollector: ScriptDiagnosticsMessageCollector,
    body: () -> ResultWithDiagnostics<CompiledScript>
): ResultWithDiagnostics<CompiledScript> {
    val cache = scriptCompilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]?.get(ScriptingHostConfiguration.jvm.compilationCache)

    val cached = cache?.get(script, scriptCompilationConfiguration)

    return if (cached != null) cached.asSuccess(messageCollector.diagnostics)
    else body().also {
        if (cache != null && it is ResultWithDiagnostics.Success) {
            cache.store(it.value, script, scriptCompilationConfiguration)
        }
    }
}

private fun compileImpl(
    script: SourceCode,
    context: SharedScriptCompilationContext,
    initialConfiguration: ScriptCompilationConfiguration,
    messageCollector: ScriptDiagnosticsMessageCollector
): ResultWithDiagnostics<CompiledScript> {
    val mainKtFile =
        getScriptKtFile(
            script,
            context.baseScriptCompilationConfiguration,
            context.environment.project,
            messageCollector
        )
            .valueOr { return it }

    if (messageCollector.hasErrors()) return failure(messageCollector)

    val (sourceFiles, sourceDependencies) = collectRefinedSourcesAndUpdateEnvironment(
        context,
        mainKtFile,
        initialConfiguration,
        messageCollector
    )

    if (messageCollector.hasErrors() || sourceDependencies.any { it.sourceDependencies is ResultWithDiagnostics.Failure }) {
        return failure(messageCollector)
    }

    val dependenciesProvider = ScriptDependenciesProvider.getInstance(context.environment.project)
    val getScriptConfiguration = { ktFile: KtFile ->
        (dependenciesProvider?.getScriptConfiguration(ktFile)?.configuration ?: context.baseScriptCompilationConfiguration)
            .with {
                // Adjust definitions so all compiler dependencies are saved in the resulting compilation configuration, so evaluation
                // performed with the expected classpath
                // TODO: make this logic obsolete by injecting classpath earlier in the pipeline
                val depsFromConfiguration = get(dependencies)?.flatMapTo(HashSet()) { (it as? JvmDependency)?.classpath ?: emptyList() }
                val depsFromCompiler = context.environment.configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)
                    .mapNotNull { if (it is JvmClasspathRoot && !it.isSdkRoot) it.file else null }
                if (!depsFromConfiguration.isNullOrEmpty()) {
                    val missingDeps = depsFromCompiler.filter { !depsFromConfiguration.contains(it) }
                    if (missingDeps.isNotEmpty()) {
                        dependencies.append(JvmDependency(missingDeps))
                    }
                } else {
                    dependencies.append(JvmDependency(depsFromCompiler))
                }
            }
    }

    return doCompile(context, script, sourceFiles, sourceDependencies, messageCollector, getScriptConfiguration)
}

internal fun registerPackageFragmentProvidersIfNeeded(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    environment: KotlinCoreEnvironment
) {
    scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies]?.forEach { dependency ->
        if (dependency is JvmDependencyFromClassLoader) {
            // TODO: consider implementing deduplication
            PackageFragmentProviderExtension.registerExtension(
                environment.project,
                PackageFragmentFromClassLoaderProviderExtension(
                    dependency.classLoaderGetter, scriptCompilationConfiguration, environment.configuration
                )
            )
        }
    }
}

private fun doCompile(
    context: SharedScriptCompilationContext,
    script: SourceCode,
    sourceFiles: List<KtFile>,
    sourceDependencies: List<ScriptsCompilationDependencies.SourceDependencies>,
    messageCollector: ScriptDiagnosticsMessageCollector,
    getScriptConfiguration: (KtFile) -> ScriptCompilationConfiguration
): ResultWithDiagnostics<KJvmCompiledScript> {

    registerPackageFragmentProvidersIfNeeded(getScriptConfiguration(sourceFiles.first()), context.environment)

    val analysisResult = analyze(sourceFiles, context.environment)

    if (!analysisResult.shouldGenerateCode) return failure(
        script,
        messageCollector,
        "no code to generate"
    )
    if (analysisResult.isError() || messageCollector.hasErrors()) return failure(
        messageCollector
    )

    val generationState =
        generate(analysisResult, sourceFiles, context.environment.configuration, messageCollector)

    if (messageCollector.hasErrors()) return failure(
        messageCollector
    )

    return makeCompiledScript(
        generationState,
        script,
        sourceFiles.first(),
        sourceDependencies,
        getScriptConfiguration
    ).onSuccess { compiledScript ->
        ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
    }
}

private fun analyze(sourceFiles: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult {
    val messageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]!!

    val analyzerWithCompilerReport = AnalyzerWithCompilerReport(
        messageCollector,
        environment.configuration.languageVersionSettings,
        environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    )

    analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
        val project = environment.project
        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            project,
            sourceFiles,
            NoScopeRecordCliBindingTrace(),
            environment.configuration,
            environment::createPackagePartProvider
        )
    }
    return analyzerWithCompilerReport.analysisResult
}

private fun generate(
    analysisResult: AnalysisResult, sourceFiles: List<KtFile>, kotlinCompilerConfiguration: CompilerConfiguration,
    messageCollector: MessageCollector
): GenerationState {
    val diagnosticsReporter = DiagnosticReporterFactory.createReporter()
    return GenerationState.Builder(
        sourceFiles.first().project,
        ClassBuilderFactories.BINARIES,
        analysisResult.moduleDescriptor,
        analysisResult.bindingContext,
        sourceFiles,
        kotlinCompilerConfiguration
    ).codegenFactory(
        if (kotlinCompilerConfiguration.getBoolean(JVMConfigurationKeys.IR))
            JvmIrCodegenFactory(
                kotlinCompilerConfiguration,
                kotlinCompilerConfiguration.get(CLIConfigurationKeys.PHASE_CONFIG),
            ) else DefaultCodegenFactory
    ).diagnosticReporter(
        diagnosticsReporter
    ).build().also {
        KotlinCodegenFacade.compileCorrectFiles(it)
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector)
    }
}
