/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.api.Project
import org.jetbrains.kotlin.build.KotlinCompilationModel
import org.jetbrains.kotlin.build.KotlinNativePlatformModel
import org.jetbrains.kotlin.build.KotlinPlatformModel
import org.jetbrains.kotlin.build.KotlinProjectModel
import org.jetbrains.kotlin.build.KotlinTargetModel
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.konan.target.KonanTarget

internal open class KotlinPlatformTypeInGradle(
    val platformType: KotlinPlatformType,
) : KotlinPlatformModel {
    override val isAndroid: Boolean get() = platformType == KotlinPlatformType.androidJvm
    override val isJvm: Boolean get() = platformType == KotlinPlatformType.jvm
}

internal class KotlinNativePlatformInGradle(
    val konanTarget: KonanTarget,
) : KotlinNativePlatformModel, KotlinPlatformTypeInGradle(platformType = KotlinPlatformType.native)

internal suspend fun Project.awaitProjectModel(): KotlinProjectModel {
    val kotlinProjectModelKey = "kotlinProjectModel"
    val existingModel = multiplatformExtension.extraProperties.getOrNull(kotlinProjectModelKey)
    @Suppress("UNCHECKED_CAST")
    if (existingModel != null) return (existingModel as Future<KotlinProjectModel>).await()
    val future = CompletableFuture<KotlinProjectModel>()
    multiplatformExtension.extraProperties[kotlinProjectModelKey] = future

    val projectModel = KotlinProjectModel(
        KotlinProjectModel.Id(project.path)
    )

    val sourceSets = multiplatformExtension.awaitSourceSets().associateWith {
        val sourceSetModel = projectModel.addSourceSet(it.name)
        sourceSetModel
    }

    multiplatformExtension.awaitSourceSets().forEach { sourceSet ->
        val sourceSetModel = sourceSets[sourceSet]!!
        sourceSet.dependsOn.forEach { dependsOnSourceSet ->
            val dependsOnSourceSetModel = sourceSets[dependsOnSourceSet]!!
            sourceSetModel.addDependsOnSourceSet(dependsOnSourceSetModel)
        }
    }

    val compilationMap = mutableMapOf<KotlinCompilation<*>, KotlinCompilationModel>()
    for (target in multiplatformExtension.awaitTargets()) {
        if (target is KotlinMetadataTarget) continue

        val platformType = if (target is KotlinNativeTarget) {
            KotlinNativePlatformInGradle(target.konanTarget)
        } else {
            KotlinPlatformTypeInGradle(target.platformType)
        }

        val targetModel = projectModel.addTarget(
            name = target.name,
            platformType = platformType,
        )

        for (compilation in target.compilations) {
            val defaultSourceSetName = sourceSets[compilation.defaultSourceSet]
            checkNotNull(defaultSourceSetName) { "Expected source set for '$target' target compilation '$compilation'" }
            val compilationModel = targetModel.addCompilation(compilation.name, defaultSourceSetName)
            compilationMap[compilation] = compilationModel
        }
    }

    for ((kotlinGradleCompilation, compilationModel) in compilationMap) {
        for (associatedCompilation in kotlinGradleCompilation.associatedCompilations) {
            val associatedCompilationModel = compilationMap[associatedCompilation]
            checkNotNull(associatedCompilationModel) { "Expected compilation model for '$associatedCompilation'" }
            compilationModel.associateWith(associatedCompilationModel)
        }
    }

    future.complete(projectModel)
    return projectModel
}

internal fun KotlinProjectModel.getTargetModel(target: KotlinTarget): KotlinTargetModel {
    val id = KotlinTargetModel.Id(target.name)
    val targetModel = targets[id]
    checkNotNull(targetModel) { "Expected target model for '$target'" }
    return targetModel
}

internal fun KotlinProjectModel.getCompilationModel(compilation: KotlinCompilation<*>): KotlinCompilationModel {
    val targetModel = getTargetModel(compilation.target)

    val id = KotlinCompilationModel.Id(compilation.name)
    val compilationModel = targetModel.compilations[id]
    checkNotNull(compilationModel) { "Expected compilation model for '$compilation'" }
    return compilationModel
}