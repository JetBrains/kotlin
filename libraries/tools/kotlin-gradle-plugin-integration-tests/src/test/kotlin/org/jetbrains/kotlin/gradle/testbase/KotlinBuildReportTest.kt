/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.Path
import kotlin.test.assertEquals

@OtherGradlePluginTests
@DisplayName("Kotlin build report parsing")
class KotlinBuildReportTest {

    @DisplayName("compile iterations are extracted per task, without recompilation reasons")
    @Test
    fun compileIterations() {
        assertEquals(
            listOf(
                listOf(Path("lib/src/main/kotlin/a.kt")),
                listOf(Path("lib/src/main/kotlin/b.kt"), Path("lib/src/main/kotlin/c.kt")),
            ),
            extractKotlinBuildReportCompileIterations(extractKotlinBuildReportTaskLog(report, ":lib:compileKotlin")),
        )
        assertEquals(emptyList(), extractKotlinBuildReportCompileIterations(extractKotlinBuildReportTaskLog(report, ":app:compileKotlin")))
    }

    @DisplayName("compilation log only contains lines of the requested task")
    @Test
    fun compilationLog() {
        assertEquals(
            "  Non-incremental compilation will be performed: UNKNOWN_CHANGES_IN_GRADLE_INPUTS\n",
            extractKotlinBuildReportTaskLog(report, ":app:compileKotlin"),
        )
        assertEquals("", extractKotlinBuildReportTaskLog(report, ":jar"))
    }

    @DisplayName("compile iterations of all tasks are extracted from a whole report")
    @Test
    fun compileIterationsAcrossTasks() {
        val report = /* language=text */ """
            |Compilation log for task ':lib:compileKotlin':
            |  Compile iteration:
            |    lib/src/main/kotlin/a.kt <- Modified or added since the last build
            |Compilation log for task ':app:compileKotlin':
            |  Compile iteration:
            |    app/src/main/kotlin/b.kt <- Was affected
            |""".trimMargin()

        assertEquals(
            listOf(listOf(Path("lib/src/main/kotlin/a.kt")), listOf(Path("app/src/main/kotlin/b.kt"))),
            extractKotlinBuildReportCompileIterations(report),
        )
    }

    @DisplayName("task log isn't mixed up with a task whose path ends the same")
    @Test
    fun overlappingTaskPaths() {
        val report = /* language=text */ """
            |Compilation log for task ':lib:compileKotlin':
            |  lib log
            |Compilation log for task ':compileKotlin':
            |  root log
            |""".trimMargin()

        assertEquals("  root log\n", extractKotlinBuildReportTaskLog(report, ":compileKotlin"))
        assertEquals("  lib log\n", extractKotlinBuildReportTaskLog(report, ":lib:compileKotlin"))
    }

    @DisplayName("Windows line endings are supported")
    @Test
    fun windowsLineEndings() {
        assertEquals(
            listOf(
                listOf(Path("lib/src/main/kotlin/a.kt")),
                listOf(Path("lib/src/main/kotlin/b.kt"), Path("lib/src/main/kotlin/c.kt")),
            ),
            extractKotlinBuildReportCompileIterations(report.replace("\n", "\r\n")),
        )
    }

    @DisplayName("locations of all reports written by a build are found")
    @Test
    fun reportLocations() {
        val output = /* language=text */ """
            |Kotlin build report is written to file:///tmp/included-build.txt
            |BUILD SUCCESSFUL in 1s
            |Kotlin build report is written to file:///tmp/root-build.txt
            |""".trimMargin()

        assertEquals(
            listOf(URI("file:///tmp/included-build.txt"), URI("file:///tmp/root-build.txt")),
            extractKotlinBuildReportLocations(output.lineSequence()),
        )
    }

    companion object {
        private val report = /* language=text */ """
            |Task ':lib:compileKotlin' finished in 0.361 s
            |Task info:
            |  Kotlin language version: 2.5
            |Compilation log for task ':lib:compileKotlin':
            |  Compiling using the daemon strategy
            |  Compile iteration:
            |    lib/src/main/kotlin/a.kt <- Modified or added since the last build
            |  Deleting /tmp/a.class on clearing cache for /tmp/a.kt
            |  Compile iteration:
            |    lib/src/main/kotlin/b.kt <- Was affected; Other reason
            |    lib/src/main/kotlin/c.kt
            |  Incremental compilation completed
            |Time metrics:
            |  Total Gradle task time: 0.361 s
            |Task ':app:compileKotlin' finished in 0.1 s
            |Compilation log for task ':app:compileKotlin':
            |  Non-incremental compilation will be performed: UNKNOWN_CHANGES_IN_GRADLE_INPUTS
            |Task ':jar' finished in 0.003 s
            |Time metrics:
            |  Total Gradle task time: 0.003 s
            |""".trimMargin()
    }
}
