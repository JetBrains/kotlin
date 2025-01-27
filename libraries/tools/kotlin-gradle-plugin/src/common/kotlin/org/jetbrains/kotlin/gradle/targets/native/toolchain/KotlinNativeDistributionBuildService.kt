/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.konanTargets
import org.jetbrains.kotlin.gradle.targets.native.internal.getNativeDistributionDependencies

/**
 * This service provides functionality to set a Kotlin/Native dependencies without breaking the configuration cache.
 */
internal abstract class KotlinNativeDistributionBuildService : BuildService<KotlinNativeDistributionBuildService.Parameters> {
    interface Parameters : BuildServiceParameters {
        val kotlinNativeBundleBuildService: Property<KotlinNativeBundleBuildService>
    }

    companion object {
        private const val BUILD_SERVICE_NAME = "kotlinNativeDistributionBuildService"

        fun registerIfAbsent(
            project: Project,
            kotlinNativeBundleBuildService: Provider<KotlinNativeBundleBuildService>,
        ): Provider<KotlinNativeDistributionBuildService> {
            return project.gradle.sharedServices.registerIfAbsent(
                BUILD_SERVICE_NAME,
                KotlinNativeDistributionBuildService::class.java
            ) {
                it.parameters.kotlinNativeBundleBuildService.set(kotlinNativeBundleBuildService)
            }
        }
    }

    internal fun getNativeDistributionDependencies(
        project: Project,
        commonizerTarget: CommonizerTarget,
    ): FileCollection {
        val kotlinNativeProvider =
            KotlinNativeFromToolchainProvider(project, commonizerTarget.konanTargets, parameters.kotlinNativeBundleBuildService)
        return project.getNativeDistributionDependencies(
            kotlinNativeProvider.konanDistributionProvider,
            commonizerTarget
        )
    }
}