/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompiler
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.provisionedXcodeCompilerArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.lang.AssertionError

@Tag("caches")
@EnforcedHostTarget
class NativeStaticCacheSanityTest : AbstractNativeSimpleTest() {

    @BeforeEach
    fun setUp() {
        assumeHostTargetIsCacheable()
    }

    // KT-89383
    @Test
    fun absoluteLibraryPathIsAcceptedWhenCachingLibrary() = doTestLibraryPathIsAcceptedWhenCachingLibrary(useRelativePath = false)

    // KT-89383
    @Test
    fun relativeLibraryPathIsAcceptedWhenCachingLibrary() = doTestLibraryPathIsAcceptedWhenCachingLibrary(useRelativePath = true)

    // KT-89383
    private fun doTestLibraryPathIsAcceptedWhenCachingLibrary(useRelativePath: Boolean) {
        val sourceFile = createFile("lib.kt") {
            """
                fun hello() = "hi"
            """.trimIndent()
        }

        val klibFile = createFile("lib.klib")
        val cacheDir = createDir("cache")

        // Compile a library.
        callNativeCompiler(
            sourceFile.absolutePath,
            "-p", "library",
            "-o", klibFile.absolutePath,
        )

        // Prepare a cache for user lib using a relative path.
        callNativeCompiler(
            "-g",
            "-p", "static_cache",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${if (useRelativePath) klibFile.relativeTo(userDir) else klibFile}"
        )
    }

    @Test
    fun eagerInitializationOrderIsCorrectWithoutCaches() = doTestEagerInitializationOrderIsCorrect(useCaches = false)

    @Test
    fun eagerInitializationOrderIsCorrectWithCaches() {
        assertThrows(AssertionError::class.java) {
            doTestEagerInitializationOrderIsCorrect(useCaches = true)
        }
    }

    private fun doTestEagerInitializationOrderIsCorrect(useCaches: Boolean) {
        val cacheDir = createDir("cache")

        // Compile a.kt as a separate module:
        val aKlibFile = createFile("a.klib")
        callNativeCompiler(
            createFile("a.kt") {
                """
                    @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                    package a

                    @kotlin.native.EagerInitialization
                    val value = run {
                        println("A initializer")
                        42
                    }
                """.trimIndent()
            }.absolutePath,
            "-p", "library",
            "-o", aKlibFile.absolutePath,
        )

        if (useCaches) {
            callNativeCompiler(
                "-g",
                "-p", "static_cache",
                "-Xcache-directory=${cacheDir.absolutePath}",
                "-Xcache-directory=${systemCacheDir.absolutePath}",
                "-Xadd-cache=${aKlibFile.absolutePath}",
            )
        }

        // Compile b.kt as a separate module:
        val bKlibFile = createFile("b.klib")
        callNativeCompiler(
            createFile("b.kt") {
                $$"""
                    @file:OptIn(kotlin.ExperimentalStdlibApi::class)
                    
                    package b
                    
                    @kotlin.native.EagerInitialization
                    val observed = run {
                        println("B initializer sees ${a.value}")
                        a.value
                    }
                """.trimIndent()
            }.absolutePath,
            "-p", "library",
            "-l", aKlibFile.absolutePath,
            "-o", bKlibFile.absolutePath,
        )

        if (useCaches) {
            callNativeCompiler(
                "-g",
                "-p", "static_cache",
                "-l", aKlibFile.absolutePath,
                "-Xcache-directory=${cacheDir.absolutePath}",
                "-Xcache-directory=${systemCacheDir.absolutePath}",
                "-Xadd-cache=${bKlibFile.absolutePath}",
            )
        }

        // Compile the app in one-stage mode:
        val appFile = createFile("app.kexe")
        callNativeCompiler(
            createFile("main.kt") {
                """
                    fun main() {
                        println(b.observed)
                    }
                """.trimIndent()
            }.absolutePath,
            "-p", "program",
            "-Xcache-directory=${cacheDir.absolutePath}".takeIf { useCaches },
            "-Xcache-directory=${systemCacheDir.absolutePath}".takeIf { useCaches },
            "-l", bKlibFile.absolutePath,
            "-l", aKlibFile.absolutePath,
            "-o", appFile.absolutePath,
        )

        runExecutableAndVerify(
            testCase = TestCase(
                id = TestCaseId.Named("main"),
                kind = TestKind.STANDALONE_NO_TR,
                modules = emptySet(),
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                nominalPackageName = PackageName.EMPTY,
                checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                    outputMatcher = TestRunCheck.OutputMatcher { output ->
                        assertEquals(
                            /* expected = */ """
                                A initializer
                                B initializer sees 42
                                42
                            """.trimIndent(),
                            /* actual = */ output.trim()
                        )
                        true
                    }
                ),
                extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
            ),
            executable = TestExecutable(
                executable = TestCompilationArtifact.Executable(appFile),
                loggedCompilationToolCall = LoggedData.NoopCompilerCall(appFile),
                testNames = listOf(TestName("main"))
            ),
        )
    }

    private val systemCacheDir: File by lazy {
        testRunSettings.get<KotlinNativeHome>().librariesDir.resolve("cache").listFiles().orEmpty().filter {
            it.isDirectory &&
                    it.name.startsWith(HostManager.host.name) &&
                    "-g" in it.name &&
                    "STATIC" in it.name
        }.minByOrNull { it.name.length } ?: fail { "System cache directory not found" }
    }

    private val userDir: File by lazy {
        File(System.getProperty("user.dir"))
    }

    private fun createFile(name: String, contents: (() -> String)? = null): File {
        val file = buildDir.resolve(name)
        if (contents != null) file.writeText(contents.invoke()) else file.createNewFile()
        return file
    }

    private fun createDir(name: String): File {
        return buildDir.resolve(name).also { it.mkdirs() }
    }

    private fun callNativeCompiler(vararg args: String?) {
        val compilerArgs = (args.filterNotNull() + provisionedXcodeCompilerArgs).toTypedArray()
        val result = callCompiler(
            compilerArgs = compilerArgs,
            kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        )

        assertEquals(ExitCode.OK, result.exitCode) {
            buildString {
                appendLine("Compilation failed with exit code = ${result.exitCode}")
                appendLine("Command-line arguments: ${compilerArgs.joinToString(" ")}")
                appendLine("Compiler output:")
                appendLine(result.toolOutput)
            }
        }
    }

    companion object {
        private fun assumeHostTargetIsCacheable() {
            assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        }
    }
}
