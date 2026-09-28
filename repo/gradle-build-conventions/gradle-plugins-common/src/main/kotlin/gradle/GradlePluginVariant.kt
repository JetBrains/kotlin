/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package gradle

/**
 * Gradle's plugins common variants.
 * See also the compatibility matrix: https://docs.gradle.org/current/userguide/compatibility.html#kotlin
 *
 * [minimalSupportedGradleVersion] - Minimal Gradle version that is supported in this variant
 * [gradleApiVersion] - Gradle API dependency version. Usually should be the same as [minimalSupportedGradleVersion].
 * [gradleApiJavadocUrl] - Gradle URL for the given API.The last enum entry should always point to 'current'.
 * [bundledKotlinVersion] - The version of the bundled Kotlin. Used to control the kotlin-stdlib version and the values of `-api-version`, `-language-version` arguments
 */
enum class GradlePluginVariant(
    val sourceSetName: String,
    val minimalSupportedGradleVersion: String,
    val gradleApiVersion: String,
    val gradleApiJavadocUrl: String,
    val bundledKotlinVersion: String,

    /**
     * We want to align the runtimes of kotlinx serialization with the lowest supported stdlib embedded in Gradle. E.g. in Gradle 7.6 the
     * stdlib in 1.7.10, so we use "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0" which had 1.7.10 stdlib dependency
     */
    private val kotlinxJsonSerializationVersion: String? = null,
) {
    /**
     * Tests rely on these entries being sorted
     */
    GRADLE_MIN("main", "8.14", "8.14", "https://docs.gradle.org/8.14/javadoc/", "2.0", "1.7.3"),
    GRADLE_96("gradle96", "9.6", "9.6.0","https://docs.gradle.org/current/javadoc/", "2.2"),
    ;

    val compatibleKotlinxJsonSerializationVersion: String
        get() = kotlinxJsonSerializationVersion
            ?: error("Compatible kotlinx-serialization-json should only be used for ${GRADLE_MIN.name} plugin variant and not with ${this.name}")

    companion object {
        const val COMPILE_KOTLIN_VERSION = "1.8"
        const val GRADLE_COMMON_COMPILE_API_VERSION = "9.7.0"

        val MIDDLE_GRADLE_VARIANT_FOR_TESTS = GradlePluginVariant.values().run { this[size / 2] }
        val MAXIMUM_SUPPORTED_GRADLE_VARIANT = GradlePluginVariant.values().last()
    }
}
