/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.attributes.*
import org.gradle.util.GradleVersion
import java.io.Serializable

enum class KotlinPlatformType(val attributeValue: String): Named, Serializable {
    COMMON("common"), JVM("jvm"), JS("js"), ANDROID_JVM("androidJvm"), NATIVE("native");

    override fun toString(): String = name
    override fun getName(): String = name

    class CompatibilityRule : AttributeCompatibilityRule<String> {
        override fun execute(details: CompatibilityCheckDetails<String?>) = with(details) {
            if (producerValue == JVM.attributeValue && consumerValue == ANDROID_JVM.attributeValue)
                compatible()

            // Allow the input metadata configuration consume platform-specific artifacts if no metadata is available, KT-26834
            if (consumerValue == COMMON.attributeValue)
                compatible()
        }
    }

    class DisambiguationRule : AttributeDisambiguationRule<String> {
        override fun execute(details: MultipleCandidatesDetails<String?>) = with(details) {
            if (candidateValues == setOf(ANDROID_JVM.attributeValue, JVM.attributeValue))
                closestMatch(ANDROID_JVM.attributeValue)

            if (COMMON.attributeValue in candidateValues)
                // then the consumer requests common or requests no platform-specific artifacts,
                // so common is the best match, KT-26834
                closestMatch(COMMON.attributeValue)
        }
    }

    companion object {
        val ATTRIBUTE = Attribute.of(
            "org.jetbrains.kotlin.platform.type",
            String::class.java
        )

        fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
            attributesSchema.attribute(KotlinPlatformType.ATTRIBUTE).run {
                if (isGradleVersionAtLeast(4, 0)) {
                    compatibilityRules.add(CompatibilityRule::class.java)
                    disambiguationRules.add(DisambiguationRule::class.java)
                }
            }
        }
    }
}

private fun isGradleVersionAtLeast(major: Int, minor: Int) =
    GradleVersion.current() >= GradleVersion.version("$major.$minor")