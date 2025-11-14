/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.ModuleCompilerEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertAnalyzedFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.generateCodeFromIr
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirLibrarySessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.flattenAndFilterOwnProviders
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.registerLibrarySessionComponents
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
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
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
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
            // Messages from earlier snippets should not leak into the next snippet
            state.messageCollector.clear()
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
            val projectEnvironment = compilerContext.environment.toVfsBasedProjectEnvironment()
            val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
            val projectFileSearchScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))

            val moduleDataProvider = ReplModuleDataProvider(classpath.map(File::toPath))

            val sessionFactoryContext = FirJvmSessionFactory.Context(
                configuration = compilerContext.environment.configuration,
                projectEnvironment = projectEnvironment,
                librariesScope = projectFileSearchScope,
            )
            val sharedLibrarySession = FirJvmSessionFactory.createSharedLibrarySession(
                mainModuleName = moduleName,
                extensionRegistrars = extensionRegistrars,
                languageVersionSettings = languageVersionSettings,
                context = sessionFactoryContext,
            )

            FirJvmSessionFactory.createLibrarySession(
                sharedLibrarySession,
                moduleDataProvider = moduleDataProvider,
                extensionRegistrars = extensionRegistrars,
                languageVersionSettings = languageVersionSettings,
                context = sessionFactoryContext,
            )

            return K2ReplCompilationState(
                scriptCompilationConfiguration,
                hostConfiguration,
                projectEnvironment,
                moduleDataProvider,
                messageCollector,
                compilerContext,
                sharedLibrarySession,
                sessionFactoryContext,
            )
        }
    }
}

class K2ReplCompilationState(
    internal val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    internal val hostConfiguration: ScriptingHostConfiguration,
    internal val projectEnvironment: VfsBasedProjectEnvironment,
    internal val moduleDataProvider: ReplModuleDataProvider,
    internal val messageCollector: ScriptDiagnosticsMessageCollector,
    internal val compilerContext: SharedScriptCompilationContext,
    internal val sharedLibrarySession: FirSession,
    internal val sessionFactoryContext: FirJvmSessionFactory.Context,
) {
    var lastCompiledSnippet: LinkedSnippetImpl<CompiledSnippet>? = null
}

class ReplModuleDataProvider(baseLibraryPaths: List<Path>) : ModuleDataProvider() {

    val baseDependenciesModuleData = makeLibraryModuleData(Name.special("<REPL-base>"))

    private fun makeLibraryModuleData(name: Name): FirModuleData = FirBinaryDependenciesModuleData(name)

    val pathToModuleData: MutableMap<Path, FirModuleData> = mutableMapOf()
    val moduleDataHistory: MutableList<FirModuleData> = mutableListOf()

    init {
        baseLibraryPaths.associateTo(pathToModuleData) { it to baseDependenciesModuleData }
        moduleDataHistory.add(baseDependenciesModuleData)
    }

    override val allModuleData: Collection<FirModuleData>
        get() = moduleDataHistory

    override val regularDependenciesModuleData: FirModuleData
        get() = baseDependenciesModuleData

    override fun getModuleData(path: Path?): FirModuleData? {
        val normalizedPath = path?.normalize() ?: return null
        pathToModuleData[normalizedPath]?.let { return it }
        for ((libPath, moduleData) in pathToModuleData) {
            if (normalizedPath.startsWith(libPath)) return moduleData
        }
        return null
    }

    override fun getModuleDataPaths(moduleData: FirModuleData): Set<Path>? =
        pathToModuleData.entries.mapNotNullTo(mutableSetOf()) { if (it.value == moduleData) it.key else null }.takeIf { it.isNotEmpty() }

    fun addNewLibraryModuleDataIfNeeded(libraryPaths: List<Path>): Pair<FirModuleData?, List<Path>> {
        val newLibraryPaths = libraryPaths.filter { it !in pathToModuleData }
        if (newLibraryPaths.isEmpty()) return null to emptyList()
        val newDependenciesModuleData = makeLibraryModuleData(Name.special("<REPL-lib-${moduleDataHistory.size + 1}>"))
        newLibraryPaths.associateTo(pathToModuleData) { it to newDependenciesModuleData }
        moduleDataHistory.add(newDependenciesModuleData)
        return newDependenciesModuleData to newLibraryPaths
    }

    fun addNewSnippetModuleData(name: Name): FirModuleData =
        FirSourceModuleData(
            name,
            dependencies = moduleDataHistory.filter { it.dependencies.isEmpty() },
            dependsOnDependencies = emptyList(),
            friendDependencies = moduleDataHistory.filter { it.dependencies.isNotEmpty() },
            JvmPlatforms.defaultJvmPlatform,
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
    val compilerConfiguration = state.compilerContext.environment.configuration.copy().apply {
        jvmTarget = selectJvmTarget(scriptCompilationConfiguration, messageCollector)
    }
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

    val (libModuleData, newClassPath) = state.moduleDataProvider.addNewLibraryModuleDataIfNeeded(classpath.map(File::toPath))

    if (newClassPath.isNotEmpty()) {
        state.compilerContext.environment.updateClasspath(newClassPath.map { JvmClasspathRoot(it.toFile()) })
    }

    val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
    if (libModuleData != null) {
        createReplAdditionalLibrariesSession(libModuleData, state, extensionRegistrars, compilerConfiguration)
        KotlinJavaPsiFacade.getInstance(project).clearPackageCaches()
    }

    val moduleData = state.moduleDataProvider.addNewSnippetModuleData(Name.special("<REPL-snippet-${snippet.name!!}>"))

    val session = FirJvmSessionFactory.createSourceSession(
        moduleData,
        AbstractProjectFileSearchScope.EMPTY,
        createIncrementalCompilationSymbolProviders = { null },
        extensionRegistrars,
        compilerConfiguration,
        // TODO: from script config
        context = state.sessionFactoryContext,
        needRegisterJavaElementFinder = true,
        isForLeafHmppModule = false,
        init = {},
    )
    val rawFir = session.buildFirFromKtFiles(allSourceFiles)

    val (scopeSession, fir) = session.runResolution(rawFir)
    // checkers
    session.runCheckers(scopeSession, fir, diagnosticsReporter, MppCheckerKind.Common)
    session.runCheckers(scopeSession, fir, diagnosticsReporter, MppCheckerKind.Platform)

    val frontendOutput = AllModulesFrontendOutput(listOf(SingleModuleFrontendOutput(session, scopeSession, fir)))

    if (diagnosticsReporter.hasErrors) {
        diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
        return failure(messageCollector)
    }

    val irInput = convertAnalyzedFirToIr(compilerConfiguration, targetId, frontendOutput, compilerEnvironment)

    val generationState = generateCodeFromIr(irInput, compilerEnvironment)

    diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)

    if (diagnosticsReporter.hasErrors) {
        return failure(messageCollector)
    }

    return makeCompiledScript(
        generationState,
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

@OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
private fun createReplAdditionalLibrariesSession(
    libModuleData: FirModuleData,
    state: K2ReplCompilationState,
    extensionRegistrars: List<FirExtensionRegistrar>,
    compilerConfiguration: CompilerConfiguration,
) {
    FirCliSession(FirSession.Kind.Library).apply session@{
        libModuleData.bindSession(this@session)

        registerCliCompilerAndCommonComponents(compilerConfiguration.languageVersionSettings, false)
        registerLibrarySessionComponents(state.sessionFactoryContext)
        register(FirBuiltinSyntheticFunctionInterfaceProvider::class, state.sharedLibrarySession.syntheticFunctionInterfacesSymbolProvider)

        val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        register(FirKotlinScopeProvider::class, kotlinScopeProvider)

        FirSessionConfigurator(this).apply {
            for (extensionRegistrar in extensionRegistrars) {
                registerExtensions(extensionRegistrar.configure())
            }
        }.configure()
        registerCommonComponentsAfterExtensionsAreConfigured()

        val providers = buildList {
            val projectEnvironment = state.sessionFactoryContext.projectEnvironment
            val searchScope = state.moduleDataProvider.getModuleDataPaths(libModuleData)?.let { paths ->
                projectEnvironment.getSearchScopeByClassPath(paths)
            } ?: state.sessionFactoryContext.librariesScope
            val kotlinClassFinder1 = projectEnvironment.getKotlinClassFinder(searchScope)
            add(
                JvmClassFileBasedSymbolProvider(
                    this@session,
                    state.moduleDataProvider,
                    kotlinScopeProvider,
                    state.sessionFactoryContext.packagePartProviderForLibraries,
                    kotlinClassFinder1,
                    projectEnvironment.getFirJavaFacade(this@session, libModuleData, state.sessionFactoryContext.librariesScope),
                )
            )
        }
        register(
            StructuredProviders::class,
            StructuredProviders(
                sourceProviders = emptyList(),
                dependencyProviders = providers,
                sharedProvider = state.sharedLibrarySession.symbolProvider,
            )
        )

        val providersWithShared = providers + state.sharedLibrarySession.symbolProvider.flattenAndFilterOwnProviders()

        val symbolProvider = FirCachingCompositeSymbolProvider(this, providersWithShared)
        register(FirSymbolProvider::class, symbolProvider)
        register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
    }
}

// Find the appropriate jvm target for the compiler from the ScriptCompilationConfiguration.
// Since this can be configured in two places, we check if both places agree on the same value (if configured twice).
// If not, CompilerOptions takes precedence and a warning is reported. We treat CompilerOptions with a higher priority
// as we assume they are more likely to be under the user's control.
private fun selectJvmTarget(configuration: ScriptCompilationConfiguration, messageCollector: ScriptDiagnosticsMessageCollector): JvmTarget {
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
