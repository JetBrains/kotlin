/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import junit.framework.Assert
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// TODO: partially copypasted from LauncherReplTest, consider extracting common parts to some (new) test util module
fun runWithKotlinc(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    workDirectory: File? = null,
    classpath: List<File> = emptyList(),
    additionalEnvVars: Iterable<Pair<String, String>>? = null
) {
    val executableName = "kotlinc"
    // TODO:
    val executableFileName =
        if (System.getProperty("os.name").contains("windows", ignoreCase = true)) "$executableName.bat" else executableName
    val launcherFile = File("dist/kotlinc/bin/$executableFileName")
    Assert.assertTrue("Launcher script not found, run dist task: ${launcherFile.absolutePath}", launcherFile.exists())

    val args = arrayListOf(launcherFile.absolutePath).apply {
        if (classpath.isNotEmpty()) {
            add("-cp")
            add(classpath.joinToString(File.pathSeparator))
        }
        add("-script")
        add(scriptPath)
    }
    val processBuilder = ProcessBuilder(args)
    if (workDirectory != null) {
        processBuilder.directory(workDirectory)
    }
    if (additionalEnvVars != null) {
        processBuilder.environment().putAll(additionalEnvVars)
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
            Assert.fail("Process terminated forcibly")
        }
        stdoutThread.join(300)
        Assert.assertFalse("stdout thread not finished", stdoutThread.isAlive)
        Assert.assertNull(stdoutException.value)
        stderrThread.join(300)
        Assert.assertFalse("stderr thread not finished", stderrThread.isAlive)
        Assert.assertNull(stderrException.value)
        Assert.assertEquals(expectedOutPatterns.size, processOut.size)
        for ((expectedPattern, actualLine) in expectedOutPatterns.zip(processOut)) {
            Assert.assertTrue(
                "line \"$actualLine\" do not match with expected pattern \"$expectedPattern\"",
                Regex(expectedPattern).matches(actualLine)
            )
        }
        Assert.assertEquals(expectedExitCode, process.exitValue())

    } catch (e: Throwable) {
        println("OUT:\n${processOut.joinToString("\n")}")
        println("ERR:\n${processErr.joinToString("\n")}")
        throw e
    }
}

fun runWithK2JVMCompiler(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    classpath: List<File> = emptyList()
) {
    val args = arrayListOf("-kotlin-home", "dist/kotlinc").apply {
        if (classpath.isNotEmpty()) {
            add("-cp")
            add(classpath.joinToString(File.pathSeparator))
        }
        add("-script")
        add(scriptPath)
    }
    runWithK2JVMCompiler(args.toTypedArray(), expectedOutPatterns, expectedExitCode)
}

fun runWithK2JVMCompiler(
    args: Array<String>,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0
) {
    val (out, err, ret) = captureOutErrRet {
        CLITool.doMainNoExit(
            K2JVMCompiler(),
            args
        )
    }
    try {
        val outLines = out.lines()
        Assert.assertEquals(
            "Expecting pattern:\n  ${expectedOutPatterns.joinToString("\n  ")}\nGot:\n  ${outLines.joinToString("\n  ")}",
            expectedOutPatterns.size, outLines.size
        )
        for ((expectedPattern, actualLine) in expectedOutPatterns.zip(outLines)) {
            Assert.assertTrue(
                "line \"$actualLine\" do not match with expected pattern \"$expectedPattern\"",
                Regex(expectedPattern).matches(actualLine)
            )
        }
        Assert.assertEquals(expectedExitCode, ret.code)
    } catch (e: Throwable) {
        println("OUT:\n$out")
        println("ERR:\n$err")
        throw e
    }
}


internal fun <T> captureOutErrRet(body: () -> T): Triple<String, String, T> {
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()
    val prevOut = System.out
    val prevErr = System.err
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(errStream))
    val ret = try {
        body()
    } finally {
        System.out.flush()
        System.err.flush()
        System.setOut(prevOut)
        System.setErr(prevErr)
    }
    return Triple(outStream.toString().trim(), errStream.toString().trim(), ret)
}

internal fun <R> withTempDir(keyName: String = "tmp", body: (File) -> R) {
    val tempDir = Files.createTempDirectory(keyName).toFile()
    try {
        body(tempDir)
    } finally {
        tempDir.deleteRecursively()
    }
}

internal fun <R> withDisposable(body: (Disposable) -> R) {
    val disposable = Disposer.newDisposable()
    try {
        body(disposable)
    } finally {
        Disposer.dispose(disposable)
    }
}


