/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.writeText

@DisplayName("Compose compiler Gradle plugin")
class ComposeIT : KGPBaseTest() {

    @DisplayName("Should not affect Android project where compose is not enabled")
    @AndroidGradlePluginTests
    @GradleAndroidTest
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
                assertCompilerArgument(
                    ":compileDebugKotlin",
                    "-P plugin:androidx.compose.compiler.plugins.kotlin:generateFunctionKeyMetaClasses=false," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=false," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:intrinsicRemember=false," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:nonSkippingGroupOptimization=false," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:strongSkipping=false," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:traceMarkersEnabled=false",
                    LogLevel.INFO
                )
            }
        }
    }

    @DisplayName("Should work correctly when compose in Android is enabled")
    @AndroidGradlePluginTests
    @GradleAndroidTest
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
            }
        }
    }

    @DisplayName("Should not break build cache relocation")
    @AndroidGradlePluginTests
    @GradleAndroidTest
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
}