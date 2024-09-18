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
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import java.io.File
import java.io.Serializable

/**
 * NodeJS package manager API
 */
interface NpmApiExecution<out T : PackageManagerEnvironment> : Serializable {
    fun preparedFiles(nodeJs: NodeJsEnvironment): Collection<File>

    fun prepareRootProject(
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: @UnsafeVariance T,
        rootProjectName: String,
        rootProjectVersion: String,
        subProjects: Collection<PreparedKotlinCompilationNpmResolution>,
    )

    fun resolveRootProject(
        services: ServiceRegistry,
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: @UnsafeVariance T,
        cliArgs: List<String>,
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

internal fun asNodeJsEnvironment(nodeJsRoot: NodeJsRootExtension, nodeJsEnv: NodeJsEnv) = NodeJsEnvironment(
    nodeJsRoot.rootPackageDirectory,
    nodeJsEnv.executable,
    nodeJsRoot.packageManagerExtension.get().packageManager
)