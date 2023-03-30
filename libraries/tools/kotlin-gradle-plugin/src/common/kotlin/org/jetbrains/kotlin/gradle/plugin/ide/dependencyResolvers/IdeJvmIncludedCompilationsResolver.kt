/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinProjectCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.project
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.tooling.core.withClosure

internal object IdeJvmIncludedCompilationsResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val compilation = sourceSet.internal.compilations.singleOrNull { it.platformType == KotlinPlatformType.jvm } ?: return emptySet()
        if (!compilation.isMain()) return emptySet()
        if (compilation !is KotlinJvmCompilation) return emptySet()

        return resolve(compilation.target)
    }

    fun resolve(jvmTarget: KotlinJvmTarget): Set<IdeaKotlinSourceDependency> {
        val result = jvmTarget.includedPublishableCompilations
            .flatMap { it.allKotlinSourceSets }
            .map { friendSourceSet ->
                IdeaKotlinSourceDependency(
                    type = IdeaKotlinSourceDependency.Type.Friend,
                    coordinates = IdeaKotlinSourceCoordinates(
                        project = IdeaKotlinProjectCoordinates(friendSourceSet.project),
                        sourceSetName = friendSourceSet.name
                    )
                )
            }

        return result.toSet()
    }
}

private val KotlinJvmTarget.includedPublishableCompilations: List<KotlinJvmCompilation> get() {
    val artifactsTask = project.tasks.getByName(artifactsTaskName)
    // Here we assume that we get all participating compilations
    val allTArtifactsDependencies = artifactsTask
        .withClosure { it.taskDependencies.getDependencies(null) }
        .map { it.name }

    val compileTaskNameToCompilation = compilations.flatMap { compilation ->
        listOfNotNull(
            compilation.compileTaskProvider.name,
            compilation.compileJavaTaskProvider?.name
        ).map { it to compilation }
    }.toMap()

    return allTArtifactsDependencies.mapNotNull { compileTaskNameToCompilation[it] }
}