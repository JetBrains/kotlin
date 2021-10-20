/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.attributes.*
import org.gradle.util.GradleVersion
import java.io.Serializable

enum class KotlinPlatformType: Named, Serializable {
    common, jvm, js, androidJvm, native, wasm;

    override fun toString(): String = name
    override fun getName(): String = name

    class CompatibilityRule : AttributeCompatibilityRule<KotlinPlatformType> {
        override fun execute(details: CompatibilityCheckDetails<KotlinPlatformType>) = with(details) {
            if (producerValue == jvm && consumerValue == androidJvm)
                compatible()

            // Allow the input metadata configuration consume platform-specific artifacts if no metadata is available, KT-26834
            if (consumerValue == common)
                compatible()
        }
    }

    class DisambiguationRule : AttributeDisambiguationRule<KotlinPlatformType> {
        override fun execute(details: MultipleCandidatesDetails<KotlinPlatformType?>) = with(details) {
            if (consumerValue in candidateValues) {
                closestMatch(checkNotNull(consumerValue))
                return@with
            }

            /**
             * If the consumer doesn't request anything specific and matches both JVM and Android,
             * then assume that it's an ordinary pure-Java consumer. If it's a pure-Android consumer, it will have
             * other means of disambiguation that will take precedence
             * (the buildType attribute, the target JVM environment = android)
             */
            if (consumerValue == null && androidJvm in candidateValues && jvm in candidateValues) {
                closestMatch(jvm)
                return@with
            }

            if (common in candidateValues && jvm !in candidateValues && androidJvm !in candidateValues) {
                // then the consumer requests common or requests no platform-specific artifacts,
                // so common is the best match, KT-26834; apply this rule only when no JVM variant is available,
                // as doing otherwise would conflict with Gradle java's disambiguation rules and lead to KT-32239
                closestMatch(common)
                return@with
            }
        }
    }

    companion object {
        val attribute = Attribute.of(
            "org.jetbrains.kotlin.platform.type",
            KotlinPlatformType::class.java
        )

        fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
            attributesSchema.attribute(KotlinPlatformType.attribute).run {
                compatibilityRules.add(CompatibilityRule::class.java)
                disambiguationRules.add(DisambiguationRule::class.java)
            }
        }
    }
}
