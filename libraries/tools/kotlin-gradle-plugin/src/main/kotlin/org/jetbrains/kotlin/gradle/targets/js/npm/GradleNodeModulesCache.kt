/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.jetbrains.kotlin.gradle.internal.ProcessedFilesCache
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import java.io.File

/**
 * Cache for storing already created [GradleNodeModule]s
 */
internal class GradleNodeModulesCache(val nodeJs: NodeJsRootExtension) : AutoCloseable {
    companion object {
        const val STATE_FILE_NAME = ".visited"
    }

    val project: Project get() = nodeJs.rootProject
    internal val dir = nodeJs.nodeModulesGradleCacheDir
    private val cache = ProcessedFilesCache(project, dir, STATE_FILE_NAME, "9")

    @Synchronized
    fun get(
        dependency: ResolvedDependency,
        artifact: ResolvedArtifact
    ): GradleNodeModule? = cache.getOrCompute(artifact.file) {
        buildImportedPackage(dependency, artifact)
    }?.let {
        GradleNodeModule(it)
    }

    private fun buildImportedPackage(
        dependency: ResolvedDependency,
        artifact: ResolvedArtifact
    ): File? {
        val module = GradleNodeModuleBuilder(project, dependency, listOf(artifact), this)
        module.visitArtifacts()
        return module.rebuild()
    }

    @Synchronized
    override fun close() {
        cache.close()
    }
}