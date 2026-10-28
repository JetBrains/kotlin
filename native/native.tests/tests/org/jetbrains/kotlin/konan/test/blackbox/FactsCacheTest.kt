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
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PlatformLibs
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.provisionedXcodeCompilerArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("caches")
@EnforcedHostTarget
class FactsCacheTest : AbstractNativeSimpleTest() {
    @Test
    fun cleanBuildWritesManifest() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val bSource = buildDir.resolve("B.kt").apply { writeText("fun b(x: Int): Int = x + 1") }
        val aSource = buildDir.resolve("A.kt").apply { writeText("fun a(x: Int): Int = b(x) * 2") }
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(a(20)) }") }
        val library = buildDir.resolve("library.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        compile(bSource.path, aSource.path, "-p", "library", "-o", library.path)
        compile(mainSource.path, "-p", "library", "-l", library.path, "-o", main.path)

        val output = compile(
            "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}", "-Xfacts-cache-enabled",
            "-Xinclude=${main.path}", "-l", library.path, "-o", executable.path,
        )
        println(output)

        val libraryCache = findLibraryCache(cacheDir)
        val fileDirs = libraryCache.listFiles()!!.filter { it.isDirectory }
        assertEquals(2, fileDirs.size) { "Expected one directory per file in $libraryCache: $fileDirs" }
        fileDirs.forEach { assertTrue(it.resolve("bin").isDirectory) { "No bin/ in $it" } }
        assertTrue(libraryCache.resolve("cache_manifest").isFile) { "No cache_manifest in $libraryCache" }
        val manifest = libraryCache.resolve("cache_manifest.csv").readText()
        println(manifest)
        val bDefs = manifest.facts("defines", "/B.kt")
        val aUses = manifest.facts("uses", "/A.kt")
        assertTrue(bDefs.isNotEmpty()) { manifest }
        assertTrue(aUses.isNotEmpty()) { manifest }
        assertTrue(aUses.any { bDefs[it.key] == it.value }) { manifest }

        verifyOutput(executable, "42")
    }

    @Test
    fun secondBuildReadsManifest() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)

        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val bSource = buildDir.resolve("B.kt").apply { writeText("fun b(x: Int): Int = x + 1") }
        val aSource = buildDir.resolve("A.kt").apply { writeText("fun a(x: Int): Int = b(x) * 2") }
        val mainSource = buildDir.resolve("main.kt").apply { writeText("fun main() { println(a(20)) }") }
        val library = buildDir.resolve("library.klib")
        val main = buildDir.resolve("main.klib")
        val executable = buildDir.resolve("main.kexe")

        fun buildAll(): String {
            compile(bSource.path, aSource.path, "-p", "library", "-o", library.path)
            compile(mainSource.path, "-p", "library", "-l", library.path, "-o", main.path)
            return compile(
                "-g", "-verbose", "-Xenable-incremental-compilation", "-Xic-cache-dir=${cacheDir.path}", "-Xfacts-cache-enabled",
                "-Xinclude=${main.path}", "-l", library.path, "-o", executable.path,
            ).also { println(it) }
        }

        buildAll()
        verifyOutput(executable, "42")
        val firstManifest = findLibraryCache(cacheDir).resolve("cache_manifest.csv").readText()

        bSource.writeText("fun b(x: Int): Int = x + 2")
        val output = buildAll()
        verifyOutput(executable, "44")

        val dirtyLines = output.lines().filter { "DIRTY" in it }
        assertTrue(dirtyLines.any { "(changed)" in it && it.endsWith("/B.kt") }) { output }
        assertTrue(dirtyLines.none { it.endsWith("/A.kt") || it.endsWith("/main.kt") }) { output }

        val manifest = findLibraryCache(cacheDir).resolve("cache_manifest.csv").readText()
        println(manifest)
        assertEquals(firstManifest.facts("uses", "/A.kt"), manifest.facts("uses", "/A.kt")) { manifest }
        assertEquals(firstManifest.facts("defines", "/B.kt"), manifest.facts("defines", "/B.kt")) { manifest }
    }

    private fun findLibraryCache(cacheDir: File): File =
        cacheDir.walkTopDown().single { it.isDirectory && it.name == "library-per-file-cache" }

    private fun String.facts(relation: String, fileSuffix: String): Map<Pair<String, String>, String> =
        parseFactsCsv(this)
            .filter { it[0] == relation && it[1].endsWith(fileSuffix) }
            .associate { (it[2] to it[3]) to it[4] }

    private fun compile(vararg args: String): String {
        val compilerArgs = buildList {
            addAll(args)
            testRunSettings.get<PlatformLibs>().compilerFlag?.let { add(it) }
            addAll(provisionedXcodeCompilerArgs)
        }.toTypedArray()
        (val exitCode, val toolOutput) = callCompiler(
            compilerArgs = compilerArgs,
            kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        )
        assertEquals(ExitCode.OK, exitCode) {
            "Compilation failed: ${compilerArgs.joinToString(" ")}\n$toolOutput"
        }
        return toolOutput
    }

    private fun verifyOutput(executable: File, expected: String) {
        runExecutableAndVerify(
            testCase = TestCase(
                id = TestCaseId.Named("main"),
                kind = TestKind.STANDALONE_NO_TR,
                modules = emptySet(),
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                nominalPackageName = PackageName.EMPTY,
                checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                    outputMatcher = TestRunCheck.OutputMatcher { output ->
                        assertEquals(expected, output.trim())
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
