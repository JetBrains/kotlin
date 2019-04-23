/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.internal.ProcessedFilesCache
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

internal class GradleNodeModulesBuilder(val project: Project) : AutoCloseable {
    companion object {
        const val STATE_FILE_NAME = ".visited"
    }

    internal val dir = project.buildDir.resolve("node_modules_gradle")
    private val cache = ProcessedFilesCache(project, dir, STATE_FILE_NAME, "5")
    private val visited = mutableSetOf<ResolvedDependency>()

    val modules
        get() = cache.targets.map {
            GradleNodeModule(it, dir.resolve(it))
        }

    fun visitCompilation(compilation: KotlinCompilation<KotlinCommonOptions>) {
        val project = compilation.target.project
        
        // classpath
        compilation.relatedConfigurationNames.forEach { configurationName ->
            val configuration = project.configurations.getByName(configurationName)
            if (configuration.isCanBeResolved) {
                configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                    visitDependency(compilation, it)
                }
            }
        }
    }

    private fun visitDependency(
        compilation: KotlinCompilation<KotlinCommonOptions>,
        dependency: ResolvedDependency
    ) {
        if (!visited.add(dependency)) return

        visitArtifacts(dependency, dependency.moduleArtifacts)

        dependency.children.forEach {
            visitDependency(compilation, it)
        }
    }

    private fun visitArtifacts(
        dependency: ResolvedDependency,
        artifacts: MutableSet<ResolvedArtifact>
    ) {
        val lazyDirName: String? by lazy {
            val module = GradleNodeModuleBuilder(project, dependency, artifacts, this)
            module.visitArtifacts()
            module.rebuild()?.canonicalFile?.relativeTo(dir)?.path
        }

        artifacts.forEach { artifact ->
            if (artifact.id.componentIdentifier !is ProjectComponentIdentifier) {
                cache.getOrCompute(artifact.file) {
                    lazyDirName
                }
            }
        }
    }

    override fun close() {
        cache.close()
    }
}