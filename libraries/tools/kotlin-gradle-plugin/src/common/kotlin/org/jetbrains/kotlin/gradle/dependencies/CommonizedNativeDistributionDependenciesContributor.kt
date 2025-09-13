/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizedNativeDistributionKlibsOrNull
import org.jetbrains.kotlin.gradle.targets.native.internal.sharedCommonizerTarget

internal class CommonizedNativeDistributionDependencies(
    val commonizerTarget: CommonizerTarget,
    override val files: FileCollection
) : KotlinSourceSetDependencies {}

internal object CommonizedNativeDistributionDependenciesContributor :
    KotlinSourceSetDependenciesContributor<CommonizedNativeDistributionDependencies> {

    override suspend fun invoke(sourceSet: InternalKotlinSourceSet): List<CommonizedNativeDistributionDependencies>? {
        val project = sourceSet.project
        val commonizerTarget = sourceSet.internal.sharedCommonizerTarget.await() ?: return null
        val files = project.commonizedNativeDistributionKlibsOrNull(commonizerTarget) ?: return null
        val fileCollection = project.files(files)
        return listOf(
            CommonizedNativeDistributionDependencies(commonizerTarget, fileCollection)
        )
    }
}