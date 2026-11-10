/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OtherGradlePluginTests
@DisplayName("Task output extraction")
class OutputHelpersTest {

    @DisplayName("INFO task output keeps content after a blank line logged by the task")
    @Test
    fun infoTaskOutputContainsBlankLines() {
        val output = /* language=text */ """
            |Resolve mutations for :compileKotlin (Thread[included builds,5,main]) started.
            |:compileKotlin (Thread[included builds,5,main]) started.
            |
            |> Task :compileKotlin
            |w: some warning
            |
            |i: Kotlin compiler args: -jvm-target 11
            |
            |> Task :jar
            |jar output
            |
            |0 problems were found storing the configuration cache.
            |
            |See the complete report at file:///report.html
            |
            |BUILD SUCCESSFUL in 1s
            |""".trimMargin()

        assertEquals(
            /* language=text */ """
            |w: some warning
            |
            |i: Kotlin compiler args: -jvm-target 11
            """.trimMargin(),
            getOutputForTask(":compileKotlin", output, LogLevel.INFO),
        )
        assertEquals("jar output", getOutputForTask(":jar", output, LogLevel.INFO))
    }
}
