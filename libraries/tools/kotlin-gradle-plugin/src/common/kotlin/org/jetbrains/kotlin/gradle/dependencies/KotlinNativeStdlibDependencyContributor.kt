/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.utils.konanDistribution

internal class KotlinNativeStdlibDependency(
    override val files: FileCollection,
) : KotlinSourceSetDependencies

internal object KotlinNativeStdlibDependencyContributor :
    KotlinSourceSetDependenciesContributor<KotlinNativeStdlibDependency> {

    override suspend fun invoke(sourceSet: InternalKotlinSourceSet): List<KotlinNativeStdlibDependency>? {
        if (!sourceSet.isNativeSourceSet.await()) return null
        val project = sourceSet.project

        return listOf(
            KotlinNativeStdlibDependency(
                project.files(project.konanDistribution.stdlib)
            )
        )
    }
}