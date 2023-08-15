/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.Installation
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinProjectNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver

internal interface UsesKotlinNpmResolutionManager : Task {
    @get:Internal
    val npmResolutionManager: Property<KotlinNpmResolutionManager>
}

/**
 * [KotlinNpmResolutionManager] is build service which holds state of resolution process of JS-related projects.
 *
 * Terms:
 * `*Resolver` means entities which should exist only in Configuration phase
 * `*Resolution` means entities which should be created from `*Resolver` in the end of Configuration phase (when all projects are registered themselves)
 *
 * The process is following:
 * Every project register itself via [NpmResolverPlugin] in [KotlinRootNpmResolver].
 * [KotlinRootNpmResolver] creates for every project [KotlinProjectNpmResolver],
 * and [KotlinProjectNpmResolver] creates [KotlinCompilationNpmResolver] for every compilation.
 * [KotlinCompilationNpmResolver] exist to resolve all JS-related dependencies (inter-project dependencies and NPM dependencies)`.
 * In [KotlinCompilationNpmResolver] one can get [KotlinCompilationNpmResolver.compilationNpmResolution] to get resolution,
 * but it must be called only after all projects were registered in [KotlinRootNpmResolver]
 *
 * After configuration phase, on execution, tasks can call [org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.kotlinNpmResolutionManager]
 * It provides [KotlinRootNpmResolution] into [KotlinNpmResolutionManager] and since then it stores all information about resolution process in execution phase
 */
abstract class KotlinNpmResolutionManager : BuildService<KotlinNpmResolutionManager.Parameters> {

    interface Parameters : BuildServiceParameters {
        val resolution: Property<KotlinRootNpmResolution>

        val gradleNodeModulesProvider: Property<GradleNodeModulesCache>
    }

    val resolution
        get() = parameters.resolution

    @Volatile
    var state: ResolutionState = ResolutionState.Configuring(resolution.get())

    sealed class ResolutionState {

        class Configuring(val resolution: KotlinRootNpmResolution) : ResolutionState()

        open class Prepared(val preparedInstallation: Installation) : ResolutionState()

        class Installed() : ResolutionState()

        class Error(val wrappedException: Throwable) : ResolutionState()
    }

    internal fun isConfiguringState(): Boolean =
        this.state is ResolutionState.Configuring

    internal fun prepare(
        logger: Logger,
        npmEnvironment: NpmEnvironment,
        yarnEnvironment: YarnEnvironment,
    ) = prepareIfNeeded(logger = logger, npmEnvironment, yarnEnvironment)

    internal fun installIfNeeded(
        args: List<String> = emptyList(),
        services: ServiceRegistry,
        logger: Logger,
        npmEnvironment: NpmEnvironment,
        yarnEnvironment: YarnEnvironment,
    ): Unit? {
        synchronized(this) {
            if (state is ResolutionState.Installed) {
                return Unit
            }

            if (state is ResolutionState.Error) {
                return null
            }

            return try {
                val installation: Installation = prepareIfNeeded(logger = logger, npmEnvironment, yarnEnvironment)
                installation.install(args, services, logger, npmEnvironment, yarnEnvironment)
                state = ResolutionState.Installed()
            } catch (e: Exception) {
                state = ResolutionState.Error(e)
                throw e
            }
        }
    }

    private fun prepareIfNeeded(
        logger: Logger,
        npmEnvironment: NpmEnvironment,
        yarnEnvironment: YarnEnvironment,
    ): Installation {
        val state0 = this.state
        return when (state0) {
            is ResolutionState.Prepared -> {
                state0.preparedInstallation
            }

            is ResolutionState.Configuring -> {
                synchronized(this) {
                    val state1 = this.state
                    when (state1) {
                        is ResolutionState.Prepared -> state1.preparedInstallation
                        is ResolutionState.Configuring -> {
                            state1.resolution.prepareInstallation(
                                logger,
                                npmEnvironment,
                                yarnEnvironment,
                                this
                            ).also {
                                this.state = ResolutionState.Prepared(it)
                            }
                        }

                        is ResolutionState.Installed -> error("Project already installed")
                        is ResolutionState.Error -> throw state1.wrappedException
                    }
                }
            }

            is ResolutionState.Installed -> error("Project already installed")
            is ResolutionState.Error -> throw state0.wrappedException
        }
    }
}