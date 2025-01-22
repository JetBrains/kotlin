/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

class Npm internal constructor(
    private val objects: ObjectFactory,
) : NpmApiExecution<NpmEnvironment> {

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
        npmExec(
            services = services,
            logger = logger,
            nodeJs = nodeJs,
            environment = packageManagerEnvironment,
            dir = nodeJs.rootPackageDir.map { it.asFile },
            description = NpmApiExecution.resolveOperationDescription("npm"),
            args = cliArgs,
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
        npmExec(
            services = services,
            logger = logger,
            nodeJs = nodeJs,
            environment = environment,
            dir = objects.directoryProperty().fileValue(dir).asFile,
            description = description,
            args = args
        )
    }

    fun npmExec(
        services: ServiceRegistry,
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        environment: NpmEnvironment,
        dir: Provider<File>,
        description: String,
        args: List<String>,
    ) {
        services.execWithProgress(description, objects = objects) { exec ->
            val arguments: List<String> = mutableListOf<String>().apply {
                add("install")
                addAll(args)
                if (logger.isDebugEnabled) add("--verbose")
                if (environment.ignoreScripts) add("--ignore-scripts")
            }.filter { it.isNotEmpty() }

            if (!environment.standalone) {
                val nodeExecutable = nodeJs.nodeExecutable
                val nodePath = File(nodeExecutable).parent
                exec.launchOpts.environment.put(
                    "PATH",
                    "$nodePath${File.pathSeparator}${System.getenv("PATH")}",
                )
            }

            val command = environment.executable

            exec.launchOpts.executable.set(command)
            exec.launchOpts.workingDir.fileProvider(dir)

            exec.setArguments(arguments)
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
