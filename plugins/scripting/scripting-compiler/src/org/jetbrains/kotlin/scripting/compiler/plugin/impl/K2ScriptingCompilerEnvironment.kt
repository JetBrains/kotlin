/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.javaInterop
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.compiler.plugin.registerInProject
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.disableStandardScriptDefinition
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.fir.FirBinaryDependenciesModuleData
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirSharableJavaComponents
import org.jetbrains.kotlin.fir.session.firCachesFactoryForCliMode
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingK2CompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.*
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.JvmDependency

interface K2ScriptingCompilerEnvironment {
    val baseScriptCompilationConfiguration: ScriptCompilationConfiguration
    val hostConfiguration: ScriptingHostConfiguration
    val projectEnvironment: VfsBasedProjectEnvironment
    val moduleDataProvider: ScriptingModuleDataProvider
    val messageCollector: ScriptDiagnosticsMessageCollector
    val extensionRegistrars: List<FirExtensionRegistrar>
    val sharedLibrarySession: FirSession
    val dummySessionForAnnotationResolution: FirSession?
}

internal interface K2ScriptingCompilerEnvironmentInternal : K2ScriptingCompilerEnvironment {
    override var dummySessionForAnnotationResolution: FirSession?
    val predefinedJavaComponents: FirSharableJavaComponents
    val compilerContext: SharedScriptCompilationContext
    val packagePartProvider: PackagePartProvider
    val sessionFactoryContext: FirJvmSessionFactory.Context
    fun updateContext(configuration: CompilerConfiguration)
}

internal class K2ScriptingCompilerEnvironmentImpl(
    shared: ScriptingSharedState<ScriptingModuleDataProvider>,
    override val baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    override val messageCollector: ScriptDiagnosticsMessageCollector,
    override val compilerContext: SharedScriptCompilationContext,
) : K2ScriptingCompilerEnvironmentInternal {
    override val hostConfiguration: ScriptingHostConfiguration = shared.hostConfiguration
    override val moduleDataProvider: ScriptingModuleDataProvider = shared.moduleDataProvider
    override val projectEnvironment: VfsBasedProjectEnvironment = shared.librarySessions.projectEnvironment
    override val extensionRegistrars: List<FirExtensionRegistrar> = shared.librarySessions.extensionRegistrars
    override val sharedLibrarySession: FirSession = shared.librarySessions.sharedLibrarySession
    override var sessionFactoryContext: FirJvmSessionFactory.Context = shared.librarySessions.sessionFactoryContext
    override var dummySessionForAnnotationResolution: FirSession? = null
    override val predefinedJavaComponents: FirSharableJavaComponents = FirSharableJavaComponents(firCachesFactoryForCliMode)
    override val packagePartProvider: PackagePartProvider =
        projectEnvironment.getPackagePartProvider(sessionFactoryContext.librariesClasspath)

    override fun updateContext(configuration: CompilerConfiguration) {
        val previous = sessionFactoryContext
        sessionFactoryContext = FirJvmSessionFactory.Context(
            configuration = configuration,
            projectEnvironment = previous.projectEnvironment,
            librariesClasspath = previous.librariesClasspath,
            javaInterop = previous.javaInterop,
        )
    }
}

open class ScriptingModuleDataProvider(private val baseName: String, baseLibraryPaths: List<Path>) : ModuleDataProvider() {

    protected val baseDependenciesModuleData = makeLibraryModuleData(Name.special("<$baseName-base>"))

    private fun makeLibraryModuleData(name: Name): FirModuleData = FirBinaryDependenciesModuleData(name)

    protected val moduleDataWithFilters: MutableMap<FirModuleData, LibraryPathFilter> =
        mutableMapOf(baseDependenciesModuleData to LibraryPathFilter.LibraryList(baseLibraryPaths.toSet()))

    protected val moduleDataHistory: MutableList<FirModuleData> = mutableListOf(baseDependenciesModuleData)

    override val allModuleData: Collection<FirModuleData>
        get() = moduleDataHistory

    override val regularDependenciesModuleData: FirModuleData
        get() = baseDependenciesModuleData

    override fun getModuleDataPaths(moduleData: FirModuleData): Set<Path>? =
        (moduleDataWithFilters[moduleData] as? LibraryPathFilter.LibraryList)?.libs

    override fun getModuleData(path: Path?): FirModuleData? {
        val normalizedPath = path?.normalize()
        for ([moduleData, filter] in moduleDataWithFilters.entries) {
            if (filter.accepts(normalizedPath)) {
                return moduleData
            }
        }
        return null
    }

    fun addNewLibraryModuleDataIfNeeded(libraryPaths: List<Path>): Pair<FirModuleData?, List<Path>> {
        val newLibraryPaths = libraryPaths.filter { getModuleData(it) == null }
        if (newLibraryPaths.isEmpty()) return null to emptyList()
        val newDependenciesModuleData = makeLibraryModuleData(Name.special("<$baseName-lib-${moduleDataHistory.size + 1}>"))
        moduleDataWithFilters[newDependenciesModuleData] = LibraryPathFilter.LibraryList(newLibraryPaths.toSet())
        moduleDataHistory.add(newDependenciesModuleData)
        return newDependenciesModuleData to newLibraryPaths
    }

    /**
     * [isDummy] should be set to true for the module data that should be excluded from the history, e.g. in the session for annotation resolution
     */
    fun addNewScriptModuleData(name: Name, isDummy: Boolean = false): FirModuleData =
        FirSourceModuleData(
            name,
            dependencies = moduleDataHistory.filter { it.dependencies.isEmpty() }.asReversed(),
            dependsOnDependencies = emptyList(),
            friendDependencies = moduleDataHistory.filter { it.dependencies.isNotEmpty() },
            JvmPlatforms.defaultJvmPlatform,
        ).also { if (!isDummy) moduleDataHistory.add(it) }
}

fun createIsolatedCompilerState(
    messageCollector: ScriptDiagnosticsMessageCollector,
    rootDisposable: Disposable,
    baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    configureCompiler: CompilerConfiguration.() -> Unit = {},
): K2ScriptingCompilerEnvironment {
    val compilerContext = createIsolatedCompilationContext(
        baseScriptCompilationConfiguration,
        hostConfiguration,
        messageCollector,
        rootDisposable,
        configureCompiler
    )
    return createCompilerState(compilerContext, messageCollector, hostConfiguration)
}

fun createCompilerStateFromEnvironment(
    environment: KotlinCoreEnvironment,
    messageCollector: ScriptDiagnosticsMessageCollector,
    baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
): K2ScriptingCompilerEnvironment {
    val compilerContext = createCompilationContextFromEnvironment(baseScriptCompilationConfiguration, environment, messageCollector)
    return createCompilerState(compilerContext, messageCollector, hostConfiguration)
}

fun createCompilerState(
    compilerContext: SharedScriptCompilationContext,
    messageCollector: ScriptDiagnosticsMessageCollector,
    hostConfiguration: ScriptingHostConfiguration,
): K2ScriptingCompilerEnvironment {
    val compilerConfiguration = compilerContext.environment.configuration
    val moduleName = (compilerConfiguration.get(CommonConfigurationKeys.MODULE_NAME)?.let { Name.guessByFirstCharacter(it) }
        ?: Name.special("<script-module>"))
    val scriptCompilationConfiguration = compilerContext.baseScriptCompilationConfiguration
    val classpath = scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies].orEmpty().flatMap {
        (it as? JvmDependency)?.classpath ?: emptyList()
    }
    val shared = createScriptingSharedState(
        compilerContext, hostConfiguration, moduleName, classpath,
        registerPlugins = {
            val extensionStorage = CompilerPluginRegistrar.ExtensionStorage()
            with(ScriptingK2CompilerPluginRegistrar()) { extensionStorage.registerExtensions(compilerConfiguration) }
            extensionStorage.registerInProject(compilerContext.environment.project) {
                "Error on plugin registration: ${it.javaClass.name}"
            }
        },
    ) { ScriptingModuleDataProvider(moduleName.asStringStripSpecialMarkers(), it.map(File::toPath)) }

    return K2ScriptingCompilerEnvironmentImpl(shared, scriptCompilationConfiguration, messageCollector, compilerContext)
}

internal class ScriptingLibrarySessions(
    val projectEnvironment: VfsBasedProjectEnvironment,
    val extensionRegistrars: List<FirExtensionRegistrar>,
    val sessionFactoryContext: FirJvmSessionFactory.Context,
    val sharedLibrarySession: FirSession,
)

internal class ScriptingSharedState<P : ModuleDataProvider>(
    val hostConfiguration: ScriptingHostConfiguration,
    val moduleDataProvider: P,
    val librarySessions: ScriptingLibrarySessions,
)

/**
 * The resulting host configuration is also stored as the scripting host configuration of the compiler configuration.
 */
internal fun <P : ModuleDataProvider> createScriptingSharedState(
    compilerContext: SharedScriptCompilationContext,
    hostConfiguration: ScriptingHostConfiguration,
    moduleName: Name,
    classpath: List<File>,
    registerPlugins: () -> Unit = {},
    createModuleDataProvider: (List<File>) -> P,
): ScriptingSharedState<P> {
    val compilerConfiguration = compilerContext.environment.configuration
    val hostConfigurationWithProvider =
        compilerConfiguration.addScriptDefinitionAndConfigureHost(hostConfiguration, compilerContext.baseScriptCompilationConfiguration)
    compilerConfiguration.scriptingHostConfiguration = hostConfigurationWithProvider
    registerPlugins()
    val moduleDataProvider = createModuleDataProvider(classpath)
    val librarySessions = createScriptingLibrarySessions(compilerContext, moduleName, classpath, moduleDataProvider)
    return ScriptingSharedState(hostConfigurationWithProvider, moduleDataProvider, librarySessions)
}

internal fun CompilerConfiguration.addScriptDefinitionAndConfigureHost(
    hostConfiguration: ScriptingHostConfiguration,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
): ScriptingHostConfiguration {
    add(
        ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
        ScriptDefinition.FromConfigurations(hostConfiguration, scriptCompilationConfiguration, null)
    )
    val scriptDefinitionProvider = CliScriptDefinitionProvider(disableStandardScriptDefinition).also {
        it.setScriptDefinitionsSources(getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES))
        it.setScriptDefinitions(getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS))
    }
    return hostConfiguration.with {
        scriptCompilationConfigurationProvider(ScriptCompilationConfigurationProviderOverDefinitionProvider(scriptDefinitionProvider))
        scriptRefinedCompilationConfigurationsCache(ScriptRefinedCompilationConfigurationCacheImpl())
    }
}

/**
 * Should be called after the compiler plugins are registered, since the FIR extension registrars are taken from the configuration.
 */
internal fun createScriptingLibrarySessions(
    compilerContext: SharedScriptCompilationContext,
    moduleName: Name,
    classpath: List<File>,
    moduleDataProvider: ModuleDataProvider,
): ScriptingLibrarySessions {
    val compilerConfiguration = compilerContext.environment.configuration
    val languageVersionSettings = compilerConfiguration.languageVersionSettings
    compilerContext.environment.updateClasspath(classpath.map { JvmClasspathRoot(it) })
    val projectEnvironment = compilerContext.environment.toVfsBasedProjectEnvironment()
    val extensionRegistrars = compilerConfiguration.getCompilerExtensions(FirExtensionRegistrar)
    val sessionFactoryContext = FirJvmSessionFactory.Context(
        configuration = compilerConfiguration,
        projectEnvironment = projectEnvironment,
        librariesClasspath = JvmClasspath.ProjectLibraries(),
        javaInterop = projectEnvironment.javaInterop(compilerConfiguration, withJavaSources = false),
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
    return ScriptingLibrarySessions(projectEnvironment, extensionRegistrars, sessionFactoryContext, sharedLibrarySession)
}
