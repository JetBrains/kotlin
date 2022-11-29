/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
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
        val expectedTasks = mutableListOf(
            ":lib:cinteropMyCinteropLinuxX64",
            ":lib:commonizeCInterop",
            ":lib:compileKotlinLinuxX64",
            ":lib:copyCinteropMyCinteropLinuxX64",
            ":lib:linkExecutableDebugExecutableLinuxX64",
            ":lib:linkSharedDebugSharedLinuxX64",
            ":lib:linkStaticDebugStaticLinuxX64",
            ":lib:linkDebugTestLinuxX64",
        )

        if (HostManager.hostIsMac) {
            expectedTasks += listOf(
                ":lib:cinteropMyCinteropIosX64",
                ":lib:compileKotlinIosX64",
                ":lib:copyCinteropMyCinteropIosX64",
                ":lib:assembleMyframeDebugFrameworkIosArm64",
                ":lib:assembleMyfatframeDebugFatFramework",
                ":lib:assembleLibDebugXCFramework",
                ":lib:compileTestKotlinIosX64",
                ":lib:linkDebugTestIosX64",
            )
        }

        project("native-configuration-cache", gradleVersion) {
            testConfigurationCacheOf(
                "build",
                executedTaskNames = expectedTasks,
                checkConfigurationCacheFileReport = false,
                buildOptions = defaultBuildOptions.copy(
                    configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL,
                    warningMode = WarningMode.All,
                    freeArgs = listOf(
                        // remove after KT-49933 is fixed
                        "-x", ":lib:transformCommonMainDependenciesMetadata",
                        "-x", ":lib:transformCommonMainCInteropDependenciesMetadata",
                    )
                )
            )
        }
    }

    @NativeGradlePluginTests
    @DisplayName("works with commonizer")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4)
    @GradleTest
    fun testCommonizer(gradleVersion: GradleVersion) {
        project("native-configuration-cache", gradleVersion) {
            val buildOptions = defaultBuildOptions.copy(
                configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL,
                warningMode = WarningMode.All
            )
            build(
                ":lib:commonizeCInterop",
                ":commonizeNativeDistribution",
                buildOptions = buildOptions
            ) {
                // Reduce the problem numbers when a Task become compatible with GCC.
                // When all tasks support GCC, replace these assertions with `testConfigurationCacheOf`
                assertOutputContains("1 problem was found storing the configuration cache.")
                assertOutputContains(
                    """Task `\S+` of type `[\w.]+CInteropMetadataDependencyTransformationTask`: .+(at execution time is unsupported)|(not supported with the configuration cache)"""
                        .toRegex()
                )
            }
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
