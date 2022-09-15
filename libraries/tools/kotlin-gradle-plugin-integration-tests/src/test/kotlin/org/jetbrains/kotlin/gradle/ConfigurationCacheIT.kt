/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.targets.js.dukat.ExternalsOutputFormat
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Configuration cache")
class ConfigurationCacheIT : AbstractConfigurationCacheIT() {

    @DisplayName("works in simple Kotlin project")
    @GradleTest
    @JvmGradlePluginTests
    fun testSimpleKotlinJvmProject(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            testConfigurationCacheOf(":compileKotlin")
        }
    }

    @JvmGradlePluginTests
    @DisplayName("works with publishing")
    @GradleTest
    fun testJvmWithMavenPublish(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            buildGradle.modify {
                //language=Groovy
                """
                plugins {
                    id 'maven-publish'
                }
                
                $it
                
                group = "com.example"
                version = "1.0"
                
                publishing.repositories {
                    maven {
                        url = "${'$'}buildDir/repo"
                    }
                }
                
                publishing.publications {
                    maven(MavenPublication) {
                        from(components["java"])
                    }
                }
                """.trimIndent()
            }

            testConfigurationCacheOf(":publishMavenPublicationToMavenRepository", checkUpToDateOnRebuild = false)
        }
    }

    @MppGradlePluginTests
    @DisplayName("works with MPP publishing")
    @GradleTest
    fun testMppWithMavenPublish(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            // KT-49933: Support Gradle Configuration caching with HMPP
            val publishedTargets = listOf(/*"kotlinMultiplatform",*/ "jvm6", "nodeJs", "linux64", "mingw64", "mingw86")

            testConfigurationCacheOf(
                ":buildKotlinToolingMetadata", // Remove it when KT-49933 is fixed and `kotlinMultiplatform` publication works
                *(publishedTargets.map { ":publish${it.replaceFirstChar { it.uppercaseChar() }}PublicationToMavenRepository" }.toTypedArray()),
                checkUpToDateOnRebuild = false
            )
        }
    }

    @NativeGradlePluginTests
    @DisplayName("works with native tasks in complex project")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4)
    @GradleTest
    fun testNativeTasks(gradleVersion: GradleVersion) {
        project("native-configuration-cache", gradleVersion) {
            val buildOptions = defaultBuildOptions.copy(
                configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL,
                warningMode = WarningMode.All
            )
            // These tasks currently don't support Configuration Cache and marked as [Task::notCompatibleWithConfigurationCache]
            val configCacheIncompatibleTaskTypes = listOf(
                "CInteropMetadataDependencyTransformationTask",
                "TransformKotlinGranularMetadata"
            )

            build("build", buildOptions = buildOptions) {
                // Reduce the problem numbers when a Task become compatible with GCC.
                // When all tasks support GCC, replace these assertions with `testConfigurationCacheOf`
                assertOutputContains("4 problems were found storing the configuration cache, 2 of which seem unique.")
                configCacheIncompatibleTaskTypes.forEach { taskType ->
                    assertOutputContains(
                        """Task `\S+` of type `[\w.]+$taskType`: .+(at execution time is unsupported)|(not supported with the configuration cache)"""
                            .toRegex()
                    )
                }

                // TODO: Enable `warningMode = Fail` back and remove these asserts when KGP supports Gradle 8.0
                assertOutputContains("The TestReport.destinationDir property has been deprecated.")
                assertOutputContains("The TestReport.reportOn(Object...) method has been deprecated.")
            }
        }
    }

    @NativeGradlePluginTests
    @DisplayName("works with commonizer")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4)
    @GradleTest
    fun testCommonizer(gradleVersion: GradleVersion) {
        project("native-configuration-cache", gradleVersion) {
            val buildOptions = buildOptions.copy(freeArgs = listOf("--rerun-tasks"))
            testConfigurationCacheOf(
                ":lib:commonizeCInterop",
                ":commonizeNativeDistribution",
                buildOptions = buildOptions,
                // TODO(alakotka): Report will be still generated due to incorrect consumption of properties and environmental variables.
                //  https://docs.gradle.org/7.4.2/userguide/configuration_cache.html#config_cache:requirements:reading_sys_props_and_env_vars
                checkConfigurationCacheFileReport = false,
                checkUpToDateOnRebuild = false
            )
        }
    }

    @OtherGradlePluginTests
    @DisplayName("with project using incremental kapt")
    @GradleTest
    fun testIncrementalKaptProject(gradleVersion: GradleVersion) {
        project("kaptIncrementalCompilationProject", gradleVersion) {
            setupIncrementalAptProject("AGGREGATING")

            testConfigurationCacheOf(
                ":compileKotlin",
                ":kaptKotlin",
                buildOptions = defaultBuildOptions.copy(
                    incremental = true,
                    kaptOptions = BuildOptions.KaptOptions(
                        verbose = true,
                        incrementalKapt = true,
                        includeCompileClasspath = false
                    )
                )
            )
        }
    }

    // Set min Gradle version to 6.8 because of using DependencyResolutionManagement API to add repositories.
    @JvmGradlePluginTests
    @DisplayName("with instance execution")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_6_8)
    @GradleTest
    fun testInstantExecution(gradleVersion: GradleVersion) {
        project("instantExecution", gradleVersion) {
            testConfigurationCacheOf(
                "assemble",
                executedTaskNames = listOf(":lib-project:compileKotlin")
            )
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KT-43605: instant execution with buildSrc")
    @GradleTest
    fun testInstantExecutionWithBuildSrc(gradleVersion: GradleVersion) {
        project("instantExecutionWithBuildSrc", gradleVersion) {
            testConfigurationCacheOf(
                "build",
                executedTaskNames = listOf(":compileKotlin")
            )
        }
    }

    @JvmGradlePluginTests
    @DisplayName("instant execution works with included build plugin")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_6_8)
    @GradleTest
    fun testInstantExecutionWithIncludedBuildPlugin(gradleVersion: GradleVersion) {
        project("instantExecutionWithIncludedBuildPlugin", gradleVersion) {
            testConfigurationCacheOf(
                "build",
                executedTaskNames = listOf(":compileKotlin")
            )
        }
    }

    @JsGradlePluginTests
    @DisplayName("works with Dukat")
    @GradleTest
    fun testConfigurationCacheDukatSrc(gradleVersion: GradleVersion) {
        testConfigurationCacheDukat(gradleVersion)
    }

    @JsGradlePluginTests
    @DisplayName("works with Dukat binaries")
    @GradleTest
    fun testConfigurationCacheDukatBinaries(gradleVersion: GradleVersion) {
        testConfigurationCacheDukat(gradleVersion) {
            gradleProperties.modify {
                """
                
                ${ExternalsOutputFormat.externalsOutputFormatProperty}=${ExternalsOutputFormat.BINARY}
                """.trimIndent()
            }
        }
    }

    private fun testConfigurationCacheDukat(
        gradleVersion: GradleVersion,
        configure: TestProject.() -> Unit = {}
    ) = project("dukat-integration/both", gradleVersion) {
        buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
        configure(this)
        testConfigurationCacheOf(
            "irGenerateExternalsIntegrated",
            executedTaskNames = listOf(":irGenerateExternalsIntegrated")
        )
    }

    @MppGradlePluginTests
    @DisplayName("works in MPP withJava project")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0, maxVersion = TestVersions.Gradle.G_7_1)
    @GradleTest
    fun testJvmWithJavaConfigurationCache(gradleVersion: GradleVersion) {
        project("mppJvmWithJava", gradleVersion) {
            build("jar")

            build("jar") {
                assertOutputContains("Reusing configuration cache.")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("with build report")
    @GradleTest
    fun testBuildReportSmokeTestForConfigurationCache(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.FILE))
        ) {
            build("assemble") {
                assertBuildReportPathIsPrinted()
            }

            build("assemble") {
                assertBuildReportPathIsPrinted()
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("with build build scan report")
    @GradleTest
    fun testBuildScanReportSmokeTestForConfigurationCache(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.BUILD_SCAN), logLevel = LogLevel.DEBUG)
            build("clean", "assemble", "-Pkotlin.build.report.build_scan.custom_values_limit=0", "--scan", buildOptions = buildOptions) {
                assertOutputContains("Can't add any more custom values into build scan")
            }

            build("clean", "assemble", "-Pkotlin.build.report.build_scan.custom_values_limit=0", "--scan", buildOptions = buildOptions) {
                assertOutputContains("Can't add any more custom values into build scan")
            }
        }
    }
}

abstract class AbstractConfigurationCacheIT : KGPBaseTest() {
    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(configurationCache = true)

    protected fun TestProject.testConfigurationCacheOf(
        vararg taskNames: String,
        executedTaskNames: List<String>? = null,
        checkUpToDateOnRebuild: Boolean = true,
        checkConfigurationCacheFileReport: Boolean = true,
        buildOptions: BuildOptions = defaultBuildOptions
    ) {
        assertSimpleConfigurationCacheScenarioWorks(
            *taskNames,
            executedTaskNames = executedTaskNames,
            checkUpToDateOnRebuild = checkUpToDateOnRebuild,
            checkConfigurationCacheFileReport = checkConfigurationCacheFileReport,
            buildOptions = buildOptions,
        )
    }
}
