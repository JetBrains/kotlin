/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import java.nio.file.Files

/**
 * Handles simple case, when there are no npm depenencies required, but some tasks steel needs
 * node_modules with symlinked packages and packages_imported
 */
class NpmSimpleLinker(rootProject: Project) {
    private val rootProjectNodeModules = rootProject.nodeJs.root.rootPackageDir.resolve(NpmProject.NODE_MODULES)

    fun link(projects: Collection<NpmProjectPackage>) {
        rootProjectNodeModules.listFiles()?.forEach {
            val path = it.toPath()
            when {
                Files.isSymbolicLink(path) ->
                    Files.delete(path)
                else -> it.deleteRecursively()
            }

        }

        rootProjectNodeModules.mkdirs()

        // packages
        projects.forEach {
            Files.createSymbolicLink(getNodeModulePath(it.npmProject.name), it.npmProject.dir.canonicalFile.toPath())
        }

        // packages_imported
        projects.flatMapTo(mutableSetOf()) {
            it.gradleDependencies.externalModules.map { gradleNodeModule -> gradleNodeModule }
        }.forEach {
            Files.createSymbolicLink(getNodeModulePath(it.name), it.path.canonicalFile.toPath())
        }
    }

    private fun getNodeModulePath(name: String) =
        rootProjectNodeModules.resolve(name).canonicalFile.toPath()
}