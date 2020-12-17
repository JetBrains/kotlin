/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import java.io.File

class YarnWorkspaces : YarnBasics() {
    override fun resolveProject(resolvedNpmProject: KotlinCompilationNpmResolution) = Unit

    override fun preparedFiles(nodeJs: NodeJsRootExtension): Collection<File> {
        return listOf(
            nodeJs
                .rootPackageDir
                .resolve(NpmProject.PACKAGE_JSON)
        )
    }

    override fun prepareRootProject(
        rootProject: Project?,
        nodeJs: NodeJsRootExtension,
        rootProjectName: String,
        rootProjectVersion: String,
        logger: Logger,
        subProjects: Collection<KotlinCompilationNpmResolution>,
        resolutions: Map<String, String>
    ) {
//        check(rootProject == rootProject.rootProject)
        rootProject?.let { setup(it) }
        return prepareRootPackageJson(
            nodeJs,
            rootProjectName,
            rootProjectVersion,
            logger,
            subProjects,
            resolutions
        )
    }

    private fun prepareRootPackageJson(
        nodeJs: NodeJsRootExtension,
        rootProjectName: String,
        rootProjectVersion: String,
        logger: Logger,
        npmProjects: Collection<KotlinCompilationNpmResolution>,
        resolutions: Map<String, String>
    ) {
        val rootPackageJsonFile = preparedFiles(nodeJs).single()

        saveRootProjectWorkspacesPackageJson(
            rootProjectName,
            rootProjectVersion,
            logger,
            npmProjects,
            resolutions,
            rootPackageJsonFile
        )
    }

    override fun resolveRootProject(
        services: ServiceRegistry,
        logger: Logger,
        nodeJs: NodeJsRootExtension,
        yarnHome: File,
        npmProjects: Collection<KotlinCompilationNpmResolution>,
        skipExecution: Boolean,
        cliArgs: List<String>
    ) {
        val nodeJsWorldDir = nodeJs.rootPackageDir

        yarnExec(
            services,
            logger,
            nodeJs,
            yarnHome,
            nodeJsWorldDir,
            NpmApi.resolveOperationDescription("yarn"),
            cliArgs
        )
        nodeJs.rootNodeModulesStateFile.writeText(System.currentTimeMillis().toString())

        yarnLockReadTransitiveDependencies(nodeJsWorldDir, npmProjects.flatMap { it.externalNpmDependencies })
    }

    private fun saveRootProjectWorkspacesPackageJson(
        rootProjectName: String,
        rootProjectVersion: String,
        logger: Logger,
        npmProjects: Collection<KotlinCompilationNpmResolution>,
        resolutions: Map<String, String>,
        rootPackageJsonFile: File
    ) {
        val nodeJsWorldDir = rootPackageJsonFile.parentFile
        val rootPackageJson = PackageJson(rootProjectName, rootProjectVersion)
        rootPackageJson.private = true

        val npmProjectWorkspaces = npmProjects.map { it.npmProject.dir.relativeTo(nodeJsWorldDir).path }
        val importedProjectWorkspaces =
            YarnImportedPackagesVersionResolver(logger, npmProjects, nodeJsWorldDir).resolveAndUpdatePackages()

        rootPackageJson.workspaces = npmProjectWorkspaces + importedProjectWorkspaces
        rootPackageJson.resolutions = resolutions
        rootPackageJson.saveTo(
            rootPackageJsonFile
        )
    }
}