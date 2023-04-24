/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.junit.jupiter.api.DisplayName
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.KtAssert.fail
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

@DisplayName("FUS statistic")
class FusStatisticsIT : KGPBaseTest() {
    @DisplayName("for dokka")
    @GradleTest
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

    private fun TestProject.applyDokka() {
        buildGradle.replaceText(
            "plugins {",
            """
                    plugins {
                        id("org.jetbrains.dokka") version "1.7.10"
                    """.trimIndent()
        )
        buildGradle.modify {
            """
                        import org.jetbrains.dokka.DokkaConfiguration.Visibility
                        import org.jetbrains.dokka.gradle.DokkaTask
                        $it
                        tasks.withType(DokkaTask.class) {
                            dokkaSourceSets.configureEach {
                                documentedVisibilities.set([
                                        Visibility.PUBLIC,
                                        Visibility.PROTECTED
                                ])
                            }
                        }
                    """.trimIndent()

        }
    }

    private val GradleProject.fusStatisticsPath: Path
        get() {
            val statisticsDir = projectPath.resolve("kotlin-profile")
            return Files.list(statisticsDir).use {
                val files = it.asSequence().toList()
                files.singleOrNull() ?: fail("The directory must contain a single statistics file, but got: $files")
            }
        }

    @DisplayName("general fields")
    @GradleTest
    fun testFusStatistics(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
        ) {
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                val fusStatisticsPath = fusStatisticsPath
                assertFileContains(
                    fusStatisticsPath,
                    "OS_TYPE",
                    "BUILD_FAILED=false",
                    "EXECUTED_FROM_IDEA=false",
                    "BUILD_FINISH_TIME",
                    "GRADLE_VERSION",
                    "KOTLIN_STDLIB_VERSION",
                    "KOTLIN_COMPILER_VERSION",
                )
                assertFileDoesNotContain(
                    fusStatisticsPath,
                    "ENABLED_DOKKA",
                    "ENABLED_DOKKA_HTML",
                ) // asserts that we do not put DOKKA metrics everywhere just in case
            }
        }
    }

    @DisplayName("for failed build")
    @GradleTest
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
}