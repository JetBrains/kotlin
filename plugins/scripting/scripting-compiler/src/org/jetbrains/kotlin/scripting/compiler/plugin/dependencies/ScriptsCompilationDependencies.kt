/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.dependencies

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.createSourceFilesFromSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.host.FileBasedScriptSource

data class ScriptsCompilationDependencies(
    val classpath: List<File>,
    val sources: List<KtFile>,
    val sourceDependencies: List<SourceDependencies>
) {
    data class SourceDependencies(
        val scriptFile: KtFile,
        val sourceDependencies: ResultWithDiagnostics<List<KtFile>>
    )
}

// recursively collect dependencies from initial and imported scripts
fun collectScriptsCompilationDependencies(
    configuration: CompilerConfiguration,
    project: Project,
    initialSources: Iterable<KtFile>,
    providedConfiguration: ScriptCompilationConfiguration? = null
): ScriptsCompilationDependencies {
    val collectedClassPath = ArrayList<File>()
    val collectedSources = ArrayList<KtFile>()
    val collectedSourceDependencies = ArrayList<ScriptsCompilationDependencies.SourceDependencies>()
    var remainingSources = initialSources
    val knownSourcePaths = initialSources.mapNotNullTo(HashSet()) { it.virtualFile?.path }
    val importsProvider = ScriptDependenciesProvider.getInstance(project)
    val psiManager by lazy(LazyThreadSafetyMode.NONE) { PsiManager.getInstance(project) }
    if (importsProvider != null) {
        while (true) {
            val newRemainingSources = ArrayList<KtFile>()
            for (source in remainingSources) {
                when (val refinedConfiguration = importsProvider.getScriptConfigurationResult(source, providedConfiguration)) {
                    null -> {}
                    is ResultWithDiagnostics.Failure -> {
                        collectedSourceDependencies.add(ScriptsCompilationDependencies.SourceDependencies(source, refinedConfiguration))
                    }
                    is ResultWithDiagnostics.Success -> {
                        collectedClassPath.addAll(refinedConfiguration.value.dependenciesClassPath)

                        val sourceDependencies = refinedConfiguration.value.importedScripts.mapNotNull {
                            if (it is KtFileScriptSource) it.ktFile
                            else {
                                val virtualFileSource = (it as? VirtualFileScriptSource)
                                    ?: error("expecting script sources resolved to virtual files here")
                                (psiManager.findFile(virtualFileSource.virtualFile) as? KtFile).also {
                                    if (it == null) {
                                        configuration.report(
                                            CompilerMessageSeverity.ERROR,
                                            "imported file is not kotlin source: ${virtualFileSource.virtualFile.path}",
                                            // TODO: consider receiving and using precise location from the resolver in the future
                                            CompilerMessageLocation.create(source.virtualFile.path)
                                        )
                                    }
                                }
                            }
                        }
                        if (sourceDependencies.isNotEmpty()) {
                            collectedSourceDependencies.add(
                                ScriptsCompilationDependencies.SourceDependencies(
                                    source,
                                    sourceDependencies.asSuccess(refinedConfiguration.reports)
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