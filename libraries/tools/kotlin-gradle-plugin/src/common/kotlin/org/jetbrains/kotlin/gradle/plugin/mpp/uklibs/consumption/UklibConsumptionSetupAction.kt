/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.*
import org.gradle.api.attributes.Usage.*
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractExecutable
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_API
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_METADATA
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_API
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_METADATA
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.native.resolvableApiConfiguration
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import org.jetbrains.kotlin.gradle.utils.registerTransformForArtifactType

internal val UklibConsumptionSetupAction = KotlinProjectSetupAction {
    when (project.kotlinPropertiesProvider.kmpResolutionStrategy) {
        KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs -> setupUklibConsumption()
        KmpResolutionStrategy.StandardKMPResolution -> { /* do nothing */ }
    }
}

/**
 * Thread Uklib artifacts through existing resolvable configurations using transforms:
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
    allowPSMBasedKMPToResolveLenientlyAndSelectBestMatchingVariant()
    allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(targets)
}

private fun Project.allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(
    targets: NamedDomainObjectCollection<KotlinTarget>
) {
    targets.configureEach { target ->
        /**
         * We use the [uklibFragmentPlatformAttribute]:
         *  1. To force the transform through Gradle attributes. At this stage the value just needs to be consistent with the value
         *  requested by the target's resolvable configuration so that transform runs
         *  2. To select the single matching fragment from the Uklib during the transform by the respective attribute value in the Umanifest
         */
        val uklibFragmentPlatformAttribute = when (target.uklibFragmentPlatformAttribute) {
            is UklibFragmentPlatformAttribute.ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest -> target.uklibFragmentPlatformAttribute.convertToStringForConsumption()
            is UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndPublishInUmanifest,
            is UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndFailOnPublication,
            is UklibFragmentPlatformAttribute.FailOnConsumptionAndPublication
                -> return@configureEach
        }

        dependencies.registerTransformForArtifactType(
            UnzippedUklibToPlatformCompilationTransform::class.java,
            fromArtifactType = uklibArtifactType,
            toArtifactType = uklibArtifactType,
        ) {
            with(it.from) {
                attribute(uklibStateAttribute, uklibStateDecompressed)
                attribute(uklibViewAttribute, uklibViewAttributeWholeUklib)
            }
            with(it.to) {
                attribute(uklibStateAttribute, uklibStateDecompressed)
                attribute(uklibViewAttribute, uklibFragmentPlatformAttribute)
            }

            it.parameters.targetFragmentAttribute.set(uklibFragmentPlatformAttribute)
        }

        /**
         * FIXME: Changing the requested Usage in jvm configurations will influence existing compatibility/disambiguation rules. Maybe
         * that is desirable, but maybe there should be a flag to opt-out
         */
        /**
         * FIXME: This set of configurations is not correct. At the least:
         * - hostSpecificMetadataConfiguration probably must be able to resolve leniently
         */
        target.compilations.configureEach { compilation ->
            /**
             * All resolvable configurations should select for KOTLIN_UKLIB_API Usage. Relying on Usage specifically is crucial because it
             * is an attribute with an explicit precedence and if there is a disambiguation rule that reduces the set of compatible variants
             * to 1 using Usage, the disambiguation phase ends which is what we want in [SelectBestMatchingVariantForKmpResolutionUsage].
             */
            compilation as InternalKotlinCompilation
            listOfNotNull<Pair<Configuration, Usage>>(
                compilation.internal.configurations.compileDependencyConfiguration to usageByName(KOTLIN_UKLIB_API),
                compilation.internal.configurations.runtimeDependencyConfiguration?.let { it to usageByName(KOTLIN_UKLIB_RUNTIME) },
            ).forEach {
                it.first.applyUklibAttributes(it.second, uklibFragmentPlatformAttribute)
            }
        }

        if (target is KotlinNativeTarget) {
            target.binaries.all {
                val uklibResolvingConfiguration = when (it) {
                    is Framework -> it.exportConfigurationName
                    is AbstractNativeLibrary -> it.exportConfigurationName
                    is AbstractExecutable -> {
                        null
                    }
                }
                if (uklibResolvingConfiguration != null) {
                    configurations.named(uklibResolvingConfiguration).configure {
                        it.applyUklibAttributes(usageByName(KOTLIN_UKLIB_API), uklibFragmentPlatformAttribute)
                    }
                }
            }
            target.compilations.configureEach { compilation ->
                compilation.resolvableApiConfiguration().applyUklibAttributes(
                    usageByName(KOTLIN_UKLIB_API),
                    uklibFragmentPlatformAttribute,
                )
            }
        }

        if (target is KotlinJvmTarget) {
            /**
             * FIXME: Unit test that Java base plugin resolvable configurations can consume Uklibs. Right now this is only covered by crude IT
             */
            javaSourceSets.configureEach { sourceSet ->
                configurations.named(sourceSet.runtimeClasspathConfigurationName).configure {
                    it.applyUklibAttributes(usageByName(KOTLIN_UKLIB_RUNTIME), uklibFragmentPlatformAttribute)
                    it.attributes.attribute<KotlinPlatformType>(
                        KotlinPlatformType.attribute,
                        KotlinPlatformType.jvm,
                    )
                }
                configurations.named(sourceSet.compileClasspathConfigurationName).configure {
                    it.applyUklibAttributes(usageByName(KOTLIN_UKLIB_API), uklibFragmentPlatformAttribute)
                    it.attributes.attribute<KotlinPlatformType>(
                        KotlinPlatformType.attribute,
                        KotlinPlatformType.jvm,
                    )
                }
            }
        }
    }
}

private fun Configuration.applyUklibAttributes(
    usage: Usage,
    uklibFragmentPlatformAttribute: String,
) {
    with(attributes) {
        attribute(USAGE_ATTRIBUTE, usage)
        attribute(uklibStateAttribute, uklibStateDecompressed)
        attribute(uklibViewAttribute, uklibFragmentPlatformAttribute)
        attribute(isUklib, isUklibTrue)
    }
}

private fun Project.registerCompressedUklibArtifact() {
    with(dependencies.artifactTypes.create(Uklib.UKLIB_EXTENSION).attributes) {
        attribute(uklibStateAttribute, uklibStateCompressed)
        attribute(uklibViewAttribute, uklibViewAttributeWholeUklib)
    }
}

private fun Project.allowUklibsToDecompress() {
    dependencies.registerTransformForArtifactType(
        UnzipUklibTransform::class.java,
        fromArtifactType = uklibArtifactType,
        toArtifactType = uklibArtifactType,
    ) {
        it.from.attribute(uklibStateAttribute, uklibStateCompressed)
        it.to.attribute(uklibStateAttribute, uklibStateDecompressed)
    }
}

private fun Project.allowMetadataConfigurationsToResolveUnzippedUklib(
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) {
    sourceSets.configureEach {
        with(it.internal.resolvableMetadataConfiguration.attributes) {
            attribute(USAGE_ATTRIBUTE, usageByName(KOTLIN_UKLIB_METADATA))
            attribute(uklibStateAttribute, uklibStateDecompressed)
            attribute(uklibViewAttribute, uklibViewAttributeWholeUklib)
            attribute(isUklib, isUklibTrue)
        }
    }
}

/**
 * Use compatibility rules to allow lenient interlibrary resolution of KMP dependencies:
 * - Make everyone compatible with "common" KotlinPlatformType to allow platform configurations to fall back to metadata for dependencies inheritance
 * - The Usage disambiguation rule controls the preference for selected variant
 */
private fun Project.allowPSMBasedKMPToResolveLenientlyAndSelectBestMatchingVariant() {
    dependencies.attributesSchema.attribute(USAGE_ATTRIBUTE) { strategy ->
        strategy.compatibilityRules.add(AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolutionUsage::class.java)
        strategy.disambiguationRules.add(SelectBestMatchingVariantForKmpResolutionUsage::class.java)
    }
    dependencies.attributesSchema.attribute(KotlinPlatformType.attribute) { strategy ->
        strategy.compatibilityRules.add(AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolution::class.java)
    }
}

private class AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolution : AttributeCompatibilityRule<KotlinPlatformType> {
    override fun execute(details: CompatibilityCheckDetails<KotlinPlatformType>) = with(details) {
        consumerValue?.name ?: return@with
        val producer = producerValue?.name ?: return@with
        if (producer == KotlinPlatformType.common.name) compatible()
    }
}

internal class AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolutionUsage : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
        val consumerUsage = consumerValue?.name ?: return@with
        val producerUsage = producerValue?.name ?: return@with
        if (
            mapOf(
                /**
                 * KOTLIN_UKLIB_API is requested in platform compile dependency configurations
                 */
                KOTLIN_UKLIB_API to setOf(
                    /**
                     * Allow Uklib consumer to resolve regular KMP platform apiElements. Compile dependency configurations continue
                     * requesting platform-specific KMP attributes, so the exact selection is still controlled with attributes like
                     * KotlinPlatformType and konanTargetAttribute
                     */
                    KOTLIN_API,
                    /**
                     * Allow selecting Maven POM-only and Gradle JVM components as they will have JAVA_API usage. In klib compilations we
                     * already filter out non .klib files.
                     *
                     * FIXME: Do we have to reproduce other Java-specific compatibility rules here? In runtime as well?
                     */
                    JAVA_API,
                    /**
                     * Fallback to metadata variant to inherit dependencies for lenient interlibrary dependencies. Platform configurations
                     * throw away metadata jars in [ThrowAwayMetadataJarsTransform]. GMT now always resolves and special-handles the case
                     * when platform configurations resolved into metadata jar.
                     *
                     * KotlinPlatformType platform -> common compatibility is enabled by [AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolution]
                     */
                    KOTLIN_METADATA
                ),
                /**
                 * KOTLIN_UKLIB_RUNTIME is requested in platform runtime dependency configurations
                 */
                KOTLIN_UKLIB_RUNTIME to setOf(
                    /**
                     * Same as above, compatibility with current KMP publication
                     */
                    KOTLIN_RUNTIME,
                    /**
                     * Compatibility with all the Maven POM-only and Gradle JVM producers
                     */
                    JAVA_RUNTIME,
                    JAVA_API,
                    /**
                     * Same as above. Fallback to metadata variant to resolve and inherit dependencies
                     */
                     KOTLIN_METADATA,
                    /**
                     * Handle pre-HMPP metadata and specifically dom-api-compat
                     */
                     KOTLIN_API,
                ),
                /**
                 * KOTLIN_UKLIB_METADATA is requested in per source set resolvableMetadataConfigurations. This Usage isn't published.
                 * Disambiguation is supposed to work through KotlinPlatformType
                 * FIXME: No, disambiguation is supposed to work through [SelectBestMatchingVariantForKmpResolutionUsage]
                 * recheck and update the comment above
                 *
                 * FIXME: There is a quirk in KotlinPlatformType.CompatibilityRule where "common" can select any other platform type. We
                 * should probably disable this compatibility rules for Uklib consumption to avoid resolution ambiguity
                 */
                KOTLIN_UKLIB_METADATA to setOf(
                    /**
                     * Allow selecting Uklib
                     */
                    KOTLIN_UKLIB_API,
                    /**
                     * Select PSM jar in current KMP publication. KotlinPlatformType controls which PSM jar gets selected in case of
                     * host-specific metadata
                     *
                     * FIXME: Fix and test host-specific resolvable configurations, right now they will not resolve?
                     */
                    KOTLIN_METADATA,
                    /**
                     * Handle pre-HMPP metadata and specifically dom-api-compat
                     *
                     * FIXME: Test against exact pre-HMPP publication metadata instead of dom-api-compat
                     */
                    KOTLIN_API,
                    /**
                     * Compatibility with all the Maven POM-only and Gradle JVM producers for dependency inheritance
                     */
                    JAVA_API,
                ),
            )[consumerUsage]?.contains(producerUsage) == true
        ) compatible()
    }
}

internal class SelectBestMatchingVariantForKmpResolutionUsage : AttributeDisambiguationRule<Usage> {
    override fun execute(details: MultipleCandidatesDetails<Usage>) = details.run {
        val consumerUsage = consumerValue?.name ?: return@run

        details.candidateValues

        /**
         * FIXME: Right now this disambiguation rule gets registered after [JavaEcosystemSupport.UsageDisambiguationRules] and
         * disambiguation logic when e.g. consumerValue == KOTLIN_UKLIB_API and KOTLIN_UKLIB_API was in the candidate set is not actually
         * selected here, but is instead accidentally called from the java rule.
         *
         * To make sure our rule is in control, we can try to reshuffle rules in [setAttributeDisambiguationPrecedence]
         */
        mapOf(
            KOTLIN_UKLIB_API to listOf(
                KOTLIN_UKLIB_API,
                /**
                 * Prefer platform apiElements if it is available when consuming standard KMP publication for compilation
                 */
                KOTLIN_API,
                /**
                 * Same as above. Means this is jvm compile dependency configuration
                 *
                 * FIXME: Maybe just request a different Usage for JVM instead?
                 */
                JAVA_API,
                /**
                 * Fallback to metadata if the platform is not available
                 */
                KOTLIN_METADATA
            ),
            KOTLIN_UKLIB_RUNTIME to listOf(
                /**
                 * Prefer UKlib runtime
                 */
                KOTLIN_UKLIB_RUNTIME,
                /**
                 * If we are looking at a pre-UKlib component, select the respective runtime
                 */
                KOTLIN_RUNTIME,
                /**
                 * If we are looking at a JVM component also take the runtime
                 */
                JAVA_RUNTIME,
                JAVA_API,
                /**
                 * Fallback to metadata if the platform is not available
                 */
                KOTLIN_METADATA,
            ),
            KOTLIN_UKLIB_METADATA to listOf(
                /**
                 * Prefer metadata from Uklib
                 */
                KOTLIN_UKLIB_API,
                /**
                 * Otherwise select PSM jar
                 */
                KOTLIN_METADATA,
                /**
                 * FIXME: Java case probably doesn't need disambiguation?
                 */
            ),
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

/**
 * This attribute is crucial to ensure that in a component with a mix of Uklib and KMP variants, Gradle skips over the "longest match is
 * super set" check in [MultipleCandidateMatcher.longestMatchIsSuperSetOfAllOthers] and proceeds to disambiguation phase. This attribute
 * must be published and must only exist on Uklib variants.
 *
 * See [UklibResolutionTestsWithMockComponents]
 */
internal val isUklib = Attribute.of("org.jetbrains.kotlin.uklib", String::class.java)
internal val isUklibTrue = "true"