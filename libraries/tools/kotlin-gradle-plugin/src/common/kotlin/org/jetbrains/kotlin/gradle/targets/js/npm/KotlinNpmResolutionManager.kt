/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.Installation
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution

internal interface UsesKotlinNpmResolutionManager : Task {
    @get:Internal
    val npmResolutionManager: Property<KotlinNpmResolutionManager>
}

abstract class KotlinNpmResolutionManager : BuildService<KotlinNpmResolutionManager.Parameters> {

    interface Parameters : BuildServiceParameters {
        val resolution: Property<KotlinRootNpmResolution>

        // pulled up from compilation resolver since it was failing with ClassNotFoundException on deserialization, see KT-49061
        val packageJsonHandlers: MapProperty<String, List<PackageJson.() -> Unit>>

        val gradleNodeModulesProvider: Property<GradleNodeModulesCache>
        val compositeNodeModulesProvider: Property<CompositeNodeModulesCache>
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

//    internal val packageJsonFiles: Collection<File>
//        get() = state.npmProjects.map { it.packageJsonFile }

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