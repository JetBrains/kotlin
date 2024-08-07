/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.pathString

@DisplayName("FUS statistic")
@JvmGradlePluginTests
class FusPluginIT : KGPBaseTest() {
    private val reportRelativePath = "reports"

    @DisplayName("smoke test for fus-statistics-gradle-plugin")
    @GradleTest
    fun smokeTestForFusStatisticsPlugin(gradleVersion: GradleVersion) {
        val metricName = "METRIC_NAME"
        val metricValue = 1
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                 ${applyFusPluginAndCreateTestFusTask(it)}
                
                 ${registerTaskAndReportMetric("test-fus", metricName, metricValue)}
               
                """.trimIndent()
            }

            build(
                "test-fus",
                "-Pkotlin.fus.statistics.path=${projectPath.resolve(reportRelativePath).pathString}",
                "-Pkotlin.session.logger.root.path=${projectPath.resolve(reportRelativePath).pathString}",
            ) {
                val fusReport = getFusReportFile(gradleVersion)
                assertFileContains(
                    fusReport,
                    "$metricName=$metricValue",
                    "BUILD FINISHED"
                )
            }
        }
    }

    @DisplayName("test override metrics for fus-statistics-gradle-plugin")
    @GradleTest
    fun testMetricsOverrideForFusStatisticsPlugin(gradleVersion: GradleVersion) {
        val metricName = "METRIC_NAME"
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                ${applyFusPluginAndCreateTestFusTask(it)}
                
                ${registerTaskAndReportMetric("test-fus", metricName, "1")}
                
                ${registerTaskAndReportMetric("test-fus-second", metricName, "2")}
            
                """.trimIndent()
            }

            build(
                "test-fus", "test-fus-second",
                "-Pkotlin.fus.statistics.path=${projectPath.resolve(reportRelativePath).pathString}",
                "-Pkotlin.session.logger.root.path=${projectPath.resolve(reportRelativePath).pathString}",
            ) {
                //for Gradle 8.9 the task execution order can be changed
                assertOutputContainsAny(
                    "Try to override $metricName metric: current value is \"1\", new value is \"2\"",
                    "Try to override $metricName metric: current value is \"2\", new value is \"1\""
                )
                val fusReport = getFusReportFile(gradleVersion)
                assertFileContains(
                    fusReport,
                    "$metricName=",
                    "BUILD FINISHED"
                )
            }
        }
    }

    @DisplayName("test invalid fus report directory")
    @GradleTest
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_8_0)
    fun testInvalidFusReportDir(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.modify {
                """
                ${applyFusPluginAndCreateTestFusTask(it)}
                
                ${registerTaskAndReportMetric("test-fus", "metricName", "metricValue")}
                
                """.trimIndent()
            }

            //For kotlin.fus.statistics.path= a root folder will be used, no permission is graded to create /kotlin-fus folder
            build("test-fus", "-Pkotlin.fus.statistics.path=") {
                assertOutputContains("Failed to create directory '/kotlin-fus' for FUS report. FUS report won't be created")
            }
        }
    }

    private fun TestProject.getFusReportFile(
        gradleVersion: GradleVersion,
    ): Path {
        return if (gradleVersion < GradleVersion.version("8.2")) {
            projectPath.getSingleFileInDir("$reportRelativePath/kotlin-fus")
        } else {
            projectPath.getSingleFileInDir("$reportRelativePath/kotlin-profile")
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

    private fun applyFusPluginAndCreateTestFusTask(buildScript: String) = """
        ${addBuildScriptDependency()}    
                        
        $buildScript
                    
        ${applyFusStatisticPlugin()}
                    
        ${createTestFusTaskClass()}
    """


    private fun registerTaskAndReportMetric(taskName: String, metricName: String, metricValue: Any) =
        """
            tasks.register("$taskName", TestFusTask.class) {
                doLast {
                      fusStatisticsBuildService.get().reportMetric("$metricName", "$metricValue", null)
                }
           }
           """

}