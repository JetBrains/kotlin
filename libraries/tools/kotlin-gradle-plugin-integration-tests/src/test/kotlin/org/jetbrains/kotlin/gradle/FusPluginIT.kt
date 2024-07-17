/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.pathString

@DisplayName("FUS statistic")
class FusPluginIT : KGPBaseTest() {

    @DisplayName("smoke test for fus-statistics-gradle-plugin")
    @GradleTest
    fun smokeTestForFusStatisticsPlugin(gradleVersion: GradleVersion) {
        val metricName = "METRIC_NAME"
        val metricValue = 1
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                ${addBuildScriptDependency()}    
                    
                $it
                
                ${applyFusStatisticPlugin()}
                
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

    private fun addBuildScriptDependency() = """
        buildscript {
            dependencies {
                classpath "org.jetbrains.kotlin:fus-statistics-gradle-plugin:${'$'}kotlin_version"
            }
        }
    """.trimIndent()

    private fun applyFusStatisticPlugin() = """
        plugins.apply("org.jetbrains.kotlin.fus-statistics-gradle-plugin")
    """.trimIndent()

    private fun createTestFusTaskClass() = """
        import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
        import org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService

        class TestFusTask extends DefaultTask implements UsesGradleBuildFusStatisticsService {

            private Property<GradleBuildFusStatisticsService> fusStatisticsBuildService = project.objects.property(GradleBuildFusStatisticsService.class)

            Property getFusStatisticsBuildService(){
                return fusStatisticsBuildService
            }

        }
    """.trimIndent()

    @DisplayName("test override metrics for fus-statistics-gradle-plugin")
    @GradleTest
    fun testMetricsOverrideForFusStatisticsPlugin(gradleVersion: GradleVersion) {
        val metricName = "METRIC_NAME"
        val metricValue = 1
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                ${addBuildScriptDependency()}    
                    
                $it
                
                ${applyFusStatisticPlugin()}
                
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