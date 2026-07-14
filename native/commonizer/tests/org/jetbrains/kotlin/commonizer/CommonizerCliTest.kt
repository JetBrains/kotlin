/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.cli.NativeDistributionListTargets
import org.jetbrains.kotlin.commonizer.cli.Task
import org.jetbrains.kotlin.commonizer.cli.parseTasksFromCommandLineArguments
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonizerCliTest {
    private lateinit var testMethodName: String

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        testMethodName = testInfo.testMethod.get().name
    }


    @Test
    fun simpleArgfile() {
        doTestWithArgfile(
            """
            native-dist-print-targets
            -distribution-path
            ${Files.createTempDirectory(testMethodName).absolutePathString()}
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
            ${Files.createTempDirectory(testMethodName).absolutePathString()}
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
        val tempFile = File.createTempFile("CommonizerCliTest", testMethodName)
        tempFile.writeText(contents)
        val tasks = parseTasksFromCommandLineArguments(arrayOf("@${tempFile.absoluteFile}"))
        assertions(tasks)
    }
}
