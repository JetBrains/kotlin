/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.attributes.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.io.Serializable

/**
 * An [Attribute] representing the Kotlin target compilation platform.
 *
 * This attribute is available in [org.gradle.api.artifacts.ConsumableConfiguration]s created by the Kotlin Gradle plugin
 * for the given [org.gradle.api.Project].
 * The attribute is also published as part of the project
 * [Gradle metadata publication](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html).
 *
 * To configure the default [AttributeCompatibilityRule] and [AttributeDisambiguationRule]
 * use the [KotlinPlatformType.setupAttributesMatchingStrategy] method.
 *
 * The instance of this attribute can be accessed via the [KotlinPlatformType.attribute] property.
 */
enum class KotlinPlatformType : Named, Serializable {

    /**
     * Indicates a compilation result that is shared across different platforms.
     */
    common,

    /**
     * Represents a compilation output compiled for the JVM Kotlin platform.
     */
    jvm,

    /**
     * Represents a compilation output compiled for the JS Kotlin platform.
     */
    js,

    /**
     * Represents a compilation output compiled for the Android Kotlin platform.
     *
     * Note: This attribute value is only used for the Android code compiled with the [Android SDK](https://developer.android.com/tools).
     * Code compiled using the [Android NDK](https://developer.android.com/ndk) has [native] type.
     */
    androidJvm,

    /**
     * Represents a compilation output compiled for the Native Kotlin platform.
     */
    native,

    /**
     * Represents a compilation output compiled for the Wasm Kotlin platform.
     */
    wasm;

    /**
     * @suppress
     */
    override fun toString(): String = name

    /**
     * @suppress
     */
    override fun getName(): String = name

    /**
     * Provides a default [AttributeCompatibilityRule] for this attribute.
     */
    class CompatibilityRule : AttributeCompatibilityRule<KotlinPlatformType> {

        /**
         * Executes the compatibility check for the Kotlin platform type attribute.
         */
        override fun execute(details: CompatibilityCheckDetails<KotlinPlatformType>) = with(details) {
            if (producerValue == jvm && consumerValue == androidJvm)
                compatible()

            // Allow the input metadata configuration to consume platform-specific artifacts if no metadata is available, KT-26834
            if (consumerValue == common)
                compatible()
        }
    }

    /**
     * Provides a default [AttributeCompatibilityRule] for this attribute.
     */
    class DisambiguationRule : AttributeDisambiguationRule<KotlinPlatformType> {

        /**
         * Executes the attribute disambiguation rule for the Kotlin platform type attribute.
         */
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

    /**
     * Provides additional static properties and constants for the [KotlinPlatformType].
     */
    companion object {

        /**
         * An instance of this attribute.
         */
        val attribute = Attribute.of(
            "org.jetbrains.kotlin.platform.type",
            KotlinPlatformType::class.java
        )

        /**
         * Configures the default Kotlin Gradle plugin attributes matching strategy for the provided [attributesSchema].
         */
        fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
            attributesSchema.attribute(KotlinPlatformType.attribute).run {
                compatibilityRules.add(CompatibilityRule::class.java)
                disambiguationRules.add(DisambiguationRule::class.java)
            }
        }
    }
}
