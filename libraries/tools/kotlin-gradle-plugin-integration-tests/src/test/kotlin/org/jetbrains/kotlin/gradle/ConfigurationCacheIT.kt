/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.targets.js.dukat.ExternalsOutputFormat
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Configuration cache")
class ConfigurationCacheIT : AbstractConfigurationCacheIT() {

    @DisplayName("works in simple Kotlin project")
    @GradleTest
    fun testSimpleKotlinJvmProject(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            testConfigurationCacheOf(":compileKotlin")
        }
    }

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

    @DisplayName("works with MPP publishing")
    @GradleTest
    @OptIn(ExperimentalStdlibApi::class)
    fun testMppWithMavenPublish(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            // KT-49933: Support Gradle Configuration caching with HMPP
            val publishedTargets = listOf(/*"kotlinMultiplatform",*/ "jvm6", "nodeJs")

            testConfigurationCacheOf(
                *(publishedTargets.map { ":publish${it.replaceFirstChar { it.uppercaseChar() }}PublicationToMavenRepository" }.toTypedArray()),
                checkUpToDateOnRebuild = false
            )
        }
    }

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
                        useWorkers = true,
                        incrementalKapt = true,
                        includeCompileClasspath = false
                    )
                )
            )
        }
    }

    // Set min Gradle version to 6.8 because of using DependencyResolutionManagement API to add repositories.
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

    @DisplayName("works with Dukat")
    @GradleTest
    fun testConfigurationCacheDukatSrc(gradleVersion: GradleVersion) {
        testConfigurationCacheDukat(gradleVersion)
    }

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

    @DisplayName("with build report")
    @GradleTest
    fun testBuildReportSmokeTestForConfigurationCache(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.FILE))
            build("assemble", buildOptions = buildOptions) {
                assertOutputContains("Kotlin build report is written to")
            }

            build("assemble", buildOptions = buildOptions) {
                assertOutputContains("Kotlin build report is written to")
            }
        }
    }
}

@SimpleGradlePluginTests
abstract class AbstractConfigurationCacheIT : KGPBaseTest() {
    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(configurationCache = true)

    protected fun TestProject.testConfigurationCacheOf(
        vararg taskNames: String,
        executedTaskNames: List<String>? = null,
        checkUpToDateOnRebuild: Boolean = true,
        buildOptions: BuildOptions = defaultBuildOptions
    ) {
        assertSimpleConfigurationCacheScenarioWorks(
            *taskNames,
            executedTaskNames = executedTaskNames,
            checkUpToDateOnRebuild = checkUpToDateOnRebuild,
            buildOptions = buildOptions,
        )
    }
}
