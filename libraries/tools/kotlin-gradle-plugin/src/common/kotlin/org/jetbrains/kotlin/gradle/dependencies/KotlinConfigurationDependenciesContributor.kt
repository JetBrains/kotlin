/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.singleKotlinCompilationOrNull

internal class KotlinDependencyFromConfiguration(
    val configurationName: String,
    override val files: FileCollection,
) : KotlinSourceSetDependencies

internal object KotlinDependencyFromConfigurationContributor :
    KotlinSourceSetDependenciesContributor<KotlinDependencyFromConfiguration> {

    override suspend fun invoke(sourceSet: InternalKotlinSourceSet): List<KotlinDependencyFromConfiguration>? {
        val compilation = sourceSet.singleKotlinCompilationOrNull() ?: return null

        /** Exclude Android because its compileDependencyConfiguration can't be resolved directly */
        if (compilation.target is KotlinAndroidTarget) return null
        val project = sourceSet.project
        val configuration = project.configurations.getByName(compilation.compileDependencyConfigurationName)
        val files = configuration.incoming.files

        return listOf(KotlinDependencyFromConfiguration(configuration.name, files))
    }
}