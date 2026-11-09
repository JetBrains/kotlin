/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.io.propertyList
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCInteropArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.Properties

// The golden files were generated with the old library resolution logic, see KT-84665.
@Tag("cinterop")
class CInteropKlibDependenciesTest : AbstractNativeSimpleTest() {

    @Test
    fun testOnlyStdlibInDepends() {
        val testDataDir = testDataDir("onlyStdlib")
        verifyManifestDepends(testDataDir.resolve("single.depends.txt")) { extraArgs ->
            cinterop(testDataDir, "single", *extraArgs)
        }
    }

    @Test
    fun testPlatformLibsInDepends() {
        Assumptions.assumeTrue(targets.testTarget.family == Family.OSX)

        val testDataDir = testDataDir("platformLibs")
        verifyManifestDepends(testDataDir.resolve("platformUser.depends.txt")) { extraArgs ->
            cinterop(testDataDir, "platformUser", *extraArgs, noDefaultLibs = false)
        }
    }

    // Only a should be in c's depends.
    // B (although linked) is not used anywhere in c, so cinterop tools drops it from depends.
    @Test
    fun testCliLibrariesInDepends() {
        val testDataDir = testDataDir("cliLibraries")
        verifyManifestDepends(testDataDir.resolve("c.depends.txt")) { extraArgs ->
            val aKlib = cinterop(testDataDir, "a", *extraArgs)
            val bKlib = cinterop(testDataDir, "b", *extraArgs)
            cinterop(
                testDataDir,
                "c",
                *extraArgs,
                "-l", aKlib.klibFile.canonicalPath,
                "-l", bKlib.klibFile.canonicalPath,
            )
        }
    }

    // KT-90839: hermetic builds pass the stdlib with -l and have no klib/ in konan.home.
    @Test
    fun testStdlibFromCliWithoutDistributionKlibs() {
        val librariesDir = testRunSettings.get<KotlinNativeHome>().librariesDir

        val stdlibPath = librariesDir.resolve("common/stdlib").assertExists().path
        val posixPath = librariesDir.resolve("platform/${targets.testTarget}/org.jetbrains.kotlin.native.platform.posix").assertExists().path

        fun doTest(vararg args: String) {
            verifyManifestDepends(testDataDir("onlyStdlib").resolve("single.depends.txt")) { extraArgs ->
                cinterop(
                    testDataDir("onlyStdlib"),
                    "single",
                    *extraArgs,
                    *args,
                )
            }
        }

        doTest()
        doTest("-l", stdlibPath)
        doTest("-l", stdlibPath, "-l", posixPath)

        with(assertThrows<CompilationToolException> { cinterop(testDataDir("onlyStdlib"), "single", "-nostdlib") }) {
            assertTrue("Failed to load libraries:" in reason)
            assertTrue("stdlib is not found:" in reason)
        }
        doTest("-nostdlib", "-l", stdlibPath)
        doTest("-nostdlib", "-l", stdlibPath, "-l", posixPath)
    }

    private fun testDataDir(scenario: String): File =
        getAbsoluteFile("native/native.tests/testData/CInterop/klibDependencies/$scenario")

    private fun verifyManifestDepends(
        expectedDependsFile: File,
        buildTargetKlib: (extraArgs: Array<String>) -> KLIB,
    ) {
        // Pin 2.4 because depends= is omitted starting from KLIB ABI 2.5 (KT-60874).
        val targetKlib = buildTargetKlib(arrayOf("-Xklib-abi-compatibility-level", "2.4"))
        assertEqualsToFile(expectedDependsFile, targetKlib.readDepends())
        targetKlib.klibFile.deleteRecursively()
    }

    private fun cinterop(
        testDataDir: File,
        name: String,
        vararg additionalArgs: String,
        noDefaultLibs: Boolean = true,
    ): KLIB = cinteropToLibrary(
        defFile = testDataDir.resolve("$name.def"),
        outputDir = buildDir,
        freeCompilerArgs = TestCInteropArgs("-Xmodule-name", name, "-nopack", *additionalArgs),
        noDefaultLibs = noDefaultLibs,
    ).assertSuccess().resultingArtifact

    private fun KLIB.readDepends(): String {
        val manifestFile = klibFile.resolve("default/manifest")
        val manifestProperties = manifestFile.bufferedReader().use { reader -> Properties().apply { load(reader) } }
        return manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).sorted().joinToString(" ")
    }

    private fun File.assertExists(): File {
        assertTrue(exists()) { "File or directory does not exist: $this" }
        return this
    }
}
