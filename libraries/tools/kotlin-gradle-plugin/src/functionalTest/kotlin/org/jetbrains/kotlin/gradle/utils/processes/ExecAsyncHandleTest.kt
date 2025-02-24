/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.utils.processes

import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecException
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.util.assertContains
import org.jetbrains.kotlin.gradle.utils.processes.ExecAsyncHandle.Companion.execAsync
import org.jetbrains.kotlin.util.assertDoesNotThrow
import org.jetbrains.kotlin.util.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.FileSystems
import kotlin.system.exitProcess
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class ExecAsyncHandleTest {

    @Test
    fun `when ExecAsync runs successfully expect ExecResult returns success`() {
        val handle = buildTestHandle()

        val result = handle.start().waitForResult()

        assertEquals(0, result?.exitValue)

        assertDoesNotThrow {
            result?.assertNormalExitValue()
        }
    }

    @Test
    fun `when setting stdout and stderr in ExecAsync, expect process logs are forwarded`() {

        class TestOutputStream : ByteArrayOutputStream() {
            var isClosed: Boolean = false
                private set

            override fun close() {
                isClosed = true
                super.close()
            }
        }

        val processStdout = TestOutputStream()
        val processStderr = TestOutputStream()

        val handle = buildTestHandle {
            args(
                "logToStdOut=here's some stdout",
                "logToStdErr=and also some stderr",
                "logToStdOut=with another stdout line",
                "logToStdErr=plus some more stderr, lucky you!",
            )
            standardOutput = processStdout
            errorOutput = processStderr
        }

        handle.start().waitForResult()

        assertEquals(
            listOf("here's some stdout", "with another stdout line", ""),
            processStdout.toString().lines()
        )
        assertEquals(
            listOf("and also some stderr", "plus some more stderr, lucky you!", ""),
            processStderr.toString().lines()
        )

        assertTrue(processStdout.isClosed, "Verify ExecHandle automatically closes standardOutput after completion")
        assertTrue(processStderr.isClosed, "Verify ExecHandle automatically closes errorOutput after completion")
    }

    @Test
    fun `when setting stdin in ExecAsync, expect process receives input`() {
        val processStdout = ByteArrayOutputStream()
        val stdinForProcess = ByteArrayInputStream("Blah blah stdin content".toByteArray())

        try {
            val handle = buildTestHandle {
                args("logStdin")
                standardOutput = processStdout
                standardInput = stdinForProcess
            }

            handle.start()

            handle.waitForResult()

            assertEquals(
                listOf("stdin: Blah blah stdin content", ""),
                processStdout.toString().lines(),
            )

        } finally {
            processStdout.close()
            stdinForProcess.close()
        }
    }

    @Test
    fun `when waiting for process returns quickly if process already completed`() {
        val handle = buildTestHandle()

        handle.start()

        val result1 = handle.waitForResult()
        val result2 = handle.waitForResult()

        assertEquals(0, result1?.exitValue)
        assertEquals(0, result2?.exitValue)

        assertDoesNotThrow { result1?.assertNormalExitValue() }
        assertDoesNotThrow { result2?.assertNormalExitValue() }
    }

    @Test
    fun `when process exits with failure - expect ExecResult fails`() {
        val handle = buildTestHandle {
            args("exitWith=123")
        }

        val result = handle.start().waitForResult()

        assertEquals(123, result?.exitValue)

        val exception = assertThrows<ExecException> { result?.assertNormalExitValue() }

        assertNotNull(exception.message) { message ->
            assertContains("finished with non-zero exit value 123", message)
        }
    }

    @Test
    fun `when process cannot be started - expect start fails`() {
        val handle = buildTestHandle {
            executable = "no_such_command"
        }

        val exception = handle.start().waitForFailure()

        assertNotNull(exception?.message) { message ->
            assertEquals("A problem occurred starting process 'command 'no_such_command''", message)
        }
    }

    @Test
    fun `when ExecAsyncHandle is aborted - expect result is unavailable`() {
        val handle = buildTestHandle {
            args("sleep=${15.seconds}")
        }

        handle.start()

        handle.abort()

        val result = handle.waitForResult()

        assertNull(result)

        assertDoesNotThrow("aborting an already-aborted process shouldn't do anything") {
            handle.abort()
        }
    }

    @Test
    fun `when process has completed - expect aborting does nothing`() {
        val handle = buildTestHandle()

        val result = handle.start().waitForResult()

        assertEquals(0, result?.exitValue)

        handle.abort()

        assertEquals(0, result?.exitValue)
    }

    @Test
    fun `when process has failed - expect aborting does nothing`() {
        val handle = buildTestHandle {
            args("exitWith=99")
        }

        val result = handle.start().waitForResult()

        assertEquals(99, result?.exitValue)

        val exception = assertThrows<ExecException> {
            result?.assertNormalExitValue()
        }
        assertNotNull(exception.message) { message ->
            assertContains("finished with non-zero exit value 99", message)
        }

        assertDoesNotThrow {
            handle.abort()
        }
    }

    companion object {
        /** The current FQN of the test class. Used to launch [main] as a Java application. */
        private val APP_FQN: String = ExecAsyncHandleTest::class.qualifiedName!!

        @JvmStatic
        fun main(args: Array<String>) {
            args.forEach { arg ->
                when {
                    arg.startsWith("exitWith") -> {
                        exitProcess(arg.substringAfter("=").toInt())
                    }
                    arg.startsWith("logToStdOut") -> {
                        println(arg.substringAfter("="))
                    }
                    arg.startsWith("logToStdErr") -> {
                        System.err.println(arg.substringAfter("="))
                    }
                    arg.startsWith("sleep") -> {
                        Thread.sleep(kotlin.time.Duration.parse(arg.substringAfter("=")).inWholeMilliseconds)
                    }
                    arg == "logStdin" -> {
                        val line = System.`in`.bufferedReader().readLine()
                        println("stdin: $line")
                    }
                }
            }
        }

        /**
         * Build a test handle for launching [main].
         *
         * Uses the current Java process and classpath.
         */
        private fun buildTestHandle(
            configure: ExecSpec.() -> Unit = {},
        ): ExecAsyncHandle {
            val project = ProjectBuilder.builder().build()
            val execOps = project.serviceOf<ExecOperations>()
            return execOps.execAsync("test") {
                it.args(APP_FQN)
                it.executable = currentJavaExecutable
                it.environment("CLASSPATH", currentClasspath)
                it.configure()
            }
        }

        /** The current Java executable. Used to launch [main]. */
        private val currentJavaExecutable: String by lazy {
            ProcessHandle.current().info().command().orElseThrow()
        }

        private val currentClasspath: String by lazy {
            listOfNotNull(
                System.getenv("CLASSPATH"),
                System.getProperty("java.class.path"),
            ).joinToString(FileSystems.getDefault().separator)
        }
    }
}
