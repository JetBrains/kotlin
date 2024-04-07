/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.gradle.api.attributes.Usage.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.*
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled

object KotlinUsages {
    const val KOTLIN_API = "kotlin-api"
    const val KOTLIN_RUNTIME = "kotlin-runtime"
    const val KOTLIN_METADATA = "kotlin-metadata"

    /**
     * Platform CInterop usage:
     * These are CInterop files that represent executable .klibs for a given konan target
     * ! NOTE !: This usage is compatible with [KOTLIN_API], [JAVA_API] and [JAVA_RUNTIME]
     */
    const val KOTLIN_CINTEROP = "kotlin-cinterop"

    /**
     * Commonized CInterop usage:
     * This CInterops are produced by the commonizer.
     * ! Note !: This usage is intended only for project to project dependencies.
     * Unlike [KOTLIN_CINTEROP] this usage is not marked compatible with [KOTLIN_API], [JAVA_API] or [JAVA_RUNTIME]
     */
    const val KOTLIN_COMMONIZED_CINTEROP = "kotlin-commonized-cinterop"
    const val KOTLIN_SOURCES = "kotlin-sources"

    /**
     * Multiplatform resources usage:
     * Resources variants for native, wasmJs and wasmWasi targets publish with these usages. To resolve a resources configuration with
     * transitive dependencies that might not have a resource variant, we must use a compatibility rule to take dependencies from
     * the klib variant. For native that is a "kotlin-api" variant and for wasmJs and wasmWasi that is a "kotlin-runtime" variant.
     */
    const val KOTLIN_RESOURCES = "kotlin-multiplatformresources"
    const val KOTLIN_RESOURCES_JS = "kotlin-multiplatformresourcesjs"

    // Following two constants were removed in Gradle 8.0 from 'Usages' class
    private const val JAVA_RUNTIME_CLASSES = "java-runtime-classes"
    private const val JAVA_RUNTIME_RESOURCES = "java-runtime-resources"

    val values = setOf(KOTLIN_API, KOTLIN_RUNTIME)

    private val jvmPlatformTypes: Set<KotlinPlatformType> = setOf(jvm, androidJvm)

    internal fun consumerApiUsage(project: Project, platformType: KotlinPlatformType) = project.usageByName(
        when {
            platformType in jvmPlatformTypes -> JAVA_API
            platformType == common
                    /** The kotlinExtension check below can be removed when legacy [KotlinPlatformCommonPlugin] is also removed. */
                    && project.kotlinExtension is KotlinMultiplatformExtension
                    && !project.isCompatibilityMetadataVariantEnabled -> KOTLIN_METADATA
            else -> KOTLIN_API
        }
    )

    internal fun consumerApiUsage(target: KotlinTarget): Usage =
        consumerApiUsage(target.project, target.platformType)

    internal fun consumerRuntimeUsage(project: Project, platformType: KotlinPlatformType) = project.usageByName(
        when (platformType) {
            in jvmPlatformTypes -> JAVA_RUNTIME
            else -> KOTLIN_RUNTIME
        }
    )

    internal fun consumerRuntimeUsage(target: KotlinTarget) = consumerRuntimeUsage(target.project, target.platformType)

    internal fun producerApiUsage(project: Project, platformType: KotlinPlatformType) = project.usageByName(
        when (platformType) {
            in jvmPlatformTypes -> "java-api-jars"
            else -> KOTLIN_API
        }
    )

    internal fun producerApiUsage(target: KotlinTarget) = producerApiUsage(target.project, target.platformType)

    internal fun producerRuntimeUsage(project: Project, platformType: KotlinPlatformType) = project.usageByName(
        when (platformType) {
            // This attribute is deprecated in Gradle and additionally to Usage attribute
            // it implicitly adds `org.gradle.libraryelements=jar`
            in jvmPlatformTypes -> "java-runtime-jars"
            else -> KOTLIN_RUNTIME
        }
    )

    internal fun producerRuntimeUsage(target: KotlinTarget) = producerRuntimeUsage(target.project, target.platformType)

    private class KotlinJavaRuntimeJarsCompatibility : AttributeCompatibilityRule<Usage> {
        // When Gradle resolves a plain old JAR dependency with no metadata attached, the Usage attribute of that dependency
        // is 'java-runtime-jars'. This rule tells Gradle that Kotlin consumers can consume plain old JARs:
        override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
            when {
                consumerValue?.name == KOTLIN_API && producerValue?.name.let { it == JAVA_API || it == "java-api-jars" } ->
                    compatible()
                consumerValue?.name in values && producerValue?.name.let { it == JAVA_RUNTIME || it == "java-runtime-jars" } ->
                    compatible()
            }
        }
    }

    private val javaUsagesForKotlinMetadataConsumers = listOf("java-api-jars", JAVA_API, "java-runtime-jars", JAVA_RUNTIME)

    private class KotlinMetadataCompatibility : AttributeCompatibilityRule<Usage> {
        override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
            // ensure that a consumer that requests 'kotlin-metadata' can also consumer 'kotlin-api' artifacts or the
            // 'java-*' ones (these are how Gradle represents a module that is published with no Gradle module metadata)
            if (
                consumerValue?.name == KOTLIN_METADATA &&
                (producerValue?.name == KOTLIN_API || producerValue?.name in javaUsagesForKotlinMetadataConsumers)
            ) {
                compatible()
            }
        }
    }

    private class KotlinCinteropCompatibility : AttributeCompatibilityRule<Usage> {
        private val compatibleProducerValues = setOf(KOTLIN_API, JAVA_API, JAVA_RUNTIME)
        override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
            if (consumerValue?.name == KOTLIN_CINTEROP && producerValue?.name in compatibleProducerValues) {
                compatible()
            }
        }
    }

    private class KotlinResourcesCompatibility : AttributeCompatibilityRule<Usage> {
        override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
            /**
             * When resolving resources using KotlinTarget.resourcesConfiguration, if a dependency doesn't have resources variant, we must
             * take dependencies from the klib variant because they might contain transitive resources variants.
             * */
            val consumerValueName = consumerValue?.name
            val producerValueName = producerValue?.name
            if (consumerValueName == null || producerValueName == null) return

            if (
                mapOf(
                    KOTLIN_RESOURCES to KOTLIN_API,
                    KOTLIN_RESOURCES_JS to KOTLIN_RUNTIME,
                )[consumerValueName] == producerValueName
            ) {
                compatible()
            }
        }
    }

    private class KotlinCinteropDisambiguation : AttributeDisambiguationRule<Usage> {
        override fun execute(details: MultipleCandidatesDetails<Usage?>) = details.run {
            if (consumerValue?.name == KOTLIN_CINTEROP) {
                val candidateNames = candidateValues.map { it?.name }
                when {
                    KOTLIN_CINTEROP in candidateNames -> chooseCandidateByName(KOTLIN_CINTEROP)
                    KOTLIN_API in candidateNames -> chooseCandidateByName(KOTLIN_API)
                    JAVA_API in candidateNames -> chooseCandidateByName(JAVA_API)
                    else -> Unit
                }
            }
        }
    }

    private class KotlinMetadataDisambiguation : AttributeDisambiguationRule<Usage> {
        override fun execute(details: MultipleCandidatesDetails<Usage>) = details.run {
            if (consumerValue?.name == KOTLIN_METADATA) {
                // Prefer Kotlin metadata, but if there's no such variant then accept 'kotlin-api' or the Java usages
                // (see the compatibility rule):
                val acceptedProducerValues = listOf(KOTLIN_METADATA, KOTLIN_API, *javaUsagesForKotlinMetadataConsumers.toTypedArray())
                val candidatesMap = candidateValues.associateBy { it.name }
                acceptedProducerValues.firstOrNull { it in candidatesMap }?.let { closestMatch(candidatesMap.getValue(it)) }
            }
        }
    }

    private class KotlinUsagesDisambiguation : AttributeDisambiguationRule<Usage> {
        override fun execute(details: MultipleCandidatesDetails<Usage?>) = with(details) {
            val candidateNames = candidateValues.map { it?.name }.toSet()

            // if both API and runtime artifacts are chosen according to the compatibility rules, then
            // the consumer requested nothing specific, so provide them with the runtime variant, which is more complete:
            if (candidateNames.filterNotNull().toSet() == setOf(KOTLIN_RUNTIME, KOTLIN_API)) {
                chooseCandidateByName(KOTLIN_RUNTIME)
            }

            val javaApiUsages = setOf(JAVA_API, "java-api-jars")
            val javaRuntimeUsages = setOf("java-runtime-jars", JAVA_RUNTIME)

            if (javaApiUsages.any { it in candidateNames } &&
                javaRuntimeUsages.any { it in candidateNames } &&
                values.none { it in candidateNames }
            ) {
                when (consumerValue?.name) {
                    KOTLIN_API, in javaApiUsages ->
                        chooseCandidateByName(javaApiUsages.first { it in candidateNames })
                    null, KOTLIN_RUNTIME, in javaRuntimeUsages ->
                        chooseCandidateByName(javaRuntimeUsages.first { it in candidateNames })
                }
            }

            if (JAVA_RUNTIME_CLASSES in candidateNames && JAVA_RUNTIME_RESOURCES in candidateNames && KOTLIN_RUNTIME in candidateNames) {
                chooseCandidateByName(KOTLIN_RUNTIME)
            }
        }
    }

    private fun MultipleCandidatesDetails<Usage?>.chooseCandidateByName(name: String?): Unit {
        closestMatch(candidateValues.single { it?.name == name }!!)
    }

    internal fun setupAttributesMatchingStrategy(
        attributesSchema: AttributesSchema,
        isKotlinGranularMetadata: Boolean,
        isKotlinResourcesCompatibilityRuleEnabled: Boolean
    ) {
        attributesSchema.attribute(USAGE_ATTRIBUTE) { strategy ->
            strategy.compatibilityRules.add(KotlinJavaRuntimeJarsCompatibility::class.java)
            strategy.disambiguationRules.add(KotlinUsagesDisambiguation::class.java)

            strategy.compatibilityRules.add(KotlinCinteropCompatibility::class.java)
            strategy.disambiguationRules.add(KotlinCinteropDisambiguation::class.java)

            if (isKotlinGranularMetadata) {
                strategy.compatibilityRules.add(KotlinMetadataCompatibility::class.java)
                strategy.disambiguationRules.add(KotlinMetadataDisambiguation::class.java)
            }

            // Only enable resources compatibility rule when resources configuration is used, so that for variant reselection klibs aren't selected
            if (isKotlinResourcesCompatibilityRuleEnabled) {
                strategy.compatibilityRules.add(KotlinResourcesCompatibility::class.java)
            }
        }
    }
}