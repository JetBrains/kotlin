/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.artifacts.VariantMetadata
import org.gradle.api.attributes.*
import org.gradle.api.attributes.Usage.*
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.common
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
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_JAVA_API
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_JAVA_RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_METADATA
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_FALLBACK_VARIANT
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.native.resolvableApiConfiguration
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import org.jetbrains.kotlin.gradle.utils.named
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
    workaroundLegacySkikoResolutionKT77539()
    workaroundKotlinTestJsResolutionKT81387()
    registerGenericFallbackVariantInAllComponentsKT81412()
    makeConfigurationsWithoutAttributesPreferJvmVariantsInKmpPublicationsWithoutCategoryKT81488()
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
            val resolvableConfigurationToUsage = if (compilation.target.platformType in setOf(jvm, androidJvm)) {
                listOfNotNull<Pair<Configuration, Usage>>(
                    compilation.internal.configurations.compileDependencyConfiguration to usageByName(KOTLIN_UKLIB_JAVA_API),
                    compilation.internal.configurations.runtimeDependencyConfiguration?.let { it to usageByName(KOTLIN_UKLIB_JAVA_RUNTIME) },
                )
            } else {
                listOfNotNull<Pair<Configuration, Usage>>(
                    compilation.internal.configurations.compileDependencyConfiguration to usageByName(KOTLIN_UKLIB_API),
                    compilation.internal.configurations.runtimeDependencyConfiguration?.let { it to usageByName(KOTLIN_UKLIB_RUNTIME) },
                )
            }
            resolvableConfigurationToUsage.forEach {
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
                    it.applyUklibAttributes(usageByName(KOTLIN_UKLIB_JAVA_RUNTIME), uklibFragmentPlatformAttribute)
                    it.attributes.attribute<KotlinPlatformType>(
                        KotlinPlatformType.attribute,
                        KotlinPlatformType.jvm,
                    )
                }
                configurations.named(sourceSet.compileClasspathConfigurationName).configure {
                    it.applyUklibAttributes(usageByName(KOTLIN_UKLIB_JAVA_API), uklibFragmentPlatformAttribute)
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
        strategy.compatibilityRules.add(AllowPlatformConfigurationsToFallBackToMetadataAndJvmForLenientKmpResolution::class.java)
        strategy.disambiguationRules.add(SelectBestMatchingKotlinPlatformType::class.java)
    }
}

/**
 * KT-77539: Skiko used to publish with a hacky Android variant which was actually a jvm("android") target. The artifacts weren't actually
 * published to Maven central. In the future Skiko should start publishing with proper androidTarget() attributes, and this hack will no
 * longer be necessary
 */
private fun Project.workaroundLegacySkikoResolutionKT77539() {
    dependencies.components.withModule("org.jetbrains.skiko:skiko") { skikoModule ->
        val configureAndroidEnvironment: (VariantMetadata) -> Unit = {
            it.attributes.attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                project.objects.named(TargetJvmEnvironment.ANDROID),
            )
        }
        skikoModule.withVariant("androidApiElements-published") { configureAndroidEnvironment(it) }
        skikoModule.withVariant("androidRuntimeElements-published") { configureAndroidEnvironment(it) }

        val configureJvmEnvironment: (VariantMetadata) -> Unit = {
            it.attributes.attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                project.objects.named(TargetJvmEnvironment.STANDARD_JVM),
            )
        }
        skikoModule.withVariant("awtApiElements-published") { configureJvmEnvironment(it) }
        skikoModule.withVariant("awtRuntimeElements-published") { configureJvmEnvironment(it) }

        // Workaround for KT-81459
        listOf(
            "metadataApiElements",
            "metadataSourcesElements",
            "iosArm64ApiElements-published",
            "iosArm64SourcesElements-published",
            "iosArm64MetadataElements-published",
            "iosSimulatorArm64ApiElements-published",
            "iosSimulatorArm64SourcesElements-published",
            "iosSimulatorArm64MetadataElements-published",
            "iosX64ApiElements-published",
            "iosX64SourcesElements-published",
            "iosX64MetadataElements-published",
            "jsApiElements-published",
            "jsRuntimeElements-published",
            "jsSourcesElements-published",
            "linuxArm64ApiElements-published",
            "linuxArm64SourcesElements-published",
            "linuxX64ApiElements-published",
            "linuxX64SourcesElements-published",
            "macosArm64ApiElements-published",
            "macosArm64SourcesElements-published",
            "macosArm64MetadataElements-published",
            "macosX64ApiElements-published",
            "macosX64SourcesElements-published",
            "macosX64MetadataElements-published",
            "tvosArm64ApiElements-published",
            "tvosArm64SourcesElements-published",
            "tvosArm64MetadataElements-published",
            "tvosSimulatorArm64ApiElements-published",
            "tvosSimulatorArm64SourcesElements-published",
            "tvosSimulatorArm64MetadataElements-published",
            "tvosX64ApiElements-published",
            "tvosX64SourcesElements-published",
            "tvosX64MetadataElements-published",
            "wasmJsApiElements-published",
            "wasmJsRuntimeElements-published",
            "wasmJsSourcesElements-published",
        ).forEach { variantName ->
            skikoModule.withVariant(variantName) {
                it.attributes.attribute(
                    TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    project.objects.named(NON_JVM_ENVIRONMENT),
                )
            }
        }
    }
}

private fun Project.workaroundKotlinTestJsResolutionKT81387() {
    dependencies.components.withModule("org.jetbrains.kotlin:kotlin-test-js") {
        it.addVariant("stubKotlinTestJsFallbackFor_KT-81387") {
            it.attributes.attribute(
                USAGE_ATTRIBUTE,
                project.objects.named(KOTLIN_METADATA),
            )
            it.attributes.attribute(
                Category.CATEGORY_ATTRIBUTE,
                project.categoryByName(Category.LIBRARY),
            )
            it.attributes.attribute(
                KotlinPlatformType.attribute,
                common,
            )
        }
    }
}

private fun Project.registerGenericFallbackVariantInAllComponentsKT81412() {
    dependencies.components.all {
        it.addVariant("fallbackVariant_KT-81412") {
            it.attributes.attribute(
                USAGE_ATTRIBUTE,
                project.objects.named(KOTLIN_UKLIB_FALLBACK_VARIANT),
            )
            it.attributes.attribute(
                Category.CATEGORY_ATTRIBUTE,
                project.categoryByName(Category.LIBRARY),
            )
        }
    }
}

private fun Project.makeConfigurationsWithoutAttributesPreferJvmVariantsInKmpPublicationsWithoutCategoryKT81488() {
    dependencies.components.all {
        it.withVariant("jvmApiElements-published") {
            it.attributes.attribute(
                Category.CATEGORY_ATTRIBUTE,
                project.categoryByName(Category.LIBRARY),
            )
        }
        it.withVariant("jvmRuntimeElements-published") {
            it.attributes.attribute(
                Category.CATEGORY_ATTRIBUTE,
                project.categoryByName(Category.LIBRARY),
            )
        }
    }
}

private class AllowPlatformConfigurationsToFallBackToMetadataAndJvmForLenientKmpResolution : AttributeCompatibilityRule<KotlinPlatformType> {
    override fun execute(details: CompatibilityCheckDetails<KotlinPlatformType>) = with(details) {
        consumerValue?.name ?: return@with
        // Fallback to metadata
        if (producerValue == common)
            compatible()

        // Fallback to Kotlin JVM
        if (producerValue == jvm)
            compatible()
    }
}

private class SelectBestMatchingKotlinPlatformType : AttributeDisambiguationRule<KotlinPlatformType> {
    override fun execute(details: MultipleCandidatesDetails<KotlinPlatformType>) {
        val matchingValue = details.candidateValues.singleOrNull { it == details.consumerValue }
        if (matchingValue != null) {
            details.closestMatch(matchingValue)
        }
    }
}

internal class AllowPlatformConfigurationsToFallBackToMetadataForLenientKmpResolutionUsage : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
        val consumerUsage = consumerValue?.name ?: return@with
        val producerUsage = producerValue?.name ?: return@with

        val apiElements = setOf(
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
             * Classified JVM POM dependencies are synthesized as "java-runtime": KT-81467
             */
            JAVA_RUNTIME,
            /**
             * Fallback to metadata variant to inherit dependencies for lenient interlibrary dependencies. Platform configurations
             * throw away metadata jars in [ThrowAwayMetadataJarsTransform]. GMT now always resolves and special-handles the case
             * when platform configurations resolved into metadata jar.
             *
             * KotlinPlatformType platform -> common compatibility is enabled by [AllowPlatformConfigurationsToFallBackToMetadataAndJvmForLenientKmpResolution]
             */
            KOTLIN_METADATA,
            KOTLIN_UKLIB_FALLBACK_VARIANT,
        )
        val runtimeElements = setOf(
            /**
             * Same as above, compatibility with current KMP publication
             */
            KOTLIN_RUNTIME,
            /**
             * Compatibility with all the Maven POM-only and Gradle JVM producers
             *
             * FIXME: This compatibility rule is wrong: KT-81349
             */
            JAVA_RUNTIME,
            /**
             * Same as above. Fallback to metadata variant to resolve and inherit dependencies
             */
            KOTLIN_METADATA,
            /**
             * Handle pre-HMPP metadata and specifically dom-api-compat
             *
             * FIXME: Remove this case in KT-81350
             */
            KOTLIN_API,
            KOTLIN_UKLIB_FALLBACK_VARIANT,
        )
        if (
            mapOf(
                /**
                 * Fallback variant for Android resolution
                 * Android compile/runtime classpath request the JAVA_* usages. Since Android is not published in the UKlib and we don't
                 * support lenient resolution in Android variants we register a fallback without dependencies for Android. The only use case
                 * should be pre-UKlib KMP publication.
                 *
                 * We should remove this compatibility in the future when all KMP libraries will have a proper or stub JVM variant
                 */
                JAVA_API to setOf(KOTLIN_UKLIB_FALLBACK_VARIANT),
                JAVA_RUNTIME to setOf(KOTLIN_UKLIB_FALLBACK_VARIANT),
                /**
                 * KOTLIN_UKLIB_API is requested in platform compile dependency configurations
                 */
                KOTLIN_UKLIB_API to apiElements,
                KOTLIN_UKLIB_JAVA_API to apiElements + setOf(KOTLIN_UKLIB_API),
                /**
                 * KOTLIN_UKLIB_RUNTIME is requested in platform runtime dependency configurations
                 */
                KOTLIN_UKLIB_RUNTIME to runtimeElements,
                KOTLIN_UKLIB_JAVA_RUNTIME to runtimeElements + setOf(KOTLIN_UKLIB_RUNTIME),
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
                     * FIXME: Remove this case in KT-81350
                     */
                    KOTLIN_API,
                    /**
                     * Compatibility with all the Maven POM-only and Gradle JVM producers for dependency inheritance
                     */
                    JAVA_API,
                    /**
                     * Classified JVM POM dependencies are synthesized as "java-runtime": KT-81467
                     */
                    JAVA_RUNTIME,
                    KOTLIN_UKLIB_FALLBACK_VARIANT,
                ),
            )[consumerUsage]?.contains(producerUsage) == true
        ) compatible()
    }
}

internal class SelectBestMatchingVariantForKmpResolutionUsage : AttributeDisambiguationRule<Usage> {
    override fun execute(details: MultipleCandidatesDetails<Usage>) = details.run {
        val consumerUsage = consumerValue?.name ?: return@run

        /**
         * FIXME: Right now this disambiguation rule gets registered after [JavaEcosystemSupport.UsageDisambiguationRules] and
         * disambiguation logic when e.g. consumerValue == KOTLIN_UKLIB_API and KOTLIN_UKLIB_API was in the candidate set is not actually
         * selected here, but is instead accidentally called from the java rule.
         *
         * To make sure our rule is in control, we can try to reshuffle rules in [setAttributeDisambiguationPrecedence]
         */
        val nonJvmApiElements = listOf(
            KOTLIN_UKLIB_API,
            /**
             * Prefer platform apiElements if it is available when consuming standard KMP publication for compilation
             *
             * FIXME: Is this also a compatibility for dom-api-compat
             */
            KOTLIN_API,
        )
        val jvmApiElements = listOf(
            KOTLIN_UKLIB_API,
        )
        val runtimeElements = listOf(
            /**
             * Prefer UKlib runtime
             */
            KOTLIN_UKLIB_RUNTIME,
            /**
             * If we are looking at a pre-UKlib component, select the respective runtime
             */
            KOTLIN_RUNTIME,
        )
        mapOf(
            KOTLIN_UKLIB_JAVA_API to jvmApiElements,
            KOTLIN_UKLIB_API to nonJvmApiElements,
            KOTLIN_UKLIB_JAVA_RUNTIME to runtimeElements,
            KOTLIN_UKLIB_RUNTIME to runtimeElements,
            KOTLIN_UKLIB_METADATA to listOf(
                /**
                 * Prefer metadata from Uklib
                 */
                KOTLIN_UKLIB_API,
                /**
                 * Otherwise select PSM jar
                 */
                KOTLIN_METADATA,
                JAVA_API,
                JAVA_RUNTIME,
            ),
        )[consumerUsage]?.let {
            if (closestMatchToFirstAppropriateCandidate(it)) {
                return@run
            }
        }

        val candidateValuesSet = details.candidateValues.map { it.name }.toSet()
        val isRootKmpComponentWithJvm =
            candidateValuesSet.containsAll(listOf(JAVA_API, KOTLIN_METADATA))
                    || candidateValuesSet.containsAll(listOf(JAVA_RUNTIME, KOTLIN_METADATA))
        if (isRootKmpComponentWithJvm) {
            when (consumerUsage) {
                KOTLIN_UKLIB_API, KOTLIN_UKLIB_RUNTIME -> {
                    details.closestMatchToFirstAppropriateCandidate(listOf(KOTLIN_METADATA))
                    return@run
                }
                KOTLIN_UKLIB_JAVA_API -> {
                    details.closestMatchToFirstAppropriateCandidate(listOf(JAVA_API))
                    return@run
                }
                KOTLIN_UKLIB_JAVA_RUNTIME -> {
                    details.closestMatchToFirstAppropriateCandidate(listOf(JAVA_RUNTIME))
                    return@run
                }
            }
        }

        val isJvmOnlyComponent = candidateValuesSet.containsAll(listOf(JAVA_API))
                || candidateValuesSet.containsAll(listOf(JAVA_RUNTIME))
        if (isJvmOnlyComponent) {
            when (consumerUsage) {
                KOTLIN_UKLIB_API, KOTLIN_UKLIB_JAVA_API -> {
                    details.closestMatchToFirstAppropriateCandidate(listOf(JAVA_API, JAVA_RUNTIME))
                    return@run
                }
                KOTLIN_UKLIB_RUNTIME, KOTLIN_UKLIB_JAVA_RUNTIME -> {
                    details.closestMatchToFirstAppropriateCandidate(listOf(JAVA_RUNTIME))
                    return@run
                }
            }
        }

        if (details.candidateValues.map { it.name }.toSet().containsAll(listOf(KOTLIN_METADATA))) {
            details.closestMatchToFirstAppropriateCandidate(listOf(KOTLIN_METADATA))
            return@run
        }

        if (details.candidateValues.map { it.name }.toSet().containsAll(listOf(KOTLIN_UKLIB_FALLBACK_VARIANT))) {
            details.closestMatchToFirstAppropriateCandidate(listOf(KOTLIN_UKLIB_FALLBACK_VARIANT))
            return@run
        }

        return@run
    }

    private fun MultipleCandidatesDetails<Usage>.closestMatchToFirstAppropriateCandidate(acceptedProducerValues: List<String>): Boolean {
        val candidatesMap = candidateValues.associateBy { it.name }
        return acceptedProducerValues.firstOrNull { it in candidatesMap }?.let { closestMatch(candidatesMap.getValue(it)); true } ?: false
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
