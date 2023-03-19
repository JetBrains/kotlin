/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.logging.Logger
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import java.io.File

class YarnWorkspaces : YarnBasics() {
    override fun preparedFiles(nodeJs: NpmEnvironment): Collection<File> {
        return listOf(
            nodeJs
                .rootPackageDir
                .resolve(NpmProject.PACKAGE_JSON)
        )
    }

    override fun prepareRootProject(
        nodeJs: NpmEnvironment,
        rootProjectName: String,
        rootProjectVersion: String,
        logger: Logger,
        subProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        resolutions: Map<String, String>,
    ) {
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
        nodeJs: NpmEnvironment,
        rootProjectName: String,
        rootProjectVersion: String,
        logger: Logger,
        npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
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
        nodeJs: NpmEnvironment,
        yarn: YarnEnvironment,
        npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        cliArgs: List<String>
    ) {
        val nodeJsWorldDir = nodeJs.rootPackageDir

        yarnExec(
            services,
            logger,
            nodeJs,
            yarn,
            nodeJsWorldDir,
            NpmApi.resolveOperationDescription("yarn"),
            cliArgs
        )
    }

    private fun saveRootProjectWorkspacesPackageJson(
        rootProjectName: String,
        rootProjectVersion: String,
        logger: Logger,
        npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        resolutions: Map<String, String>,
        rootPackageJsonFile: File
    ) {
        val nodeJsWorldDir = rootPackageJsonFile.parentFile
        val rootPackageJson = PackageJson(rootProjectName, rootProjectVersion)
        rootPackageJson.private = true

        val npmProjectWorkspaces = npmProjects.map { it.npmProjectDir.relativeTo(nodeJsWorldDir).path }
        val importedProjectWorkspaces =
            YarnImportedPackagesVersionResolver(logger, npmProjects, nodeJsWorldDir).resolveAndUpdatePackages()

        rootPackageJson.workspaces = npmProjectWorkspaces + importedProjectWorkspaces
        rootPackageJson.resolutions = resolutions
        rootPackageJson.saveTo(
            rootPackageJsonFile
        )
    }
}