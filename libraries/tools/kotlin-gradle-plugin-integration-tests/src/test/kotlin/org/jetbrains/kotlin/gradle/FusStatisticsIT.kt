/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.streams.toList
import kotlin.test.assertTrue

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
        "USE_CLASSPATH_SNAPSHOT=true"
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
                assertFileContains(fusStatisticsPath, "COCOAPODS_PLUGIN_ENABLED=true", "ENABLED_HMPP=true", "MPP_PLATFORMS")
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
    @OsCondition(
        supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS],
        enabledOnCI = [OS.LINUX, OS.MAC], //Fails on windows KT-65227
    )
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0]
    )
    fun testProjectWithBuildSrcForGradleVersion7(gradleVersion: GradleVersion) {
        //KT-64022 there are a different build instances in buildSrc and rest project:
        project(
            "instantExecutionWithBuildSrc",
            gradleVersion,
        ) {
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFilesCombinedContains(
                    Files.list(projectPath.resolve("kotlin-profile")).toList(),
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
        //KT-64022
        //there are a different build instances in buildSrc and rest project

        project(
            "instantExecutionWithIncludedBuildPlugin",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = true)
        ) {
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                Files.list(projectPath.resolve("kotlin-profile")).forEach {
                    assertFileContains(it, *expectedMetrics)
                }
            }
            Files.list(projectPath.resolve("kotlin-profile")).forEach {
                assertTrue(it.deleteIfExists())
            }

            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                Files.list(projectPath.resolve("kotlin-profile")).forEach {
                    assertFileContains(it, *expectedMetrics)
                }
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

    @DisplayName("fus metric for multiproject")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    fun testFusStatisticsForMultiproject(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
        ) {
            //Collect metrics from BuildMetricsService also
            build(
                "compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath",
                buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.FILE))
            ) {
                assertFileContains(
                    fusStatisticsPath,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=2",
                    "NUMBER_OF_SUBPROJECTS=2",
                    "COMPILATIONS_COUNT=2"
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
        testFusStatisticsWithConfigurationCache(gradleVersion, false)
    }

    @DisplayName("general fields with configuration cache and project isolation")
    @GradleTest
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_1,
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    fun testFusStatisticsWithConfigurationCacheAndProjectIsolation(gradleVersion: GradleVersion) {
        testFusStatisticsWithConfigurationCache(gradleVersion, true)
    }

    fun testFusStatisticsWithConfigurationCache(gradleVersion: GradleVersion, isProjectIsolationEnabled: Boolean) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = true,
                projectIsolation = isProjectIsolationEnabled,
                buildReport = listOf(BuildReportType.FILE)
            ),
        ) {
            build(
                "compileKotlin",
                "-Pkotlin.session.logger.root.path=$projectPath",
            ) {
                assertConfigurationCacheStored()
                assertFileContains(
                    fusStatisticsPath,
                    *expectedMetrics,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                    "NUMBER_OF_SUBPROJECTS=1",
                    "COMPILATIONS_COUNT=1"
                )
            }

            assertTrue(fusStatisticsPath.deleteIfExists())
            build("clean", buildOptions = buildOptions)

            build(
                "compileKotlin",
                "-Pkotlin.session.logger.root.path=$projectPath",
            ) {
                assertConfigurationCacheReused()
                assertFileContains(
                    fusStatisticsPath,
                    *expectedMetrics,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                    "NUMBER_OF_SUBPROJECTS=1",
                    "COMPILATIONS_COUNT=1"
                )
            }
        }
    }

    @DisplayName("configuration type metrics")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    fun testConfigurationTypeFusMetrics(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build(
                "compileKotlin",
                "-Pkotlin.session.logger.root.path=$projectPath",
            ) {
                assertFileContains(
                    fusStatisticsPath,
                    "CONFIGURATION_COMPILE_ONLY_COUNT=1",
                    "CONFIGURATION_API_COUNT=1",
                    "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                    "CONFIGURATION_RUNTIME_ONLY_COUNT=1",
                )
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