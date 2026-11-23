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
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PlatformLibs
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.provisionedXcodeCompilerArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("caches")
@EnforcedHostTarget
class CacheBatchTest : AbstractNativeSimpleTest() {
    @Test
    fun cleanBatchSupportsIncrementalRebuilds() = doTest(backendThreads = 1)

    @Test
    fun parallelCleanBatchSupportsIncrementalRebuilds() = doTest(backendThreads = 4)

    private fun doTest(backendThreads: Int) {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val rebuiltCaches = buildDir.resolve("rebuilt.txt")
        val librarySource = buildDir.resolve("library.kt")
        val eagerSource = buildDir.resolve("eager.kt")
        val unusedSource = buildDir.resolve("unused.kt").apply { writeText("fun unused() = 0") }
        val library = buildDir.resolve("library.klib")
        val consumer = buildDir.resolve("consumer.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        fun compileLibrary(value: Int, includeUnused: Boolean = true) {
            librarySource.writeText("""
                class Value {
                    ${if (value == 2) "private val padding = listOf(1, 2, 3)" else ""}
                    val value = $value
                }
                inline fun inlineValue() = $value
                var sideEffect = 0
            """.trimIndent())
            eagerSource.writeText("""
                @file:OptIn(kotlin.ExperimentalStdlibApi::class)
                @kotlin.native.EagerInitialization
                private val initialized = run { sideEffect = $value }
            """.trimIndent())
            compileKlib(
                librarySource.path, eagerSource.path,
                unusedSource.path.takeIf { includeUnused },
                "-o", library.path,
            )
        }

        compileLibrary(1)
        // The libraries deliberately share a serialized source path; ownership must also include the library.
        val consumerSource = librarySource.apply {
            writeText("fun consume() = inlineValue() + Value().value + sideEffect")
        }
        compileKlib(consumerSource.path, "-l", library.path, "-o", consumer.path)
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(consume()) }") }
        compileKlib(mainSource.path, "-l", library.path, "-l", consumer.path, "-o", main.path)

        fun link(): String = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xdump-built-caches-to=${rebuiltCaches.path}", "-Xbackend-threads=$backendThreads",
            "-Xinclude=${main.path}", "-l", library.path, "-l", consumer.path, "-o", executable.path,
        )

        assertTrue(link().contains("CACHING BATCH:"))
        val initialArchives = rebuiltCaches.readLines().filter { it.isNotBlank() }.toSet()
        assertEquals(5, initialArchives.count { it.startsWith(cacheDir.path) })
        verifyOutput(executable, "3")

        assertFalse(link().contains("CACHING BATCH:"))
        assertTrue(rebuiltCaches.readText().isBlank())
        verifyOutput(executable, "3")

        compileLibrary(2)
        // The changed library, its dependent that used the changed inline function and the main library calling
        // into the dependent are all rebuilt in a single batched compilation, each one rebuilding only its dirty files.
        val incremental = link()
        assertEquals(setOf("library", "consumer", "main"), incremental.batchLibraries()) { incremental }
        val changedArchives = rebuiltCaches.readLines().filter { it.isNotBlank() }
        assertTrue(changedArchives.any { "consumer-per-file-cache" in it })
        verifyOutput(executable, "6")

        compileLibrary(2, includeUnused = false)
        assertFalse(link().contains("CACHING BATCH:"))
        assertEquals(1, initialArchives.count { !File(it).exists() })
        verifyOutput(executable, "6")

        link()
        assertTrue(rebuiltCaches.readText().isBlank())

        // The per-file caches are stored under a target-flavor subdirectory of the cache directory.
        fun deletePerFileCache(libraryName: String) {
            val libraryCaches = cacheDir.walkTopDown().filter { it.isDirectory && it.name == "$libraryName-per-file-cache" }.toList()
            assertEquals(1, libraryCaches.size)
            libraryCaches.single().deleteRecursively()
        }

        // The batch is not just for clean builds: all the per-file caches needing a full rebuild
        // are batched against the up-to-date caches of the rest.
        deletePerFileCache("consumer")
        deletePerFileCache("main")
        assertTrue(link().contains("CACHING BATCH:"))
        val partialBatchArchives = rebuiltCaches.readLines().filter { it.isNotBlank() }
        assertEquals(2, partialBatchArchives.size)
        assertTrue(partialBatchArchives.none { "library-per-file-cache" in it })
        verifyOutput(executable, "6")

        // The changed library is rebuilt in the very same batched compilation as its dependents needing a full
        // rebuild: the dependents are compiled there against the dependency's fresh IR, while the dependency
        // itself rebuilds only its dirty files.
        compileLibrary(3, includeUnused = false)
        deletePerFileCache("consumer")
        deletePerFileCache("main")
        val mixedBatch = link()
        assertEquals(setOf("library", "consumer", "main"), mixedBatch.batchLibraries()) { mixedBatch }
        verifyOutput(executable, "9")
    }

    @Test
    fun batchRebuildWithCachedIntermediateDependency() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val library = buildDir.resolve("library.klib")
        val consumer = buildDir.resolve("consumer.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        val librarySource = buildDir.resolve("library.kt").apply { writeText("fun value() = 42") }
        compileKlib(librarySource.path, "-o", library.path)
        val consumerSource = buildDir.resolve("consumer.kt").apply { writeText("fun consume() = value()") }
        compileKlib(consumerSource.path, "-l", library.path, "-o", consumer.path)
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(consume()) }") }
        compileKlib(mainSource.path, "-l", library.path, "-l", consumer.path, "-o", main.path)

        fun link(): String = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xinclude=${main.path}", "-l", library.path, "-l", consumer.path, "-o", executable.path,
        )

        assertTrue(link().contains("CACHING BATCH:"))
        verifyOutput(executable, "42")

        // Keep the intermediate consumer cache: its recorded dependencies refer to library,
        // which now needs rebuilding together with main.
        for (libraryName in listOf("library", "main")) {
            val libraryCaches = cacheDir.walkTopDown().filter { it.isDirectory && it.name == "$libraryName-per-file-cache" }.toList()
            assertEquals(1, libraryCaches.size)
            assertTrue(libraryCaches.single().deleteRecursively())
        }

        link()
        verifyOutput(executable, "42")
    }

    @Test
    fun batchFollowsExternalDependencyCaching() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val autoCacheDir = buildDir.resolve("auto-cache").apply { mkdirs() }
        // The libraries under this directory are cached automatically into [autoCacheDir] instead of incrementally.
        val externalDir = buildDir.resolve("external").apply { mkdirs() }
        val external = externalDir.resolve("external.klib")
        val library = buildDir.resolve("library.klib")
        val consumer = buildDir.resolve("consumer.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        fun compileExternal(value: Int) {
            val externalSource = buildDir.resolve("external.kt").apply { writeText("fun externalValue() = $value") }
            compileKlib(externalSource.path, "-o", external.path)
        }

        compileExternal(1)
        val librarySource = buildDir.resolve("library.kt").apply { writeText("fun value() = externalValue() + 1") }
        compileKlib(librarySource.path, "-l", external.path, "-o", library.path)
        val consumerSource = buildDir.resolve("consumer.kt").apply { writeText("fun consume() = value() + externalValue()") }
        compileKlib(consumerSource.path, "-l", external.path, "-l", library.path, "-o", consumer.path)
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(consume()) }") }
        compileKlib(mainSource.path, "-l", external.path, "-l", library.path, "-l", consumer.path, "-o", main.path)

        fun link(): String = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xauto-cache-from=${externalDir.path}", "-Xauto-cache-dir=${autoCacheDir.path}",
            "-Xinclude=${main.path}", "-l", external.path, "-l", library.path, "-l", consumer.path, "-o", executable.path,
        )

        fun String.cachesExternal() = lines().any { "CACHING " in it && it.trimEnd().endsWith(external.name) }

        // The external dependency has no cache yet: it is cached first, and its dependents are still batched afterwards.
        val output = link()
        assertTrue(output.cachesExternal())
        assertTrue(output.contains("CACHING BATCH:"))
        verifyOutput(executable, "3")

        // An updated external dependency gets a new auto cache, and all its dependents need a full rebuild: in a batch again.
        compileExternal(2)
        val updatedOutput = link()
        assertTrue(updatedOutput.cachesExternal())
        assertTrue(updatedOutput.contains("CACHING BATCH:"))
        verifyOutput(executable, "5")
    }

    @Test
    fun batchSkipsLibrariesWithTestRunner() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val library = buildDir.resolve("library.klib")
        val consumer = buildDir.resolve("consumer.klib")
        val tests = buildDir.resolve("tests.klib")
        val executable = buildDir.resolve("tests.kexe")

        val librarySource = buildDir.resolve("library.kt").apply { writeText("fun value() = 1") }
        compileKlib(librarySource.path, "-o", library.path)
        val consumerSource = buildDir.resolve("consumer.kt").apply { writeText("fun consume() = value() + 1") }
        compileKlib(consumerSource.path, "-l", library.path, "-o", consumer.path)
        val testsSource = buildDir.resolve("tests.kt").apply {
            writeText("""
                import kotlin.test.*

                class ConsumerTest {
                    @Test
                    fun testConsume() = assertEquals(2, consume())
                }
            """.trimIndent())
        }
        compileKlib(testsSource.path, "-l", library.path, "-l", consumer.path, "-o", tests.path)

        val output = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}", "-generate-test-runner",
            "-Xinclude=${tests.path}", "-l", library.path, "-l", consumer.path, "-o", executable.path,
        )
        // The test runner is generated in a dedicated compilation of the included library; the rest is still batched.
        val batch = output.lines().singleOrNull { "CACHING BATCH:" in it }
            ?.substringAfter("CACHING BATCH:")?.split(",")?.map { it.trim() }?.toSet()
        assertEquals(setOf("library", "consumer"), batch) { output }
        verifyOutput(executable) { output ->
            assertTrue(output.contains("ConsumerTest.testConsume")) { output }
            assertTrue(output.contains("PASSED")) { output }
        }
    }

    @Test
    fun batchSplitsLibrariesWithConflictingSignatures() {
        assumeTrue(HostManager.host.family == Family.OSX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val core = buildDir.resolve("core.klib")
        val lib1 = buildDir.resolve("lib1.klib")
        val lib2 = buildDir.resolve("lib2.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        val coreSource = buildDir.resolve("core.kt").apply { writeText("package app\n\nclass Data(val n: Int)") }
        compileKlib(coreSource.path, "-o", core.path)
        // KT-75928: sibling libraries declaring the same internal extension get the same top-level signature.
        // The IR linker cannot deserialize both exhaustively in one compilation, so they must not share a batch.
        val lib1Source = buildDir.resolve("lib1.kt").apply {
            writeText("package app\n\ninternal fun Data.tagged() = n + 1\n\nfun aValue(d: Data) = d.tagged()")
        }
        compileKlib(lib1Source.path, "-l", core.path, "-o", lib1.path)
        val lib2Source = buildDir.resolve("lib2.kt").apply {
            writeText("package app\n\ninternal fun Data.tagged() = n + 1\n\nfun bValue(d: Data) = d.tagged()")
        }
        compileKlib(lib2Source.path, "-l", core.path, "-o", lib2.path)
        val mainSource = buildDir.resolve("main.kt").apply {
            writeText("import app.*\n\nfun main() { println(aValue(Data(1)) + bValue(Data(2))) }")
        }
        compileKlib(mainSource.path, "-l", core.path, "-l", lib1.path, "-l", lib2.path, "-o", main.path)

        val output = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xinclude=${main.path}", "-l", core.path, "-l", lib1.path, "-l", lib2.path, "-o", executable.path,
        )
        val batches = output.lines().filter { "CACHING BATCH:" in it }
            .map { line -> line.substringAfter("CACHING BATCH:").split(",").map { it.trim() }.toSet() }
        assertTrue(Regex("SPLITTING BATCH: lib[12] and lib[12] share a top-level signature").containsMatchIn(output)) { output }
        // main depends on both lib1 and lib2, so its compilation loads the clashing pair whatever the batch is;
        // it must not be batched with either of them being deserialized exhaustively.
        assertTrue(Regex("SPLITTING BATCH: main and lib[12] share a top-level signature.*\\(via lib[12] and lib[12]\\)").containsMatchIn(output)) { output }
        assertEquals(listOf(setOf("core", "lib1"), setOf("lib2"), setOf("main")), batches) { output }
        verifyOutput(executable, "5")
    }

    @Test
    fun batchRebuildWithConflictingCachedInlineDependencies() = doConflictingCachedInlineDependenciesTest(overlappingClosures = false)

    @Test
    fun batchRebuildWithConflictingSignaturesInOverlappingClosures() = doConflictingCachedInlineDependenciesTest(overlappingClosures = true)

    private fun doConflictingCachedInlineDependenciesTest(overlappingClosures: Boolean) {
        assumeTrue(HostManager.host.family == Family.OSX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val lib1 = buildDir.resolve("lib1.klib")
        val lib2 = buildDir.resolve("lib2.klib")
        val consumer1 = buildDir.resolve("consumer1.klib")
        val consumer2 = buildDir.resolve("consumer2.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        val lib1Source = buildDir.resolve("lib1.kt").apply {
            writeText("""
                package app
                @PublishedApi internal inline fun tagged() = 10
                inline fun left() = tagged()
            """.trimIndent())
        }
        compileKlib(lib1Source.path, "-o", lib1.path)
        val lib2Source = buildDir.resolve("lib2.kt").apply {
            writeText("""
                package app
                @PublishedApi internal inline fun tagged() = 20
                inline fun right() = tagged()
                fun marker() = 1
            """.trimIndent())
        }
        compileKlib(lib2Source.path, "-o", lib2.path)
        // Referencing marker() puts both conflicting libraries in consumer1's dependency closure, but only
        // lib1's tagged() body is needed to compile consumer1 on its own. Consumer2 needs lib2's body instead.
        val consumer1Source = buildDir.resolve("consumer1.kt").apply {
            writeText("package app\nfun consumeLeft() = left()${if (overlappingClosures) " + marker() - 1" else ""}")
        }
        compileKlib(
            consumer1Source.path, "-l", lib1.path,
            "-l".takeIf { overlappingClosures }, lib2.path.takeIf { overlappingClosures },
            "-o", consumer1.path,
        )
        val consumer2Source = buildDir.resolve("consumer2.kt").apply { writeText("package app\nfun consumeRight() = right()") }
        compileKlib(consumer2Source.path, "-l", lib2.path, "-o", consumer2.path)
        val mainSource = buildDir.resolve("main.kt").apply {
            writeText("import app.*\nfun main() { println(consumeLeft()); println(consumeRight()) }")
        }
        compileKlib(mainSource.path, "-l", lib1.path, "-l", lib2.path, "-l", consumer1.path, "-l", consumer2.path, "-o", main.path)

        fun link() = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xinclude=${main.path}", "-l", lib1.path, "-l", lib2.path, "-l", consumer1.path, "-l", consumer2.path,
            "-o", executable.path,
        )

        link()
        verifyOutput(executable, "10\n20")

        // Keep both conflicting libraries cached. The consumers have distinct declarations, but rebuilding them
        // together must still keep the two tagged() bodies separate when deserializing their cached inline dependencies.
        for (libraryName in listOf("consumer1", "consumer2")) {
            val libraryCaches = cacheDir.walkTopDown().filter { it.isDirectory && it.name == "$libraryName-per-file-cache" }.toList()
            assertEquals(1, libraryCaches.size)
            assertTrue(libraryCaches.single().deleteRecursively())
        }

        val output = link()
        verifyOutput(executable, "10\n20")
        if (overlappingClosures) {
            // consumer1 alone loads both lib1 and lib2: the clash is pre-existing, but no other library may join its batch.
            assertTrue(Regex("SEALING BATCH: consumer1 loads lib[12] and lib[12] sharing a top-level signature").containsMatchIn(output)) { output }
        } else {
            assertTrue(Regex("SPLITTING BATCH: consumer[12] and consumer[12] share a top-level signature.*\\(via lib[12] and lib[12]\\)").containsMatchIn(output)) { output }
        }
        // Both consumers end up in groups of their own, so they are cached separately rather than as a batch.
        assertFalse("CACHING BATCH:" in output) { output }
        assertTrue("CACHING ${consumer1.path}" in output && "CACHING ${consumer2.path}" in output) { output }
    }

    @Test
    fun cleanBatchProducesSameCachesAsSeparateBuilds() = doSameCachesTest(backendThreads = 1)

    @Test
    fun parallelCleanBatchProducesSameCachesAsSeparateBuilds() = doSameCachesTest(backendThreads = 4)

    private fun doSameCachesTest(backendThreads: Int) {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val batchedCacheDir = buildDir.resolve("batched-cache")
        // The libraries deliberately share a serialized source path; ownership must also include the library.
        val source = buildDir.resolve("library.kt")
        val eagerSource = buildDir.resolve("eager.kt")
        val library = buildDir.resolve("library.klib")
        val consumer = buildDir.resolve("consumer.klib")

        source.writeText("""
            class Value {
                private val padding = listOf(1, 2, 3)
                val value = 1
            }
            inline fun inlineValue() = 2
            var sideEffect = 0
            fun libraryMain() = println(inlineValue())
        """.trimIndent())
        eagerSource.writeText("""
            @file:OptIn(kotlin.ExperimentalStdlibApi::class)
            @kotlin.native.EagerInitialization
            private val initialized = run { sideEffect = 3 }
        """.trimIndent())
        compileKlib(source.path, eagerSource.path, "-o", library.path)
        source.writeText("fun main() = println(inlineValue() + Value().value + sideEffect)")
        compileKlib(source.path, "-l", library.path, "-o", consumer.path)

        fun link(vararg args: String): String = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xbackend-threads=$backendThreads", "-o", buildDir.resolve("main.kexe").path, *args,
        )

        // Both libraries are clean, so their caches are built in a single batch.
        assertTrue(link("-Xinclude=${consumer.path}", "-l", library.path).contains("CACHING BATCH:"))
        assertTrue(cacheDir.renameTo(batchedCacheDir))
        cacheDir.mkdirs()

        // Only one clean library per link, so each cache is built separately (into the same location,
        // as the cache directory path may be embedded into the caches).
        assertFalse(link("-Xinclude=${library.path}", "-e", "libraryMain").contains("CACHING BATCH:"))
        assertFalse(link("-Xinclude=${consumer.path}", "-l", library.path).contains("CACHING BATCH:"))

        fun File.contents(): Map<String, File> =
            walkTopDown().filter { it.isFile }.associateBy { it.relativeTo(this).path }

        val batchedFiles = batchedCacheDir.contents()
        val separateFiles = cacheDir.contents()
        assertEquals(separateFiles.keys.sorted(), batchedFiles.keys.sorted())
        assertTrue(batchedFiles.keys.any { it.endsWith(".a") })
        val differentFiles = batchedFiles.filter {
            !it.value.readBytes().contentEquals(separateFiles.getValue(it.key).readBytes())
        }
        assertTrue(differentFiles.isEmpty()) { "Batched caches differ from separately built ones: ${differentFiles.keys.sorted()}" }
    }

    @Test
    fun batchedPartialRebuildMatchesUnbatched() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val batchedCacheDir = buildDir.resolve("batched-cache").apply { mkdirs() }
        val unbatchedCacheDir = buildDir.resolve("unbatched-cache").apply { mkdirs() }
        val rebuiltCaches = buildDir.resolve("rebuilt.txt")
        val coreSource = buildDir.resolve("core.kt")
        val untouchedSource = buildDir.resolve("untouched.kt").apply { writeText("fun untouched(): Int = 100") }
        val midSource = buildDir.resolve("mid.kt").apply { writeText("fun mid(x: Int): Int = core(x) * 2") }
        val topSource = buildDir.resolve("top.kt").apply { writeText("fun top(x: Int): Int = mid(x) + core(x)") }
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(top(20)) }") }
        val core = buildDir.resolve("core.klib")
        val mid = buildDir.resolve("mid.klib")
        val top = buildDir.resolve("top.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        fun compileCore(delta: Int) {
            coreSource.writeText("inline fun core(x: Int): Int = x + $delta")
            compileKlib(coreSource.path, untouchedSource.path, "-o", core.path)
        }

        compileCore(1)
        compileKlib(midSource.path, "-l", core.path, "-o", mid.path)
        compileKlib(topSource.path, "-l", core.path, "-l", mid.path, "-o", top.path)
        compileKlib(mainSource.path, "-l", core.path, "-l", mid.path, "-l", top.path, "-o", main.path)

        fun link(cacheDir: File, vararg extraArgs: String): String = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xdump-built-caches-to=${rebuiltCaches.path}",
            "-Xinclude=${main.path}", "-l", core.path, "-l", mid.path, "-l", top.path,
            "-o", executable.path, *extraArgs,
        )

        val batchedClean = link(batchedCacheDir)
        assertEquals(setOf("core", "mid", "top", "main"), batchedClean.batchLibraries()) { batchedClean }
        verifyOutput(executable, "63")

        val unbatchedClean = link(unbatchedCacheDir, "-Xbinary=batchedCacheBuild=false")
        assertFalse("CACHING BATCH:" in unbatchedClean) { unbatchedClean }
        verifyOutput(executable, "63")

        // The inline function body changes: core.kt, mid.kt, top.kt and main.kt are dirty, untouched.kt is not.
        compileCore(2)

        // All dirty libraries are rebuilt in one batch, each rebuilding only its dirty file and reusing untouched caches.
        val batched = link(batchedCacheDir)
        assertEquals(setOf("core", "mid", "top", "main"), batched.batchLibraries()) { batched }
        verifyOutput(executable, "66")
        val batchedRebuiltCaches = rebuiltCaches.readRebuiltCaches(batchedCacheDir)

        val unbatched = link(unbatchedCacheDir, "-Xbinary=batchedCacheBuild=false")
        assertFalse("CACHING BATCH:" in unbatched) { unbatched }
        verifyOutput(executable, "66")
        assertEquals(rebuiltCaches.readRebuiltCaches(unbatchedCacheDir), batchedRebuiltCaches) {
            "The rebuilt caches of the batched run differ from the ones of the unbatched run"
        }
        // Only the dirty files are rebuilt: one per library, leaving core's untouched.kt untouched.
        assertRebuiltCaches(batchedRebuiltCaches, "core" to 1, "mid" to 1, "top" to 1, "main" to 1)

        // Nothing has changed since the batched rebuild: every library is up to date.
        val noop = link(batchedCacheDir)
        assertFalse("CACHING" in noop) { noop }
        assertTrue(rebuiltCaches.readText().isBlank())
        verifyOutput(executable, "66")
    }

    @Test
    fun batchedPartialRebuildWithSameRelativePathInTwoLibraries() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val rebuiltCaches = buildDir.resolve("rebuilt.txt")
        // Both libraries contain a file with the same path, but with different declarations.
        val sharedSource = buildDir.resolve("shared.kt")
        val extraSource1 = buildDir.resolve("extra1.kt")
        val extraSource2 = buildDir.resolve("extra2.kt")
        val lib1 = buildDir.resolve("lib1.klib")
        val lib2 = buildDir.resolve("lib2.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        fun compileKlibs(shared1: Int, extra1: Int, shared2: Int) {
            sharedSource.writeText("fun shared1(): Int = $shared1")
            extraSource1.writeText("fun extra1(): Int = $extra1")
            compileKlib(sharedSource.path, extraSource1.path, "-o", lib1.path)
            sharedSource.writeText("fun shared2(): Int = $shared2")
            extraSource2.writeText("fun extra2(): Int = 0")
            compileKlib(sharedSource.path, extraSource2.path, "-o", lib2.path)
        }

        fun link(): String = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xdump-built-caches-to=${rebuiltCaches.path}",
            "-Xinclude=${main.path}", "-l", lib1.path, "-l", lib2.path, "-o", executable.path,
        )

        compileKlibs(shared1 = 1, extra1 = 10, shared2 = 2)
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(shared1() + extra1() + shared2()) }") }
        compileKlib(mainSource.path, "-l", lib1.path, "-l", lib2.path, "-o", main.path)
        val clean = link()
        assertEquals(setOf("lib1", "lib2", "main"), clean.batchLibraries()) { clean }
        verifyOutput(executable, "13")

        // Both libraries change in different files while sharing the shared.kt path: lib1's shared.kt stays cached
        // and lib1 rebuilds extra1.kt, while lib2 rebuilds shared.kt, all in one batch together with main.
        compileKlibs(shared1 = 1, extra1 = 100, shared2 = 200)
        val partial = link()
        assertEquals(setOf("lib1", "lib2", "main"), partial.batchLibraries()) { partial }
        assertRebuiltCaches(rebuiltCaches.readRebuiltCaches(), "lib1" to 1, "lib2" to 1, "main" to 1)
        verifyOutput(executable, "301")
    }

    @Test
    fun parallelBatchedPartialRebuildReusesCleanSiblingCache() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val rebuiltCaches = buildDir.resolve("rebuilt.txt")
        val coreSource = buildDir.resolve("core.kt")
        val cleanSource = buildDir.resolve("clean.kt").apply {
            writeText("""
                class Multiplier(val factor: Int) {
                    fun apply(x: Int): Int = x * factor
                }
                inline fun cleanBonus(): Int = 100
            """.trimIndent())
        }
        val core = buildDir.resolve("core.klib")
        val consumer = buildDir.resolve("consumer.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        fun compileCore(delta: Int) {
            coreSource.writeText("inline fun core(x: Int): Int = Multiplier(2).apply(x + $delta)")
            compileKlib(coreSource.path, cleanSource.path, "-o", core.path)
        }

        compileCore(1)
        val consumerSource = buildDir.resolve("consumer.kt").apply {
            writeText("fun consume(x: Int): Int = core(x) + Multiplier(3).apply(1) + cleanBonus()")
        }
        compileKlib(consumerSource.path, "-l", core.path, "-o", consumer.path)
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(consume(10)) }") }
        compileKlib(mainSource.path, "-l", core.path, "-l", consumer.path, "-o", main.path)

        fun link(): String = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xdump-built-caches-to=${rebuiltCaches.path}", "-Xbackend-threads=4",
            "-Xinclude=${main.path}", "-l", core.path, "-l", consumer.path, "-o", executable.path,
        )

        val clean = link()
        assertEquals(setOf("core", "consumer", "main"), clean.batchLibraries()) { clean }
        verifyOutput(executable, "125")

        // Only core.kt changes; consumer.kt (dirty in the same batch) still references Multiplier and cleanBonus()
        // from core's untouched clean.kt, whose existing per-file cache must be reused across backend threads.
        compileCore(2)
        val partial = link()
        assertEquals(setOf("core", "consumer", "main"), partial.batchLibraries()) { partial }
        assertRebuiltCaches(rebuiltCaches.readRebuiltCaches(), "core" to 1, "consumer" to 1, "main" to 1)
        verifyOutput(executable, "127")

        val noop = link()
        assertFalse("CACHING" in noop) { noop }
        assertTrue(rebuiltCaches.readText().isBlank())
    }

    @Test
    fun batchedPartialRebuildWithAddedFileInCachedLibrary() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val rebuiltCaches = buildDir.resolve("rebuilt.txt")
        val coreSource = buildDir.resolve("core.kt").apply { writeText("fun core(): Int = 10") }
        val addedSource = buildDir.resolve("added.kt").apply { writeText("inline fun added(): Int = 20") }
        val consumerSource = buildDir.resolve("consumer.kt")
        val core = buildDir.resolve("core.klib")
        val consumer = buildDir.resolve("consumer.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        fun compileKlibs(includeAdded: Boolean) {
            compileKlib(coreSource.path, addedSource.path.takeIf { includeAdded }, "-o", core.path)
            consumerSource.writeText(
                if (includeAdded) "fun consume(): Int = core() + added()" else "fun consume(): Int = core()"
            )
            compileKlib(consumerSource.path, "-l", core.path, "-o", consumer.path)
        }

        compileKlibs(includeAdded = false)
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(consume()) }") }
        compileKlib(mainSource.path, "-l", core.path, "-l", consumer.path, "-o", main.path)

        fun link(): String = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}",
            "-Xdump-built-caches-to=${rebuiltCaches.path}",
            "-Xinclude=${main.path}", "-l", core.path, "-l", consumer.path, "-o", executable.path,
        )

        val clean = link()
        assertEquals(setOf("core", "consumer", "main"), clean.batchLibraries()) { clean }
        verifyOutput(executable, "10")

        // Adding a new file makes core's existing per-file cache incomplete (core.kt is still cached, added.kt is not).
        // Both core's new file and consumer's updated file are rebuilt in one batch while reusing core.kt's cache.
        compileKlibs(includeAdded = true)
        val partial = link()
        assertEquals(setOf("core", "consumer", "main"), partial.batchLibraries()) { partial }
        assertRebuiltCaches(rebuiltCaches.readRebuiltCaches(), "core" to 1, "consumer" to 1, "main" to 1)
        verifyOutput(executable, "30")

        val noop = link()
        assertFalse("CACHING" in noop) { noop }
        assertTrue(rebuiltCaches.readText().isBlank())
    }

    private fun String.batchLibraries(): Set<String>? =
        lines().singleOrNull { "CACHING BATCH:" in it }?.substringAfter("CACHING BATCH:")?.split(",")?.map { it.trim() }?.toSet()

    private fun File.readRebuiltCaches(cacheDir: File? = null): List<String> =
        readLines().filter { it.isNotBlank() }.map { if (cacheDir != null) it.removePrefix(cacheDir.path) else it }.sorted()

    private fun assertRebuiltCaches(rebuiltCaches: List<String>, vararg expected: Pair<String, Int>) {
        assertEquals(expected.sumOf { it.second }, rebuiltCaches.size) { rebuiltCaches.joinToString("\n") }
        for ([libraryName, count] in expected) {
            assertEquals(count, rebuiltCaches.count { "$libraryName-per-file-cache" in it }) {
                "Unexpected rebuilt cache count for $libraryName:\n${rebuiltCaches.joinToString("\n")}"
            }
        }
    }

    private fun compileKlib(vararg args: String?) = compile(
        // Keep inline calls in the KLIB so their bodies are read during native cache compilation.
        "-p", "library", "-XXLanguage:-IrIntraModuleInlinerBeforeKlibSerialization", *args,
    )

    private fun compile(vararg args: String?): String {
        val compilerArgs = buildList {
            addAll(args.filterNotNull())
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
        return result.toolOutput
    }

    private fun verifyOutput(executable: File, expected: String) = verifyOutput(executable) { output ->
        assertEquals(expected, output.trim())
    }

    private fun verifyOutput(executable: File, verify: (String) -> Unit) {
        runExecutableAndVerify(
            testCase = TestCase(
                id = TestCaseId.Named("main"),
                kind = TestKind.STANDALONE_NO_TR,
                modules = emptySet(),
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                nominalPackageName = PackageName.EMPTY,
                checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                    outputMatcher = TestRunCheck.OutputMatcher { output ->
                        verify(output)
                        true
                    }
                ),
                extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
            ),
            executable = TestExecutable(
                executable = TestCompilationArtifact.Executable(executable),
                loggedCompilationToolCall = LoggedData.NoopCompilerCall(executable),
                testNames = listOf(TestName("main"))
            ),
        )
    }
}
