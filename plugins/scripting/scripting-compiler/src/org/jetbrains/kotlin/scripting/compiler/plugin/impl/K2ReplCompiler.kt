/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.ModuleCompilerEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertAnalyzedFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.generateCodeFromIr
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.FirModuleCapabilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirSharableJavaComponents
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.session.firCachesFactoryForCliMode
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplCompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.collectScriptsCompilationDependencies
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

class K2ReplCompiler(
    private val state: K2ReplCompilationState,
) : ReplCompiler<CompiledSnippet> {

    override val lastCompiledSnippet: LinkedSnippet<CompiledSnippet>?
        get() = state.lastCompiledSnippet

    override suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        snippets.forEach { snippet ->
            val res = compileImpl(state, snippet, configuration)
            when (res) {
                is ResultWithDiagnostics.Success -> {
                    state.lastCompiledSnippet = state.lastCompiledSnippet.add(res.value)
                }
                is ResultWithDiagnostics.Failure -> {
                    return res
                }
            }
        }
        return state.lastCompiledSnippet?.asSuccess() ?: ResultWithDiagnostics.Failure("No snippets provided".asErrorDiagnostics())
    }

    suspend fun compile(snippet: SourceCode): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> =
        compile(snippet, state.scriptCompilationConfiguration)

    companion object {

        fun createCompilationState(
            messageCollector: ScriptDiagnosticsMessageCollector,
            rootDisposable: Disposable,
            scriptCompilationConfiguration: ScriptCompilationConfiguration,
            hostConfiguration: ScriptingHostConfiguration =
                ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                    repl {
                        firReplHistoryProvider(FirReplHistoryProviderImpl())
                        isReplSnippetSource { sourceFile, scriptSource -> true }
                    }
                }
        ): K2ReplCompilationState {

            val moduleName = Name.special("<REPL>")
            val compilerContext = createIsolatedCompilationContext(
                scriptCompilationConfiguration,
                hostConfiguration,
                messageCollector,
                rootDisposable
            ) {
                add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, ReplCompilerPluginRegistrar(hostConfiguration))
            }
            val project = compilerContext.environment.project
            val languageVersionSettings = compilerContext.environment.configuration.languageVersionSettings
            val classpath = scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies].orEmpty().flatMap {
                (it as? JvmDependency)?.classpath ?: emptyList()
            }
            compilerContext.environment.updateClasspath(classpath.map { JvmClasspathRoot(it) })
            val projectEnvironment = VfsBasedProjectEnvironment(
                project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            ) { compilerContext.environment.createPackagePartProvider(it) }
            val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
            val projectFileSearchScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))
            val packagePartProvider = projectEnvironment.getPackagePartProvider(projectFileSearchScope)
            val predefinedJavaComponents = FirSharableJavaComponents(firCachesFactoryForCliMode)

            val sessionProvider = FirProjectSessionProvider()

            val moduleDataProvider = ReplModuleDataProvider(classpath.map(File::toPath))

            FirJvmSessionFactory.createLibrarySession(
                mainModuleName = moduleName,
                sessionProvider = sessionProvider,
                moduleDataProvider = moduleDataProvider,
                projectEnvironment = projectEnvironment,
                extensionRegistrars = extensionRegistrars,
                scope = projectFileSearchScope,
                packagePartProvider = packagePartProvider,
                languageVersionSettings = languageVersionSettings,
                predefinedJavaComponents = predefinedJavaComponents,
            )

            return K2ReplCompilationState(
                scriptCompilationConfiguration,
                hostConfiguration,
                predefinedJavaComponents,
                projectEnvironment,
                moduleDataProvider,
                sessionProvider,
                messageCollector,
                compilerContext,
                packagePartProvider
            )
        }
    }
}

class K2ReplCompilationState(
    internal val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    internal val hostConfiguration: ScriptingHostConfiguration,
    internal val predefinedJavaComponents: FirSharableJavaComponents,
    internal val projectEnvironment: VfsBasedProjectEnvironment,
    internal val moduleDataProvider: ReplModuleDataProvider,
    internal val sessionProvider: FirProjectSessionProvider,
    internal val messageCollector: ScriptDiagnosticsMessageCollector,
    internal val compilerContext: SharedScriptCompilationContext,
    internal val packagePartProvider: PackagePartProvider,
) {
    var lastCompiledSnippet: LinkedSnippetImpl<CompiledSnippet>? = null
}

class ReplModuleDataProvider(baseLibraryPaths: List<Path>) : ModuleDataProvider() {

    val baseDependenciesModuleData = makeLibraryModuleData(Name.special("<REPL-base>"))

    private fun makeLibraryModuleData(name: Name): FirModuleDataImpl = FirModuleDataImpl(
        name,
        dependencies = emptyList(),
        dependsOnDependencies = emptyList(),
        friendDependencies = emptyList(),
        platform,
        FirModuleCapabilities.Empty
    )

    val pathToModuleData: MutableMap<Path, FirModuleData> = mutableMapOf()
    val moduleDataHistory: MutableList<FirModuleData> = mutableListOf()

    init {
        baseLibraryPaths.associateTo(pathToModuleData) { it to baseDependenciesModuleData }
        moduleDataHistory.add(baseDependenciesModuleData)
    }

    override val platform: TargetPlatform
        get() = JvmPlatforms.unspecifiedJvmPlatform

    override val allModuleData: Collection<FirModuleData>
        get() = moduleDataHistory

    override fun getModuleData(path: Path?): FirModuleData? {
        val normalizedPath = path?.normalize()
        return if (normalizedPath != null) {
            pathToModuleData[normalizedPath]
        } else {
            null
        }
    }

    fun addNewLibraryModuleDataIfNeeded(libraryPaths: List<Path>): Pair<FirModuleData?, List<Path>> {
        val newLibraryPaths = libraryPaths.filter { it !in pathToModuleData }
        if (newLibraryPaths.isEmpty()) return null to emptyList()
        val newDependenciesModuleData = makeLibraryModuleData(Name.special("<REPL-lib-${moduleDataHistory.size + 1}>"))
        newLibraryPaths.associateTo(pathToModuleData) { it to newDependenciesModuleData }
        moduleDataHistory.add(newDependenciesModuleData)
        return newDependenciesModuleData to newLibraryPaths
    }

    fun addNewSnippetModuleData(name: Name): FirModuleData =
        FirModuleDataImpl(
            name,
            dependencies = moduleDataHistory.filter { it.dependencies.isEmpty() },
            dependsOnDependencies = emptyList(),
            friendDependencies = moduleDataHistory.filter { it.dependencies.isNotEmpty() },
            platform,
            FirModuleCapabilities.Empty
        ).also { moduleDataHistory.add(it) }
}

@OptIn(LegacyK2CliPipeline::class)
private fun compileImpl(
    state: K2ReplCompilationState,
    snippet: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration
): ResultWithDiagnostics<CompiledSnippet> {

    val initialScriptCompilationConfiguration = scriptCompilationConfiguration.refineBeforeParsing(snippet).valueOr {
        return it
    }
    val project = state.projectEnvironment.project
    val messageCollector = state.messageCollector
    val compilerConfiguration = state.compilerContext.environment.configuration
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
    val renderDiagnosticName = compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    val compilerEnvironment = ModuleCompilerEnvironment(state.projectEnvironment, diagnosticsReporter)
    val targetId = TargetId(snippet.name!!, "java-production")

    // PSI files
    val snippetKtFile =
        getScriptKtFile(snippet, initialScriptCompilationConfiguration, project, messageCollector).valueOr {
            return it
        }

    // configuration refinement with the additional sources collection
    val allSourceFiles = mutableListOf(snippetKtFile)
    val (classpath, newSources, sourceDependencies) =
        collectScriptsCompilationDependencies(
            compilerConfiguration,
            project,
            allSourceFiles,
            initialScriptCompilationConfiguration
        )
    allSourceFiles.addAll(newSources)

    var hasSyntaxErrors = false
    // PSI syntax errors reporting
    allSourceFiles.forEach {
        val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(it, messageCollector)
        if (syntaxErrorReport.isHasErrors && it == snippetKtFile && syntaxErrorReport.isAllErrorsAtEof) {
            messageCollector.report(ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code"))
        }
        hasSyntaxErrors = hasSyntaxErrors || syntaxErrorReport.isHasErrors
    }
    checkKotlinPackageUsageForPsi(compilerConfiguration, allSourceFiles, messageCollector)

    // Updating compiler options
    val configurationsProvider = ScriptConfigurationsProvider.getInstance(project)
    val baseCompilerOptions = state.scriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions]
    val updatedCompilerOptions = allSourceFiles.flatMapTo(mutableListOf<String>()) { file ->
        configurationsProvider?.getScriptConfiguration(file)?.configuration?.get(
            ScriptCompilationConfiguration.compilerOptions
        )?.takeIf { it != baseCompilerOptions } ?: emptyList()
    }
    if (updatedCompilerOptions.isNotEmpty()) {
        compilerConfiguration.updateWithCompilerOptions(
            updatedCompilerOptions,
            messageCollector,
            state.compilerContext.ignoredOptionsReportingState,
            true
        )
    }

    val (_, newClassPath) = state.moduleDataProvider.addNewLibraryModuleDataIfNeeded(classpath.map(File::toPath))

    if (newClassPath.isNotEmpty()) {
        state.compilerContext.environment.updateClasspath(newClassPath.map { JvmClasspathRoot(it.toFile()) })
    }

    val moduleData = state.moduleDataProvider.addNewSnippetModuleData(Name.special("<REPL-snippet-${snippet.name!!}>"))

    val session = FirJvmSessionFactory.createModuleBasedSession(
        moduleData,
        state.sessionProvider,
        AbstractProjectFileSearchScope.EMPTY,
        state.projectEnvironment,
        createIncrementalCompilationSymbolProviders = { null },
        FirExtensionRegistrar.getInstances(project),
        compilerConfiguration.languageVersionSettings,
        jvmTarget = JvmTarget.DEFAULT, // TODO: from script config
        lookupTracker = null,
        enumWhenTracker = null,
        importTracker = null,
        state.predefinedJavaComponents,
        needRegisterJavaElementFinder = true,
        init = {},
    )

    val rawFir = session.buildFirFromKtFiles(allSourceFiles)

    val (scopeSession, fir) = session.runResolution(rawFir)
    // checkers
    session.runCheckers(scopeSession, fir, diagnosticsReporter, MppCheckerKind.Common)
    session.runCheckers(scopeSession, fir, diagnosticsReporter, MppCheckerKind.Platform)

    val analysisResults = FirResult(listOf(ModuleCompilerAnalyzedOutput(session, scopeSession, fir)))

    if (diagnosticsReporter.hasErrors) {
        diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
        return failure(messageCollector)
    }

    val irInput = convertAnalyzedFirToIr(compilerConfiguration, targetId, analysisResults, compilerEnvironment)

    val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment)

    diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)

    if (diagnosticsReporter.hasErrors) {
        return failure(messageCollector)
    }

    return makeCompiledScript(
        codegenOutput.generationState,
        snippet,
        snippetKtFile,
        sourceDependencies,
        { ktFile ->
            configurationsProvider?.getScriptConfigurationResult(ktFile, initialScriptCompilationConfiguration)
                ?.valueOrNull()?.configuration ?: initialScriptCompilationConfiguration
        },
        extractResultFields(irInput.irModuleFragment)
    ).onSuccess { compiledScript ->
        ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
    }
}