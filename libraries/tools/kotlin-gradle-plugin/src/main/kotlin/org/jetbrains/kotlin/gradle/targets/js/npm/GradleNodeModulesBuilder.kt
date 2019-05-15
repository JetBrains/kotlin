/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.internal.ProcessedFilesCache

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

    val projects = mutableSetOf<Project>()

    fun visitConfiguration(configuration: Configuration) {
        if (configuration.isCanBeResolved) {
            configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                visitDependency(it)
            }
        }
    }

    private fun visitDependency(dependency: ResolvedDependency) {
        if (!visited.add(dependency)) return

        visitArtifacts(dependency, dependency.moduleArtifacts)

        dependency.children.forEach {
            visitDependency(it)
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
            val componentIdentifier = artifact.id.componentIdentifier
            if (componentIdentifier is ProjectComponentIdentifier) {
                val dependentProject = project.findProject(componentIdentifier.projectPath)
                    ?: error("Cannot find project ${componentIdentifier.projectPath}")

                projects.add(dependentProject)
            } else {
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