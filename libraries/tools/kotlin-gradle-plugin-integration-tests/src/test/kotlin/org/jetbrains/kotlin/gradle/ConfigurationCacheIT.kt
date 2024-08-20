/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
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
    @GradleTest
    fun testMppWithMavenPublish(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            val publishedTargets = listOf("kotlinMultiplatform", "jvm6", "nodeJs", "linux64", "mingw64")
            testConfigurationCacheOf(
                taskNames = publishedTargets
                    .map { ":publish${it.replaceFirstChar(Char::uppercaseChar)}PublicationToLocalRepoRepository" }
                    .toTypedArray(),
                checkUpToDateOnRebuild = false
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("KT-63363: all metadata jar works well with configuration cache")
    @GradleTest
    @TestMetadata("new-mpp-lib-and-app/sample-lib")
    fun testAllMetadataJarWithConfigurationCache(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            testConfigurationCacheOf(":allMetadataJar")
        }
    }

    @NativeGradlePluginTests
    @DisplayName("works with commonizer")
    @GradleTest
    fun testCommonizer(gradleVersion: GradleVersion) {
        project("native-configuration-cache", gradleVersion) {
            build(":lib:cleanNativeDistributionCommonization")

            build(":lib:compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":lib:commonizeNativeDistribution")
                assertTasksExecuted(":lib:compileCommonMainKotlinMetadata")
                assertConfigurationCacheStored()
            }

            build("clean", ":lib:cleanNativeDistributionCommonization") {
                assertTasksExecuted(":lib:cleanNativeDistributionCommonization")
                assertConfigurationCacheStored()
            }

            build(":lib:compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":lib:commonizeNativeDistribution")
                assertTasksExecuted(":lib:compileCommonMainKotlinMetadata")
                assertConfigurationCacheReused()
            }
        }
    }

    @NativeGradlePluginTests
    @DisplayName("Configuration cache works with Kotlin Native bundle and its dependencies downloading")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_6],
    )
    @OsCondition(
        supportedOn = [OS.LINUX, OS.MAC], // disabled on Windows because of tmp dir problem KT-62761
        enabledOnCI = [OS.LINUX, OS.MAC],
    )
    @GradleTest
    fun testWithDownloadingKotlinNativeAndDependencies(gradleVersion: GradleVersion, @TempDir konanTempDir: Path) {
        // with Configuration Cache we currently have such a problem KT-66423
        val buildOptions = buildOptionsToAvoidKT66423(gradleVersion, konanTempDir)
        project("native-configuration-cache", gradleVersion, buildOptions = buildOptions) {

            build(":lib:compileCommonMainKotlinMetadata") {
                assertConfigurationCacheStored()
            }

            build(":lib:compileCommonMainKotlinMetadata") {
                assertConfigurationCacheReused()
            }
        }
    }

    @NativeGradlePluginTests
    @GradleTest
    fun testCInteropCommonizer(gradleVersion: GradleVersion) {
        project("native-configuration-cache", gradleVersion) {
            testConfigurationCacheOf(":lib:commonizeCInterop")
        }
    }

    @DisplayName("KT-66452: There are no false-positive Configuration Cache failures with native tasks")
    @NativeGradlePluginTests
    @GradleTest
    fun testFalsePositiveWithNativeTasks(gradleVersion: GradleVersion) {
        nativeProject("native-simple-project", gradleVersion) {
            buildGradleKts.modify {
                //language=kotlin
                """
                |import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
                |import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
                |
                |$it
                |
                |tasks.register("notCCCompatibleTask") {
                |    notCompatibleWithConfigurationCache("not really")
                |}
                |
                |tasks.withType<KotlinNativeCompile> {
                |    dependsOn("notCCCompatibleTask")
                |}
                |
                |tasks.withType<KotlinNativeLink> {
                |    dependsOn("notCCCompatibleTask")
                |}
                """.trimMargin()
            }
            build(":commonizeNativeDistribution")
            build(":compileCommonMainKotlinMetadata")
            build(":linkLinuxArm64")
        }
    }

    @DisplayName("KT-66452: There are no false-positive Configuration Cache failures with cinterop")
    @NativeGradlePluginTests
    @GradleTest
    fun testFalsePositiveWithCInteropTask(gradleVersion: GradleVersion) {

        nativeProject("cinterop-with-header", gradleVersion) {
            buildGradleKts.modify {
                //language=kotlin
                """
                |import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
                |
                |$it
                |
                |tasks.register("notCCCompatibleTask") {
                |    notCompatibleWithConfigurationCache("not really")
                |}
                |
                |tasks.withType<CInteropProcess> {
                |    dependsOn("notCCCompatibleTask")
                |}
                """.trimMargin()
            }
            build(":cinteropCinteropNative")
        }
    }

    @MppGradlePluginTests
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_8_0 // configuration cache and precompiled script plugins fails on earlier versions
    )
    @GradleTest
    fun `test composite build with precompiled script plugins and multiplatform`(gradleVersion: GradleVersion) {
        project("composite-build-with-precompiled-script-plugins", gradleVersion) {
            settingsGradleKts.replaceText(
                "pluginManagement {",
                """
                    pluginManagement {
                        includeBuild("build-logic")
                """.trimIndent()
            )
            subProject("build-logic").projectPath.addPluginManagementToSettings()

            testConfigurationCacheOf(
                ":lib:transformCommonMainDependenciesMetadata",
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

    @JvmGradlePluginTests
    @DisplayName("with instance execution")
    @GradleTest
    fun testInstantExecution(gradleVersion: GradleVersion) {
        project(
            "instantExecution",
            gradleVersion,
            // we can remove this line, when the min version of Gradle be at least 8.1
            dependencyManagement = DependencyManagement.DisabledDependencyManagement
        ) {
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
    @GradleTest
    fun testJvmWithJavaConfigurationCache(gradleVersion: GradleVersion) {
        project("mppJvmWithJava", gradleVersion) {
            build("jvmWithJavaJar")
            build("jvmWithJavaJar") {
                assertOutputContains("Reusing configuration cache.")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("with build report")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
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
        super.defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED)

    protected fun TestProject.testConfigurationCacheOf(
        vararg taskNames: String,
        executedTaskNames: List<String>? = null,
        checkUpToDateOnRebuild: Boolean = true,
        buildOptions: BuildOptions = this.buildOptions,
    ) {
        assertSimpleConfigurationCacheScenarioWorks(
            *taskNames,
            executedTaskNames = executedTaskNames,
            checkUpToDateOnRebuild = checkUpToDateOnRebuild,
            buildOptions = buildOptions,
        )
    }

    protected fun buildOptionsToAvoidKT66423(gradleVersion: GradleVersion, konanTempDir: Path) =
        if (gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_8_6)) {
            defaultBuildOptions.copy(
                konanDataDir = konanDir,
                nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
                    version = System.getProperty("kotlinNativeVersion")
                )
            )
        } else defaultBuildOptions.copy(
            configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
            konanDataDir = konanTempDir,
            nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
                // set the KGP's default Kotlin Native version, because in CI we don't have K/N versions in maven repo for each build
                version = null
            )
        )
}
