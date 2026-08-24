/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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

            validateFusFiles(
                "test-fus",
                fusFilesType = listOf(FusFileType.KOTLIN_PROFILE_FILES),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "$metricName=$metricValue",
                    "BUILD FINISHED"
                )
            }
        }
    }

    @DisplayName("with configuration cache and project isolation")
    @GradleTest
    fun withConfigurationCacheAndProjectIsolation(gradleVersion: GradleVersion) {
        val executionTimeValue = "EXECUTION_METRIC_VALUE"
        val configurationTimeMetricName = "CONFIGURATION_METRIC_NAME"
        project(
            "incrementalMultiproject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
            ),
        ) {
            listOf(subProject("lib"), subProject("app")).forEach { project ->
                project.buildGradle.modify {
                    """
                    |${applyFusPluginAndCreateTestFusTask(it)}
                    |
                    |${registerTaskAndReportMetric("test-fus", project.projectName, executionTimeValue)}
                    |
                    |import org.jetbrains.kotlin.gradle.fus.ConfigurationMetricsKt
                    |import org.jetbrains.kotlin.gradle.fus.Metric
                    |import org.jetbrains.kotlin.gradle.fus.UniqueId
                    |
                    |use(ConfigurationMetricsKt) {
                    |    project.addGradleConfigurationPhaseMetric( { [ Metric.newInstance("$configurationTimeMetricName","${project.projectName}", UniqueId.@Companion.DEFAULT) ] } )
                    |}
                    """.trimMargin()
                }
            }

            val fusReportRootDirectory = projectPath.resolve(reportRelativePath)

            validateFusFiles(
                "test-fus",
                buildAction = BuildActions.build,
                fusFilesType = listOf(FusFileType.KOTLIN_PROFILE_FILES),
                fusReportRootDirectory = fusReportRootDirectory,
                buildAssertions = { assertConfigurationCacheStored() }
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "app=$executionTimeValue",
                    "lib=$executionTimeValue",
                    "$configurationTimeMetricName=app",
                    "$configurationTimeMetricName=lib",
                    "BUILD FINISHED"
                )
            }

            val firstBuildId = checkBuildReportIdInFusReportAndReturn(fusReportRootDirectory)

            fusReportRootDirectory.deleteRecursively()
            build("clean")

            validateFusFiles(
                "test-fus",
                buildAction = BuildActions.build,
                fusFilesType = listOf(FusFileType.KOTLIN_PROFILE_FILES),
                fusReportRootDirectory = fusReportRootDirectory,
                buildAssertions = { assertConfigurationCacheReused() }
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "$configurationTimeMetricName=app",
                    "$configurationTimeMetricName=lib",
                    "BUILD FINISHED"
                )
            }

            val secondBuildId = checkBuildReportIdInFusReportAndReturn(fusReportRootDirectory)

            assertNotEquals(firstBuildId, secondBuildId, "Build is should be unique for every build")

            fusReportRootDirectory.deleteRecursively()
        }
    }

    private fun checkBuildReportIdInFusReportAndReturn(fusReportRootDirectory: Path): String {
        val fusReports = fusReportRootDirectory.resolve("kotlin-profile").toFile().listFiles()
        val buildIds = fusReports?.filter { !it.name.endsWith(".finish-profile") }?.map { it.readText().lines()[0] }
            ?.distinct() //the first line is build id
        assertEquals(1, buildIds?.size, "Build is in all FUS files should be the same.")
        return buildIds!![0] //all checks were made on the assertion above
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

            validateFusFiles(
                "test-fus", "test-fus-second",
                fusFilesType = listOf(FusFileType.KOTLIN_PROFILE_FILES),
            ) { fusFiles ->
                //Metrics should not be overridden and both metrics should be in the file
                assertFilesCombinedContains(
                    fusFiles,
                    "Build: ",
                    "$metricName=1",
                    "$metricName=2",
                    "BUILD FINISHED"
                )
            }
        }
    }

    @DisplayName("test configuration metrics with different classloaders")
    @GradleTest
    fun testConfigurationMetricsOnly(gradleVersion: GradleVersion) {
        project("multiClassloaderProject", gradleVersion) {

            listOf("subProjectA", "subProjectB").forEach { subProject ->
                subProject(subProject).buildGradleKts.modify {
                    """
                        import org.jetbrains.kotlin.gradle.fus.addGradleConfigurationPhaseMetric
                        import org.jetbrains.kotlin.gradle.fus.Metric
                        import org.jetbrains.kotlin.gradle.fus.UniqueId
                        
                        ${applyFusStatisticPlugin(it)}
                        
                        project.addGradleConfigurationPhaseMetric { listOf(Metric("$subProject","value", UniqueId.DEFAULT)) }
                    """.trimIndent()
                }
            }

            validateFusFiles(
                "assemble",
                fusFilesType = listOf(FusFileType.KOTLIN_PROFILE_FILES),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "subProjectA=value",
                    "subProjectB=value",
                    "BUILD FINISHED"
                )
            }

            //the second build reports the same configuration metrics from the configuration cache
            validateFusFiles(
                "assemble",
                fusFilesType = listOf(FusFileType.KOTLIN_PROFILE_FILES),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "subProjectA=value",
                    "subProjectB=value",
                    "BUILD FINISHED"
                )
            }
        }
    }

    private fun addBuildScriptDependency() =
        """
        |buildscript {
        |    dependencies {
        |        classpath "org.jetbrains.kotlin:fus-statistics-gradle-plugin:${'$'}kotlin_version"
        |    }
        |}
        """.trimMargin()

    private fun applyFusStatisticPlugin(buildScript: String/*, apply: Boolean = true*/) =
        """
        |$buildScript
        |
        |plugins.apply("org.jetbrains.kotlin.fus-statistics-gradle-plugin")
        """.trimMargin()

    private fun createTestFusTaskClass() =
        """
        |import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
        |import org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService
        |
        |class TestFusTask extends DefaultTask implements UsesGradleBuildFusStatisticsService {
        |
        |    private Property<GradleBuildFusStatisticsService> fusStatisticsBuildService = project.objects.property(GradleBuildFusStatisticsService.class)
        |
        |    Property getFusStatisticsBuildService(){
        |        return fusStatisticsBuildService
        |    }
        |
        |}
        """.trimMargin()

    private fun applyFusPluginAndCreateTestFusTask(buildScript: String) =
        """
        |${addBuildScriptDependency()}    
        |            
        |${applyFusStatisticPlugin(buildScript)}
        |            
        |${createTestFusTaskClass()}
        """.trimMargin()


    private fun registerTaskAndReportMetric(taskName: String, metricName: String, metricValue: Any) =
        """
        |import org.jetbrains.kotlin.gradle.fus.TaskId
        |tasks.register("$taskName", TestFusTask.class) {
        |   doLast {
        |        fusStatisticsBuildService.get().reportMetric("$metricName", "$metricValue", TaskId.newInstance(null, "$taskName"))
        |   }
        |}
        """.trimMargin()

}
