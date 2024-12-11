/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.getNativeDistributionDependencies

/**
 * This service provides functionality to set a Kotlin/Native dependencies without breaking the configuration cache.
 */
internal abstract class KotlinNativeDistributionBuildService : BuildService<BuildServiceParameters.None> {
    companion object {
        fun registerIfAbsent(project: Project): Provider<KotlinNativeDistributionBuildService> {
            return project.gradle.sharedServices.registerIfAbsent(
                "kotlinNativeDistributionBuildService",
                KotlinNativeDistributionBuildService::class.java
            ) {}
        }
    }

    internal fun getNativeDistributionDependencies(
        project: Project,
        commonizerTarget: CommonizerTarget,
    ) = project.getNativeDistributionDependencies(commonizerTarget)

}