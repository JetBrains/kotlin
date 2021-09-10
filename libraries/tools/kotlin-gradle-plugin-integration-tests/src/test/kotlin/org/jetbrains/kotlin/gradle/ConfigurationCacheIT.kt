/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.native.transformNativeTestProject
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.targets.js.dukat.ExternalsOutputFormat
import org.jetbrains.kotlin.gradle.util.createTempDir
import org.jetbrains.kotlin.gradle.util.findFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import java.net.URI
import java.util.Arrays.asList
import kotlin.test.fail

class ConfigurationCacheIT : AbstractConfigurationCacheIT() {
    @Test
    fun testSimpleKotlinJvmProject() = with(Project("kotlinProject")) {
        testConfigurationCacheOf(":compileKotlin")
    }

    @Test
    fun testJvmWithMavenPublish() = with(Project("kotlinProject")) {
        setupWorkingDir()
        gradleBuildScript().appendText("""
            apply plugin: "maven-publish"
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
        """.trimIndent())
        testConfigurationCacheOf(":publishMavenPublicationToMavenRepository", checkUpToDateOnRebuild = false)
    }

    @Test
    fun testMppWithMavenPublish() = with(transformNativeTestProject("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
        val publishedTargets = listOf("kotlinMultiplatform", "jvm6", "nodeJs")
        testConfigurationCacheOf(
            *(publishedTargets.map { ":publish${it.capitalize()}PublicationToMavenRepository" }.toTypedArray()),
            checkUpToDateOnRebuild = false
        )
    }

    @Test
    fun testIncrementalKaptProject() = with(Project("kaptIncrementalCompilationProject")) {
        setupIncrementalAptProject("AGGREGATING")

        testConfigurationCacheOf(
            ":compileKotlin",
            ":kaptKotlin",
            buildOptions = defaultBuildOptions().copy(
                incremental = true,
                kaptOptions = KaptOptions(
                    verbose = true,
                    useWorkers = true,
                    incrementalKapt = true,
                    includeCompileClasspath = false
                )
            )
        )
    }

    @Test
    fun testInstantExecution() =
        // Set min Gradle version to 6.8 because of using DependencyResolutionManagement API to add repositories.
        with(Project("instantExecution", gradleVersionRequirement = GradleVersionRequired.AtLeast("6.8"))) {
            testConfigurationCacheOf("assemble", executedTaskNames = asList(":lib-project:compileKotlin"))
        }

    // KT-43605
    @Test
    fun testInstantExecutionWithBuildSrc() = with(Project("instantExecutionWithBuildSrc")) {
        setupWorkingDir()
        testConfigurationCacheOf(
            "build", executedTaskNames = listOf(
                ":compileKotlin",
            )
        )
    }

    @Test
    fun testInstantExecutionWithIncludedBuildPlugin() =
        with(Project("instantExecutionWithIncludedBuildPlugin", gradleVersionRequirement = GradleVersionRequired.AtLeast("6.8"))) {
            setupWorkingDir()
            testConfigurationCacheOf(
                "build", executedTaskNames = listOf(
                    ":compileKotlin",
                )
            )
        }

    @Test
    fun testInstantExecutionForJs() = with(Project("instantExecutionToJs")) {
        testConfigurationCacheOf("assemble", executedTaskNames = asList(":compileKotlin2Js"))
    }

    @Test
    fun testConfigurationCacheJsPlugin() = with(Project("kotlin-js-browser-project")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)
        testConfigurationCacheOf(
            ":app:build", executedTaskNames = asList(
                ":app:packageJson",
                ":app:publicPackageJson",
                ":app:compileKotlinJs",
                ":app:processDceKotlinJs",
                ":app:browserProductionWebpack",
            )
        )
    }

    @Test
    fun testConfigurationCacheDukatSrc() = testConfigurationCacheDukat()

    @Test
    fun testConfigurationCacheDukatBinaries() = testConfigurationCacheDukat {
        gradleProperties().modify {
            """
                ${ExternalsOutputFormat.externalsOutputFormatProperty}=${ExternalsOutputFormat.BINARY}
            """.trimIndent()
        }
    }

    private fun testConfigurationCacheDukat(configure: Project.() -> Unit = {}) =
        with(Project("both", directoryPrefix = "dukat-integration")) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            gradleSettingsScript().modify(::transformBuildScriptWithPluginsDsl)
            configure(this)
            testConfigurationCacheOf(
                "irGenerateExternalsIntegrated", executedTaskNames = listOf(
                    ":irGenerateExternalsIntegrated"
                )
            )
        }

    // KT-48241
    @Test
    fun testConfigurationCacheJsWithTestDependencies() = with(transformProjectWithPluginsDsl("kotlin-js-project-with-test-dependencies")) {
        testConfigurationCacheOf("assemble", executedTaskNames = listOf(":kotlinNpmInstall"))
    }

    @Test
    fun testBuildReportSmokeTestForConfigurationCache() {
        with(Project("simpleProject")) {
            setupWorkingDir()
            val buildOptions = defaultBuildOptions().copy(withReports = listOf(BuildReportType.FILE))
            build("assemble", options = buildOptions) {
                assertContains("Kotlin build report is written to")
            }

            build("assemble", options = buildOptions) {
                assertContains("Kotlin build report is written to")
            }
        }
    }
}

abstract class AbstractConfigurationCacheIT : BaseGradleIT() {
    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(configurationCache = true)

    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.AtLeast("6.6.1")

    protected fun Project.testConfigurationCacheOf(
        vararg taskNames: String,
        executedTaskNames: List<String>? = null,
        checkUpToDateOnRebuild: Boolean = true,
        buildOptions: BuildOptions = defaultBuildOptions()
    ) {
        // First, run a build that serializes the tasks state for instant execution in further builds

        val executedTask: List<String> = executedTaskNames ?: taskNames.toList()

        build(*taskNames, options = buildOptions) {
            assertSuccessful()
            assertTasksExecuted(executedTask)
            assertContains("Calculating task graph as no configuration cache is available for tasks: ${taskNames.joinToString(separator = " ")}")
            checkInstantExecutionSucceeded()
        }

        build("clean", options = buildOptions) {
            assertSuccessful()
        }

        // Then run a build where tasks states are deserialized to check that they work correctly in this mode
        build(*taskNames, options = buildOptions) {
            assertSuccessful()
            assertTasksExecuted(executedTask)
            assertContains("Reusing configuration cache.")
        }

        if (checkUpToDateOnRebuild) {
            build(*taskNames, options = buildOptions) {
                assertSuccessful()
                assertTasksUpToDate(executedTask)
            }
        }
    }

    private fun Project.checkInstantExecutionSucceeded() {
        instantExecutionReportFile()?.let { htmlReportFile ->
            fail("Instant execution problems were found, check ${htmlReportFile.asClickableFileUrl()} for details.")
        }
    }

    /**
     * Copies all files from the directory containing the given [htmlReportFile] to a
     * fresh temp dir and returns a reference to the copied [htmlReportFile] in the new
     * directory.
     */
    private fun copyReportToTempDir(htmlReportFile: File): File =
        createTempDir("report").let { tempDir ->
            htmlReportFile.parentFile.copyRecursively(tempDir)
            tempDir.resolve(htmlReportFile.name)
        }

    /**
     * The instant execution report file, if exists, indicates problems were
     * found while caching the task graph.
     */
    private fun Project.instantExecutionReportFile() = projectDir
        .resolve("configuration-cache")
        .findFileByName("configuration-cache-report.html")
        ?.let { copyReportToTempDir(it) }

    private fun File.asClickableFileUrl(): String =
        URI("file", "", toURI().path, null, null).toString()
}
