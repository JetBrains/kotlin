/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.Usage.*
import org.jetbrains.kotlin.gradle.internal.attributes.chooseCandidateByName
import org.jetbrains.kotlin.gradle.internal.attributes.getCandidateNames
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.*
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.attributeValueByName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.setAttribute

object KotlinUsages {
    const val KOTLIN_API = "kotlin-api"
    const val KOTLIN_RUNTIME = "kotlin-runtime"
    const val KOTLIN_METADATA = "kotlin-metadata"

    const val KOTLIN_UKLIB_API = "kotlin-uklib-api"
    const val KOTLIN_UKLIB_RUNTIME = "kotlin-uklib-runtime"

    // This type is required to distinguish metadata jar configuration from a psm secondary variant.
    // At the same time, disambiguation and compatibility rules should count them as equivalent
    // to be possible to apply a transform actions chain to `kotlin-metadata` artifact to get psm.
    internal const val KOTLIN_PSM_METADATA = "kotlin-psm-metadata"

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

    private fun consumerApiUsage(project: Project, platformType: KotlinPlatformType) = project.usageByName(
        when {
            platformType in jvmPlatformTypes -> JAVA_API
            platformType == common -> KOTLIN_METADATA
            else -> KOTLIN_API
        }
    )

    internal fun consumerApiUsage(target: KotlinTarget): Usage =
        consumerApiUsage(target.project, target.platformType)

    private fun consumerRuntimeUsage(project: Project, platformType: KotlinPlatformType) = project.usageByName(
        when (platformType) {
            in jvmPlatformTypes -> JAVA_RUNTIME
            else -> KOTLIN_RUNTIME
        }
    )

    internal fun consumerRuntimeUsage(target: KotlinTarget) = consumerRuntimeUsage(target.project, target.platformType)

    private fun producerApiUsage(project: Project, platformType: KotlinPlatformType) = project.usageByName(
        when (platformType) {
            in jvmPlatformTypes -> JAVA_API
            else -> KOTLIN_API
        }
    )

    internal fun configureProducerApiUsage(attributesHolder: HasAttributes, target: KotlinTarget) {
        val apiUsage = producerApiUsage(target.project, target.platformType)
        attributesHolder.setAttribute(USAGE_ATTRIBUTE, apiUsage)
        if (apiUsage.name == JAVA_API) {
            attributesHolder.setAttribute(LIBRARY_ELEMENTS_ATTRIBUTE, target.project.attributeValueByName(LibraryElements.JAR))
        }
    }

    private fun producerRuntimeUsage(project: Project, platformType: KotlinPlatformType) = project.usageByName(
        when (platformType) {
            in jvmPlatformTypes -> JAVA_RUNTIME
            else -> KOTLIN_RUNTIME
        }
    )

    internal fun configureProducerRuntimeUsage(attributesHolder: HasAttributes, target: KotlinTarget) {
        val runtimeUsage = producerRuntimeUsage(target.project, target.platformType)
        attributesHolder.setAttribute(USAGE_ATTRIBUTE, runtimeUsage)
        if (runtimeUsage.name == JAVA_RUNTIME) {
            attributesHolder.setAttribute(LIBRARY_ELEMENTS_ATTRIBUTE, target.project.attributeValueByName(LibraryElements.JAR))
        }
    }

    private class KotlinUsagesCompatibility : AttributeCompatibilityRule<Usage> {
        override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
            when {
                consumerValue?.name == KOTLIN_API && producerValue?.name == JAVA_API ->
                    compatible()
                consumerValue?.name in values && producerValue?.name == JAVA_RUNTIME ->
                    compatible()
            }
        }
    }

    private val javaUsagesForKotlinMetadataConsumers = listOf(JAVA_API, JAVA_RUNTIME)

    private class KotlinMetadataCompatibility : AttributeCompatibilityRule<Usage> {
        override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
            // ensure that a consumer that requests 'kotlin-metadata' can also consumer 'kotlin-api' artifacts or the
            // 'java-*' ones (these are how Gradle represents a module that is published with no Gradle module metadata).
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

    private class KotlinCinteropDisambiguation : AttributeDisambiguationRule<Usage> {
        override fun execute(details: MultipleCandidatesDetails<Usage>) = details.run {
            if (consumerValue?.name == KOTLIN_CINTEROP) {
                val candidateNames = getCandidateNames()
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
            val commonCandidateList = listOf(KOTLIN_METADATA, KOTLIN_UKLIB_API, KOTLIN_API, *javaUsagesForKotlinMetadataConsumers.toTypedArray())
            if (consumerValue?.name == KOTLIN_METADATA) {
                // Prefer Kotlin metadata, but if there's no such variant then accept 'kotlin-api' or the Java usages
                // (see the compatibility rule):
                closestMatchToFirstAppropriateCandidate(commonCandidateList)
            }
        }

        private fun MultipleCandidatesDetails<Usage>.closestMatchToFirstAppropriateCandidate(acceptedProducerValues: List<String>) {
            val candidatesMap = candidateValues.associateBy { it.name }
            acceptedProducerValues.firstOrNull { it in candidatesMap }?.let { closestMatch(candidatesMap.getValue(it)) }
        }
    }

    private class KotlinUsagesDisambiguation : AttributeDisambiguationRule<Usage> {
        override fun execute(details: MultipleCandidatesDetails<Usage>) = with(details) {
            val candidateNames = getCandidateNames().toSet()

            // if both API and runtime artifacts are chosen according to the compatibility rules, then
            // the consumer requested nothing specific, so provide them with the runtime variant, which is more complete:
            if (candidateNames.filterNotNull().toSet() == setOf(KOTLIN_RUNTIME, KOTLIN_API)) {
                chooseCandidateByName(KOTLIN_RUNTIME)
            }

            if (JAVA_API in candidateNames &&
                JAVA_RUNTIME in candidateNames &&
                values.none { it in candidateNames }
            ) {
                when (consumerValue?.name) {
                    KOTLIN_API, JAVA_API -> chooseCandidateByName(JAVA_API)
                    null, KOTLIN_RUNTIME, JAVA_RUNTIME -> chooseCandidateByName(JAVA_RUNTIME)
                }
            }

            if (JAVA_RUNTIME_CLASSES in candidateNames && JAVA_RUNTIME_RESOURCES in candidateNames && KOTLIN_RUNTIME in candidateNames) {
                chooseCandidateByName(KOTLIN_RUNTIME)
            }
        }
    }

    internal fun setupAttributesMatchingStrategy(
        attributesSchema: AttributesSchema,
        isKotlinGranularMetadata: Boolean,
    ) {
        attributesSchema.attribute(USAGE_ATTRIBUTE) { strategy ->
            strategy.compatibilityRules.add(KotlinUsagesCompatibility::class.java)
            strategy.disambiguationRules.add(KotlinUsagesDisambiguation::class.java)

            strategy.compatibilityRules.add(KotlinCinteropCompatibility::class.java)
            strategy.disambiguationRules.add(KotlinCinteropDisambiguation::class.java)

            if (isKotlinGranularMetadata) {
                strategy.compatibilityRules.add(KotlinMetadataCompatibility::class.java)
                strategy.disambiguationRules.add(KotlinMetadataDisambiguation::class.java)
            }
        }
    }
}
