/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path

@DisplayName("FUS statistic")
//Tests for FUS statistics have to create new instance of KotlinBuildStatsService
class FusStatisticsIT : KGPDaemonsBaseTest() {
    private val expectedMetrics = arrayOf(
        "OS_TYPE",
        "BUILD_FAILED=false",
        "EXECUTED_FROM_IDEA=false",
        "BUILD_FINISH_TIME",
        "GRADLE_VERSION",
        "KOTLIN_STDLIB_VERSION",
        "KOTLIN_COMPILER_VERSION",
    )

    private val GradleProject.fusStatisticsPath: Path
        get() = projectPath.getSingleFileInDir("kotlin-profile")

    @DisplayName("for dokka")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    fun testDokka(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
        ) {
            applyDokka()
            build("compileKotlin", "dokkaHtml", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(
                    fusStatisticsPath,
                    "ENABLED_DOKKA",
                    "ENABLED_DOKKA_HTML"
                )
            }
        }
    }

    @DisplayName("Verify that the metric for applying the Cocoapods plugin is being collected")
    @GradleTest
    fun testMetricCollectingOfApplyingCocoapodsPlugin(gradleVersion: GradleVersion) {
        project("native-cocoapods-template", gradleVersion) {
            build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(fusStatisticsPath, "COCOAPODS_PLUGIN_ENABLED=true")
            }
        }
    }

    @DisplayName("Verify that the metric for applying the Kotlin JS plugin is being collected")
    @GradleTest
    fun testMetricCollectingOfApplyingKotlinJsPlugin(gradleVersion: GradleVersion) {
        project("simple-js-library", gradleVersion) {
            build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(fusStatisticsPath, "KOTLIN_JS_PLUGIN_ENABLED=true")
            }
        }
    }


    @DisplayName("Ensure that the metric are not collected if plugins were not applied to simple project")
    @GradleTest
    fun testAppliedPluginsMetricsAreNotCollectedInSimpleProject(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                val fusStatisticsPath = fusStatisticsPath
                assertFileContains(
                    fusStatisticsPath,
                    *expectedMetrics,
                )
                assertFileDoesNotContain(
                    fusStatisticsPath,
                    "ENABLED_DOKKA_HTML"
                ) // asserts that we do not put DOKKA metrics everywhere just in case
                assertFileDoesNotContain(fusStatisticsPath, "KOTLIN_JS_PLUGIN_ENABLED")
            }
        }
    }

    @DisplayName("for project with buildSrc")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    fun testProjectWithBuildSrc(gradleVersion: GradleVersion) {
        project(
            "instantExecutionWithBuildSrc",
            gradleVersion,
        ) {
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                val fusStatisticsPath = fusStatisticsPath
                assertFileContains(
                    fusStatisticsPath,
                    *expectedMetrics,
                    "BUILD_SRC_EXISTS=true"
                )
            }
        }
    }

    @DisplayName("for project with included build")
    @GradleTest
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_6,
        maxVersion = TestVersions.Gradle.G_8_0
    )
    fun testProjectWithIncludedBuild(gradleVersion: GradleVersion) {
        project(
            "instantExecutionWithIncludedBuildPlugin",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = true)
        ) {
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                val fusStatisticsPath = fusStatisticsPath
                assertFileContains(
                    fusStatisticsPath,
                    *expectedMetrics,
                )
            }
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                val fusStatisticsPath = fusStatisticsPath
                assertFileContains(
                    fusStatisticsPath,
                    *expectedMetrics,
                )
            }
        }
    }

    @DisplayName("for failed build")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    fun testFusStatisticsForFailedBuild(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
        ) {
            projectPath.resolve("src/main/kotlin/helloWorld.kt").modify {
                it.replace("java.util.ArrayList", "")
            }
            buildAndFail("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(
                    fusStatisticsPath,
                    "BUILD_FAILED=true",
                    "OS_TYPE",
                    "EXECUTED_FROM_IDEA=false",
                    "BUILD_FINISH_TIME",
                    "GRADLE_VERSION",
                    "KOTLIN_STDLIB_VERSION",
                    "KOTLIN_COMPILER_VERSION",
                )
            }
        }
    }

    @DisplayName("general fields with configuration cache")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    fun testFusStatisticsWithConfigurationCache(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = true),
        ) {
            build(
                "compileKotlin",
                "-Pkotlin.session.logger.root.path=$projectPath",
            ) {
                assertConfigurationCacheStored()
            }

            build(
                "compileKotlin",
                "-Pkotlin.session.logger.root.path=$projectPath",
            ) {
                assertConfigurationCacheReused()
            }
        }
    }

    private fun TestProject.applyDokka() {
        buildGradle.replaceText(
            "plugins {",
            """
                    plugins {
                        id("org.jetbrains.dokka") version "1.8.10"
                    """.trimIndent()
        )
    }

}