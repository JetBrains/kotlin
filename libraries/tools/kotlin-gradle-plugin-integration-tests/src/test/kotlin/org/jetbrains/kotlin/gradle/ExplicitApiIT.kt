/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Explicit API DSL")
class ExplicitApiIT : KGPBaseTest() {

    @JvmGradlePluginTests
    @DisplayName("Explicit api strict mode produces errors on violation")
    @GradleTest
    fun explicitApiStrict(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                """
                |
                |kotlin.explicitApi()
                """.trimMargin()
            )

            buildAndFail("compileKotlin") {
                assertTasksFailed(":compileKotlin")

                assertCompilerArgument(":compileKotlin", "-Xexplicit-api=strict")
                assertOutputContains("Visibility must be specified in explicit API mode")
            }
        }
    }

    @DisplayName("Explicit api mode is enabled only for non-test variants in Android project")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    fun explicitApiAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidSimpleApp",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
                logLevel = LogLevel.DEBUG
            ),
            buildJdk = jdkVersion.location
        ) {

            buildGradle.appendText(
                //language=groovy
                """
                |
                |kotlin.explicitApiWarning()
                |
                """.trimMargin()
            )

            build(":compileDebugKotlin") {
                assertTasksExecuted(":compileDebugKotlin")

                assertCompilerArgument(":compileDebugKotlin", "-Xexplicit-api=warning")
            }

            build(":compileDebugUnitTestKotlin") {
                assertTasksExecuted(":compileDebugUnitTestKotlin")

                assertNoCompilerArgument(":compileDebugUnitTestKotlin", "-Xexplicit-api=warning")
            }
        }
    }

    @DisplayName("Explicit api mode is enabled if added inside android extension")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    fun explicitApiInsideAndroidExtension(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidSimpleApp",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
                logLevel = LogLevel.DEBUG,
            ),
            buildJdk = jdkVersion.location
        ) {
            buildGradle.modify {
                it.replace(
                    "kotlin {",
                    """
                    |kotlin {
                    |       explicitApiWarning()
                """.trimMargin()
                )
            }
            build(":compileDebugKotlin") {
                assertTasksExecuted(":compileDebugKotlin")
                assertCompilerArgument(":compileDebugKotlin", "-Xexplicit-api=warning")
            }
            build(":compileDebugUnitTestKotlin") {
                assertTasksExecuted(":compileDebugUnitTestKotlin")
                assertNoCompilerArgument(":compileDebugUnitTestKotlin", "-Xexplicit-api=warning")
            }
        }
    }

    @DisplayName("Explicit api mode is enabled only for non-test variants in Android project in customized source directories")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    fun explicitApiCustomizedAndroidSourceSets(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidExtraSourceDirsApp",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
                logLevel = LogLevel.DEBUG,
            ),
            buildJdk = jdkVersion.location
        ) {
            buildGradle.modify {
                it.replace(
                    "compileOptions {",
                    """
                    |kotlin {
                    |       explicitApi = 'warning'
                    |    }
                    |    compileOptions {
                """.trimMargin()
                )
            }
            build(":compileDebugKotlin") {
                assertTasksExecuted(":compileDebugKotlin")
                assertCompilerArgument(":compileDebugKotlin", "-Xexplicit-api=warning")
                assertOutputContains("Visibility must be specified in explicit API mode")
            }
            build(":compileDebugUnitTestKotlin") {
                assertTasksExecuted(":compileDebugUnitTestKotlin")
                assertNoCompilerArgument(":compileDebugUnitTestKotlin", "-Xexplicit-api=warning")
            }
            build(":compileDebugAndroidTestKotlin") {
                assertTasksExecuted(":compileDebugAndroidTestKotlin")
                assertNoCompilerArgument(":compileDebugAndroidTestKotlin", "-Xexplicit-api=warning")
            }
        }
    }
}
