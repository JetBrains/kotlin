/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

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
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_4,
        additionalVersions = [TestVersions.Gradle.G_7_6],
        maxVersion = TestVersions.Gradle.G_8_1
    )
    @GradleTest
    fun testMppWithMavenPublish(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            val publishedTargets = listOf("kotlinMultiplatform", "jvm6", "nodeJs", "linux64", "mingw64", "mingw86")
            testConfigurationCacheOf(
                *(publishedTargets.map { ":publish${it.replaceFirstChar { it.uppercaseChar() }}PublicationToMavenRepository" }.toTypedArray()),
                checkUpToDateOnRebuild = false
            )
        }
    }

    @NativeGradlePluginTests
    @DisplayName("works with native tasks in complex project")
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_4,
        additionalVersions = [TestVersions.Gradle.G_7_6],
        maxVersion = TestVersions.Gradle.G_8_1
    )
    @GradleTest
    fun testNativeTasks(gradleVersion: GradleVersion) {
        val expectedTasks = mutableListOf(
            ":lib:cinteropMyCinteropLinuxX64",
            ":lib:commonizeCInterop",
            ":lib:compileKotlinLinuxX64",
            ":lib:linkExecutableDebugExecutableLinuxX64",
            ":lib:linkSharedDebugSharedLinuxX64",
            ":lib:linkStaticDebugStaticLinuxX64",
            ":lib:linkDebugTestLinuxX64",
        )

        if (HostManager.hostIsMac) {
            expectedTasks += listOf(
                ":lib:cinteropMyCinteropIosX64",
                ":lib:compileKotlinIosX64",
                ":lib:assembleMyframeDebugFrameworkIosArm64",
                ":lib:assembleMyfatframeDebugFatFramework",
                ":lib:assembleLibDebugXCFramework",
                ":lib:compileTestKotlinIosX64",
                ":lib:linkDebugTestIosX64",
                ":lib:transformCommonMainDependenciesMetadata",
                ":lib:transformCommonMainCInteropDependenciesMetadata",
                ":lib:linkDebugFrameworkIosArm64",
                ":lib:linkDebugFrameworkIosX64",
                ":lib:linkDebugFrameworkIosFat",
                ":lib:linkReleaseFrameworkIosArm64",
                ":lib:linkReleaseFrameworkIosX64",
                ":lib:linkReleaseFrameworkIosFat",
            )
        }

        project("native-configuration-cache", gradleVersion) {
            testConfigurationCacheOf(
                "build",
                executedTaskNames = expectedTasks,
            )
        }
    }

    @NativeGradlePluginTests
    @DisplayName("works with commonizer")
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_4,
        additionalVersions = [TestVersions.Gradle.G_7_6],
        maxVersion = TestVersions.Gradle.G_8_1
    )
    @GradleTest
    fun testCommonizer(gradleVersion: GradleVersion, @TempDir konanHome: Path) {
        project("native-configuration-cache", gradleVersion) {
            build(
                ":commonizeNativeDistribution",
            ) {
                assertOutputContains("0 problems were found storing the configuration cache.")
            }

            // Override kotlin native home location to be able to run clean native distribution commonization task
            // since by default it is global location on host
            val buildOptions = defaultBuildOptions.copy(
                freeArgs = listOf("-Porg.jetbrains.kotlin.native.home=$konanHome")
            )
            build(":cleanNativeDistributionCommonization", buildOptions = buildOptions) {
                assertOutputContains("0 problems were found storing the configuration cache.")
            }

            testConfigurationCacheOf(":lib:commonizeCInterop")
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
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
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
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.MIN_SUPPORTED,
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
        maxVersion = TestVersions.Gradle.G_8_1
    )
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
    @DisplayName("with build scan report")
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.MIN_SUPPORTED,
        additionalVersions = [TestVersions.Gradle.G_7_6], //build scan reports doesn't work properly for Gradle 8.0
        maxVersion = TestVersions.Gradle.G_8_1
    )
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
        buildOptions: BuildOptions = this.buildOptions
    ) {
        assertSimpleConfigurationCacheScenarioWorks(
            *taskNames,
            executedTaskNames = executedTaskNames,
            checkUpToDateOnRebuild = checkUpToDateOnRebuild,
            buildOptions = buildOptions,
        )
    }
}
