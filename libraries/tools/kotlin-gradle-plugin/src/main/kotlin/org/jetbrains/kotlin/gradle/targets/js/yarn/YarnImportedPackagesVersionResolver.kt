/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import com.google.gson.Gson
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModule
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import java.io.File

class YarnImportedPackagesVersionResolver(
    private val rootProject: Project,
    private val npmProjects: Collection<KotlinCompilationNpmResolution>,
    private val nodeJsWorldDir: File
) {
    private val resolvedVersion = mutableMapOf<String, String>()
    private val importedProjectWorkspaces = mutableListOf<String>()
    private val externalModules = npmProjects.flatMapTo(mutableSetOf()) {
        it.externalGradleDependencies
    }

    fun resolveAndUpdatePackages(): MutableList<String> {
        resolve()
        updatePackages()
        return importedProjectWorkspaces
    }

    private fun resolve() {
        externalModules.groupBy { it.name }.forEach { (name, versions) ->
            val selected: GradleNodeModule = if (versions.size > 1) {
                val sorted = versions.sortedBy { it.semver }
                rootProject.logger.warn(
                    "There are multiple versions of \"$name\" used in nodejs build: ${sorted.joinToString(", ") { it.version }}. " +
                            "Only latest version will be used."
                )
                val selected = sorted.last()
                resolvedVersion[name] = selected.version
                selected
            } else versions.single()

            importedProjectWorkspaces.add(selected.path.relativeTo(nodeJsWorldDir).path)
        }
    }

    private fun updatePackages() {
        if (resolvedVersion.isEmpty()) return

        npmProjects.forEach {
            updatePackageJson(it.packageJson, it.npmProject.packageJsonFile)
        }

        externalModules.forEach {
            val packageJsonFile = it.path.resolve(NpmProject.PACKAGE_JSON)
            val packageJson = packageJsonFile.reader().use {
                Gson().fromJson<PackageJson>(it, PackageJson::class.java)
            }

            updatePackageJson(packageJson, packageJsonFile)
        }
    }

    private fun updatePackageJson(
        packageJson: PackageJson,
        path: File
    ) {
        val updates = listOf(packageJson.dependencies, packageJson.devDependencies).map { updateVersionsMap(it) }
        if (updates.any { it }) {
            packageJson.saveTo(path)
        }
    }

    private fun updateVersionsMap(map: MutableMap<String, String>): Boolean {
        var doneSomething = false
        map.iterator().forEachRemaining {
            val resolved = resolvedVersion[it.key]
            if (resolved != null) {
                it.setValue(resolved)
                doneSomething = true
            }
        }
        return doneSomething
    }
}