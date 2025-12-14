/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.dependencies

import com.intellij.openapi.vfs.originalFile
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.resolvedImportScripts
import org.jetbrains.kotlin.utils.topologicalSort
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty

data class ScriptsCompilationDependencies(
    val classpath: List<File>,
    val sources: List<SourceCode>,
    val sourceDependencies: List<SourceDependencies>
) {
    data class SourceDependencies(
        val script: SourceCode,
        val sourceDependencies: ResultWithDiagnostics<List<SourceCode>>
    )
}

// recursively collect dependencies from initial and imported scripts
@Deprecated("Use collectScriptsCompilationDependenciesRecursively instead")
fun collectScriptsCompilationDependencies(
    initialSources: Iterable<SourceCode>,
    @Suppress("DEPRECATION")
    getScriptCompilationConfiguration: (SourceCode) -> org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult?
): ScriptsCompilationDependencies {
    val collectedClassPath = ArrayList<File>()
    val collectedSources = ArrayList<SourceCode>()
    val collectedSourceDependencies = ArrayList<ScriptsCompilationDependencies.SourceDependencies>()
    var remainingSources = initialSources
    val knownSourcePaths = initialSources.mapNotNullTo(HashSet()) { it.locationId }
    while (true) {
        val newRemainingSources = ArrayList<SourceCode>()
        for (source in remainingSources) {
            when (val refinedConfiguration = getScriptCompilationConfiguration(source)) {
                null -> {}
                is ResultWithDiagnostics.Failure -> {
                    collectedSourceDependencies.add(ScriptsCompilationDependencies.SourceDependencies(source, refinedConfiguration))
                }
                is ResultWithDiagnostics.Success -> {
                    collectedClassPath.addAll(refinedConfiguration.value.dependenciesClassPath)

                    val sourceDependencies = refinedConfiguration.value.importedScripts
                    if (sourceDependencies.isNotEmpty()) {
                        collectedSourceDependencies.add(
                            ScriptsCompilationDependencies.SourceDependencies(
                                source,
                                sourceDependencies.asSuccess(refinedConfiguration.reports)
                            )
                        )

                        val newSources = sourceDependencies.filterNot { knownSourcePaths.contains(it.locationId) }
                        for (newSource in newSources) {
                            collectedSources.add(newSource)
                            newRemainingSources.add(newSource)
                            knownSourcePaths.add(newSource.locationId!!)
                        }
                    }
                }
            }
        }
        if (newRemainingSources.isEmpty()) break
        else {
            remainingSources = newRemainingSources
        }
    }
    return ScriptsCompilationDependencies(
        collectedClassPath.distinctBy { it.absolutePath },
        collectedSources,
        collectedSourceDependencies
    )
}


// recursively collect dependencies from initial and imported scripts, returns the main sources list sorted topologically
fun collectScriptsCompilationDependenciesRecursively(
    initialSources: Iterable<SourceCode>,
    getScriptCompilationConfiguration: (SourceCode) -> ResultWithDiagnostics<ScriptCompilationConfiguration>?
): ResultWithDiagnostics<ScriptsCompilationDependencies> {
    val collectedClassPath = ArrayList<File>()
    val collectedSources = ArrayList<SourceCode>()
    val collectedSourceDependencies = ArrayList<ScriptsCompilationDependencies.SourceDependencies>()
    var remainingSources = initialSources
    val knownSourcePaths = initialSources.mapNotNullTo(HashSet()) { it.locationId }
    var hasErrors = false
    val diagnostics = ArrayList<ScriptDiagnostic>()
    while (true) {
        val newRemainingSources = ArrayList<SourceCode>()
        for (source in remainingSources) {
            when (val refinedConfiguration = getScriptCompilationConfiguration(source)) {
                null -> {}
                is ResultWithDiagnostics.Failure -> {
                    diagnostics.addAll(refinedConfiguration.reports)
                    hasErrors = true
                }
                is ResultWithDiagnostics.Success -> {
                    diagnostics.addAll(refinedConfiguration.reports)
                    collectedClassPath.addAll(refinedConfiguration.value[ScriptCompilationConfiguration.dependencies].toClassPathOrEmpty())

                    refinedConfiguration.value.let {
                        it[ScriptCompilationConfiguration.resolvedImportScripts]
                    }?.takeIf { it.isNotEmpty() }?.let { sourceDependencies ->
                        collectedSourceDependencies.add(
                            ScriptsCompilationDependencies.SourceDependencies(
                                source,
                                sourceDependencies.asSuccess(refinedConfiguration.reports)
                            )
                        )

                        val newSources = sourceDependencies.filterNot { knownSourcePaths.contains(it.uniqueLocationId()) }
                        for (newSource in newSources) {
                            collectedSources.add(newSource)
                            newRemainingSources.add(newSource)
                            knownSourcePaths.add(newSource.uniqueLocationId())
                        }
                    }
                }
            }
        }
        if (newRemainingSources.isEmpty()) break
        else {
            remainingSources = newRemainingSources
        }
    }
    if (hasErrors) {
        return ResultWithDiagnostics.Failure(diagnostics)
    } else {
        class CycleDetected(val node: SourceCode) : Throwable()

        val sortedSources = try {
            topologicalSort(
                collectedSources, reportCycle = { throw CycleDetected(it) }
            ) {
                collectedSourceDependencies.find { it.script == this }?.sourceDependencies?.valueOrNull() ?: emptyList()
            }.reversed()
        } catch (e: CycleDetected) {
            return ResultWithDiagnostics.Failure(
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    "Unable to handle recursive script dependencies, cycle detected on file ${e.node.name ?: e.node.locationId}",
                    sourcePath = e.node.locationId
                )
            )
        }
        return ScriptsCompilationDependencies(
            collectedClassPath.distinctBy { it.absolutePath },
            sortedSources,
            collectedSourceDependencies
        ).asSuccess(diagnostics)
    }
}

private fun SourceCode.uniqueLocationId(): String =
    when (this) {
        is FileScriptSource -> file.normalize().absolutePath.toSystemIndependentScriptPath()
        is VirtualFileScriptSource -> (virtualFile.originalFile() ?: virtualFile).path.toSystemIndependentScriptPath()
        else -> locationId ?: "\$${text.hashCode().toHexString()}"
    }

private fun String.toSystemIndependentScriptPath(): String = replace('\\', '/')
