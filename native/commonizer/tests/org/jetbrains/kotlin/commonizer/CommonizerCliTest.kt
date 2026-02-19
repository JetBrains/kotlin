/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.cli.NativeDistributionListTargets
import org.jetbrains.kotlin.commonizer.cli.Task
import org.jetbrains.kotlin.commonizer.cli.TaskType
import org.jetbrains.kotlin.commonizer.cli.parseTasksFromCommandLineArguments
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonizerCliTest {
    @Rule
    @JvmField
    var testName: TestName = TestName()

    @Test
    fun simpleArgfile() {
        doTestWithArgfile(
            """
            native-dist-print-targets
            -distribution-path
            ${Files.createTempDirectory(testName.methodName).absolutePathString()}
            """.trimIndent()
        ) { tasks ->
            assertEquals(1, tasks.size, "Expected exactly one task to be parsed")
            val singleTask = tasks.single()
            assertTrue(
                singleTask is NativeDistributionListTargets,
                "Expected the parsed task to be NativeDistributionListTargets, actual type is ${singleTask::class}"
            )
        }
    }

    @Test
    fun simpleArgFileWithEscaping() {
        doTestWithArgfile(
            """
            "native-dist-print-targets"
            -distribution-path
            ${Files.createTempDirectory(testName.methodName).absolutePathString()}
            """.trimIndent()
        ) { tasks ->
            assertEquals(1, tasks.size, "Expected exactly one task to be parsed")
            val singleTask = tasks.single()
            assertTrue(
                singleTask is NativeDistributionListTargets,
                "Expected the parsed task to be NativeDistributionListTargets, actual type is ${singleTask::class}"
            )
        }
    }

    private fun doTestWithArgfile(contents: String, assertions: (List<Task>) -> Unit) {
        val tempFile = File.createTempFile("CommonizerCliTest", testName.methodName)
        tempFile.writeText(contents)
        val tasks = parseTasksFromCommandLineArguments(arrayOf("@${tempFile.absoluteFile}"))
        assertions(tasks)
    }
}