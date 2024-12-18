/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.SessionWithSources
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.fileBelongsToModuleForPsi
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.isCommonSourceForPsi
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.prepareJvmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.MinimizedFrontendContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.*
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.ScriptsCompilationDependencies
import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.ScriptResultFieldData
import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.scriptResultFieldDataAttr
import org.jetbrains.kotlin.scripting.compiler.plugin.services.scriptDefinitionProviderService
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.resolvedImportScripts
import org.jetbrains.kotlin.utils.topologicalSort
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.impl._languageVersion
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

                val previousMessageCollector = environment.configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY]
                try {
                    environment.configuration.messageCollector = messageCollector

                    compileImpl(script, context, initialConfiguration, messageCollector)
                } finally {
                    if (previousMessageCollector != null)
                        environment.configuration.messageCollector = previousMessageCollector
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

    val configurationsProvider = ScriptConfigurationsProvider.getInstance(context.environment.project)
    val getScriptConfiguration = { ktFile: KtFile ->
        val refinedConfiguration =
            configurationsProvider?.getScriptConfigurationResult(ktFile, context.baseScriptCompilationConfiguration)
                ?.valueOrNull()?.configuration ?: context.baseScriptCompilationConfiguration
        refinedConfiguration.with {
            _languageVersion(context.environment.configuration.languageVersionSettings.languageVersion.versionString)
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

    val diagnosticsReporter = DiagnosticReporterFactory.createReporter(messageCollector)
    val generationState = GenerationState(
        sourceFiles.first().project,
        analysisResult.moduleDescriptor,
        context.environment.configuration,
        diagnosticReporter = diagnosticsReporter,
    )

    val codegenFactory = JvmIrCodegenFactory(context.environment.configuration)

    val psi2irInput =
        CodegenFactory.IrConversionInput.Companion.fromGenerationStateAndFiles(generationState, sourceFiles, analysisResult.bindingContext)
    val backendInput = codegenFactory.convertToIr(psi2irInput)

    codegenFactory.generateModule(generationState, backendInput)
    CodegenFactory.doCheckCancelled(generationState)
    generationState.factory.done()

    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
        diagnosticsReporter,
        messageCollector,
        context.environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    )

    if (messageCollector.hasErrors()) return failure(
        messageCollector
    )

    return makeCompiledScript(
        generationState,
        script,
        sourceFiles.first(),
        sourceDependencies,
        getScriptConfiguration,
        extractResultFields(backendInput.irModuleFragment)
    ).onSuccess { compiledScript ->
        ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
    }
}

private fun analyze(sourceFiles: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult {
    val messageCollector = environment.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

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
            NoScopeRecordCliBindingTrace(project),
            environment.configuration,
            environment::createPackagePartProvider
        )
    }
    return analyzerWithCompilerReport.analysisResult
}

@OptIn(LegacyK2CliPipeline::class)
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

    val configuration = context.environment.configuration

    val targetId = TargetId(
        configuration[CommonConfigurationKeys.MODULE_NAME] ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
        "java-production"
    )

    val renderDiagnosticName = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)

    val projectEnvironment = context.environment.toVfsBasedProjectEnvironment()
    val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter)

    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
    val incrementalCompilationScope = createIncrementalCompilationScope(
        configuration,
        projectEnvironment,
        incrementalExcludesScope = null
    )?.also { librariesScope -= it }

    val session = prepareJvmSessionsForScripting(
        projectEnvironment,
        configuration,
        sourceFiles,
        rootModuleNameAsString = targetId.name,
        friendPaths = emptyList(),
        librariesScope,
        isScript = { false },
        createProviderAndScopeForIncrementalCompilation = { files ->
            createContextForIncrementalCompilation(
                configuration,
                projectEnvironment,
                compilerEnvironment.projectEnvironment.getSearchScopeBySourceFiles(files.map { KtPsiSourceFile(it) }),
                emptyList(),
                incrementalCompilationScope
            )
        }
    ).single().session

    val scriptDefinitionProviderService = session.scriptDefinitionProviderService

    scriptDefinitionProviderService?.run {
        definitionProvider = ScriptDefinitionProvider.getInstance(context.environment.project)
        configurationProvider = ScriptConfigurationsProvider.getInstance(context.environment.project)
    }

    val rawFir = session.buildFirFromKtFiles(sourceFiles) //.reversed()

    val orderedRawFir =
        if (scriptDefinitionProviderService == null) rawFir
        else {
            val rawFirDeps = rawFir.associateWith { firFile ->
                ((firFile.sourceFile as? KtPsiSourceFile)?.psiFile as? KtFile)?.let { ktFile ->
                    val scriptCompilationConfiguration =
                        scriptDefinitionProviderService.configurationProvider?.getScriptConfiguration(ktFile)?.configuration
                    scriptCompilationConfiguration?.get(ScriptCompilationConfiguration.resolvedImportScripts)?.mapNotNull { depSource ->
                        (depSource as? VirtualFileScriptSource)?.virtualFile?.let { depVFile ->
                            rawFir.find { ((it.sourceFile as? KtPsiSourceFile)?.psiFile as? KtFile)?.virtualFile == depVFile }
                        }
                    }
                }.orEmpty()
            }

            class CycleDetected(val node: FirFile) : Throwable()

            try {
                topologicalSort(
                    rawFir, reportCycle = { throw CycleDetected(it) }
                ) {
                    rawFirDeps[this] ?: emptyList()
                }.reversed()
            } catch (e: CycleDetected) {
                return ResultWithDiagnostics.Failure(
                    ScriptDiagnostic(
                        ScriptDiagnostic.unspecifiedError,
                        "Unable to handle recursive script dependencies, cycle detected on file ${e.node.name}",
                        sourcePath = e.node.sourceFile?.path
                    )
                )
            }
        }

    val (scopeSession, fir) = session.runResolution(orderedRawFir)
    // checkers
    session.runCheckers(scopeSession, fir, diagnosticsReporter, MppCheckerKind.Common)
    session.runCheckers(scopeSession, fir, diagnosticsReporter, MppCheckerKind.Platform)

    val analysisResults = FirResult(listOf(ModuleCompilerAnalyzedOutput(session, scopeSession, fir)))

    if (diagnosticsReporter.hasErrors) {
        diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
        return failure(messageCollector)
    }

    val irInput = convertAnalyzedFirToIr(configuration, targetId, analysisResults, compilerEnvironment)

    val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment)

    diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)

    if (diagnosticsReporter.hasErrors) {
        return failure(messageCollector)
    }

    return makeCompiledScript(
        codegenOutput.generationState,
        script,
        sourceFiles.first(),
        sourceDependencies,
        getScriptConfiguration,
        extractResultFields(irInput.irModuleFragment)
    ).onSuccess { compiledScript ->
        ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun extractResultFields(irModule: IrModuleFragment): MutableMap<FqName, ScriptResultFieldData> {
    val resultFields = mutableMapOf<FqName, ScriptResultFieldData>()
    irModule.files.forEach { file ->
        file.declarations.forEach { declaration ->
            (declaration as? IrClass)?.scriptResultFieldDataAttr?.let {
                resultFields[declaration.kotlinFqName] = it
            }
        }
    }
    return resultFields
}

@LegacyK2CliPipeline
private fun prepareJvmSessionsForScripting(
    projectEnvironment: VfsBasedProjectEnvironment,
    configuration: CompilerConfiguration,
    files: List<KtFile>,
    rootModuleNameAsString: String,
    friendPaths: List<String>,
    librariesScope: AbstractProjectFileSearchScope,
    isScript: (KtFile) -> Boolean,
    createProviderAndScopeForIncrementalCompilation: (List<KtFile>) -> IncrementalCompilationContext?,
): List<SessionWithSources<KtFile>> {
    val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)
    return MinimizedFrontendContext(projectEnvironment, MessageCollector.NONE, extensionRegistrars, configuration).prepareJvmSessions(
        files, rootModuleNameAsString, friendPaths, librariesScope, isCommonSourceForPsi, isScript,
        fileBelongsToModuleForPsi, createProviderAndScopeForIncrementalCompilation
    )
}
