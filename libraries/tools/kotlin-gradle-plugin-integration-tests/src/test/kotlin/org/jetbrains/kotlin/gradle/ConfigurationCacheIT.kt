/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.findFileByName
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import java.io.File
import java.net.URI
import java.util.Arrays.asList
import kotlin.test.fail

open class ConfigurationCacheIT : BaseGradleIT() {
    private val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v4_1_0

    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(
            androidHome = KotlinTestUtils.findAndroidSdk(),
            androidGradlePluginVersion = androidGradlePluginVersion,
            configurationCache = true
        )

    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.AtLeast("6.6-milestone-2")

    @Test
    fun testSimpleKotlinJvmProject() = with(Project("kotlinProject")) {
        testConfigurationCacheOf(":compileKotlin")
    }

    @Test
    fun testSimpleKotlinAndroidProject() = with(Project("android-dagger", directoryPrefix = "kapt2")) {
        applyAndroid40Alpha4KotlinVersionWorkaround()
        projectDir.resolve("gradle.properties").appendText("\nkapt.incremental.apt=false")
        testConfigurationCacheOf(":app:compileDebugKotlin", ":app:kaptDebugKotlin", ":app:kaptGenerateStubsDebugKotlin")
    }

    @Test
    fun testIncrementalKaptProject() = with(getIncrementalKaptProject()) {
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

    private fun getIncrementalKaptProject() =
        Project("kaptIncrementalCompilationProject").apply {
            setupIncrementalAptProject("AGGREGATING")
        }

    internal fun Project.testConfigurationCacheOf(
        vararg taskNames: String,
        executedTaskNames: List<String>? = null,
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

        build("clean") {
            assertSuccessful()
        }

        // Then run a build where tasks states are deserialized to check that they work correctly in this mode
        build(*taskNames, options = buildOptions) {
            assertSuccessful()
            assertTasksExecuted(executedTask)
            assertContains("Reusing configuration cache.")
        }

        build(*taskNames, options = buildOptions) {
            assertSuccessful()
            assertTasksUpToDate(executedTask)
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
        createTempDir().let { tempDir ->
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

    /**
     * Android Gradle plugin 4.0-alpha4 depends on the EAP versions of some o.j.k modules.
     * Force the current Kotlin version, so the EAP versions are not queried from the
     * test project's repositories, where there's no 'kotlin-eap' repo.
     * TODO remove this workaround once an Android Gradle plugin version is used that depends on the stable Kotlin version
     */
    private fun Project.applyAndroid40Alpha4KotlinVersionWorkaround() {
        setupWorkingDir()

        val resolutionStrategyHack = """
            configurations.all { 
                resolutionStrategy.dependencySubstitution.all { dependency ->
                    def requested = dependency.requested
                    if (requested instanceof ModuleComponentSelector && requested.group == 'org.jetbrains.kotlin') {
                        dependency.useTarget requested.group + ':' + requested.module + ':' + '${defaultBuildOptions().kotlinVersion}'
                    }
                }
            }
        """.trimIndent()

        gradleBuildScript().appendText("\n" + """
            buildscript {
                $resolutionStrategyHack
            }
            $resolutionStrategyHack
        """.trimIndent())
    }

    @Test
    fun testInstantExecution() = with(Project("instantExecution")) {
        testConfigurationCacheOf("assemble", executedTaskNames = asList(":lib-project:compileKotlin"))
    }

    @Test
    fun testInstantExecutionForJs() = with(Project("instantExecutionToJs")) {
        testConfigurationCacheOf("assemble", executedTaskNames = asList(":compileKotlin2Js"))
    }
}
