/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseCompilations
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal val UklibConsumptionSetupAction = KotlinProjectSetupAction {
    when (project.kotlinPropertiesProvider.uklibResolutionStrategy) {
        UklibResolutionStrategy.ResolveUklibsInMavenComponents -> project.launch { setupUklibConsumption() }
        UklibResolutionStrategy.IgnoreUklibs -> { /* do nothing */ }
    }
}

/**
 * Resolve Uklib artifacts using transforms:
 * - Request a known [uklibTargetAttributeAttribute] in all resolvable configurations that should be able to resolve uklibs
 * - Register transform "zipped -> unzipped uklib"
 * - Register transform "unzipped uklib -> [uklibTargetAttributeAttribute]"
 */
private suspend fun Project.setupUklibConsumption() {
    val sourceSets = multiplatformExtension.awaitSourceSets()
    val targets = multiplatformExtension.awaitTargets()
    AfterFinaliseCompilations.await()

    registerZippedUklibArtifact()
    allowUklibsToUnzip()
    allowMetadataConfigurationsToResolveUnzippedUklib(sourceSets)
    allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(targets)
}

private fun Project.allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(
    targets: NamedDomainObjectCollection<KotlinTarget>
) {
    targets.configureEach { target ->
        val destinationAttribute = when (target) {
            is KotlinNativeTarget -> target.uklibFragmentPlatformAttribute
            is KotlinJsIrTarget -> target.uklibFragmentPlatformAttribute
            is KotlinJvmTarget -> target.uklibFragmentPlatformAttribute
            else -> return@configureEach
        }

        dependencies.registerTransform(UnzippedUklibToPlatformCompilationTransform::class.java) {
            it.from
                .attribute(uklibStateAttribute, uklibStateUnzipped)
                .attribute(uklibTargetAttributeAttribute, uklibTargetAttributeUnknown)
            it.to
                .attribute(uklibStateAttribute, uklibStateUnzipped)
                .attribute(uklibTargetAttributeAttribute, destinationAttribute.safeToConsume())

            it.parameters.targetFragmentAttribute.set(destinationAttribute.safeToConsume())
            it.parameters.fakeTransform.set(kotlinPropertiesProvider.fakeUkibTransforms)
        }

        // FIXME: Refactor this and encode what configurations should be allowed to transform per KotlinTarget somewhere around [uklibFragmentPlatformAttribute]
        target.compilations.configureEach {
            listOfNotNull(
                it.internal.configurations.compileDependencyConfiguration,
                it.internal.configurations.runtimeDependencyConfiguration,
            ).forEach {
                it.attributes {
                    it.attribute(uklibStateAttribute, uklibStateUnzipped)
                    it.attribute(uklibTargetAttributeAttribute, destinationAttribute.safeToConsume())
                }
            }
        }
    }
}

private fun Project.registerZippedUklibArtifact() {
    with(dependencies.artifactTypes.create(Uklib.UKLIB_EXTENSION).attributes) {
        attribute(uklibStateAttribute, uklibStateZipped)
        attribute(uklibTargetAttributeAttribute, uklibTargetAttributeUnknown)
    }
}

private fun Project.allowUklibsToUnzip() {
    dependencies.registerTransform(UnzipUklibTransform::class.java) {
        it.from.attribute(uklibStateAttribute, uklibStateZipped)
        it.to.attribute(uklibStateAttribute, uklibStateUnzipped)
        it.parameters.performUnzip.set(!kotlinPropertiesProvider.fakeUkibTransforms)
    }
}

private fun Project.allowMetadataConfigurationsToResolveUnzippedUklib(
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) {
    sourceSets.configureEach {
        with(it.internal.resolvableMetadataConfiguration) {
            attributes {
                it.attribute(uklibStateAttribute, uklibStateUnzipped)
                it.attribute(uklibTargetAttributeAttribute, KotlinPlatformType.common.name)
            }
        }
    }
    dependencies.registerTransform(UnzippedUklibToMetadataCompilationTransform::class.java) {
        it.from
            .attribute(uklibStateAttribute, uklibStateUnzipped)
            .attribute(uklibTargetAttributeAttribute, uklibTargetAttributeUnknown)
        it.to
            .attribute(uklibStateAttribute, uklibStateUnzipped)
            .attribute(uklibTargetAttributeAttribute, KotlinPlatformType.common.name)
    }
}


