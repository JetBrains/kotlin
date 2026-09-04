/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.KmpModuleKind
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplCompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.*
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.collectScriptsCompilationDependenciesRecursively
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.FirScriptCompilationComponent
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlin.scripting.compiler.plugin.services.putImportedSnippetOrSnippet
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

/**
 * @param convertToFir the parser seam: builds raw FIR for every [SourceCode] taking part in a snippet compilation
 * (the snippet itself and its imports). LightTree by default; a PSI-based converter can be injected by embedders that
 * need PSI-backed sources, mirroring [ScriptJvmK2CompilerImpl].
 */
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

            // Snippet sources are named `.repl.<fileExtension>`, so this definition is found by the
            // standard `isScript` extension check
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
            // Passed via FirScriptCompilationComponent: FirScriptDefinitionProviderService prefers a
            // session's own hostConfiguration over its classpath-discovery-based fallback

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

    // Session used for the K2 configuration refinement (file annotations collection), see `getOrCreateSessionForAnnotationResolution`
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

    /**
     * When [isDummy] is true, the module data is excluded from the history (for example the session used
     * for annotation resolution).
     */
    fun addNewSnippetModuleData(name: Name, isDummy: Boolean = false): FirModuleData =
        FirSourceModuleData(
            name,
            dependencies = moduleDataHistory.filter { it.dependencies.isEmpty() },
            dependsOnDependencies = emptyList(),
            friendDependencies = moduleDataHistory.filter { it.dependencies.isNotEmpty() },
            JvmPlatforms.defaultJvmPlatform,
        ).also { if (!isDummy) moduleDataHistory.add(it) }
}

@OptIn(LegacyK2CliPipeline::class, SessionConfiguration::class, DirectDeclarationsAccess::class)
private fun compileImpl(
    state: K2ReplCompilationState,
    snippet: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile,
): ResultWithDiagnostics<CompiledSnippet> {
    // TODO: ensure that currentLineId passing is only used for single snippet compilation
    val priority = state.scriptCompilationConfiguration[ScriptCompilationConfiguration.repl.currentLineId]?.no
        ?: state.hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]?.getSnippetCount()

    // The snippet id travels to the FIR configurators via the refined configuration (`repl.currentLineId`),
    // so the result field naming does not depend on the parser used to build the snippet
    val initialScriptCompilationConfiguration =
        if (priority == null) scriptCompilationConfiguration
        else scriptCompilationConfiguration.with { repl.currentLineId(LineId(priority, 0, snippet.text.hashCode())) }
    val project = state.projectEnvironment.project
    val messageCollector = state.messageCollector
    val compilerConfiguration = state.compilerContext.environment.configuration.copy().apply {
        jvmTarget = selectJvmTarget(scriptCompilationConfiguration, messageCollector)
    }
    val diagnosticsReporter = DiagnosticsCollectorImpl()
    val renderDiagnosticName = compilerConfiguration.renderDiagnosticInternalName
    val compilerEnvironment = ModuleCompilerEnvironment(state.projectEnvironment, diagnosticsReporter)
    val targetId = TargetId(snippet.name!!, "java-production")

    // K2 (FIR-based) configuration refinement, the same as in ScriptJvmK2CompilerImpl: the file annotations are collected
    // from the raw FIR built by `convertToFir` in a dedicated session, without the legacy PSI-based ScriptConfigurationsProvider
    fun ScriptCompilationConfiguration.refineAll(source: SourceCode): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        refineAllForK2(source, state.hostConfiguration) { script, configuration ->
            collectAndResolveScriptAnnotationsViaFir(
                script, configuration, state.hostConfiguration,
                { _, _ -> state.getOrCreateSessionForAnnotationResolution() },
                convertToFir
            )
        }

    val refinedSnippetConfiguration = initialScriptCompilationConfiguration.refineAll(snippet).valueOr { return it }

    // The refined configurations cache is used by the FIR scripting services (see FirScriptDefinitionProviderService),
    // so the snippet configurators see the per-snippet configuration (incl. `repl.currentLineId`)
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
    // The imported scripts should be analyzed before the snippet
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
        val projectEnvironment = state.sessionFactoryContext.projectEnvironment
        val searchScope = state.moduleDataProvider.getModuleDataPaths(libModuleData)?.let { paths ->
            projectEnvironment.getSearchScopeByClassPath(paths)
        } ?: state.sessionFactoryContext.librariesScope

        createScriptingAdditionalLibrariesSession(
            libModuleData,
            state.sessionFactoryContext,
            state.moduleDataProvider,
            state.sharedLibrarySession,
            extensionRegistrars,
            compilerConfiguration,
            getKotlinClassFinder = { projectEnvironment.getKotlinClassFinder(searchScope) },
            getJavaFacade = { projectEnvironment.getFirJavaFacade(it, libModuleData, state.sessionFactoryContext.librariesScope) }
        )
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
        kmpModuleKind = KmpModuleKind.SingleModule,
        init = {},
    )

    session.register(
        FirScriptCompilationComponent::class,
        FirScriptCompilationComponent(
            state.hostConfiguration,
            getSessionForAnnotationResolution = { _, _ -> state.getOrCreateSessionForAnnotationResolution() }
        )
    )

    val sourcesToFir = allSourceFiles.associateWith { it.convertToFir(session, diagnosticsReporter) }
    val rawFir = sourcesToFir.values.toList()

    // syntax errors reporting
    if (diagnosticsReporter.hasErrors) {
        if (diagnosticsReporter.isIncompleteSnippet(snippet)) {
            messageCollector.report(ScriptDiagnostic(ScriptDiagnostic.incompleteCode, "Incomplete code"))
        }
        diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
        return failure(messageCollector)
    }

    // The imported scripts are compiled as snippets preceding the current one (see `isReplSnippetSource` above): they are registered
    // in the history before the resolution, in the dependency order, so their declarations are visible to the snippets that follow
    // (the history is only updated after the body resolution otherwise, which is too late for the same-session snippets).
    // They do not consume the snippet numbers (see FirReplHistoryProviderWithImports).
    state.hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]?.let { historyProvider ->
        for (importedSource in newSources) {
            val importedSnippet = sourcesToFir[importedSource]?.declarations?.firstIsInstanceOrNull<FirReplSnippet>() ?: continue
            historyProvider.putImportedSnippetOrSnippet(importedSnippet.symbol)
        }
    }

    val [scopeSession, fir] = session.runResolution(rawFir)
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

/**
 * The session used for the K2 configuration refinement (see [collectAndResolveScriptAnnotationsViaFir]): the snippet is
 * built into raw FIR there only to collect and evaluate its file annotations, so the module data is not added to the REPL history.
 */
@OptIn(SessionConfiguration::class)
private fun K2ReplCompilationState.getOrCreateSessionForAnnotationResolution(): FirSession =
    dummySessionForAnnotationResolution ?: run {
        val compilerConfiguration = compilerContext.environment.configuration
        FirJvmSessionFactory.createSourceSession(
            moduleDataProvider.addNewSnippetModuleData(Name.special("<raw-snippet>"), isDummy = true),
            AbstractProjectFileSearchScope.EMPTY,
            createIncrementalCompilationSymbolProviders = { null },
            compilerConfiguration.getCompilerExtensions(FirExtensionRegistrar),
            compilerConfiguration,
            context = sessionFactoryContext,
            needRegisterJavaElementFinder = true,
            kmpModuleKind = KmpModuleKind.SingleModule,
            init = {},
        ).apply {
            register(
                FirScriptCompilationComponent::class,
                FirScriptCompilationComponent(hostConfiguration, getSessionForAnnotationResolution = { _, _ -> this })
            )
            dummySessionForAnnotationResolution = this
        }
    }

/**
 * The LightTree counterpart of the PSI "all syntax errors are at EOF" heuristic used to signal to REPL hosts
 * that the snippet is incomplete rather than wrong: all errors reported for the [snippet] are syntax errors
 * located at the end of its text.
 */
private fun BaseDiagnosticsCollector.isIncompleteSnippet(snippet: SourceCode): Boolean {
    val snippetDiagnostics = diagnosticsByFile.entries
        .filter { entry -> entry.key?.let { it.path == snippet.locationId || it.name == snippet.name } == true }
        .flatMap { it.value }
        .filter { it.severity == Severity.ERROR }
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
