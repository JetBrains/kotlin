/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirBinaryDependenciesModuleData
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty

class ScriptCompilerState internal constructor(
    baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    ignoredOptionsReportingState: IgnoredOptionsReportingState,
    messageCollector: ScriptDiagnosticsMessageCollector,
    hostConfiguration: ScriptingHostConfiguration,
) : AbstractScriptingCompilerState<ScriptingModuleDataProvider>(
    baseScriptCompilationConfiguration,
    compilerConfiguration,
    projectEnvironment,
    ignoredOptionsReportingState,
    messageCollector,
    hostConfiguration,
    compilerConfiguration.scriptModuleName,
    { ScriptingModuleDataProvider(compilerConfiguration.scriptModuleName.asStringStripSpecialMarkers(), it.map(File::toPath)) },
) {
    override fun createModuleDataForAnnotationResolution(): FirModuleData =
        moduleDataProvider.addNewScriptModuleData(Name.special("<raw-script>"), isDummy = true)

    internal fun getOrCreateSessionForAnnotationResolution(scriptCompilationConfiguration: ScriptCompilationConfiguration): FirSession {
        val dependencies = scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies].toClassPathOrEmpty()
        if (dependencies.isNotEmpty()) {
            configureLibrarySessionIfNeeded(this, compilerConfiguration, dependencies)
        }
        return sessionForAnnotationResolution
    }
}

private val CompilerConfiguration.scriptModuleName: Name
    get() = get(CommonConfigurationKeys.MODULE_NAME)?.let { Name.guessByFirstCharacter(it) } ?: Name.special("<script-module>")

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
): ScriptCompilerState =
    createIsolatedScriptingState(
        baseScriptCompilationConfiguration, hostConfiguration, messageCollector, rootDisposable, configureCompiler
    ) { initialConfiguration, compilerConfiguration, projectEnvironment, ignoredOptionsReportingState ->
        ScriptCompilerState(
            initialConfiguration,
            compilerConfiguration,
            projectEnvironment,
            ignoredOptionsReportingState,
            messageCollector,
            hostConfiguration,
        )
    }

/**
 * The scripting compiler plugin should be already registered in the [environment].
 */
fun createCompilerStateFromEnvironment(
    environment: KotlinCoreEnvironment,
    messageCollector: ScriptDiagnosticsMessageCollector,
    baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
): ScriptCompilerState {
    val ignoredOptionsReportingState = IgnoredOptionsReportingState()
    val initialConfiguration = environment.configuration.applyScriptCompilationConfiguration(
        baseScriptCompilationConfiguration, messageCollector, ignoredOptionsReportingState
    )
    return ScriptCompilerState(
        initialConfiguration,
        environment.configuration,
        environment.toVfsBasedProjectEnvironment(),
        ignoredOptionsReportingState,
        messageCollector,
        hostConfiguration,
    )
}
