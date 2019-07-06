/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import java.io.File

object YarnWorkspaces : YarnBasics() {
    override fun resolveProject(resolvedNpmProject: KotlinCompilationNpmResolution) = Unit

    override fun resolveRootProject(rootProject: Project, subProjects: Collection<KotlinCompilationNpmResolution>): NpmApi.Result {
        check(rootProject == rootProject.rootProject)
        setup(rootProject)
        return resolveWorkspaces(rootProject, subProjects)
    }

    private fun resolveWorkspaces(
        rootProject: Project,
        npmProjects: Collection<KotlinCompilationNpmResolution>
    ): NpmApi.Result {
        val upToDateChecks = npmProjects.map {
            YarnUpToDateCheck(it.npmProject)
        }

        if (upToDateChecks.all { it.upToDate }) return NpmApi.Result.upToDate

        val nodeJsWorldDir = NodeJsRootPlugin.apply(rootProject).rootPackageDir

        saveRootProjectWorkspacesPackageJson(rootProject, npmProjects, nodeJsWorldDir)

        yarnExec(rootProject, nodeJsWorldDir, NpmApi.resolveOperationDescription("yarn"))
        yarnLockReadTransitiveDependencies(nodeJsWorldDir, npmProjects.flatMap { it.externalNpmDependencies })

        upToDateChecks.forEach {
            it.commit()
        }

        return NpmApi.Result.executed
    }

    private fun saveRootProjectWorkspacesPackageJson(
        rootProject: Project,
        npmProjects: Collection<KotlinCompilationNpmResolution>,
        nodeJsWorldDir: File
    ) {
        val rootPackageJson = PackageJson(rootProject.name, rootProject.version.toString())
        rootPackageJson.private = true

        val npmProjectWorkspaces = npmProjects.map { it.npmProject.dir.relativeTo(nodeJsWorldDir).path }
        val importedProjectWorkspaces = YarnImportedPackagesVersionResolver(rootProject, npmProjects, nodeJsWorldDir).resolveAndUpdatePackages()

        rootPackageJson.workspaces = npmProjectWorkspaces + importedProjectWorkspaces
        rootPackageJson.saveTo(
            nodeJsWorldDir.resolve(NpmProject.PACKAGE_JSON)
        )
    }
}