/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
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
import org.jetbrains.kotlin.gradle.utils.setAttribute

internal val UklibConsumptionSetupAction = KotlinProjectSetupAction {
    when (project.kotlinPropertiesProvider.uklibResolutionStrategy) {
        UklibResolutionStrategy.ResolveUklibsInMavenComponents -> setupUklibConsumption()
        UklibResolutionStrategy.IgnoreUklibs -> { /* do nothing */ }
    }
}

/**
 * Resolve Uklib artifacts using transforms:
 * - Request a known [uklibViewAttribute] in all resolvable configurations that should be able to resolve uklibs
 * - Register transform "zipped -> unzipped uklib"
 * - Register transform "unzipped uklib -> [uklibViewAttribute]"
 */
private fun Project.setupUklibConsumption() {
    val sourceSets = multiplatformExtension.sourceSets
    val targets = multiplatformExtension.targets

    registerCompressedUklibArtifact()
    allowUklibsToDecompress()
    allowMetadataConfigurationsToResolveUnzippedUklib(sourceSets)
    allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(targets)
}

private fun Project.allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(
    targets: NamedDomainObjectCollection<KotlinTarget>
) {
    targets.configureEach { target ->
        /**
         * We use this attribute:
         *  1. To force the transform through Gradle attributes. The value itself doesn't matter, but it needs to be consistent with the
         *  value requested by the target's resolvable configuration
         *  2. To select the single matching fragment from the Uklib by the respective attribute value
         */
        val uklibFragmentPlatformAttribute = when (target) {
            is KotlinNativeTarget -> target.uklibFragmentPlatformAttribute
            is KotlinJsIrTarget -> target.uklibFragmentPlatformAttribute
            is KotlinJvmTarget -> target.uklibFragmentPlatformAttribute
            else -> return@configureEach
        }.safeToConsume()

        dependencies.registerTransform(UnzippedUklibToPlatformCompilationTransform::class.java) {
            with(it.from) {
                setAttribute(uklibStateAttribute, uklibStateDecompressed)
                setAttribute(uklibViewAttribute, uklibViewAttributeWholeUklib)
            }
            with(it.to) {
                setAttribute(uklibStateAttribute, uklibStateDecompressed)
                setAttribute(uklibViewAttribute, uklibFragmentPlatformAttribute)
            }

            it.parameters.targetFragmentAttribute.set(uklibFragmentPlatformAttribute)
            it.parameters.fakeTransform.set(kotlinPropertiesProvider.fakeUkibTransforms)
        }

        // FIXME: Refactor this and encode what configurations should be allowed to transform per KotlinTarget somewhere around [uklibFragmentPlatformAttribute]
        target.compilations.configureEach {
            listOfNotNull(
                it.internal.configurations.compileDependencyConfiguration,
                it.internal.configurations.runtimeDependencyConfiguration,
            ).forEach {
                with(it.attributes) {
                    setAttribute(uklibStateAttribute, uklibStateDecompressed)
                    setAttribute(uklibViewAttribute, uklibFragmentPlatformAttribute)
                }
            }
        }
    }
}

private fun Project.registerCompressedUklibArtifact() {
    with(dependencies.artifactTypes.create(Uklib.UKLIB_EXTENSION).attributes) {
        setAttribute(uklibStateAttribute, uklibStateCompressed)
        setAttribute(uklibViewAttribute, uklibViewAttributeWholeUklib)
    }
}

private fun Project.allowUklibsToDecompress() {
    dependencies.registerTransform(UnzipUklibTransform::class.java) {
        it.from.setAttribute(uklibStateAttribute, uklibStateCompressed)
        it.to.setAttribute(uklibStateAttribute, uklibStateDecompressed)
        it.parameters.fakeUklibUnzip.set(kotlinPropertiesProvider.fakeUkibTransforms)
    }
}

private fun allowMetadataConfigurationsToResolveUnzippedUklib(
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) {
    sourceSets.configureEach {
        with(it.internal.resolvableMetadataConfiguration.attributes) {
            setAttribute(uklibStateAttribute, uklibStateDecompressed)
            setAttribute(uklibViewAttribute, uklibViewAttributeWholeUklib)
        }
    }
}


