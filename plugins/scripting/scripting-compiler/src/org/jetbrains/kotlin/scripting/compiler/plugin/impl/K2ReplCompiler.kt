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
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.javaInterop
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
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
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplCompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.*
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.collectScriptsCompilationDependenciesRecursively
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with
import kotlin.script.experimental.impl._isSyntheticSnippet
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

class K2ReplCompiler(
    private val state: K2ReplCompilationState,
    private val convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile = SourceCode::convertToFirViaLightTree,
) : ReplCompiler<CompiledSnippet> {

    override val lastCompiledSnippet: LinkedSnippet<CompiledSnippet>?
        get() = state.lastCompiledSnippet

    override suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        snippets.forEach { mainSnippet ->
            val [updatedConfiguration, syntheticSnippets] = configuration.prependSyntheticSnippets(mainSnippet).valueOr { return it }
            val snippetsWithSynthetics = syntheticSnippets + mainSnippet
            snippetsWithSynthetics.forEach { snippet ->
                // Messages from earlier snippets should not leak into the next snippet
                state.messageCollector.clear()
                val res =
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
                when (res) {
                    is ResultWithDiagnostics.Success -> {
                        state.lastCompiledSnippet = state.lastCompiledSnippet.add(res.value)
                    }
                    is ResultWithDiagnostics.Failure -> {
                        return res
                    }
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
                        isReplSnippetSource { _, _ -> true }
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

            val compilerConfiguration = compilerContext.environment.configuration
            compilerConfiguration.add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromConfigurations(hostConfiguration, scriptCompilationConfiguration, null)
            )
            val definitionSources = compilerConfiguration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES)
            val definitions = compilerConfiguration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS)
            val scriptDefinitionProvider = CliScriptDefinitionProvider(
                compilerConfiguration.disableStandardScriptDefinition
            ).also {
                it.setScriptDefinitionsSources(definitionSources)
                it.setScriptDefinitions(definitions)
            }
            val hostConfigurationWithProvider = hostConfiguration.with {
                scriptCompilationConfigurationProvider(ScriptCompilationConfigurationProviderOverDefinitionProvider(scriptDefinitionProvider))
                scriptRefinedCompilationConfigurationsCache(ScriptRefinedCompilationConfigurationCacheImpl())
            }
            val project = compilerContext.environment.project
            val languageVersionSettings = compilerContext.environment.configuration.languageVersionSettings
            val classpath = scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies].orEmpty().flatMap {
                when (it) {
                    is JvmDependency -> it.classpath
                    // JvmDependencyFromClassLoader (for example when
                    // `kotlin.jsr223.experimental.resolve.dependencies.from.context.classloader=true`)
                    // is honored in K1 via PackageFragmentFromClassLoaderProviderExtension. K2 FIR does
                    // not use that extension point, so eagerly extract the classpath from the classloader.
                    // This drops the K1 laziness for K2, but lets stdlib (HashMap, etc.) resolve in FIR.
                    is JvmDependencyFromClassLoader -> scriptCompilationClasspathFromContext(
                        classLoader = it.getClassLoader(scriptCompilationConfiguration),
                        wholeClasspath = true,
                        unpackJarCollections = true,
                    )
                    else -> emptyList()
                }
            }
            compilerContext.environment.updateClasspath(classpath.map { JvmClasspathRoot(it) })
            val projectEnvironment = compilerContext.environment.toVfsBasedProjectEnvironment()
            val extensionRegistrars = compilerContext.environment.configuration.getCompilerExtensions(FirExtensionRegistrar)
            val librariesClasspath = JvmClasspath.ProjectLibraries()

            val moduleDataProvider = ReplModuleDataProvider(classpath.map(File::toPath))

            val sessionFactoryContext = FirJvmSessionFactory.Context(
                configuration = compilerContext.environment.configuration,
                projectEnvironment = projectEnvironment,
                librariesClasspath = librariesClasspath,
                javaInterop = projectEnvironment.javaInterop(compilerContext.environment.configuration, withJavaSources = false),
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
                hostConfigurationWithProvider,
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

    /**
     * Stripped down FirSession needed to resolve annotations with their arguments before starting the actual snippet compilation.
     */
    internal var dummySessionForAnnotationResolution: FirSession? = null

    val project get() = projectEnvironment.project
}

class ReplModuleDataProvider(baseLibraryPaths: List<Path>) : ModuleDataProvider() {

    val baseDependenciesModuleData = makeLibraryModuleData(Name.special("<REPL-base>"))

    private fun makeLibraryModuleData(name: Name): FirModuleData = FirBinaryDependenciesModuleData(name)

    val pathToModuleData: MutableMap<Path, FirModuleData> = mutableMapOf()
    val moduleDataHistory: MutableList<FirModuleData> = mutableListOf()

    init {
        baseLibraryPaths.map { it.toAbsolutePath().normalize() }.associateWithTo(pathToModuleData) { baseDependenciesModuleData }
        moduleDataHistory.add(baseDependenciesModuleData)
    }

    override val allModuleData: Collection<FirModuleData>
        get() = moduleDataHistory

    override val regularDependenciesModuleData: FirModuleData
        get() = baseDependenciesModuleData

    override fun getModuleData(path: Path?): FirModuleData? {
        val normalizedPath = path?.toAbsolutePath()?.normalize() ?: return null
        pathToModuleData[normalizedPath]?.let { return it }
        for ([libPath, moduleData] in pathToModuleData) {
            if (normalizedPath.startsWith(libPath)) return moduleData
        }
        return null
    }

    override fun getModuleDataPaths(moduleData: FirModuleData): Set<Path>? =
        pathToModuleData.entries.mapNotNullTo(mutableSetOf()) { if (it.value == moduleData) it.key else null }.takeIf { it.isNotEmpty() }

    fun addNewLibraryModuleDataIfNeeded(libraryPaths: List<Path>): Pair<FirModuleData?, List<Path>> {
        val newLibraryPaths = libraryPaths.map { it.toAbsolutePath().normalize() }.filter { it !in pathToModuleData }
        if (newLibraryPaths.isEmpty()) return null to emptyList()
        val newDependenciesModuleData = makeLibraryModuleData(Name.special("<REPL-lib-${moduleDataHistory.size + 1}>"))
        newLibraryPaths.associateWithTo(pathToModuleData) { newDependenciesModuleData }
        moduleDataHistory.add(newDependenciesModuleData)
        return newDependenciesModuleData to newLibraryPaths
    }

    fun addNewSnippetModuleData(name: Name, isDummy: Boolean = false): FirModuleData =
        FirSourceModuleData(
            name,
            dependencies = moduleDataHistory.filter { it.dependencies.isEmpty() },
            dependsOnDependencies = emptyList(),
            friendDependencies = moduleDataHistory.filter { it.dependencies.isNotEmpty() },
            JvmPlatforms.defaultJvmPlatform,
        ).also { if (!isDummy) moduleDataHistory.add(it) }
}

@OptIn(LegacyK2CliPipeline::class, DirectDeclarationsAccess::class)
private fun compileImpl(
    state: K2ReplCompilationState,
    snippet: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile,
): ResultWithDiagnostics<CompiledSnippet> {
    val priority = state.scriptCompilationConfiguration[ScriptCompilationConfiguration.repl.currentSnippetNo]
        ?: state.hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]?.getSnippetCount()

    val initialScriptCompilationConfiguration =
        if (priority == null) scriptCompilationConfiguration
        else scriptCompilationConfiguration.with { repl.currentSnippetNo(priority) }
    val project = state.projectEnvironment.project
    val messageCollector = state.messageCollector
    val compilerConfiguration = state.compilerContext.environment.configuration.copy().apply {
        jvmTarget = selectJvmTarget(scriptCompilationConfiguration, messageCollector)
    }
    val diagnosticsReporter = DiagnosticsCollectorImpl()
    val renderDiagnosticName = compilerConfiguration.renderDiagnosticInternalName
    val compilerEnvironment = ModuleCompilerEnvironment(state.projectEnvironment, diagnosticsReporter)
    val targetId = TargetId(snippet.name!!, "java-production")

    fun ScriptCompilationConfiguration.refineAll(source: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        refineAllViaFir(source, state.hostConfiguration, { _, _ -> state.getOrCreateSessionForAnnotationResolution() }, convertToFir)

    // The annotation-based refinement converts the snippet and fails on its syntax errors before the check below,
    // so the incomplete snippet should be recognized here as well.
    var snippetRefinementRawFir: Pair<BaseDiagnosticsCollector, KtSourceFile?>? = null
    val refinedSnippetConfiguration = initialScriptCompilationConfiguration.refineAllViaFir(
        snippet, state.hostConfiguration, { _, _ -> state.getOrCreateSessionForAnnotationResolution() }
    ) { session, collector ->
        convertToFir(session, collector).also { snippetRefinementRawFir = collector to it.sourceFile }
    }.valueOr { failure ->
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

    // Updating compiler options
    val baseCompilerOptions = state.scriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions]
    val updatedCompilerOptions = allSourceFiles.flatMapTo(mutableListOf()) { file ->
        getRefinedConfiguration(file)[ScriptCompilationConfiguration.compilerOptions]?.takeIf { it != baseCompilerOptions } ?: emptyList()
    }
    if (updatedCompilerOptions.isNotEmpty()) {
        compilerConfiguration.updateWithCompilerOptions(
            updatedCompilerOptions,
            messageCollector,
            state.compilerContext.ignoredOptionsReportingState,
            true
        )
    }

    val [libModuleData, newClassPath] = state.moduleDataProvider.addNewLibraryModuleDataIfNeeded(classpath.map(File::toPath))

    if (newClassPath.isNotEmpty()) {
        state.compilerContext.environment.updateClasspath(newClassPath.map { JvmClasspathRoot(it.toFile()) })
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

private fun K2ReplCompilationState.getOrCreateSessionForAnnotationResolution(): FirSession =
    dummySessionForAnnotationResolution ?: run {
        val compilerConfiguration = compilerContext.environment.configuration
        createScriptSourceSession(
            moduleDataProvider.addNewSnippetModuleData(Name.special("<raw-snippet>"), isDummy = true),
            compilerConfiguration.getCompilerExtensions(FirExtensionRegistrar),
            compilerConfiguration,
            sessionFactoryContext,
            hostConfiguration,
        ).also { dummySessionForAnnotationResolution = it }
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
