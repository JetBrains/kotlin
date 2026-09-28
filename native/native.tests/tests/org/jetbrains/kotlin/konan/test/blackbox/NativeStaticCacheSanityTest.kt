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
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.provisionedXcodeCompilerArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assumptions.assumeFalse
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
    fun eagerInitializationOrderIsCorrectWithoutCaches() {
        assumeExecutableIsRun()
        doTestEagerInitializationOrderIsCorrect(useCaches = false)
    }

    @Test
    fun eagerInitializationOrderIsCorrectWithCaches() {
        assumeExecutableIsRun()
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

    // A side effect of A's eager initializer is observed through a non-eager property of the same file,
    // before the eager property itself is touched.
    @Test
    fun eagerInitializationSameFileSideEffectIsVisibleWithoutCaches() {
        assumeExecutableIsRun()
        doTestEagerInitializationSameFileSideEffectIsVisible(useCaches = false)
    }

    @Test
    fun eagerInitializationSameFileSideEffectIsVisibleWithCaches() {
        assumeExecutableIsRun()
        assertThrows(AssertionError::class.java) {
            doTestEagerInitializationSameFileSideEffectIsVisible(useCaches = true)
        }
    }

    private fun doTestEagerInitializationSameFileSideEffectIsVisible(useCaches: Boolean) = doTestEagerInitializationSideEffectIsVisible(
        scenario = "eagerSideEffectSameFile",
        useCaches = useCaches,
        libraries = listOf(
            TestLibrary(
                name = "a",
                files = mapOf(
                    "a.kt" to """
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package a

                        var service: String? = null

                        @kotlin.native.EagerInitialization
                        val registration = run {
                            service = "OK"
                            42
                        }
                    """.trimIndent(),
                ),
            ),
            TestLibrary(
                name = "b",
                files = mapOf(
                    "b.kt" to """
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package b

                        @kotlin.native.EagerInitialization
                        val observed = run {
                            val result = a.service!!
                            a.registration
                            result
                        }
                    """.trimIndent(),
                ),
            ),
        ),
    )

    // Same as above, but the non-eager property observed by B lives in a different file of A
    // than the eager initializer that writes to it.
    @Test
    fun eagerInitializationCrossFileSideEffectIsVisibleWithoutCaches() {
        assumeExecutableIsRun()
        doTestEagerInitializationCrossFileSideEffectIsVisible(useCaches = false)
    }

    @Test
    fun eagerInitializationCrossFileSideEffectIsVisibleWithCaches() {
        assumeExecutableIsRun()
        assertThrows(AssertionError::class.java) {
            doTestEagerInitializationCrossFileSideEffectIsVisible(useCaches = true)
        }
    }

    private fun doTestEagerInitializationCrossFileSideEffectIsVisible(useCaches: Boolean) = doTestEagerInitializationSideEffectIsVisible(
        scenario = "eagerSideEffectCrossFile",
        useCaches = useCaches,
        libraries = listOf(
            TestLibrary(
                name = "a",
                files = mapOf(
                    "state.kt" to """
                        package a

                        var service: String? = null
                    """.trimIndent(),
                    "init.kt" to """
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package a

                        @kotlin.native.EagerInitialization
                        val registration = run {
                            service = "OK"
                            42
                        }
                    """.trimIndent(),
                ),
            ),
            TestLibrary(
                name = "b",
                files = mapOf(
                    "b.kt" to """
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package b

                        @kotlin.native.EagerInitialization
                        val observed = run { a.service!! }
                    """.trimIndent(),
                ),
            ),
        ),
    )

    // Same as above, but A's eager initializer writes into a third library C, which knows nothing about A,
    // and B observes the side effect through C.
    @Test
    fun eagerInitializationCrossLibrarySideEffectIsVisibleWithoutCaches() {
        assumeExecutableIsRun()
        doTestEagerInitializationCrossLibrarySideEffectIsVisible(useCaches = false)
    }

    @Test
    fun eagerInitializationCrossLibrarySideEffectIsVisibleWithCaches() {
        assumeExecutableIsRun()
        assertThrows(AssertionError::class.java) {
            doTestEagerInitializationCrossLibrarySideEffectIsVisible(useCaches = true)
        }
    }

    private fun doTestEagerInitializationCrossLibrarySideEffectIsVisible(useCaches: Boolean) = doTestEagerInitializationSideEffectIsVisible(
        scenario = "eagerSideEffectCrossLibrary",
        useCaches = useCaches,
        libraries = listOf(
            TestLibrary(
                name = "c",
                files = mapOf(
                    "c.kt" to """
                        package c

                        val registry = mutableMapOf<String, String>()
                    """.trimIndent(),
                ),
            ),
            TestLibrary(
                name = "a",
                files = mapOf(
                    "a.kt" to """
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package a

                        @kotlin.native.EagerInitialization
                        val registration = run {
                            c.registry["service"] = "OK"
                            42
                        }
                    """.trimIndent(),
                ),
            ),
            TestLibrary(
                name = "b",
                files = mapOf(
                    "b.kt" to """
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package b

                        @kotlin.native.EagerInitialization
                        val observed = run {
                            val result = c.registry.getValue("service")
                            a.registration // Real dependency on A, but only after the side effect is observed.
                            result
                        }
                    """.trimIndent(),
                ),
            ),
        ),
    )

    private class TestLibrary(val name: String, val files: Map<String, String>)

    // Compiles [libraries] in order, each depending on all the preceding ones, then an app whose `main`
    // prints `b.observed`, and verifies the output is "OK": `b.observed` must see the side effect performed
    // by `a.registration`'s eager initializer, because B depends on A.
    private fun doTestEagerInitializationSideEffectIsVisible(scenario: String, useCaches: Boolean, libraries: List<TestLibrary>) {
        val workDir = createDir(scenario + if (useCaches) "-cached" else "-plain")
        val cacheDir = workDir.resolve("cache").apply { mkdirs() }

        val klibFiles = mutableListOf<File>()
        for (library in libraries) {
            val sourcePaths = library.files.map { entry ->
                workDir.resolve(entry.key).apply { writeText(entry.value) }.absolutePath
            }
            val dependencyArgs = klibFiles.flatMap { listOf("-l", it.absolutePath) }
            val klibFile = workDir.resolve("${library.name}.klib")
            callNativeCompiler(
                *sourcePaths.toTypedArray(),
                "-p", "library",
                *dependencyArgs.toTypedArray(),
                "-o", klibFile.absolutePath,
            )
            if (useCaches) {
                callNativeCompiler(
                    "-g",
                    "-p", "static_cache",
                    *dependencyArgs.toTypedArray(),
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xcache-directory=${systemCacheDir.absolutePath}",
                    "-Xadd-cache=${klibFile.absolutePath}",
                )
            }
            klibFiles += klibFile
        }

        val appFile = workDir.resolve("app.kexe")
        callNativeCompiler(
            workDir.resolve("main.kt").apply {
                writeText(
                    """
                        fun main() {
                            println(b.observed)
                        }
                    """.trimIndent()
                )
            }.absolutePath,
            "-p", "program",
            "-Xcache-directory=${cacheDir.absolutePath}".takeIf { useCaches },
            "-Xcache-directory=${systemCacheDir.absolutePath}".takeIf { useCaches },
            *klibFiles.reversed().flatMap { listOf("-l", it.absolutePath) }.toTypedArray(),
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
                        assertEquals("OK", output.trim())
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

    private fun assumeExecutableIsRun() {
        // The eager initialization order is checked against the output of the executable, which is not run in the compile-only mode.
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value) { "The test requires running the executable" }
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
