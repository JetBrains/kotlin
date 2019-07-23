/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts.test

import junit.framework.Assert.*
import org.jetbrains.kotlin.scripting.compiler.plugin.runWithK2JVMCompiler
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainKtsIT {

    // TODO: partially copypasted from LauncherReplTest, consider extracting common parts to some (new) test util module
    private fun runWithKotlinc(
        scriptPath: String,
        expectedOutPatterns: List<String> = emptyList(),
        expectedExitCode: Int = 0,
        workDirectory: File? = null
    ) {
        val executableName = "kotlinc"
        // TODO:
        val executableFileName =
            if (System.getProperty("os.name").contains("windows", ignoreCase = true)) "$executableName.bat" else executableName
        val launcherFile = File("dist/kotlinc/bin/$executableFileName")
        assertTrue("Launcher script not found, run dist task: ${launcherFile.absolutePath}", launcherFile.exists())

        val mainKtsJar = File("dist/kotlinc/lib/kotlin-main-kts.jar")
        assertTrue("kotlin-main-kts.jar not found, run dist task: ${mainKtsJar.absolutePath}", mainKtsJar.exists())

        val processBuilder = ProcessBuilder(launcherFile.absolutePath, "-cp", mainKtsJar.absolutePath, "-script", scriptPath)
        if (workDirectory != null) {
            processBuilder.directory(workDirectory)
        }
        val process = processBuilder.start()

        data class ExceptionContainer(
            var value: Throwable? = null
        )

        fun InputStream.captureStream(): Triple<Thread, ExceptionContainer, ArrayList<String>> {
            val out = ArrayList<String>()
            val exceptionContainer = ExceptionContainer()
            val thread = thread {
                try {
                    reader().forEachLine {
                        out.add(it.trim())
                    }
                } catch (e: Throwable) {
                    exceptionContainer.value = e
                }
            }
            return Triple(thread, exceptionContainer, out)
        }

        val (stdoutThread, stdoutException, processOut) = process.inputStream.captureStream()
        val (stderrThread, stderrException, processErr) = process.errorStream.captureStream()

        process.waitFor(30000, TimeUnit.MILLISECONDS)

        try {
            if (process.isAlive) {
                process.destroyForcibly()
                fail("Process terminated forcibly")
            }
            stdoutThread.join(300)
            assertFalse("stdout thread not finished", stdoutThread.isAlive)
            assertNull(stdoutException.value)
            stderrThread.join(300)
            assertFalse("stderr thread not finished", stderrThread.isAlive)
            assertNull(stderrException.value)
            assertEquals(expectedOutPatterns.size, processOut.size)
            for ((expectedPattern, actualLine) in expectedOutPatterns.zip(processOut)) {
                assertTrue(
                    "line \"$actualLine\" do not match with expected pattern \"$expectedPattern\"",
                    Regex(expectedPattern).matches(actualLine)
                )
            }
            assertEquals(expectedExitCode, process.exitValue())

        } catch (e: Throwable) {
            println("OUT:\n${processOut.joinToString("\n")}")
            println("ERR:\n${processErr.joinToString("\n")}")
            throw e
        }
    }

    @Test
    fun testResolveJunit() {
        runWithKotlinc("$TEST_DATA_ROOT/hello-resolve-junit.main.kts", listOf("Hello, World!"))
    }

    @Test
    fun testImport() {
        runWithK2JVMCompiler("$TEST_DATA_ROOT/import-test.main.kts", listOf("Hi from common", "Hi from middle", "sharedVar == 5"))
    }
}
