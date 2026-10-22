/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirBinaryDependenciesModuleData
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.CompiledSnippet
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.util.LinkedSnippetImpl

class ReplCompilerState internal constructor(
    baseScriptCompilationConfiguration: ScriptCompilationConfiguration,
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    ignoredOptionsReportingState: IgnoredOptionsReportingState,
    messageCollector: ScriptDiagnosticsMessageCollector,
    hostConfiguration: ScriptingHostConfiguration,
) : AbstractScriptingCompilerState<ReplModuleDataProvider>(
    baseScriptCompilationConfiguration,
    compilerConfiguration,
    projectEnvironment,
    ignoredOptionsReportingState,
    messageCollector,
    hostConfiguration,
    Name.special("<REPL>"),
    { ReplModuleDataProvider(it.map(File::toPath)) },
) {
    var lastCompiledSnippet: LinkedSnippetImpl<CompiledSnippet>? = null

    /**
     * Failed snippets consume their numbers too, matching the hosts that count snippets themselves (e.g. JSR-223).
     */
    var nextSnippetNo: Int = this.hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]?.getSnippetCount() ?: 0

    internal class HistoryCheckpoint(val snippets: Set<FirReplSnippetSymbol>, val moduleDataCount: Int)

    internal fun makeHistoryCheckpoint(): HistoryCheckpoint = HistoryCheckpoint(
        hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]?.getSnippets()?.toSet().orEmpty(),
        moduleDataProvider.moduleDataHistory.size
    )

    /**
     * The declarations of a failed snippet and its imported scripts must not be visible to the later snippets.
     */
    internal fun rollbackHistory(checkpoint: HistoryCheckpoint) {
        hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider]?.let { historyProvider ->
            historyProvider.removeSnippets(historyProvider.getSnippets().filter { it !in checkpoint.snippets })
        }
        moduleDataProvider.removeSnippetModuleDataAddedAfter(checkpoint.moduleDataCount)
    }

    override fun createModuleDataForAnnotationResolution(): FirModuleData =
        moduleDataProvider.addNewSnippetModuleData(Name.special("<raw-snippet>"), isDummy = true)
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

    /**
     * Library module data stay, since the environment classpath is already updated with their paths.
     */
    fun removeSnippetModuleDataAddedAfter(moduleDataCount: Int) {
        if (moduleDataHistory.size <= moduleDataCount) return
        val added = moduleDataHistory.subList(moduleDataCount, moduleDataHistory.size)
        added.removeAll { it is FirSourceModuleData }
    }
}
