/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompiler
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PlatformLibs
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.provisionedXcodeCompilerArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@EnforcedHostTarget
class ParallelObjectFilesTest : AbstractNativeSimpleTest() {
    @BeforeEach
    fun requireExecutableAppleHost() {
        assumeTrue(HostManager.host.family == Family.OSX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)
    }

    @Test
    fun oneThreadUsesOneObject() = compileAndCheck(1)

    @Test
    fun twoThreadsUseTwoObjects() = compileAndCheck(2)

    @Test
    fun fourThreadsUseFourObjects() = compileAndCheck(4)

    @Test
    fun eightThreadsUseEightObjects() = compileAndCheck(8)

    private fun compileAndCheck(threads: Int) {
        val source = buildDir.resolve("main.kt").apply {
            writeText("""
                private var state = 0
                private fun first(): Int = ++state
                private fun second(): Int = ++state
                class Value(val n: Int)
                fun main(args: Array<String>) {
                    val functions = if (args.isEmpty()) listOf(::first, ::second) else listOf(::second, ::first)
                    check(functions[0]() == 1)
                    check(functions[1]() == 2)
                    val values = List(16) { Value(it + 7 + args.size) }
                    check(values.sumOf { it.n } == 232)
                    try {
                        error("expected")
                    } catch (e: IllegalStateException) {
                        check(e.message == "expected")
                    }
                    println("OK")
                }
            """.trimIndent())
        }
        val executable = buildDir.resolve("main.kexe")
        val temporaryFiles = buildDir.resolve("codegen").apply { mkdirs() }
        val compilerArgs = buildList {
            addAll(listOf(source.path, "-target", HostManager.host.name, "-o", executable.path, "-opt"))
            add("-Xin-process-obj-emission")
            add("-Xbinary=objectFileThreads=$threads")
            add("-Xtemporary-files-dir=${temporaryFiles.path}")
            testRunSettings.get<PlatformLibs>().compilerFlag?.let { add(it) }
            addAll(provisionedXcodeCompilerArgs)
        }.toTypedArray()
        val result = callCompiler(
            compilerArgs = compilerArgs,
            kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        )
        assertEquals(ExitCode.OK, result.exitCode) {
            "Compilation failed: ${compilerArgs.joinToString(" ")}\n${result.toolOutput}"
        }
        val objects = temporaryFiles.listFiles().orEmpty().filter { it.extension == "o" && "-part" in it.name }
        assertEquals(if (threads == 1) 0 else threads, objects.size) { result.toolOutput }
        objects.forEach(::assertMachOObject)
        assertTrue(temporaryFiles.listFiles().orEmpty().none { it.extension == "bc" && "-part" in it.name })
        assertTrue(executable.isFile)
        verifyOutput(executable)
    }

    private fun assertMachOObject(file: File) {
        val bytes = ByteArray(16)
        java.io.DataInputStream(file.inputStream()).use { it.readFully(bytes) }
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0xfeedfacf.toInt(), header.getInt(0), "Expected a 64-bit Mach-O file: $file")
        assertEquals(1, header.getInt(12), "Expected MH_OBJECT: $file")
    }

    private fun verifyOutput(executable: File) {
        runExecutableAndVerify(
            testCase = TestCase(
                id = TestCaseId.Named("main"),
                kind = TestKind.STANDALONE_NO_TR,
                modules = emptySet(),
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                nominalPackageName = PackageName.EMPTY,
                checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                    outputMatcher = TestRunCheck.OutputMatcher { output ->
                        assertEquals("OK", output.trim())
                        true
                    },
                ),
                extras = TestCase.NoTestRunnerExtras(entryPoint = "main"),
            ),
            executable = TestExecutable(
                executable = TestCompilationArtifact.Executable(executable),
                loggedCompilationToolCall = LoggedData.NoopCompilerCall(executable),
                testNames = listOf(TestName("main")),
            ),
        )
    }
}
