/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import org.jetbrains.kotlin.buildtools.icevents.decodeIcEvents
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("IC event reporting")
@JvmGradlePluginTests
@MustRunOnChangesInGradle
@MustRunOnChangesInBuildToolsApi
@OptIn(ExperimentalBuildToolsApi::class)
class IcEventReportingIT : KGPBaseTest() {
    private val GradleProject.icEventsFile: Path
        get() = projectPath.getSingleFileInDir(IC_EVENTS_DIR)

    @TestMetadata("simpleProject")
    @DisplayName("IC events are written and decodable in incremental compilations when build reports are disabled")
    @GradleTest
    fun testIcEventsWrittenWithBuildReportsDisabled(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(buildReport = emptyList())) {
            build("compileKotlin", "-Pkotlin.build.report.ic_event=true") {
                assertTasksExecuted(":compileKotlin")
            }
            // delete events from non-incremental initial rebuild
            icEventsFile.deleteExisting()
            val helloWorldSource = kotlinSourcesDir().resolve("helloWorld.kt")
            helloWorldSource.modify { it.replace("\" and \"", "\", \"") }

            build("compileKotlin", "-Pkotlin.build.report.ic_event=true") {
                assertTasksExecuted(":compileKotlin")
                val events = decodeIcEvents(icEventsFile.readText())

                val compilationStarted = events.filterIsInstance<IcEvent.CompilationStarted>().single()
                assertTrue(compilationStarted.isIncremental, "second compilation is expected to be incremental")

                val compileIteration = events.filterIsInstance<IcEvent.CompileIteration>().first()
                assertEquals(ExitCode.OK.name, compileIteration.exitCode)
                val sourcePath = helloWorldSource.toRealPath().absolutePathString()
                assertEquals(setOf(sourcePath), compileIteration.reasons.keys)
                val reasons = compileIteration.reasons.getValue(sourcePath)
                assertTrue(reasons.isNotEmpty(), "no recompilation reason recorded for '$sourcePath'")
                reasons.forEach { assertTrue(it.readableString.isNotBlank()) }
            }
        }
    }

    @TestMetadata("simpleProject")
    @DisplayName("IC event files are not created when IC event reporting is not enabled")
    @GradleTest
    fun testIcEventsNotWrittenByDefault(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":compileKotlin")
                assertDirectoryInProjectDoesNotExist(IC_EVENTS_DIR)
            }
        }
    }
}

private const val IC_EVENTS_DIR = "build/json"
