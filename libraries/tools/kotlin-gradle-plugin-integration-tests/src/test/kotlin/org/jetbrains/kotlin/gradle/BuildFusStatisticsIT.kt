/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.KtAssert.assertEquals
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readText

@DisplayName("Build FUS statistics")
class BuildFusStatisticsIT : KGPDaemonsBaseTest() {
    @DisplayName("works for project with buildSrc and kotlinDsl plugin")
    @GradleTest
    fun testCompatibilityBuildSrcWithKotlinDsl(gradleVersion: GradleVersion) {
        project(
            "buildSrcUsingKotlinCompilationAndKotlinPlugin",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            build("assemble") {
                //register build service for buildSrc.
                assertOutputContains("Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService: new instance")
                assertOutputContains("Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService_v2: new instance")
                // Since gradle 8 kotlinDsl was updated to 1.8 version
                // https://docs.gradle.org/8.0/release-notes.html#kotlin-dsl-updated-to-kotlin-api-level-1.8 ,
                // and it registers old service, so we don't need check with re-registering old version service.
                if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0)) {
                    // TODO(Dmitrii Krasnov): you can remove this check, when min gradle version becomes 8 or greater
                    //kotlin 1.4 in kotlinDsl does not create jmx service yet
                    assertOutputContains("Register JMX service for backward compatibility")
                }
                assertOutputDoesNotContain("[org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatHandler] Could not execute")
            }
        }
    }

    @DisplayName("smoke test for fus-statistics-gradle-plugin")
    @GradleTest
    fun smokeTestForFusStatisticsPlugin(gradleVersion: GradleVersion) {
        val metricName = "METRIC_NAME"
        val metricValue = 1
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                ${
                    it.replace(
                        "plugins {",
                        """
                               plugins {
                                  id "org.jetbrains.kotlin.fus-statistics-gradle-plugin" version "${'$'}kotlin_version"
                           """.trimIndent()
                    )
                }
                
                import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatistics
                class TestFusTask extends DefaultTask implements org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService {
                  private Property<GradleBuildFusStatistics> fusStatisticsBuildService = project.objects.property(GradleBuildFusStatistics.class)

                  org.gradle.api.provider.Property getFusStatisticsBuildService(){
                    return fusStatisticsBuildService
                  }

                }
                tasks.register("test-fus", TestFusTask.class).get().doLast {
                  fusStatisticsBuildService.get().reportMetric("$metricName", $metricValue, null)
                }
                """.trimIndent()
            }

            val reportRelativePath = "reports"
            build("test-fus", "-Pkotlin.fus.statistics.path=${projectPath.resolve(reportRelativePath).pathString}") {
                val fusReport = projectPath.getSingleFileInDir("$reportRelativePath/kotlin-fus")
                assertFileContains(
                    fusReport,
                    "METRIC_NAME=1",
                    "BUILD FINISHED"
                )
            }
        }
    }


}