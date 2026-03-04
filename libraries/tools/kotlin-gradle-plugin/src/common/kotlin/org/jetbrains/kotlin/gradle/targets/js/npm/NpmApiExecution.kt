/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.file.Directory
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnv
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import java.io.File
import java.io.Serializable

/**
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 *
 * NodeJS package manager API.
 *
 * This interface is a wrapper for calling npm package managers, like npm or yarn.
 */
interface NpmApiExecution<out T : PackageManagerEnvironment> : Serializable {

    /**
     * Get files that are necessary for executing the package manager in the root npm project.
     *
     * For example, the root project's `package.json` that describes
     * the root npm project's dependencies and workspaces.
     */
    fun preparedFiles(nodeJs: NodeJsEnvironment): Collection<File>

    fun prepareRootProject(
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: @UnsafeVariance T,
        rootProjectName: String,
        rootProjectVersion: String,
        subProjects: Collection<PreparedKotlinCompilationNpmResolution>,
    )

    /**
     * Installs npm packages (e.g. `npm install`)
     */
    fun resolveRootProject(
        services: ServiceRegistry,
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: @UnsafeVariance T,
        cliArgs: List<String>,
    )

    fun prepareTooling(dir: File)

    fun packageManagerExec(
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        environment: @UnsafeVariance T,
        dir: Provider<File>,
        description: String,
        args: List<String>,
    )

    companion object {
        fun resolveOperationDescription(packageManagerTitle: String): String =
            "Resolving NPM dependencies using $packageManagerTitle"
    }
}

data class NodeJsEnvironment(
    val rootPackageDir: Provider<Directory>,
    val nodeExecutable: String,
    val packageManager: NpmApiExecution<PackageManagerEnvironment>,
) : Serializable

internal fun asNodeJsEnvironment(
    rootPackageDirectory: Provider<Directory>,
    packageManager: Provider<NpmApiExecution<PackageManagerEnvironment>>,
    nodeJsEnv: NodeJsEnv,
) = NodeJsEnvironment(
    rootPackageDirectory,
    nodeJsEnv.executable,
    packageManager.get()
)
