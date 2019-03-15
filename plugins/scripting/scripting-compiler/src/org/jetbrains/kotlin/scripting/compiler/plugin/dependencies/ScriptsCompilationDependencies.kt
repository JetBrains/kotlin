/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.dependencies

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.createSourceFilesFromSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import java.io.File

data class ScriptsCompilationDependencies(
    val classpath: List<File>,
    val sources: List<KtFile>,
    val sourceDependencies: List<SourceDependencies>
) {
    data class SourceDependencies(
        val scriptFile: KtFile,
        val sourceDependencies: List<KtFile>
    )
}

// recursively collect dependencies from initial and imported scripts
fun collectScriptsCompilationDependencies(
    configuration: CompilerConfiguration,
    project: Project,
    initialSources: Iterable<KtFile>
): ScriptsCompilationDependencies {
    val collectedClassPath = ArrayList<File>()
    val collectedSources = ArrayList<KtFile>()
    val collectedSourceDependencies = ArrayList<ScriptsCompilationDependencies.SourceDependencies>()
    var remainingSources = initialSources
    val knownSourcePaths = initialSources.mapNotNullTo(HashSet()) { it.virtualFile?.path }
    val importsProvider = ScriptDependenciesProvider.getInstance(project)
    if (importsProvider != null) {
        while (true) {
            val newRemainingSources = ArrayList<KtFile>()
            for (source in remainingSources) {
                val dependencies = importsProvider.getScriptDependencies(source)
                if (dependencies != null) {
                    collectedClassPath.addAll(dependencies.classpath)

                    val sourceDependenciesRoots = dependencies.scripts.map {
                        KotlinSourceRoot(it.path, false)
                    }
                    val sourceDependencies =
                        createSourceFilesFromSourceRoots(
                            configuration, project, sourceDependenciesRoots,
                            // TODO: consider receiving and using precise location from the resolver in the future
                            source.virtualFile?.path?.let { CompilerMessageLocation.create(it) }
                        )
                    if (sourceDependencies.isNotEmpty()) {
                        collectedSourceDependencies.add(
                            ScriptsCompilationDependencies.SourceDependencies(
                                source,
                                sourceDependencies
                            )
                        )

                        val newSources = sourceDependencies.filterNot { knownSourcePaths.contains(it.virtualFile.path) }
                        for (newSource in newSources) {
                            collectedSources.add(newSource)
                            newRemainingSources.add(newSource)
                            knownSourcePaths.add(newSource.virtualFile.path)
                        }
                    }
                }
            }
            if (newRemainingSources.isEmpty()) break
            else {
                remainingSources = newRemainingSources
            }
        }
    }
    return ScriptsCompilationDependencies(
        collectedClassPath.distinctBy { it.absolutePath },
        collectedSources,
        collectedSourceDependencies
    )
}