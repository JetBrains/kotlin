/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.*
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_API
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_METADATA
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_API
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.setAttribute

internal val UklibConsumptionSetupAction = KotlinProjectSetupAction {
    when (project.kotlinPropertiesProvider.kmpResolutionStrategy) {
        KmpResolutionStrategy.ResolveUklibsAndResolvePSMLeniently -> setupUklibConsumption()
        KmpResolutionStrategy.StandardKMPResolution -> { /* do nothing */ }
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
    dependencies.attributesSchema.attribute(USAGE_ATTRIBUTE) { strategy ->
        strategy.compatibilityRules.add(KotlinApiMetadataAndRuntimeCanConsumeKotlinUklibApi::class.java)
        strategy.compatibilityRules.add(AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolutionUsage::class.java)
    }
    dependencies.attributesSchema.attribute(KotlinPlatformType.attribute) { strategy ->
        strategy.compatibilityRules.add(AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolution::class.java)
    }
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

private class KotlinApiMetadataAndRuntimeCanConsumeKotlinUklibApi : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
        val consumerUsage = consumerValue?.name ?: return@with
        val producerUsage = producerValue?.name ?: return@with
        // Allow consuming Uklibs in all existing configurations
        if (
            mapOf(
                KOTLIN_API to KOTLIN_UKLIB_API,
                KOTLIN_METADATA to KOTLIN_UKLIB_API,
                KOTLIN_RUNTIME to KOTLIN_UKLIB_API,
                KOTLIN_RUNTIME to KOTLIN_UKLIB_RUNTIME,
            )[consumerUsage] == producerUsage
        ) compatible()
    }
}

/**
 * Use a nuclear compatibility rules to allow lenient interlibrary resolution of KMP dependencies. When platform configuration is going to
 * resolve for GMT or for platform compilation, it will be allowed to fallback to metadata variant. S
 *
 * - Klib compilations already filter out jar files
 * - For GMT there is further special handling
 * - FIXME: jvm and android will receive 1 garbage klib? Can we write a transform to check for metadata jar? Check for presence of META-INF/kotlin-project-structure-metadata.json?
 */
private class AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolution : AttributeCompatibilityRule<KotlinPlatformType> {
    override fun execute(details: CompatibilityCheckDetails<KotlinPlatformType>) = with(details) {
        consumerValue?.name ?: return@with
        val producer = producerValue?.name ?: return@with
        if (producer == KotlinPlatformType.common.name) compatible()
    }
}

private class AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolutionUsage : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
        val consumerUsage = consumerValue?.name ?: return@with
        val producerUsage = producerValue?.name ?: return@with
        if (
            mapOf(
                KOTLIN_API to KOTLIN_METADATA,
            )[consumerUsage] == producerUsage
        ) compatible()
    }
}