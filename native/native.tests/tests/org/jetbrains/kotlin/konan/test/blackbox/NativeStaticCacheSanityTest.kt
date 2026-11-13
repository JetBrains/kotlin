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
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolCallResult
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.provisionedXcodeCompilerArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.lang.AssertionError
import java.util.Properties

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
    fun absoluteLibraryPathWithRelativeAddCacheIsAcceptedWhenCachingLibrary() =
        doTestMixedLibraryAndAddCachePaths(isLibraryPathRelative = false, isAddCachePathRelative = true)

    @Test
    fun relativeLibraryPathWithAbsoluteAddCacheIsAcceptedWhenCachingLibrary() =
        doTestMixedLibraryAndAddCachePaths(isLibraryPathRelative = true, isAddCachePathRelative = false)

    private fun doTestMixedLibraryAndAddCachePaths(isLibraryPathRelative: Boolean, isAddCachePathRelative: Boolean) {
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

        val libraryArg = if (isLibraryPathRelative) klibFile.relativeTo(userDir).path else klibFile.absolutePath
        val addCacheArg = if (isAddCachePathRelative) klibFile.relativeTo(userDir).path else klibFile.absolutePath

        // Prepare a cache for user lib using mixed relative and absolute paths.
        callNativeCompiler(
            "-g",
            "-p", "static_cache",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-library", libraryArg,
            "-Xadd-cache=$addCacheArg",
        )
    }

    @Test
    fun relativeLibraryWithAbsoluteCachedLibraryIsAccepted() =
        doTestMixedLibraryAndCachedLibraryPaths(isLibraryPathRelative = true, isCachedLibraryPathRelative = false)

    @Test
    fun absoluteLibraryWithRelativeCachedLibraryIsAccepted() =
        doTestMixedLibraryAndCachedLibraryPaths(isLibraryPathRelative = false, isCachedLibraryPathRelative = true)

    private fun doTestMixedLibraryAndCachedLibraryPaths(isLibraryPathRelative: Boolean, isCachedLibraryPathRelative: Boolean) {
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

        // Prepare a cache for the library.
        callNativeCompiler(
            "-g",
            "-p", "static_cache",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${klibFile.absolutePath}",
        )

        val cacheArtifact = cacheDir.listFiles().orEmpty().firstOrNull { it.name.startsWith("lib") }
            ?: fail { "Cache not found in $cacheDir" }

        val libraryArg = if (isLibraryPathRelative) klibFile.relativeTo(userDir).path else klibFile.absolutePath
        val cachedLibArg = if (isCachedLibraryPathRelative) klibFile.relativeTo(userDir).path else klibFile.absolutePath

        val appFile = createFile("app.kexe")
        callNativeCompiler(
            createFile("main.kt") {
                """
                    fun main() {
                        println(hello())
                    }
                """.trimIndent()
            }.absolutePath,
            "-p", "program",
            "-library", libraryArg,
            "-Xcached-library=$cachedLibArg,${cacheArtifact.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-o", appFile.absolutePath,
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
        doTestEagerInitializationOrderIsCorrect(useCaches = true)
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
        doTestEagerInitializationSameFileSideEffectIsVisible(useCaches = true)
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
        doTestEagerInitializationCrossFileSideEffectIsVisible(useCaches = true)
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
        doTestEagerInitializationCrossLibrarySideEffectIsVisible(useCaches = true)
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

    // The granular cache operations model: a metadata-only (header) cache of a library and the full cache
    // of the same library are separate artifacts. A dependent library can be compiled against the
    // dependency's metadata alone, with the dependency's native binary produced later; the final link then
    // combines the full caches of both.

    @Test
    fun dependentIsCompiledBeforeDependencyBinaryExists() {
        assumeExecutableIsRun()
        val dir = createDir("metadata-first")
        val aKlibFile = dir.resolve("a.klib")
        val bKlibFile = dir.resolve("b.klib")
        val cacheDirAMetadata = createDir("metadata-first/cache-a-metadata")
        val cacheDirAFull = createDir("metadata-first/cache-a-full")
        val cacheDirB = createDir("metadata-first/cache-b")

        // A: an eagerly initialized property, an open class subclassed by B, an exported inline function.
        callNativeCompiler(
            dir.resolve("a.kt").apply {
                writeText(
                    $$"""
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package a

                        @kotlin.native.EagerInitialization
                        val value = run {
                            println("A initializer")
                            42
                        }

                        open class Base {
                            open fun greet() = "hello"
                        }

                        inline fun decorate(text: String): String = "[$text]"
                    """.trimIndent()
                )
            }.absolutePath,
            "-p", "library",
            "-o", aKlibFile.absolutePath,
        )

        // B: depends on A, subclasses A.Base, calls A's inline function, observes A's eager initialization.
        callNativeCompiler(
            dir.resolve("b.kt").apply {
                writeText(
                    $$"""
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package b

                        @kotlin.native.EagerInitialization
                        val observed = run {
                            println("B initializer sees ${a.value}")
                            a.value
                        }

                        class Derived : a.Base() {
                            override fun greet() = a.decorate("derived")
                        }
                    """.trimIndent()
                )
            }.absolutePath,
            "-p", "library",
            "-l", aKlibFile.absolutePath,
            "-o", bKlibFile.absolutePath,
        )

        // Build the metadata-only (header) cache of A: no native code is produced.
        callNativeCompiler(
            "-g",
            "-p", "header_cache",
            "-Xcache-directory=${cacheDirAMetadata.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${aKlibFile.absolutePath}",
        )
        val aMetadataCache = cacheDirAMetadata.listFiles()!!.single { it.isDirectory }
        assertTrue(aMetadataCache.resolve("bin/${aMetadataCache.name}.header").isFile) {
            "Metadata-only cache of A was not produced in $cacheDirAMetadata"
        }

        // Compile the FULL cache of B while A has no native binary anywhere: B consumes A's klib
        // and the metadata from A's header cache only.
        callNativeCompiler(
            "-g",
            "-p", "static_cache",
            "-Xcache-directory=${cacheDirB.absolutePath}", // The cache of B is produced here.
            "-Xcache-directory=${cacheDirAMetadata.absolutePath}", // A is used via its metadata-only cache.
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-l", aKlibFile.absolutePath,
            "-Xadd-cache=${bKlibFile.absolutePath}",
        )
        val bCache = cacheDirB.listFiles()!!.single { it.isDirectory }
        assertTrue(bCache.resolve("bin/lib${bCache.name}.a").isFile) {
            "Full cache of B was not produced in $cacheDirB"
        }
        assertFalse(aMetadataCache.resolve("bin/lib${aMetadataCache.name}.a").exists()) {
            "A must not have a native binary while B is being compiled"
        }

        // Only now produce the native (full) cache of A.
        callNativeCompiler(
            "-g",
            "-p", "static_cache",
            "-Xcache-directory=${cacheDirAFull.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${aKlibFile.absolutePath}",
        )
        val aFullCache = cacheDirAFull.listFiles()!!.single { it.isDirectory }
        assertTrue(aFullCache.resolve("bin/lib${aFullCache.name}.a").isFile) {
            "Full cache of A was not produced in $cacheDirAFull"
        }

        // Link the final binary from the full caches of A and B and run it.
        val appFile = dir.resolve("app.kexe")
        callNativeCompiler(
            dir.resolve("main.kt").apply {
                writeText(
                    """
                        fun main() {
                            println(b.observed)
                            println(b.Derived().greet())
                        }
                    """.trimIndent()
                )
            }.absolutePath,
            "-p", "program",
            "-Xcache-directory=${cacheDirAFull.absolutePath}",
            "-Xcache-directory=${cacheDirB.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
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
                                [derived]
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

    @Test
    fun metadataOnlyCacheAgreesWithFullCache() {
        val dir = createDir("metadata-equivalence")
        val klibFile = dir.resolve("lib.klib")
        callNativeCompiler(
            dir.resolve("lib.kt").apply {
                writeText(
                    """
                        @file:OptIn(kotlin.ExperimentalStdlibApi::class)

                        package lib

                        @kotlin.native.EagerInitialization
                        val eager = run { 42 }

                        interface Api {
                            fun call(): Int
                        }

                        class Impl(val payload: Int) : Api {
                            override fun call() = payload
                        }

                        open class Shape(var x: Int, var y: Int) {
                            open fun area() = x * y
                        }

                        class Square(side: Int) : Shape(side, side) {
                            override fun area() = 4 * x
                        }

                        val trivial = "constant"

                        inline fun twice(value: Int): Int = value + value
                    """.trimIndent()
                )
            }.absolutePath,
            "-p", "library",
            "-o", klibFile.absolutePath,
        )

        val headerCacheDir = createDir("metadata-equivalence/cache-header")
        val fullCacheDir = createDir("metadata-equivalence/cache-full")
        for ([produce, cacheDir] in listOf("header_cache" to headerCacheDir, "static_cache" to fullCacheDir)) {
            callNativeCompiler(
                "-g",
                "-p", produce,
                "-Xcache-directory=${cacheDir.absolutePath}",
                "-Xcache-directory=${systemCacheDir.absolutePath}",
                "-Xadd-cache=${klibFile.absolutePath}",
            )
        }

        val headerCache = headerCacheDir.listFiles()!!.single { it.isDirectory }
        val fullCache = fullCacheDir.listFiles()!!.single { it.isDirectory }

        // The metadata a consumer compiles against must not depend on which tier produced the cache.
        // (The bitcode dependencies and the partial linkage issues record the code generator's usage
        // tracking and are not part of the consumer-facing metadata contract; the ObjC adapters are
        // empty either way, as the library binds no classes to ObjC names.)
        for (fileName in listOf("class_fields", "inline_bodies", "eager_init", "trivial_getters", "objc_adapters")) {
            assertArrayEquals(
                headerCache.resolve("ir/$fileName").readBytes(),
                fullCache.resolve("ir/$fileName").readBytes(),
            ) {
                "Mismatching '$fileName' between the metadata-only and the full cache of ${klibFile.path}"
            }
        }

        // Both caches must carry the same identity.
        val headerMetadata = headerCache.resolve("metadata.properties").loadProperties()
        val fullMetadata = fullCache.resolve("metadata.properties").loadProperties()
        for (key in listOf("hash", "host", "target", "compilerFingerprint")) {
            assertEquals(
                fullMetadata.getProperty(key),
                headerMetadata.getProperty(key),
            ) { "Mismatching '$key' between the metadata of the metadata-only and the full cache" }
        }
        assertEquals(
            fullMetadata.getProperty("newCompanionInitialization") ?: "false",
            headerMetadata.getProperty("newCompanionInitialization") ?: "false",
        ) { "Mismatching companion initialization order between the metadata-only and the full cache" }
    }

    @Test
    fun staleMetadataOnlyCacheIsRejectedNotReused() {
        assumeExecutableIsRun()
        val dir = createDir("stale-header")
        val klibFile = dir.resolve("lib.klib")
        val sourceFile = dir.resolve("lib.kt")

        fun compileLibrary(source: String) {
            sourceFile.writeText(source)
            callNativeCompiler(
                sourceFile.absolutePath,
                "-p", "library",
                "-o", klibFile.absolutePath,
            )
        }

        compileLibrary(
            """
                package lib

                class Box(val x: Int) {
                    fun sum() = x
                }
            """.trimIndent()
        )

        val cacheDir = createDir("stale-header/cache")
        callNativeCompiler(
            "-g",
            "-p", "header_cache",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${klibFile.absolutePath}",
        )

        // Change the library so that the metadata recorded in the cache (the class fields of Box) is stale.
        compileLibrary(
            """
                package lib

                class Box(val x: Int, val y: Int) {
                    fun sum() = x + y
                }
            """.trimIndent()
        )

        // The stale cache must be rejected with a warning rather than silently mixed with the new klib.
        val appFile = dir.resolve("app.kexe")
        val result = callNativeCompiler(
            dir.resolve("main.kt").apply {
                writeText(
                    """
                        fun main() {
                            println(lib.Box(2, 3).sum())
                        }
                    """.trimIndent()
                )
            }.absolutePath,
            "-p", "program",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-l", klibFile.absolutePath,
            "-o", appFile.absolutePath,
        )
        // Note: the CLI renderer decapitalizes the message, hence the case-insensitive check.
        assertTrue(result.toolOutput.contains("ignoring the cache for library", ignoreCase = true)) {
            "The stale cache must be reported as ignored, got:\n${result.toolOutput}"
        }

        runExecutableAndVerify(
            testCase = TestCase(
                id = TestCaseId.Named("main"),
                kind = TestKind.STANDALONE_NO_TR,
                modules = emptySet(),
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                nominalPackageName = PackageName.EMPTY,
                checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                    outputMatcher = TestRunCheck.OutputMatcher { output ->
                        assertEquals("5", output.trim())
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

    @Test
    fun metadataOnlyCacheIsUpgradedToFullCacheInPlace() {
        assumeExecutableIsRun()
        val dir = createDir("header-upgrade")
        val klibFile = dir.resolve("lib.klib")
        callNativeCompiler(
            dir.resolve("lib.kt").apply {
                writeText(
                    """
                        package lib

                        fun hello() = "hi"
                    """.trimIndent()
                )
            }.absolutePath,
            "-p", "library",
            "-o", klibFile.absolutePath,
        )

        val cacheDir = createDir("header-upgrade/cache")
        callNativeCompiler(
            "-g",
            "-p", "header_cache",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${klibFile.absolutePath}",
        )
        val cacheArtifactDir = cacheDir.listFiles()!!.single { it.isDirectory }
        val headerArtifact = cacheArtifactDir.resolve("bin/${cacheArtifactDir.name}.header")
        assertTrue(headerArtifact.isFile) { "Metadata-only cache was not produced in $cacheDir" }

        // Building the full cache into the same directory must upgrade the metadata-only cache in place
        // instead of silently doing nothing: the metadata is identical, the native binary is added.
        callNativeCompiler(
            "-g",
            "-p", "static_cache",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${klibFile.absolutePath}",
        )
        assertTrue(cacheArtifactDir.resolve("bin/lib${cacheArtifactDir.name}.a").isFile) {
            "The full cache was not produced in place of the metadata-only cache in $cacheDir"
        }
        assertFalse(headerArtifact.exists()) {
            "The metadata-only artifact must be replaced by the full cache"
        }

        val appFile = dir.resolve("app.kexe")
        callNativeCompiler(
            dir.resolve("main.kt").apply {
                writeText(
                    """
                        fun main() {
                            println(lib.hello())
                        }
                    """.trimIndent()
                )
            }.absolutePath,
            "-p", "program",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-l", klibFile.absolutePath,
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
                        assertEquals("hi", output.trim())
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

    private fun File.loadProperties(): Properties = Properties().apply { inputStream().use { load(it) } }

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

    private fun callNativeCompiler(vararg args: String?): CompilationToolCallResult {
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
        return result
    }

    companion object {
        private fun assumeHostTargetIsCacheable() {
            assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        }
    }
}
