/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.singleKotlinCompilationOrNull
import org.jetbrains.kotlin.gradle.targets.native.internal.retrievePlatformDependencies

internal class KotlinNativePlatformDependency(
    val platformType: String,
    override val files: FileCollection,
) : KotlinSourceSetDependencies

internal object KotlinNativePlatformDependenciesContributor :
    KotlinSourceSetDependenciesContributor<KotlinNativePlatformDependency> {

    override suspend fun invoke(sourceSet: InternalKotlinSourceSet): List<KotlinNativePlatformDependency>? {
        val compilation = sourceSet.singleKotlinCompilationOrNull() ?: return null
        if (compilation !is AbstractKotlinNativeCompilation) return null

        val dependencies = compilation.retrievePlatformDependencies()
        return listOf(KotlinNativePlatformDependency(
                platformType = compilation.konanTarget.name,
                files = dependencies
            )
        )
    }
}