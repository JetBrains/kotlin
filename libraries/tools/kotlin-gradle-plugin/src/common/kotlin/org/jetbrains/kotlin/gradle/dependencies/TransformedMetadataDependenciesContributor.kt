/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.isSharedSourceSet

internal class TransformedMetadataDependencies(
    override val files: FileCollection
): KotlinSourceSetDependencies

internal object TransformedMetadataDependenciesContributor : KotlinSourceSetDependenciesContributor<TransformedMetadataDependencies> {
    override suspend fun invoke(sourceSet: InternalKotlinSourceSet): List<TransformedMetadataDependencies>? {
        val project = sourceSet.project
        if (!sourceSet.isSharedSourceSet()) return null

        val transformationTask = project.locateOrRegisterMetadataDependencyTransformationTask(sourceSet)
        val ownTransformedLibraries = transformationTask.map { it.ownTransformedLibraries() }
        val fileCollection = project.files(ownTransformedLibraries)
        return listOf(TransformedMetadataDependencies(fileCollection))
    }
}

internal val CreateTransformedMetadataDependencies = KotlinProjectSetupCoroutine {
    val sharedSourceSets = project.multiplatformExtension.awaitSourceSets().filter { it.internal.isSharedSourceSet() }
    sharedSourceSets.forEach { sourceSet -> project.locateOrRegisterMetadataDependencyTransformationTask(sourceSet) }
}