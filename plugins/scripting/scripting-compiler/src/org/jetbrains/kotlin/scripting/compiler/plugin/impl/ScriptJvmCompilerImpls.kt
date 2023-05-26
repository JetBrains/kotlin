/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.ScriptsCompilationDependencies
import org.jetbrains.kotlin.scripting.compiler.plugin.services.scriptDefinitionProviderService
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
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
    ): ResultWithDiagnostics<CompiledScript> =
        withMessageCollector(script = script) { messageCollector ->
            withScriptCompilationCache(script, scriptCompilationConfiguration, messageCollector) {

                val initialConfiguration = scriptCompilationConfiguration.refineBeforeParsing(script).valueOr {
                    return@withScriptCompilationCache it
                }

                val context = createCompilationContextFromEnvironment(initialConfiguration, environment, messageCollector)

                val previousMessageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]
                try {
                    environment.configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

                    compileImpl(script, context, initialConfiguration, messageCollector)
                } finally {
                    if (previousMessageCollector != null)
                        environment.configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, previousMessageCollector)
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
    val cache = scriptCompilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]
        ?.get(ScriptingHostConfiguration.jvm.compilationCache)

    val cached = cache?.get(script, scriptCompilationConfiguration)

    return cached?.asSuccess(messageCollector.diagnostics)
        ?: body().also {
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

    checkKotlinPackageUsageForPsi(context.environment.configuration, sourceFiles, messageCollector)

    if (messageCollector.hasErrors() || sourceDependencies.any { it.sourceDependencies is ResultWithDiagnostics.Failure }) {
        return failure(messageCollector)
    }

    val dependenciesProvider = ScriptDependenciesProvider.getInstance(context.environment.project)
    val getScriptConfiguration = { ktFile: KtFile ->
        val refinedConfiguration =
            dependenciesProvider?.getScriptConfigurationResult(ktFile, context.baseScriptCompilationConfiguration)
                ?.valueOrNull()?.configuration ?: context.baseScriptCompilationConfiguration
        refinedConfiguration.with {
            // Adjust definitions so all compiler dependencies are saved in the resulting compilation configuration, so evaluation
            // performed with the expected classpath
            // TODO: make this logic obsolete by injecting classpath earlier in the pipeline
            val depsFromConfiguration = get(dependencies)?.flatMapTo(HashSet()) { (it as? JvmDependency)?.classpath ?: emptyList() }
            val depsFromCompiler = context.environment.configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)
                .mapNotNull {
                    when {
                        it is JvmClasspathRoot && !it.isSdkRoot -> it.file
                        it is JvmModulePathRoot -> it.file
                        else -> null
                    }
                }
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

    return if (context.environment.configuration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
        doCompileWithK2(context, script, sourceFiles, sourceDependencies, messageCollector, getScriptConfiguration)
    } else {
        doCompile(context, script, sourceFiles, sourceDependencies, messageCollector, getScriptConfiguration)
    }
}

internal fun registerPackageFragmentProvidersIfNeeded(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    environment: KotlinCoreEnvironment
) {
    val scriptDependencies = scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies] ?: return
    val scriptDependenciesFromClassLoader = scriptDependencies.filterIsInstance<JvmDependencyFromClassLoader>().takeIf { it.isNotEmpty() }
        ?: return
    // TODO: consider implementing deduplication/diff processing
    val alreadyRegistered =
        environment.project.extensionArea.getExtensionPoint(PackageFragmentProviderExtension.extensionPointName).extensions.any {
            (it is PackageFragmentFromClassLoaderProviderExtension) &&
                    it.scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies] == scriptDependencies
        }
    if (!alreadyRegistered) {
        scriptDependenciesFromClassLoader.forEach { dependency ->
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
        kotlinCompilerConfiguration
    ).diagnosticReporter(
        diagnosticsReporter
    ).build().also {
        KotlinCodegenFacade.compileCorrectFiles(
            sourceFiles,
            it,
            if (kotlinCompilerConfiguration.getBoolean(JVMConfigurationKeys.IR))
                JvmIrCodegenFactory(
                    kotlinCompilerConfiguration,
                    kotlinCompilerConfiguration.get(CLIConfigurationKeys.PHASE_CONFIG),
                ) else DefaultCodegenFactory
        )
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
            diagnosticsReporter,
            messageCollector,
            kotlinCompilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )
    }
}

private fun doCompileWithK2(
    context: SharedScriptCompilationContext,
    script: SourceCode,
    sourceFiles: List<KtFile>,
    sourceDependencies: List<ScriptsCompilationDependencies.SourceDependencies>,
    messageCollector: ScriptDiagnosticsMessageCollector,
    getScriptConfiguration: (KtFile) -> ScriptCompilationConfiguration
): ResultWithDiagnostics<KJvmCompiledScript> {
    val syntaxErrors = sourceFiles.fold(false) { errorsFound, ktFile ->
        AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
    }

    if (syntaxErrors) {
        return failure(messageCollector)
    }

    registerPackageFragmentProvidersIfNeeded(getScriptConfiguration(sourceFiles.first()), context.environment)

    val kotlinCompilerConfiguration = context.environment.configuration

    val targetId = TargetId(
        kotlinCompilerConfiguration[CommonConfigurationKeys.MODULE_NAME] ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
        "java-production"
    )

    val renderDiagnosticName = kotlinCompilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

    val sources = sourceFiles.map { KtPsiSourceFile(it) }

    val compilerInput = ModuleCompilerInput(
        targetId,
        GroupedKtSources(platformSources = sources, commonSources = emptySet(), sourcesByModuleName = emptyMap()),
        CommonPlatforms.defaultCommonPlatform,
        JvmPlatforms.unspecifiedJvmPlatform,
        kotlinCompilerConfiguration
    )

    val projectEnvironment = context.environment.toAbstractProjectEnvironment()
    val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter)

    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
    val incrementalCompilationScope = createIncrementalCompilationScope(
        compilerInput.configuration,
        projectEnvironment,
        incrementalExcludesScope = null
    )?.also { librariesScope -= it }
    val extensionRegistrars = (projectEnvironment as? VfsBasedProjectEnvironment)
        ?.let { FirExtensionRegistrar.getInstances(it.project) }
        .orEmpty()

    val rootModuleName = targetId.name
    val libraryList = createLibraryListForJvm(
        rootModuleName,
        kotlinCompilerConfiguration,
        friendPaths = emptyList()
    )
    val session = prepareJvmSessions(
        sourceFiles, kotlinCompilerConfiguration, projectEnvironment, Name.identifier(rootModuleName), extensionRegistrars,
        librariesScope, libraryList, isCommonSourceForPsi, fileBelongsToModuleForPsi,
        createProviderAndScopeForIncrementalCompilation = { files ->
            createContextForIncrementalCompilation(
                compilerInput.configuration,
                projectEnvironment,
                compilerEnvironment.projectEnvironment.getSearchScopeBySourceFiles(files.map { KtPsiSourceFile(it) }),
                emptyList(),
                incrementalCompilationScope
            )
        }
    ).single().session

    session.scriptDefinitionProviderService?.run {
        definitionProvider = ScriptDefinitionProvider.getInstance(context.environment.project)
        configurationProvider = ScriptDependenciesProvider.getInstance(context.environment.project)
    }

    val rawFir = session.buildFirFromKtFiles(sourceFiles)

    val (scopeSession, fir) = session.runResolution(rawFir)
    // checkers
    session.runCheckers(scopeSession, fir, diagnosticsReporter)

    val analysisResults = FirResult(listOf(ModuleCompilerAnalyzedOutput(session, scopeSession, fir)))

    if (diagnosticsReporter.hasErrors) {
        diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
        return failure(messageCollector)
    }

    val irInput = convertAnalyzedFirToIr(compilerInput, analysisResults, compilerEnvironment)

    val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment, null)

    diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)

    if (diagnosticsReporter.hasErrors) {
        return failure(messageCollector)
    }

    return makeCompiledScript(
        codegenOutput.generationState,
        script,
        sourceFiles.first(),
        sourceDependencies,
        getScriptConfiguration
    ).onSuccess { compiledScript ->
        ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
    }
}
