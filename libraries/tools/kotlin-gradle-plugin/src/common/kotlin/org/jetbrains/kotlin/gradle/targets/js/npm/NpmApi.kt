/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.logging.Logger
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnv
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnEnv
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnResolution
import java.io.File
import java.io.Serializable

/**
 * NodeJS package manager API
 */
interface NpmApi : Serializable {
    fun preparedFiles(nodeJs: NpmEnvironment): Collection<File>

    fun prepareRootProject(
        nodeJs: NpmEnvironment,
        rootProjectName: String,
        rootProjectVersion: String,
        logger: Logger,
        subProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        resolutions: Map<String, String>,
    )

    fun resolveRootProject(
        services: ServiceRegistry,
        logger: Logger,
        nodeJs: NpmEnvironment,
        yarn: YarnEnvironment,
        npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        cliArgs: List<String>
    )

    companion object {
        fun resolveOperationDescription(packageManagerTitle: String): String =
            "Resolving NPM dependencies using $packageManagerTitle"
    }
}

data class NpmEnvironment(
    val rootPackageDir: File,
    val nodeExecutable: String,
    val isWindows: Boolean,
    val packageManager: NpmApi
) : Serializable

internal val NodeJsEnv.asNpmEnvironment
    get() = NpmEnvironment(
        rootPackageDir,
        nodeExecutable,
        isWindows,
        packageManager
    )

data class YarnEnvironment(
    val executable: String,
    val standalone: Boolean,
    val ignoreScripts: Boolean,
    val yarnResolutions: List<YarnResolution>
) : Serializable

internal val YarnEnv.asYarnEnvironment
    get() = YarnEnvironment(
        executable,
        standalone,
        ignoreScripts,
        yarnResolutions
    )