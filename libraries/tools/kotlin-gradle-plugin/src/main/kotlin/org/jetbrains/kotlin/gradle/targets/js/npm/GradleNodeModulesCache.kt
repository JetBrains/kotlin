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
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs

/**
 * Cache for storing already created [GradleNodeModule]s
 */
internal class GradleNodeModulesCache(val project: Project) : AutoCloseable {
    companion object {
        const val STATE_FILE_NAME = ".visited"
    }

    internal val dir = project.nodeJs.root.nodeModulesGradleCacheDir
    private val cache = ProcessedFilesCache(project, dir, STATE_FILE_NAME, "8")

    fun ensureImported(
        artifact: ResolvedArtifact,
        dependency: ResolvedDependency,
        artifacts: MutableSet<ResolvedArtifact>,
        result: NpmGradleDependencies
    ) {
        val key = cache.getOrCompute(artifact.file) {
            val module = GradleNodeModuleBuilder(project, dependency, artifacts, this)
            module.visitArtifacts()
            module.rebuild()?.let {
                it.name + ":" + it.version
            }
        }

        if (key != null) {
            val (name, version) = key.split(":")
            result.externalModules.add(GradleNodeModule(name, version, importedPackageDir(dir, name, version)))
        }
    }

    override fun close() {
        cache.close()
    }
}