/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.GradleWithJdkTest
import org.jetbrains.kotlin.gradle.testbase.JdkVersions
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertEquals

@DisplayName("Tests on compatibility with various Gradle versions")
@JvmGradlePluginTests
class GradleCompatibilityIT : KGPBaseTest() {
    @DisplayName("Proper Gradle plugin variant is used")
    @GradleTestVersions(
        additionalVersions = [
            TestVersions.Gradle.G_7_6,
            TestVersions.Gradle.G_8_0,
            TestVersions.Gradle.G_8_1,
            TestVersions.Gradle.G_8_2,
            TestVersions.Gradle.G_8_5,
            TestVersions.Gradle.G_8_6,
            TestVersions.Gradle.G_8_8,
            TestVersions.Gradle.G_8_11,
            TestVersions.Gradle.G_8_12,
            TestVersions.Gradle.G_8_13,
        ],
    )
    @GradleTest
    fun properPluginVariantIsUsed(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            build("help") {
                val expectedVariant = when (gradleVersion) {
                    GradleVersion.version(TestVersions.Gradle.G_9_0) -> "gradle813"
                    GradleVersion.version(TestVersions.Gradle.G_8_14) -> "gradle813"
                    GradleVersion.version(TestVersions.Gradle.G_8_13) -> "gradle813"
                    GradleVersion.version(TestVersions.Gradle.G_8_12) -> "gradle811"
                    GradleVersion.version(TestVersions.Gradle.G_8_11) -> "gradle811"
                    GradleVersion.version(TestVersions.Gradle.G_8_10) -> "gradle88"
                    GradleVersion.version(TestVersions.Gradle.G_8_9) -> "gradle88"
                    GradleVersion.version(TestVersions.Gradle.G_8_8) -> "gradle88"
                    GradleVersion.version(TestVersions.Gradle.G_8_7) -> "gradle86"
                    GradleVersion.version(TestVersions.Gradle.G_8_6) -> "gradle86"
                    GradleVersion.version(TestVersions.Gradle.G_8_5) -> "gradle85"
                    GradleVersion.version(TestVersions.Gradle.G_8_4) -> "gradle82"
                    GradleVersion.version(TestVersions.Gradle.G_8_3) -> "gradle82"
                    GradleVersion.version(TestVersions.Gradle.G_8_2) -> "gradle82"
                    GradleVersion.version(TestVersions.Gradle.G_8_1) -> "gradle81"
                    GradleVersion.version(TestVersions.Gradle.G_8_0) -> "gradle80"
                    GradleVersion.version(TestVersions.Gradle.G_7_6) -> "gradle76"
                    else -> "main"
                }

                assertOutputContains("Using Kotlin Gradle Plugin $expectedVariant variant")
            }
        }
    }

    @DisplayName("Proper message is produced on an incompatible Gradle version")
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.MIN_UNSUPPORTED_VERSION_TO_CHECK,
        maxVersion = TestVersions.Gradle.MIN_UNSUPPORTED_VERSION_TO_CHECK,
    )
    @JdkVersions(versions = [JavaVersion.VERSION_11], compatibleWithGradle = false) // disable the check as it's an unsupported version
    @GradleWithJdkTest
    fun testIncompatibleGradleVersion(gradleVersion: GradleVersion, providedJdk: JdkVersions.ProvidedJdk) {
        project("kotlinProject", gradleVersion, buildJdk = providedJdk.location) {
            buildAndFail("help") {
                // assertHasDiagnostic does not work as proper diagnostic reporting may fail on unsupported Gradle versions
                assertOutputContains("The applied Kotlin Gradle is not compatible with the used Gradle version")
                assertOutputContains("Please update the Gradle version to at least Gradle ${TestVersions.Gradle.MIN_SUPPORTED}")
            }
        }
    }

    @DisplayName("KT-78785: build script resolves Gradle version specific jars")
    @Test
    fun buildScriptResolvesGradleVersionSpecificJars() {
        fun resolveKgpJars(gradleVersion: GradleVersion): List<File> {
            return project("empty", gradleVersion) {
                plugins {
                    kotlin("jvm")
                }
            }.buildScriptReturn {
                listOf(
                    KotlinMultiplatformExtension::class.java.protectionDomain.codeSource.location,
                    KotlinGradlePluginDsl::class.java.protectionDomain.codeSource.location,
                ).map { File(it.toURI()) }
            }.buildAndReturn()
        }

        val maximumGradleVariantArtifactSuffix = System.getProperty("kotlin.gradle.maximumSupportedGradleVariant.artifactSuffix")
        val resolvedJars = mapOf(
            TestVersions.Gradle.MIN_SUPPORTED to resolveKgpJars(
                gradleVersion = GradleVersion.version(TestVersions.Gradle.MIN_SUPPORTED)
            ),
            // This test relies on TestVersions.Gradle.MAX_SUPPORTED being >= maximumGradleVariantArtifactSuffix
            TestVersions.Gradle.MAX_SUPPORTED to resolveKgpJars(
                gradleVersion = GradleVersion.version(TestVersions.Gradle.MAX_SUPPORTED)
            ),
        ).mapValues {
            it.value.map {
                it.toPath().toList().last().pathString
            }
        }

        assertEquals(
            mapOf(
                TestVersions.Gradle.MIN_SUPPORTED to listOf(
                    "kotlin-gradle-plugin-${defaultBuildOptions.kotlinVersion}.jar",
                    "kotlin-gradle-plugin-api-${defaultBuildOptions.kotlinVersion}.jar",
                ),
                TestVersions.Gradle.MAX_SUPPORTED to listOf(
                    "kotlin-gradle-plugin-${defaultBuildOptions.kotlinVersion}-${maximumGradleVariantArtifactSuffix}.jar",
                    "kotlin-gradle-plugin-api-${defaultBuildOptions.kotlinVersion}-${maximumGradleVariantArtifactSuffix}.jar",
                ),
            ),
            resolvedJars,
        )
    }
}