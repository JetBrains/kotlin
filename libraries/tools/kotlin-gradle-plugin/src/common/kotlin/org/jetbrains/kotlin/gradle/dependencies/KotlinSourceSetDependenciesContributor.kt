/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.*
import org.jetbrains.kotlin.gradle.model.awaitKotlinProjectModelBridge
import org.jetbrains.kotlin.gradle.model.KotlinLibrariesInGradle
import org.jetbrains.kotlin.gradle.model.KotlinMetadataLibrariesInGradle
import org.jetbrains.kotlin.gradle.model.dependency.GradleKotlinDependencyScope
import org.jetbrains.kotlin.gradle.model.kotlinLibrariesInGradleToFileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure
import org.jetbrains.kotlin.utils.addToStdlib.castAll

internal val KotlinProjectModelK2MultiplatformConfigurator = KotlinProjectSetupCoroutine {
    val projectModelBridge = project.awaitKotlinProjectModelBridge()

    val allPlatformCompilations = projectModelBridge.model.allPlatformCompilations
    for (platformCompilationModel in allPlatformCompilations) {
        val platformCompilation = projectModelBridge.getKotlinGradleCompilation(platformCompilationModel)

        platformCompilation.compileTaskProvider.configure { compileTask ->
            check(compileTask is K2MultiplatformCompilationTask) {
                "Expected K2MultiplatformCompilationTask, got ${compileTask::class.simpleName} for $platformCompilation"
            }
            compileTask.multiplatformStructure.configure(
                compilationModel = platformCompilationModel,
                dependenciesManager = projectModelBridge.dependencyManager,
                sourcesGetter = { projectModelBridge.getKotlinGradleSourceSet(it).kotlin },
                dependenciesToFileCollection = { project.kotlinLibrariesInGradleToFileCollection(it) },
            )
        }
    }
}

/**
 * Should be moved to kotlin-project-model
 */
private fun K2MultiplatformStructure.configure(
    compilationModel: KotlinCompilationModel,
    dependenciesManager: KotlinDependenciesManager,
    sourcesGetter: (KotlinSourceSetModel) -> FileCollection,
    dependenciesToFileCollection: (List<KotlinLibrariesInGradle>) -> FileCollection,
) {
    val fragments = compilationModel.allSourceSets.map { sourceSet ->
        val dependencies = dependenciesManager.sourceSetDependencies(
            sourceSetModel = sourceSet,
            scope = GradleKotlinDependencyScope.COMPILE,
            withDependsOnDependencies = false
        )
        val friendDependencies = dependenciesManager.friendDependencies(sourceSet)

        K2MultiplatformStructure.Fragment(
            fragmentName = sourceSet.name,
            sources = sourcesGetter(sourceSet),
            dependencies = dependenciesToFileCollection(dependencies.castAll<KotlinLibrariesInGradle>()),
            friends = dependenciesToFileCollection(friendDependencies.castAll<KotlinLibrariesInGradle>())
        )
    }
    this.fragments.set(fragments)

    val refinedEdges = compilationModel
        .allSourceSets
        .flatMap { from ->
            from.dependsOn.map { to -> K2MultiplatformStructure.RefinesEdge(from.name, to.name) }
        }
    this.refinesEdges.set(refinedEdges)

    this.defaultFragmentName.set(compilationModel.defaultSourceSet.name)
}