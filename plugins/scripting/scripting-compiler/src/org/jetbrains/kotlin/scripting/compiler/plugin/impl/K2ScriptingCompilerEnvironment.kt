/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.registerInProject
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.fir.FirBinaryDependenciesModuleData
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirSharableJavaComponents
import org.jetbrains.kotlin.fir.session.firCachesFactoryForCliMode
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingK2CompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.JvmDependency

interface K2ScriptingCompilerEnvironment {
    val baseScriptCompilationConfiguration: ScriptCompilationConfiguration
    val hostConfiguration: ScriptingHostConfiguration
    val projectEnvironment: VfsBasedProjectEnvironment
    val moduleDataProvider: ScriptingModuleDataProvider
    val messageCollector: ScriptDiagnosticsMessageCollector
    val sharedLibrarySession: FirSession
}

internal interface K2ScriptingCompilerEnvironmentInternal : K2ScriptingCompilerEnvironment {
    val predefinedJavaComponents: FirSharableJavaComponents
    val compilerContext: SharedScriptCompilationContext
    val packagePartProvider: PackagePartProvider
    val sessionFactoryContext: FirJvmSessionFactory.Context
}

internal open class K2ScriptingCompilerEnvironmentImpl(
    override val baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    override val hostConfiguration: ScriptingHostConfiguration,
    override val predefinedJavaComponents: FirSharableJavaComponents,
    override val projectEnvironment: VfsBasedProjectEnvironment,
    override val moduleDataProvider: ScriptingModuleDataProvider,
    override val messageCollector: ScriptDiagnosticsMessageCollector,
    override val compilerContext: SharedScriptCompilationContext,
    override val packagePartProvider: PackagePartProvider,
    override val sharedLibrarySession: FirSession,
    override val sessionFactoryContext: FirJvmSessionFactory.Context
) : K2ScriptingCompilerEnvironmentInternal

open class ScriptingModuleDataProvider(private val baseName: String, baseLibraryPaths: List<Path>) : ModuleDataProvider() {

    protected val baseDependenciesModuleData = makeLibraryModuleData(Name.special("<$baseName-base>"))

    private fun makeLibraryModuleData(name: Name): FirModuleData = FirBinaryDependenciesModuleData(name)

    protected val pathToModuleData: MutableMap<Path, FirModuleData> = mutableMapOf()
    protected val moduleDataHistory: MutableList<FirModuleData> = mutableListOf()

    init {
        baseLibraryPaths.associateWithTo(pathToModuleData) { baseDependenciesModuleData }
        moduleDataHistory.add(baseDependenciesModuleData)
    }

    override val allModuleData: Collection<FirModuleData>
        get() = moduleDataHistory

    override val regularDependenciesModuleData: FirModuleData
        get() = baseDependenciesModuleData

    override fun getModuleDataPaths(moduleData: FirModuleData): Set<Path>? =
        pathToModuleData.entries.mapNotNullTo(mutableSetOf()) { if (it.value == moduleData) it.key else null }.takeIf { it.isNotEmpty() }

    override fun getModuleData(path: Path?): FirModuleData? {
        val normalizedPath = path?.normalize() ?: return null
        pathToModuleData[normalizedPath]?.let { return it }
        for ((libPath, moduleData) in pathToModuleData) {
            if (normalizedPath.startsWith(libPath)) return moduleData
        }
        return null
    }

    fun addNewLibraryModuleDataIfNeeded(libraryPaths: List<Path>): Pair<FirModuleData?, List<Path>> {
        val newLibraryPaths = libraryPaths.filter { it !in pathToModuleData }
        if (newLibraryPaths.isEmpty()) return null to emptyList()
        val newDependenciesModuleData = makeLibraryModuleData(Name.special("<$baseName-lib-${moduleDataHistory.size + 1}>"))
        newLibraryPaths.associateWithTo(pathToModuleData) { newDependenciesModuleData }
        moduleDataHistory.add(newDependenciesModuleData)
        return newDependenciesModuleData to newLibraryPaths
    }

    fun addNewScriptModuleData(name: Name): FirModuleData =
        FirSourceModuleData(
            name,
            dependencies = moduleDataHistory.filter { it.dependencies.isEmpty() }.asReversed(),
            dependsOnDependencies = emptyList(),
            friendDependencies = moduleDataHistory.filter { it.dependencies.isNotEmpty() },
            JvmPlatforms.defaultJvmPlatform,
        ).also { moduleDataHistory.add(it) }
}

fun createCompilerState(
    moduleName: Name,
    messageCollector: ScriptDiagnosticsMessageCollector,
    rootDisposable: Disposable,
    baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    configureCompiler: CompilerConfiguration.() -> Unit = {},
    createModuleDataProvider: (List<Path>) -> ScriptingModuleDataProvider =
        { ScriptingModuleDataProvider(moduleName.asStringStripSpecialMarkers(), it) },
): K2ScriptingCompilerEnvironment {

    val compilerContext = createIsolatedCompilationContext(
        baseScriptCompilationConfiguration,
        hostConfiguration,
        messageCollector,
        rootDisposable,
        configureCompiler
    )
    val project = compilerContext.environment.project
    val compilerConfiguration = compilerContext.environment.configuration
    val languageVersionSettings = compilerConfiguration.languageVersionSettings

    val extensionStorage = CompilerPluginRegistrar.ExtensionStorage()

    val scriptCompilationConfiguration = compilerContext.baseScriptCompilationConfiguration
    compilerConfiguration.scriptingHostConfiguration = hostConfiguration
    compilerConfiguration.add(
        ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
        ScriptDefinition.FromConfigurations(hostConfiguration, scriptCompilationConfiguration, null)
    )
    with(ScriptingK2CompilerPluginRegistrar()) { extensionStorage.registerExtensions(compilerConfiguration) }
    extensionStorage.registerInProject(project) { "Error on plugin registration: ${it.javaClass.name}" }

    val classpath = scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies].orEmpty().flatMap {
        (it as? JvmDependency)?.classpath ?: emptyList()
    }
    compilerContext.environment.updateClasspath(classpath.map { JvmClasspathRoot(it) })
    val projectEnvironment = compilerContext.environment.toVfsBasedProjectEnvironment()
    val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
    val projectFileSearchScope = PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))
    val packagePartProvider = projectEnvironment.getPackagePartProvider(projectFileSearchScope)
    val predefinedJavaComponents = FirSharableJavaComponents(firCachesFactoryForCliMode)

    val moduleDataProvider = createModuleDataProvider(classpath.map(File::toPath))

    val sessionFactoryContext = FirJvmSessionFactory.Context(
        configuration = compilerConfiguration,
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

    return K2ScriptingCompilerEnvironmentImpl(
        scriptCompilationConfiguration,
        hostConfiguration,
        predefinedJavaComponents,
        projectEnvironment,
        moduleDataProvider,
        messageCollector,
        compilerContext,
        packagePartProvider,
        sharedLibrarySession,
        sessionFactoryContext,
    )
}
