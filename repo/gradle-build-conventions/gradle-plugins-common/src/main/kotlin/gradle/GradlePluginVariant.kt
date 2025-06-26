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
) {
    GRADLE_MIN("main", "7.6", "7.6", "https://docs.gradle.org/7.6.1/javadoc/", "1.7"),
    GRADLE_80("gradle80", "8.0", "8.0", "https://docs.gradle.org/8.0.2/javadoc/", "1.8"),
    GRADLE_81("gradle81", "8.1", "8.1", "https://docs.gradle.org/8.1.1/javadoc/", "1.8"),
    GRADLE_82("gradle82", "8.2", "8.2", "https://docs.gradle.org/8.2.1/javadoc/", "1.8"),
    GRADLE_85("gradle85", "8.5", "8.5", "https://docs.gradle.org/8.5/javadoc/", "1.9"),
    GRADLE_86("gradle86", "8.6", "8.6", "https://docs.gradle.org/8.6/javadoc/", "1.9"),
    GRADLE_88("gradle88", "8.8", "8.8", "https://docs.gradle.org/8.8/javadoc/", "1.9"),
    GRADLE_811("gradle811", "8.11", "8.11", "https://docs.gradle.org/8.11/javadoc/", "2.0"),
    GRADLE_813("gradle813", "8.13", "8.13", "https://docs.gradle.org/current/javadoc/", "2.0"),
    ;

    companion object {
        const val GRADLE_COMMON = "8.14"
    }
}
