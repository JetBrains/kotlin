/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.findFileByName
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
    fun testInstantExecution() = with(Project("instantExecution")) {
        testConfigurationCacheOf("assemble", executedTaskNames = asList(":lib-project:compileKotlin"))
    }

    @Test
    fun testInstantExecutionForJs() = with(Project("instantExecutionToJs")) {
        testConfigurationCacheOf("assemble", executedTaskNames = asList(":compileKotlin2Js"))
    }
}

abstract class AbstractConfigurationCacheIT : BaseGradleIT() {
    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(configurationCache = true)

    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.AtLeast("6.6.1")

    protected fun Project.testConfigurationCacheOf(
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
}
