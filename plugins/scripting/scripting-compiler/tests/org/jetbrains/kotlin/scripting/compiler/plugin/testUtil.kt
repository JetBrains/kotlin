/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.updateWithCompilerOptions
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

const val SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY = "kotlin.script.test.base.compiler.arguments"

private fun getBaseCompilerArgumentsFromProperty(): List<String>? =
    System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.takeIf { it.isNotBlank() }?.split(' ')

// TODO: partially copypasted from LauncherReplTest, consider extracting common parts to some (new) test util module
fun runWithKotlinc(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    workDirectory: File? = null,
    classpath: List<File> = emptyList(),
    additionalEnvVars: Iterable<Pair<String, String>>? = null
) {
    runWithKotlinc(
        arrayOf("-script", scriptPath),
        expectedOutPatterns, expectedExitCode, workDirectory, classpath, additionalEnvVars
    )
}

fun runWithKotlinLauncherScript(
    launcherScriptName: String,
    compilerArgs: Iterable<String>,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    workDirectory: File? = null,
    classpath: List<File> = emptyList(),
    additionalEnvVars: Iterable<Pair<String, String>>? = null
) {
    val executableFileName =
        if (System.getProperty("os.name").contains("windows", ignoreCase = true)) "$launcherScriptName.bat" else launcherScriptName
    val launcherFile = File("dist/kotlinc/bin/$executableFileName")
    Assert.assertTrue("Launcher script not found, run dist task: ${launcherFile.absolutePath}", launcherFile.exists())

    val args = arrayListOf(launcherFile.absolutePath).apply {
        if (classpath.isNotEmpty()) {
            add("-cp")
            add(classpath.joinToString(File.pathSeparator))
        }
        getBaseCompilerArgumentsFromProperty()?.let { addAll(it) }
        addAll(compilerArgs)
    }

    runAndCheckResults(args, expectedOutPatterns, expectedExitCode, workDirectory, additionalEnvVars)
}

fun runWithKotlinc(
    compilerArgs: Array<String>,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    workDirectory: File? = null,
    classpath: List<File> = emptyList(),
    additionalEnvVars: Iterable<Pair<String, String>>? = null
) {
    runWithKotlinLauncherScript(
        "kotlinc", compilerArgs.asIterable(), expectedOutPatterns, expectedExitCode, workDirectory, classpath, additionalEnvVars
    )
}

fun runAndCheckResults(
    args: List<String>,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    workDirectory: File? = null,
    additionalEnvVars: Iterable<Pair<String, String>>? = null
) {
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
    expectedAllOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    expectedSomeErrPatterns: List<String>? = null
) {
    val argsWithBasefromProp = getBaseCompilerArgumentsFromProperty()?.let { (it + args).toTypedArray() } ?: args
    val (out, err, ret) = captureOutErrRet {
        CLITool.doMainNoExit(
            K2JVMCompiler(),
            argsWithBasefromProp
        )
    }
    try {
        val outLines = out.lines()
        Assert.assertEquals(
            "Expecting pattern:\n  ${expectedAllOutPatterns.joinToString("\n  ")}\nGot:\n  ${outLines.joinToString("\n  ")}",
            expectedAllOutPatterns.size, outLines.size
        )
        for ((expectedPattern, actualLine) in expectedAllOutPatterns.zip(outLines)) {
            Assert.assertTrue(
                "line \"$actualLine\" do not match with expected pattern \"$expectedPattern\"",
                Regex(expectedPattern).matches(actualLine)
            )
        }
        if (expectedSomeErrPatterns != null) {
            val errLines = err.lines()
            for (expectedPattern in expectedSomeErrPatterns) {
                val re = Regex(expectedPattern)
                Assert.assertTrue(
                    "Expected pattern \"$expectedPattern\" is not found in the stderr:\n${errLines.joinToString("\n")}",
                    errLines.any { re.find(it) != null }
                )
            }
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

class TestDisposable : Disposable {
    @Volatile
    var isDisposed = false
        private set

    override fun dispose() {
        isDisposed = true
    }
}

fun CompilerConfiguration.updateWithBaseCompilerArguments() {
    getBaseCompilerArgumentsFromProperty()?.let {
        updateWithCompilerOptions(it)
    }
}

