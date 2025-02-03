/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.*
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

    dependencies.attributesSchema.attribute(USAGE_ATTRIBUTE) { strategy ->
        strategy.compatibilityRules.add(KotlinApiMetadataAndRuntimeCanConsumeKotlinUklibApi::class.java)
    }

    registerCompressedUklibArtifact()
    allowUklibsToDecompress()
    allowMetadataConfigurationsToResolveUnzippedUklib(sourceSets)
    allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(targets)
    allowPSMBasedKMPToResolveLeniently(targets)
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
            // We have to invert all resolvable configurations, so that they don't prefer jvm compatibility variant in uklib publication
            with(it.internal.configurations.compileDependencyConfiguration.attributes) {
                setAttribute(USAGE_ATTRIBUTE, usageByName(KOTLIN_UKLIB_API))
            }
            it.internal.configurations.runtimeDependencyConfiguration?.attributes?.let {
                with(it) {
                    setAttribute(USAGE_ATTRIBUTE, usageByName(KOTLIN_UKLIB_RUNTIME))
                }
            }

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
                KOTLIN_METADATA to setOf(KOTLIN_UKLIB_API),
//                KOTLIN_API to setOf(KOTLIN_UKLIB_API),
//                KOTLIN_RUNTIME to setOf(KOTLIN_UKLIB_API, KOTLIN_UKLIB_RUNTIME),
            )[consumerUsage]?.contains(producerUsage) == true
        ) compatible()
    }
}

/**
 * Use a nuclear compatibility rules to allow lenient interlibrary resolution of KMP dependencies. When platform configuration is going to
 * resolve for GMT or for platform compilation, it will be allowed to fallback to metadata variant. S
 *
 * - Klib compilations already filter out jar files
 * - For GMT there is further special handling
 * - FIXME: jvm and android will receive 1 garbage jar? Can we write a transform to check for metadata jar? Check for presence of META-INF/kotlin-project-structure-metadata.json?
 */
private fun Project.allowPSMBasedKMPToResolveLeniently(
    targets: NamedDomainObjectCollection<KotlinTarget>
) {
    dependencies.attributesSchema.attribute(USAGE_ATTRIBUTE) { strategy ->
        strategy.compatibilityRules.add(AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolutionUsage::class.java)
        strategy.disambiguationRules.add(DisambiguatePlatformConfigurationsToFallBackToMetadataForLenientKmpResolutionUsage::class.java)
    }
    dependencies.attributesSchema.attribute(KotlinPlatformType.attribute) { strategy ->
        strategy.compatibilityRules.add(AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolution::class.java)
    }
    with(dependencies.artifactTypes.getByName("jar").attributes) {
        setAttribute(isMetadataJar, isMetadataJarUnknown)
    }
    dependencies.registerTransform(ThrowAwayMetadataJarsTransform::class.java) {
        it.from.setAttribute(isMetadataJar, isMetadataJarUnknown)
        it.to.setAttribute(isMetadataJar, notMetadataJar)
    }
    targets.configureEach {
        if (it is KotlinNativeTarget || it is KotlinJsIrTarget || it is KotlinJvmTarget
            // || it is KotlinAndroidTarget
            ) {
            it.compilations.configureEach {
                listOfNotNull(
                    it.internal.configurations.compileDependencyConfiguration,
                    it.internal.configurations.runtimeDependencyConfiguration,
                ).forEach {
                    with(it.attributes) {
                        setAttribute(isMetadataJar, notMetadataJar)
                    }
                }
            }
        }
    }
}

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
                // Platform compile dependency configuration
                KOTLIN_UKLIB_API to setOf(
                    // Allow uklib consumer to resolve regular KMP platform apiElements. The exact selection is still controlled with other attributes
                    KOTLIN_API,
                    // FIXME: Allow selecting java-only? Test this
                    JAVA_API,
                    // Fallback to metadata variant to inherit dependencies like in uklib publication
                    // stdlib doesn't have native variants, so for native platform configuration must fall back here
                    // runtime also???
                    KOTLIN_METADATA
                ),
                // FIXME: Test these
                KOTLIN_UKLIB_RUNTIME to setOf(KOTLIN_API, KOTLIN_RUNTIME, JAVA_RUNTIME),

                // but dom-api-compat has compatibility variant, but the usage is wrong, what???
                KOTLIN_METADATA to setOf(KOTLIN_API),
            )[consumerUsage]?.contains(producerUsage) == true
        ) compatible()
    }
}

private class DisambiguatePlatformConfigurationsToFallBackToMetadataForLenientKmpResolutionUsage : AttributeDisambiguationRule<Usage> {
    override fun execute(details: MultipleCandidatesDetails<Usage>) = details.run {
        val consumerUsage = consumerValue?.name ?: return@run

        mapOf(
            KOTLIN_UKLIB_API to listOf(
                // We are selecting for platform compilation. Prefer platform apiElements if it is available
                KOTLIN_API,
                // FIXME: Check if this is correct
                JAVA_API,
                // Fallback to metadata if platform apiElements is not available. In GMT this selection is filtered to determine visibility
                KOTLIN_METADATA
            ),
            KOTLIN_UKLIB_RUNTIME to listOf(KOTLIN_RUNTIME, KOTLIN_API, JAVA_RUNTIME, JAVA_API, KOTLIN_METADATA),
        )[consumerUsage]?.let {
            closestMatchToFirstAppropriateCandidate(it)
        }
        return@run
    }

    private fun MultipleCandidatesDetails<Usage>.closestMatchToFirstAppropriateCandidate(acceptedProducerValues: List<String>) {
        val candidatesMap = candidateValues.associateBy { it.name }
        acceptedProducerValues.firstOrNull { it in candidatesMap }?.let { closestMatch(candidatesMap.getValue(it)) }
    }
}

private val isMetadataJar = Attribute.of("org.jetbrains.kotlin.isMetadataJar", String::class.java)
internal val isMetadataJarUnknown = "unknown"
internal val notMetadataJar = "non-a-metadata-jar"