/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.pathString

@DisplayName("Build FUS statistics")
class BuildFusStatisticsIT : KGPDaemonsBaseTest() {
    @DisplayName("works for project with buildSrc and kotlinDsl plugin")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2, TestVersions.Gradle.G_8_3],
    )
    fun testCompatibilityBuildSrcWithKotlinDsl(gradleVersion: GradleVersion) {
        project(
            "buildSrcUsingKotlinCompilationAndKotlinPlugin",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            build("assemble") {
                //register build service for buildSrc.
                when {
                    // until 8.0, Gradle was embedding the Kotlin version that used a slightly different approach to detect build finish,
                    // so the service was unregistered after the finish of the buildSrc build
                    // and then registered again in the root build
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0) -> {
                        assertOutputContainsExactTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService: new instance", // the  service for buildSrc
                            1
                        )
                        assertOutputContainsExactTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService: new instance", // the legacy service for compatibility
                            1
                        )
                        assertOutputContainsExactTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService_v2: new instance", // the current default version of the service
                            1
                        )
                    }
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_3) -> {
                        assertOutputContainsExactTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService: new instance", // the legacy service for compatibility
                            1
                        )
                        assertOutputContainsExactTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsBeanService_v2: new instance", // the current default version of the service
                            1
                        )
                    }
                    //for gradle 8.3 kotlin 1.9.0 is used, log message is changed
                    gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_5) -> {
                        assertOutputContainsExactTimes(
                            "Register JMX service for backward compatibility", // the legacy service for compatibility
                            1
                        )
                        assertOutputContainsExactTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService_v2: new instance", // the current default version of the service
                            1
                        )
                    }
                    //for other versions KGP from buildSrc registered both services
                    else -> {
                        assertOutputContainsExactTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService: new instance", // the legacy service for compatibility
                            1
                        )
                        assertOutputContainsExactTimes(
                            "Instantiated class org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService_v2: new instance", // the current default version of the service
                            1
                        )

                    }
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
                ${applyFusStatisticPlugin(it)}
                
                ${createTestFusTaskClass()}
                
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

    private fun applyFusStatisticPlugin(it: String) = it.replace(
        "plugins {",
        """
                                   plugins {
                                      id "org.jetbrains.kotlin.fus-statistics-gradle-plugin" version "${'$'}kotlin_version"
                               """.trimIndent()
    )

    private fun createTestFusTaskClass() = """import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
                    class TestFusTask extends DefaultTask implements org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService {
                      private Property<GradleBuildFusStatisticsService> fusStatisticsBuildService = project.objects.property(GradleBuildFusStatisticsService.class)
    
                      org.gradle.api.provider.Property getFusStatisticsBuildService(){
                        return fusStatisticsBuildService
                      }
    
                    }"""

    @DisplayName("test override metrics for fus-statistics-gradle-plugin")
    @GradleTest
    fun testMetricsOverrideForFusStatisticsPlugin(gradleVersion: GradleVersion) {
        val metricName = "METRIC_NAME"
        val metricValue = 1
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                ${applyFusStatisticPlugin(it)}
                
                ${createTestFusTaskClass()}
                
                tasks.register("test-fus", TestFusTask.class).get().doLast {
                  fusStatisticsBuildService.get().reportMetric("$metricName", $metricValue, null)
                }
                
                tasks.register("test-fus-second", TestFusTask.class).get().doLast {
                  fusStatisticsBuildService.get().reportMetric("$metricName", 2, null)
                }
                """.trimIndent()
            }

            val reportRelativePath = "reports"
            build("test-fus", "test-fus-second", "-Pkotlin.fus.statistics.path=${projectPath.resolve(reportRelativePath).pathString}") {
                assertOutputContains("Try to override $metricName metric: current value is \"1\", new value is \"2\"")
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