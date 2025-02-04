/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.utils.processes

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.util.assertContains
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandle.ExecHandleState.Aborted
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandleBuilder.Companion.execHandleBuilder
import org.jetbrains.kotlin.util.assertDoesNotThrow
import org.jetbrains.kotlin.util.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.FileSystems
import kotlin.system.exitProcess
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

@OptIn(ExperimentalTime::class)
class ExecHandleTest {

    @Test
    fun `when ProcessHandle runs successfully expect ExecResult returns success`() {
        val builder = buildTestHandle()

        val result = builder.build().execute()

        assertEquals(0, result.exitValue)

        assertDoesNotThrow {
            result.assertNormalExitValue()
        }
    }

    @Test
    fun `when setting stdout and stderr in ExecHandleBuilder, expect process logs are forwarded`() {

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

        val builder = buildTestHandle {
            arguments += listOf(
                "logToStdOut=here's some stdout",
                "logToStdErr=and also some stderr",
                "logToStdOut=with another stdout line",
                "logToStdErr=plus some more stderr, lucky you!",
            )
            standardOutput = processStdout
            errorOutput = processStderr
        }

        builder.build().execute()

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
    fun `when setting stdin in ExecHandleBuilder, expect process receives input`() {
        val inputForProcess = PipedOutputStream()
        val processStdout = ByteArrayOutputStream()

        val builder = buildTestHandle {
            arguments += "logStdin"
            standardOutput = processStdout
            standardInput = PipedInputStream(inputForProcess)
        }

        val handle = builder.build().start()

        inputForProcess.bufferedWriter().use { writer ->
            writer.appendLine("Blah blah stdin content")
        }

        // Wait for process to finish reading and processing input
        inputForProcess.close()
        handle.waitForFinish()

        assertEquals(
            listOf("stdin: Blah blah stdin content", ""),
            processStdout.toString().lines(),
        )

        handle.abort()

        inputForProcess.close()
        processStdout.close()
    }

    @Test
    fun `when waiting for process returns quickly if process already completed`() {
        val builder = buildTestHandle()

        val handle = builder.build().start()

        val result1 = handle.waitForFinish()
        val result2 = handle.waitForFinish()

        assertEquals(0, result1.exitValue)
        assertEquals(0, result2.exitValue)

        assertDoesNotThrow { result1.assertNormalExitValue() }
        assertDoesNotThrow { result2.assertNormalExitValue() }
    }

    @Test
    fun `when process exits with failure - expect ExecResult fails`() {
        val builder = buildTestHandle {
            arguments += "exitWith=123"
        }

        val result = builder.build().start().waitForFinish()

        assertEquals(123, result.exitValue)

        val exception = assertThrows<ExecException> { result.assertNormalExitValue() }

        assertNotNull(exception.message) { message ->
            assertContains("finished with non-zero exit value 123", message)
        }
    }

    @Test
    fun `when process cannot be started - expect start fails`() {
        val builder = buildTestHandle {
            displayName = "custom-display-name"
            launchOpts {
                executable.set("no_such_command")
            }
        }

        val exception = assertThrows<ExecException> {
            builder.build().start()
        }

        assertNotNull(exception.message) { message ->
            assertEquals("A problem occurred starting process 'custom-display-name'.", message)
        }
    }

    @Test
    fun `when ExecHandle is aborted - expect process is aborted`() {
        val builder = buildTestHandle {
            arguments += "sleep=${1.minutes}"
        }

        val handle = builder.build().start()

        handle.abort()

        assertEquals(Aborted, handle.state)

        val result = handle.waitForFinish()

        assertNotEquals(0, result.exitValue)

        assertDoesNotThrow("aborting an already-aborted process shouldn't do anything") {
            handle.abort()
        }
    }

    @Test
    fun `when process has completed - expect aborting does nothing`() {
        val builder = buildTestHandle()

        val handle = builder.build()

        val result = handle.execute()

        assertEquals(0, result.exitValue)

        handle.abort()

        assertEquals(0, result.exitValue)
    }

    @Test
    fun `when process has failed - expect aborting does nothing`() {
        val builder = buildTestHandle {
            arguments += "exitWith=99"
        }

        val handle = builder.build()

        val result = handle.start().waitForFinish()

        assertEquals(99, result.exitValue)

        val exception = assertThrows<ExecException> { result.assertNormalExitValue() }
        assertNotNull(exception.message) { message ->
            assertContains("finished with non-zero exit value 99", message)
        }

        assertDoesNotThrow {
            handle.abort()
        }
    }

    companion object {
        /** The current FQN of the test class. Used to launch [main] as a Java application. */
        private val APP_FQN: String = ExecHandleTest::class.qualifiedName!!

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
            configure: ExecHandleBuilder.() -> Unit = {},
        ): ExecHandleBuilder {
            return ProjectBuilder
                .builder()
                .build()
                .objects
                .execHandleBuilder {
                    arguments += APP_FQN
                    launchOpts {
                        executable.set(currentJavaExecutable)
                        environment.put("CLASSPATH", currentClasspath)
                    }
                    configure()
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
