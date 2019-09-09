/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.findFileByName
import org.junit.Test
import java.io.File
import java.net.URI
import kotlin.test.fail

class InstantExecutionIT : BaseGradleIT() {

    // A recent enough Gradle 6.0 nightly
    private val minimumGradleVersion = GradleVersionRequired.AtLeast("6.0-20190823180744+0000")

    @Test
    fun testCompileKotlin() {
        kotlinProject().run {
            instantExecutionOf("compileKotlin") {
                assertSuccessful()
                assertTasksExecuted(":compileKotlin")
            }

            instantExecutionReportFile()?.let { htmlReportFile ->
                // for debugging, copy report so it's available after the test is finished
                val htmlReportFile = copyReportToTempDir(htmlReportFile)
                fail(
                    "Instant execution problems were found, check ${htmlReportFile.asClickableFileUrl()} for details."
                )
            }

            instantExecutionOf("compileKotlin") {
                assertSuccessful()
                assertTasksUpToDate(":compileKotlin")
            }
        }
    }

    private fun kotlinProject() = Project(
        "kotlinProject",
        gradleVersionRequirement = minimumGradleVersion
    )

    private fun Project.instantExecutionOf(vararg tasks: String, check: CompiledProject.() -> Unit) =
        build("-Dorg.gradle.unsafe.instant-execution", *tasks, check = check)

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
        .resolve(".instant-execution-state")
        .findFileByName("instant-execution-report.html")

    private fun File.asClickableFileUrl(): String =
        URI("file", "", toURI().path, null, null).toString()
}
