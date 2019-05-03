/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.attributes.*
import org.gradle.api.attributes.Usage.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast

object KotlinUsages {
    const val KOTLIN_API = "kotlin-api"
    const val KOTLIN_RUNTIME = "kotlin-runtime"
    val values = setOf(KOTLIN_API, KOTLIN_RUNTIME)

    private val jvmPlatformTypes: Set<KotlinPlatformType> = setOf(jvm, androidJvm)

    internal fun consumerApiUsage(target: KotlinTarget) = target.project.usageByName(
        when (target.platformType) {
            in jvmPlatformTypes -> JAVA_API
            else -> KOTLIN_API
        }
    )

    internal fun consumerRuntimeUsage(target: KotlinTarget) = target.project.usageByName(
        when (target.platformType) {
            in jvmPlatformTypes -> JAVA_RUNTIME
            else -> KOTLIN_RUNTIME
        }
    )

    internal fun producerApiUsage(target: KotlinTarget) = target.project.usageByName(
        when (target.platformType) {
            in jvmPlatformTypes ->
                if (isGradleVersionAtLeast(5, 3)) "java-api-jars" else JAVA_API
            else -> KOTLIN_API
        }
    )

    internal fun producerRuntimeUsage(target: KotlinTarget) = target.project.usageByName(
        when (target.platformType) {
            in jvmPlatformTypes -> JAVA_RUNTIME_JARS
            else -> KOTLIN_RUNTIME
        }
    )

    private class KotlinJavaRuntimeJarsCompatibility : AttributeCompatibilityRule<Usage> {
        // When Gradle resolves a plain old JAR dependency with no metadata attached, the Usage attribute of that dependency
        // is 'java-runtime-jars'. This rule tells Gradle that Kotlin consumers can consume plain old JARs:
        override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
            when {
                consumerValue?.name == KOTLIN_API && producerValue?.name.let { it == JAVA_API || it == "java-api-jars" } ->
                    compatible()
                consumerValue?.name in values && producerValue?.name.let { it == JAVA_RUNTIME || it == JAVA_RUNTIME_JARS } ->
                    compatible()
            }
        }
    }

    private class KotlinUsagesDisambiguation : AttributeDisambiguationRule<Usage> {
        override fun execute(details: MultipleCandidatesDetails<Usage?>) = with(details) {
            val candidateNames = candidateValues.map { it?.name }.toSet()

            fun chooseCandidateByName(name: String?): Unit = closestMatch(candidateValues.single { it?.name == name }!!)

            // if both API and runtime artifacts are chosen according to the compatibility rules, then
            // the consumer requested nothing specific, so provide them with the runtime variant, which is more complete:
            if (candidateNames.filterNotNull().toSet() == setOf(KOTLIN_RUNTIME, KOTLIN_API)) {
                chooseCandidateByName(KOTLIN_RUNTIME)
            }

            val javaApiUsages = setOf(JAVA_API, "java-api-jars")
            val javaRuntimeUsages = setOf(JAVA_RUNTIME_JARS, JAVA_RUNTIME)

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

    internal fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
        attributesSchema.attribute(Usage.USAGE_ATTRIBUTE) { strategy ->
            strategy.compatibilityRules.add(KotlinJavaRuntimeJarsCompatibility::class.java)
            strategy.disambiguationRules.add(KotlinUsagesDisambiguation::class.java)
        }
    }
}