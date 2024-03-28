/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

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
                |composeCompiler {
                |    suppressKotlinVersionCompatibilityCheck.set("${buildOptions.kotlinVersion}")
                |}
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
                            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=${buildOptions.kotlinVersion}," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=false," +
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
            buildGradleKts.appendText(
                """
                |
                |composeCompiler {
                |    suppressKotlinVersionCompatibilityCheck.set("${buildOptions.kotlinVersion}")
                |}
                """.trimMargin()
            )

            build("assembleDebug") {
                assertOutputContains("Detected Android Gradle Plugin compose compiler configuration")
            }
        }
    }
}