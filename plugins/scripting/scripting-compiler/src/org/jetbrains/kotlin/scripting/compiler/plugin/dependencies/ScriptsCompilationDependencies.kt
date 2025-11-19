/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.dependencies

import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess

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
fun collectScriptsCompilationDependencies(
    initialSources: Iterable<SourceCode>,
    getScriptCompilationConfiguration: (SourceCode) -> ScriptCompilationConfigurationResult?
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