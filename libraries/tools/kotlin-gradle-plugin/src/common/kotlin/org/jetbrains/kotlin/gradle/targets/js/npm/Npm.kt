/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.logging.Logger
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

class Npm : NpmApiExecution<NpmEnvironment> {

    override fun preparedFiles(nodeJs: NodeJsEnvironment): Collection<File> {
        return listOf(
            nodeJs
                .rootPackageDir
                .getFile()
                .resolve(NpmProject.PACKAGE_JSON)
        )
    }

    override fun prepareRootProject(
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: NpmEnvironment,
        rootProjectName: String,
        rootProjectVersion: String,
        subProjects: Collection<PreparedKotlinCompilationNpmResolution>,
    ) {
        return prepareRootPackageJson(
            nodeJs,
            rootProjectName,
            rootProjectVersion,
            subProjects,
            packageManagerEnvironment.overrides
                .associate { it.path to it.toVersionString() },
        )
    }

    private fun prepareRootPackageJson(
        nodeJs: NodeJsEnvironment,
        rootProjectName: String,
        rootProjectVersion: String,
        npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        overrides: Map<String, String>,
    ) {
        val rootPackageJsonFile = preparedFiles(nodeJs).single()

        saveRootProjectWorkspacesPackageJson(
            rootProjectName,
            rootProjectVersion,
            npmProjects,
            overrides,
            rootPackageJsonFile
        )
    }

    override fun resolveRootProject(
        services: ServiceRegistry,
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: NpmEnvironment,
        cliArgs: List<String>,
    ) {
        val nodeJsWorldDir = nodeJs.rootPackageDir.getFile()

        npmExec(
            services,
            logger,
            nodeJs,
            packageManagerEnvironment,
            nodeJsWorldDir,
            NpmApiExecution.resolveOperationDescription("npm"),
            cliArgs
        )
    }

    fun npmExec(
        services: ServiceRegistry,
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        environment: NpmEnvironment,
        dir: File,
        description: String,
        args: List<String>,
    ) {
        services.execWithProgress(description) { exec ->
            val arguments = listOf("install") + args
                .plus(
                    if (logger.isDebugEnabled) "--verbose" else ""
                )
                .plus(
                    if (environment.ignoreScripts) "--ignore-scripts" else ""
                ).filter { it.isNotEmpty() }

            if (!environment.standalone) {
                val nodeExecutable = nodeJs.nodeExecutable
                val nodePath = File(nodeExecutable).parent
                exec.environment(
                    "PATH",
                    "$nodePath${File.pathSeparator}${System.getenv("PATH")}"
                )
            }

            val command = environment.executable

            exec.executable = command
            exec.args = arguments

            exec.workingDir = dir
        }

    }

    private fun saveRootProjectWorkspacesPackageJson(
        rootProjectName: String,
        rootProjectVersion: String,
        npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        overrides: Map<String, String>,
        rootPackageJsonFile: File,
    ) {
        val nodeJsWorldDir = rootPackageJsonFile.parentFile
        val rootPackageJson = PackageJson(rootProjectName, rootProjectVersion)
        rootPackageJson.private = true

        val npmProjectWorkspaces = npmProjects.map { it.npmProjectDir.getFile().relativeTo(nodeJsWorldDir).invariantSeparatorsPath }
        val importedProjectWorkspaces =
            NpmImportedPackagesVersionResolver(npmProjects, nodeJsWorldDir).resolveAndUpdatePackages()

        rootPackageJson.workspaces = npmProjectWorkspaces + importedProjectWorkspaces
        rootPackageJson.overrides = overrides
        rootPackageJson.saveTo(
            rootPackageJsonFile
        )
    }
}