/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.api.Project
import org.jetbrains.kotlin.build.*
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.model.dependency.CommonizedCinteropMetadataLibrariesInGradle
import org.jetbrains.kotlin.gradle.model.dependency.GradleKotlinDependencyScope
import org.jetbrains.kotlin.gradle.model.dependency.KotlinCommonizedNativeDistributionInGradle
import org.jetbrains.kotlin.gradle.model.dependency.KotlinCompilationPlatformDependenciesInGradle
import org.jetbrains.kotlin.gradle.model.compilation.KotlinMetadataCompilationOutputInGradle
import org.jetbrains.kotlin.gradle.model.compilation.KotlinPlatformCompilationOutputInGradle
import org.jetbrains.kotlin.gradle.model.dependency.TransformedMetadataLibrariesInGradle
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeNativeDistributionTask
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrRegisterCInteropMetadataDependencyTransformationTask

internal class KotlinProjectModelBridgeForGradle(
    val model: KotlinProjectModel,
    private val kotlin: KotlinMultiplatformExtension,
) : KotlinProjectModelBuildToolBridge {
    val dependencyManager = KotlinDependenciesManager(this)

    fun getKotlinGradleCompilation(compilationModel: KotlinCompilationModel): KotlinCompilation<*> {
        val targetName = compilationModel.target.name
        val compilationName = compilationModel.name

        val kotlinGradleTarget = kotlin.targets.findByName(targetName)
            ?: error("Could not find Kotlin target with name $targetName")

        val compilation = kotlinGradleTarget.compilations.findByName(compilationName)
            ?: error("Could not find Kotlin '$targetName' target compilation with name $compilationName")

        return compilation
    }

    fun getKotlinGradleSourceSet(sourceSetModel: KotlinSourceSetModel): KotlinSourceSet {
        val sourceSet = kotlin.sourceSets.findByName(sourceSetModel.name)
            ?: error("Could not find Kotlin source set with name ${sourceSetModel.name}")
        return sourceSet
    }

    override fun resolvePlatformDependencies(
        compilation: KotlinCompilationModel,
        scope: KotlinDependencyScopeModel,
    ): KotlinCompilationPlatformDependenciesInGradle {
        require(scope is GradleKotlinDependencyScope) { "Unexpected dependency scope: $scope" }
        val kotlinGradleCompilation = getKotlinGradleCompilation(compilation)
        return KotlinCompilationPlatformDependenciesInGradle(kotlinGradleCompilation, scope)
    }

    override fun commonizedNativeDistribution(nativeCompilations: Set<KotlinCompilationModel>): KotlinCommonizedNativeDistributionInGradle {
        val gradleKotlinCompilations = nativeCompilations.map { getKotlinGradleCompilation(it) }
        val konanTargets = gradleKotlinCompilations.map { compilation ->
            check(compilation is KotlinNativeCompilation) {
                "To calculate commonized native distribution, only Kotlin/Native compilations are supported. " +
                        "Unexpected compilation: $compilation"
            }
            compilation.konanTarget
        }
        val commonizerTarget = CommonizerTarget(konanTargets)
        check(commonizerTarget is SharedCommonizerTarget) { "Expected SharedCommonizerTarget from $nativeCompilations, got $commonizerTarget" }
        return KotlinCommonizedNativeDistributionInGradle(
            commonizerTarget = commonizerTarget,
            project = kotlin.project,
        )
    }

    override fun metadataCompilationOutput(sourceSet: KotlinSourceSetModel): KotlinMetadataCompilationOutputInGradle {
        val gradleKotlinSourceSet = getKotlinGradleSourceSet(sourceSet)
        return KotlinMetadataCompilationOutputInGradle(kotlin.metadataTarget, gradleKotlinSourceSet.internal)
    }

    override fun platformCompilationOutput(compilation: KotlinCompilationModel): KotlinPlatformCompilationOutputInGradle {
        val gradleKotlinCompilation = getKotlinGradleCompilation(compilation)
        return KotlinPlatformCompilationOutputInGradle(gradleKotlinCompilation)
    }

    override fun transformedMetadataDependencies(sourceSet: KotlinSourceSetModel, transitive: Boolean): TransformedMetadataLibrariesInGradle {
        val gradleKotlinSourceSet = getKotlinGradleSourceSet(sourceSet)
        return TransformedMetadataLibrariesInGradle(gradleKotlinSourceSet, transitive)
    }

    override fun commonizedCinteropDependencies(sourceSet: KotlinSourceSetModel): CommonizedCinteropMetadataLibrariesInGradle {
        val gradleKotlinSourceSet = getKotlinGradleSourceSet(sourceSet)
        check(gradleKotlinSourceSet is DefaultKotlinSourceSet) { "Expected DefaultKotlinSourceSet, got ${gradleKotlinSourceSet::class.simpleName}" }
        return CommonizedCinteropMetadataLibrariesInGradle(gradleKotlinSourceSet)
    }

    override fun sourcesContainer(sourceSet: KotlinSourceSetModel): KotlinSourcesContainerInGradle {
        val gradleKotlinSourceSet = getKotlinGradleSourceSet(sourceSet)
        return KotlinSourcesContainerInGradle(gradleKotlinSourceSet)
    }
}

internal suspend fun Project.awaitKotlinProjectModelBridge(): KotlinProjectModelBridgeForGradle {
    val existing = extraProperties.getOrNull("KotlinProjectModelBridgeForGradle")
    if (existing != null) return existing as KotlinProjectModelBridgeForGradle

    val model = project.awaitProjectModel()
    val bridge = KotlinProjectModelBridgeForGradle(model,multiplatformExtension)
    createNecessaryGradleTasks(bridge)

    extraProperties.set("KotlinProjectModelBridgeForGradle", bridge)
    return bridge
}

private suspend fun Project.createNecessaryGradleTasks(bridge: KotlinProjectModelBridgeForGradle) {
    val projectModel = bridge.model

    this.commonizeNativeDistributionTask
    for (sourceSetModel in projectModel.sourceSets.values) {
        val gradleSourceSet = bridge.getKotlinGradleSourceSet(sourceSetModel)
        gradleSourceSet as DefaultKotlinSourceSet
        if (sourceSetModel.hasMetadataCompilation) {
            locateOrRegisterMetadataDependencyTransformationTask(gradleSourceSet)
            locateOrRegisterCInteropMetadataDependencyTransformationTask(gradleSourceSet)
        }
    }
}