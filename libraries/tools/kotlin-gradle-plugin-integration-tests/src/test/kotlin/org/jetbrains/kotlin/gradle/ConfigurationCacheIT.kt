/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.dukat.ExternalsOutputFormat
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.test.fail

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
            val publishedTargets = listOf("kotlinMultiplatform", "jvm6", "nodeJs")

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
    @GradleTestVersions(minVersion = "6.8.3")
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
    @GradleTestVersions(minVersion = "6.8.3")
    @GradleTest
    fun testInstantExecutionWithIncludedBuildPlugin(gradleVersion: GradleVersion) {
        project("instantExecutionWithIncludedBuildPlugin", gradleVersion) {
            testConfigurationCacheOf(
                "build",
                executedTaskNames = listOf(":compileKotlin")
            )
        }
    }

    @DisplayName("instant execution is working for JS project")
    @GradleTest
    fun testInstantExecutionForJs(gradleVersion: GradleVersion) {
        project("instantExecutionToJs", gradleVersion) {
            testConfigurationCacheOf(
                "assemble",
                executedTaskNames = listOf(":compileKotlinJs")
            )
        }
    }

    @DisplayName("is working for JS project")
    @GradleTest
    fun testConfigurationCacheJsPlugin(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            settingsGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            testConfigurationCacheOf(
                ":app:build",
                executedTaskNames = listOf(
                    ":app:packageJson",
                    ":app:publicPackageJson",
                    ":app:compileKotlinJs",
                    ":app:processDceKotlinJs",
                    ":app:browserProductionWebpack",
                )
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
        settingsGradleKts.modify(::transformBuildScriptWithPluginsDsl)
        configure(this)
        testConfigurationCacheOf(
            "irGenerateExternalsIntegrated",
            executedTaskNames = listOf(":irGenerateExternalsIntegrated")
        )
    }

    @DisplayName("works in MPP withJava project")
    @GradleTestVersions(minVersion = "7.0.2", maxVersion = "7.1.1")
    @GradleTest
    fun testJvmWithJavaConfigurationCache(gradleVersion: GradleVersion) {
        project("mppJvmWithJava", gradleVersion) {
            build("jar")

            build("jar") {
                assertOutputContains("Reusing configuration cache.")
            }
        }
    }

    @DisplayName("KT-48241: works in JS with test dependencies")
    @GradleTest
    fun testConfigurationCacheJsWithTestDependencies(gradleVersion: GradleVersion) {
        project("kotlin-js-project-with-test-dependencies", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            testConfigurationCacheOf(
                "assemble",
                executedTaskNames = listOf(":kotlinNpmInstall")
            )
        }
    }
}

@SimpleGradlePluginTests
@KGPBaseTest.GradleTestVersions(minVersion = "6.6.1")
abstract class AbstractConfigurationCacheIT : KGPBaseTest() {
    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(configurationCache = true)

    protected fun TestProject.testConfigurationCacheOf(
        vararg taskNames: String,
        executedTaskNames: List<String>? = null,
        checkUpToDateOnRebuild: Boolean = true,
        buildOptions: BuildOptions = defaultBuildOptions
    ) {
        // First, run a build that serializes the tasks state for instant execution in further builds

        val executedTask: List<String> = executedTaskNames ?: taskNames.toList()

        build(*taskNames, buildOptions = buildOptions) {
            assertTasksExecuted(*executedTask.toTypedArray())
            assertOutputContains(
                "Calculating task graph as no configuration cache is available for tasks: ${taskNames.joinToString(separator = " ")}"
            )
            assertInstantExecutionSucceeded()
        }

        build("clean", buildOptions = buildOptions)

        // Then run a build where tasks states are deserialized to check that they work correctly in this mode
        build(*taskNames, buildOptions = buildOptions) {
            assertTasksExecuted(*executedTask.toTypedArray())
            assertOutputContains("Reusing configuration cache.")
        }

        if (checkUpToDateOnRebuild) {
            build(*taskNames, buildOptions = buildOptions) {
                assertTasksUpToDate(*executedTask.toTypedArray())
            }
        }
    }

    private fun GradleProject.assertInstantExecutionSucceeded() {
        instantExecutionReportFile?.let { htmlReportFile ->
            fail("Instant execution problems were found, check ${htmlReportFile.asClickableFileUrl} for details.")
        }
    }

    /**
     * Copies all files from the directory containing the given [htmlReportFile] to a
     * fresh temp dir and returns a reference to the copied [htmlReportFile] in the new
     * directory.
     */
    @OptIn(ExperimentalPathApi::class)
    private fun copyReportToTempDir(htmlReportFile: Path): Path =
        createTempDir("report").let { tempDir ->
            htmlReportFile.parent.toFile().copyRecursively(tempDir.toFile())
            tempDir.resolve(htmlReportFile.name)
        }

    /**
     * The instant execution report file, if exists, indicates problems were
     * found while caching the task graph.
     */
    private val GradleProject.instantExecutionReportFile 
        get() = projectPath
            .resolve("build")
            .findInPath("configuration-cache-report.html")
            ?.let { copyReportToTempDir(it) }
    
    private val Path.asClickableFileUrl
        get() = URI("file", "", toUri().path, null, null).toString()
}
