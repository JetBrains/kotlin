/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.writeText

@DisplayName("Compose compiler Gradle plugin")
class ComposeIT : KGPBaseTest() {

    @DisplayName("Should not affect Android project where compose is not enabled")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("AndroidSimpleApp")
    fun testAndroidDisabledCompose(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidSimpleApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
        ) {
            buildGradle.modify { originalBuildScript ->
                """
                |plugins {
                |    id "org.jetbrains.kotlin.plugin.compose"
                |${originalBuildScript.substringAfter("plugins {")}
                |
                |dependencies {
                |    implementation "androidx.compose.runtime:runtime:1.6.4"
                |}
                """.trimMargin()
            }

            gradleProperties.appendText(
                """
                android.useAndroidX=true
                """.trimIndent()
            )

            build("assembleDebug") {
                assertOutputDoesNotContain("Detected Android Gradle Plugin compose compiler configuration")
                assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
                assertCompilerArgument(
                    ":compileDebugKotlin",
                    "-P plugin:androidx.compose.compiler.plugins.kotlin:generateFunctionKeyMetaClasses=false," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=false," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:traceMarkersEnabled=false",
                    LogLevel.INFO
                )
            }
        }
    }

    @DisplayName("Should work correctly when compose in Android is enabled")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("AndroidSimpleComposeApp")
    fun testAndroidWithCompose(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidSimpleComposeApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
        ) {
            build("assembleDebug") {
                assertOutputContains("Detected Android Gradle Plugin compose compiler configuration")
                assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
            }
        }
    }

    @DisplayName("Should not break build cache relocation")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("AndroidSimpleComposeApp")
    fun testAndroidBuildCacheRelocation(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
        @TempDir localCacheDir: Path,
    ) {
        val project1 = androidComposeAppProjectWithLocalCacheEnabled(
            gradleVersion,
            agpVersion,
            providedJdk,
            localCacheDir
        )

        val project2 = androidComposeAppProjectWithLocalCacheEnabled(
            gradleVersion,
            agpVersion,
            providedJdk,
            localCacheDir
        )

        project1.build("assembleDebug") {
            assertTasksExecuted(":compileDebugKotlin")
        }

        project2.build("assembleDebug") {
            assertTasksFromCache(":compileDebugKotlin")
        }
    }

    @DisplayName("Should work with JB Compose plugin")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_80)
    @TestMetadata("JBComposeApp")
    fun testJBCompose(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        project(
            projectName = "JBComposeApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
                .suppressDeprecationWarningsOn(
                    "JB Compose produces deprecation warning: https://github.com/JetBrains/compose-multiplatform/issues/3945"
                ) {
                    gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_8_7)
                }
        ) {
            build(":composeApp:assembleDebug") {
                assertOutputDoesNotContain("Detected Android Gradle Plugin compose compiler configuration")
                assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
            }

            build(":composeApp:desktopJar") {
                assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
            }
        }
    }

    @DisplayName("Should not suggest apply Kotlin compose plugin in JB Compose plugin")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_80)
    @TestMetadata("JBComposeApp")
    fun testAndroidJBComposeNoSuggestion(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        project(
            projectName = "JBComposeApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
        ) {
            subProject("composeApp").buildGradleKts.modify {
                it.replace("kotlin(\"plugin.compose\")", "")
            }

            buildAndFail(":composeApp:assembleDebug") {
                assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
            }
        }
    }

    private fun androidComposeAppProjectWithLocalCacheEnabled(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
        localCacheDir: Path,
    ): TestProject {
        return project(
            projectName = "AndroidSimpleComposeApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
                buildCacheEnabled = true,
            )
        ) {
            projectPath.resolve("stability-configuration.conf").writeText(
                """
                |// Consider LocalDateTime stable
                |java.time.LocalDateTime
                |// Consider kotlin collections stable
                |kotlin.collections.*
                """.trimMargin()
            )
            buildGradleKts.appendText(
                """
                |
                |composeCompiler {
                |    metricsDestination.set(project.layout.buildDirectory.dir("metrics"))
                |    reportsDestination.set(project.layout.buildDirectory.dir("reports"))
                |    stabilityConfigurationFile.set(project.layout.projectDirectory.file("stability-configuration.conf"))
                |}
                """.trimMargin()
            )

            enableLocalBuildCache(localCacheDir)
        }
    }

    @DisplayName("Run Compose compiler with runtime v1.0")
    @GradleAndroidTest
    @OtherGradlePluginTests
    @TestMetadata("AndroidSimpleApp")
    fun testComposePluginWithRuntimeV1_0(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidSimpleApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
        ) {
            buildGradle.modify { originalBuildScript ->
                """
                |plugins {
                |    id "org.jetbrains.kotlin.plugin.compose"
                |${originalBuildScript.substringAfter("plugins {")}
                |
                |dependencies {
                |    implementation "androidx.compose.runtime:runtime:1.0.0"
                |}
                """.trimMargin()
            }

            gradleProperties.appendText(
                """
                android.useAndroidX=true
                """.trimIndent()
            )

            val composableFile = projectPath.resolve("src/main/kotlin/com/example/Compose.kt").createFile()
            composableFile.appendText(
                """
                |package com.example
                |
                |import androidx.compose.runtime.Composable
                |
                |@Composable fun Test() { Test() }
            """.trimMargin())

            build("assembleDebug") {
                assertTasksExecuted(":compileDebugKotlin")
            }
        }
    }

    @DisplayName("Run Compose compiler with the latest runtime")
    @GradleAndroidTest
    @OtherGradlePluginTests
    @TestMetadata("AndroidSimpleApp")
    fun testComposePluginWithRuntimeLatest(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        val composeSnapshotId = System.getProperty("composeSnapshotId")
        val composeSnapshotVersion = System.getProperty("composeSnapshotVersion")
        project(
            projectName = "AndroidSimpleApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            dependencyManagement = DependencyManagement.DefaultDependencyManagement(
                additionalRepos = setOf("https://androidx.dev/snapshots/builds/${composeSnapshotId}/artifacts/repository")
            )
        ) {
            buildGradle.modify { originalBuildScript ->
                """
                |plugins {
                |    id "org.jetbrains.kotlin.plugin.compose"
                |${originalBuildScript.substringAfter("plugins {")}
                |
                |dependencies {
                |    implementation "androidx.compose.runtime:runtime:$composeSnapshotVersion"
                |}
                """.trimMargin()
            }

            gradleProperties.appendText(
                """
                android.useAndroidX=true
                """.trimIndent()
            )

            val composableFile = projectPath.resolve("src/main/kotlin/com/example/Compose.kt").createFile()
            composableFile.appendText(
                """
                |package com.example
                |
                |import androidx.compose.runtime.Composable
                |
                |@Composable fun Test() { Test() }
            """.trimMargin())

            build("assembleDebug") {
                assertTasksExecuted(":compileDebugKotlin")
            }
        }
    }

    companion object {
        private const val APPLY_COMPOSE_SUGGESTION =
            "The Compose compiler plugin is now a part of Kotlin, please apply the 'org.jetbrains.kotlin.plugin.compose' Gradle plugin to enable it."
    }
}