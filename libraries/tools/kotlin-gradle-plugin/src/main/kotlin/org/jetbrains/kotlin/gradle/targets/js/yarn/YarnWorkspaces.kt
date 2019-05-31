/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectPackage
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import java.io.File

object YarnWorkspaces : YarnBasics() {
    override fun resolveProject(resolvedNpmProject: NpmProjectPackage) = Unit

    override fun resolveRootProject(rootProject: Project, subProjects: MutableList<NpmProjectPackage>) {
        check(rootProject == rootProject.rootProject)
        setup(rootProject)
        resolveWorkspaces(rootProject, subProjects)
    }

    private fun resolveWorkspaces(
        rootProject: Project,
        npmProjects: MutableList<NpmProjectPackage>
    ) {
        val upToDateChecks = npmProjects.map {
            YarnUpToDateCheck(it.npmProject)
        }

        if (upToDateChecks.all { it.upToDate }) return

        val nodeJsWorldDir = rootProject.nodeJs.root.rootPackageDir

        saveRootProjectWorkspacesPackageJson(rootProject, npmProjects, nodeJsWorldDir)

        yarnExec(rootProject, nodeJsWorldDir, NpmApi.resolveOperationDescription("yarn"))
        yarnLockReadTransitiveDependencies(nodeJsWorldDir, npmProjects.flatMap { it.npmDependencies })

        upToDateChecks.forEach {
            it.commit()
        }
    }

    private fun saveRootProjectWorkspacesPackageJson(
        rootProject: Project,
        npmProjects: MutableList<NpmProjectPackage>,
        nodeJsWorldDir: File
    ) {
        val rootPackageJson = PackageJson(rootProject.name, rootProject.version.toString())
        rootPackageJson.private = true

        val npmProjectWorkspaces = npmProjects.map { it.npmProject.dir.relativeTo(nodeJsWorldDir).path }
        val importedProjectWorkspaces = npmProjects.flatMapTo(mutableSetOf()) {
            it.gradleDependencies.externalModules.map { importedProject ->
                importedProject.path.relativeTo(nodeJsWorldDir).path
            }
        }

        rootPackageJson.workspaces = npmProjectWorkspaces + importedProjectWorkspaces

        rootProject.nodeJs.packageJsonHandlers.forEach {
            it(rootPackageJson)
        }

        rootPackageJson.saveTo(
            nodeJsWorldDir.resolve(NpmProject.PACKAGE_JSON)
        )
    }
}