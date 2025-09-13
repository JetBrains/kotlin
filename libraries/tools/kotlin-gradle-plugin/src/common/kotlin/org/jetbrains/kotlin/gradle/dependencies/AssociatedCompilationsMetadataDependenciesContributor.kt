/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.findMetadataCompilation

internal class AssociatedCompilationMetadataDependencies(
    val associatedSourceSet: KotlinSourceSet,
    override val files: FileCollection
) : KotlinSourceSetDependencies

internal object AssociatedCompilationsMetadataDependenciesContributor :
    KotlinSourceSetDependenciesContributor<AssociatedCompilationMetadataDependencies> {

    override suspend fun invoke(sourceSet: InternalKotlinSourceSet): List<AssociatedCompilationMetadataDependencies>? {
        val project = sourceSet.project
        val associatedSourceSets = sourceSet.getAdditionalVisibleSourceSets()
        return associatedSourceSets.mapNotNull { associatedSourceSet ->
            val metadataCompilation = project.findMetadataCompilation(associatedSourceSet) ?: return@mapNotNull null
            AssociatedCompilationMetadataDependencies(associatedSourceSet, metadataCompilation.output.classesDirs)
        }
    }
}