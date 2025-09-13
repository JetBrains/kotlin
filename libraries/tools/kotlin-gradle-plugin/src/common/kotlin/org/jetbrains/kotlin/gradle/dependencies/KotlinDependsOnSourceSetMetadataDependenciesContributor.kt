/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.findMetadataCompilation

internal class KotlinDependsOnSourceSetMetadataDependencies(
    val sourceSetName: String,
    override val files: FileCollection
): KotlinSourceSetDependencies

internal object KotlinDependsOnSourceSetMetadataDependenciesContributor : KotlinSourceSetDependenciesContributor<KotlinDependsOnSourceSetMetadataDependencies> {
    override suspend fun invoke(sourceSet: InternalKotlinSourceSet): List<KotlinDependsOnSourceSetMetadataDependencies>? {
        val project = sourceSet.project
        return sourceSet.dependsOnClosure.mapNotNull { dependsOnSourceSet ->
            val metadataCompilation = project.findMetadataCompilation(dependsOnSourceSet) ?: return@mapNotNull null
            KotlinDependsOnSourceSetMetadataDependencies(
                sourceSetName = dependsOnSourceSet.name,
                files = metadataCompilation.output.classesDirs
            )
        }
    }
}