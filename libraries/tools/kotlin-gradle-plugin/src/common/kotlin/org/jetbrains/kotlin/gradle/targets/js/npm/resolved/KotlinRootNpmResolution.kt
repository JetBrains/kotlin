/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.gradle.api.logging.Logger
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.YarnEnvironment
import org.jetbrains.kotlin.gradle.targets.js.yarn.toVersionString
import java.io.Serializable

class KotlinRootNpmResolution(
    val projects: Map<String, KotlinProjectNpmResolution>,
    val rootProjectName: String,
    val rootProjectVersion: String,
) : Serializable {
    operator fun get(project: String) = projects[project] ?: KotlinProjectNpmResolution.empty()

    /**
     * Don't use directly, use [KotlinNpmResolutionManager.installIfNeeded] instead.
     */
    internal fun prepareInstallation(
        logger: Logger,
        npmEnvironment: NpmEnvironment,
        yarnEnvironment: YarnEnvironment,
        npmResolutionManager: KotlinNpmResolutionManager,
    ): Installation {
        synchronized(projects) {
            npmResolutionManager.parameters.gradleNodeModulesProvider.get().close()

            val projectResolutions: List<PreparedKotlinCompilationNpmResolution> = projects.values
                .flatMap { it.npmProjects }
                .map {
                    it.close(
                        npmResolutionManager,
                        logger
                    )
                }

            npmEnvironment.packageManager.prepareRootProject(
                npmEnvironment,
                rootProjectName,
                rootProjectVersion,
                logger,
                projectResolutions,
                yarnEnvironment.yarnResolutions
                    .associate { it.path to it.toVersionString() },
            )

            return Installation(
                projectResolutions
            )
        }
    }
}

class Installation(val compilationResolutions: Collection<PreparedKotlinCompilationNpmResolution>) {
    internal fun install(
        args: List<String>,
        services: ServiceRegistry,
        logger: Logger,
        npmEnvironment: NpmEnvironment,
        yarnEnvironment: YarnEnvironment,
    ) {
        synchronized(compilationResolutions) {
            npmEnvironment.packageManager.resolveRootProject(
                services,
                logger,
                npmEnvironment,
                yarnEnvironment,
                compilationResolutions,
                args
            )
        }
    }
}