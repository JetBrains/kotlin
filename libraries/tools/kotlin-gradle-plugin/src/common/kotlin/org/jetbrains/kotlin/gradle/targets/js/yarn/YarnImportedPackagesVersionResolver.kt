/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModule
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import java.io.File

class YarnImportedPackagesVersionResolver(
    private val logger: Logger,
    npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
    private val nodeJsWorldDir: File
) {
    private val resolvedVersion = mutableMapOf<String, ResolvedNpmDependency>()
    private val importedProjectWorkspaces = mutableListOf<String>()
    private val externalModules = npmProjects.flatMapTo(mutableSetOf()) {
        it.externalGradleDependencies
    }

    fun resolveAndUpdatePackages(): MutableList<String> {
        resolve(externalModules)

        return importedProjectWorkspaces
    }

    private fun resolve(modules: MutableSet<GradleNodeModule>) {
        modules.groupBy { it.name }.forEach { (name, versions) ->
            val selected: GradleNodeModule = if (versions.size > 1) {
                val sorted = versions.sortedBy { it.semver }
                logger.warn(
                    "There are multiple versions of \"$name\" used in nodejs build: ${sorted.joinToString(", ") { it.version }}. " +
                            "Only latest version will be used."
                )
                val selected = sorted.last()
                resolvedVersion[name] = ResolvedNpmDependency(
                    version = selected.version,
                    file = selected.path
                )
                selected
            } else versions.single()

            importedProjectWorkspaces.add(selected.path.relativeTo(nodeJsWorldDir).path)
        }
    }
}

private data class ResolvedNpmDependency(
    val version: String,
    val file: File
)