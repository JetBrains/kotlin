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
        assertFalse(link().contains("CACHING BATCH:"))
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

        // A dependency with pending per-file rebuilds must be rebuilt before its dependents,
        // so the dependents cannot be batched and fall back to the per-library builds.
        compileLibrary(3, includeUnused = false)
        deletePerFileCache("consumer")
        deletePerFileCache("main")
        assertFalse(link().contains("CACHING BATCH:"))
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
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
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
        assertEquals(setOf("core", "lib1", "lib2", "main"), batches.flatten().toSet()) { output }
        assertTrue(batches.none { "lib1" in it && "lib2" in it }) { output }
        assertTrue(batches.all { it.size >= 2 }) { output }
        verifyOutput(executable, "5")
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
