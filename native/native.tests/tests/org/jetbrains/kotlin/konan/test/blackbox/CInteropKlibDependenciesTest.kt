/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.io.propertyList
import org.jetbrains.kotlin.io.readProperties
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.Properties

// The golden files were generated with the old library resolution logic, see KT-84665.
@Tag("cinterop")
class CInteropKlibDependenciesTest : AbstractNativeSimpleTest() {

    private val olderAbiArgs = arrayOf("-Xklib-abi-compatibility-level", "2.4")

    @Test
    fun testOnlyStdlibInDepends() {
        val testDataDir = testDataDir("onlyStdlib")
        verifyManifestDepends(testDataDir.resolve("single.depends.txt")) { outputDir, extraArgs ->
            cinterop(testDataDir, "single", *extraArgs, outputDir = outputDir)
        }
    }

    @Test
    fun testPlatformLibsInDepends() {
        Assumptions.assumeTrue(targets.testTarget.family == Family.OSX)

        val testDataDir = testDataDir("platformLibs")
        verifyManifestDepends(testDataDir.resolve("platformUser.depends.txt")) { outputDir, extraArgs ->
            cinterop(testDataDir, "platformUser", *extraArgs, noDefaultLibs = false, outputDir = outputDir)
        }
    }

    // Only a should be in c's depends.
    // B (although linked) is not used anywhere in c, so cinterop tools drops it from depends.
    @Test
    fun testCliLibrariesInDepends() {
        val testDataDir = testDataDir("cliLibraries")
        verifyManifestDepends(testDataDir.resolve("c.depends.txt")) { outputDir, extraArgs ->
            val aKlib = cinterop(testDataDir, "a", *extraArgs, outputDir = outputDir)
            val bKlib = cinterop(testDataDir, "b", *extraArgs, outputDir = outputDir)
            cinterop(
                testDataDir,
                "c",
                *extraArgs,
                "-l", aKlib.klibFile.canonicalPath,
                "-l", bKlib.klibFile.canonicalPath,
                outputDir = outputDir,
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
            verifyManifestDepends(testDataDir("onlyStdlib").resolve("single.depends.txt")) { outputDir, extraArgs ->
                cinterop(
                    testDataDir("onlyStdlib"),
                    "single",
                    *extraArgs,
                    *args,
                    outputDir = outputDir,
                )
            }
        }

        doTest()
        doTest("-l", stdlibPath)
        doTest("-l", stdlibPath, "-l", posixPath)

        with(assertThrows<CompilationToolException> { doTest("-nostdlib") }) {
            assertTrue("Failed to load libraries:" in reason)
            assertTrue("stdlib is not found:" in reason)
        }
        doTest("-nostdlib", "-l", stdlibPath)
        doTest("-nostdlib", "-l", stdlibPath, "-l", posixPath)
    }

    private fun testDataDir(scenario: String): File =
        getAbsoluteFile("native/native.tests/testData/CInterop/klibDependencies/$scenario")

    private fun verifyManifestDepends(
        expectedOlderAbiDependsFile: File,
        buildTargetKlib: (outputDir: File, extraArgs: Array<String>) -> KLIB,
    ) {
        val defaultAbiKlib = buildTargetKlib(buildDir.resolve("defaultAbi"), emptyArray())
        assertNull(defaultAbiKlib.readManifestProperties().getProperty(KLIB_PROPERTY_DEPENDS))
        defaultAbiKlib.klibFile.deleteRecursively()

        val olderAbiKlib = buildTargetKlib(buildDir.resolve("olderAbi"), olderAbiArgs)
        assertEqualsToFile(expectedOlderAbiDependsFile, olderAbiKlib.readDepends())
        olderAbiKlib.klibFile.deleteRecursively()
    }

    private fun cinterop(
        testDataDir: File,
        name: String,
        vararg additionalArgs: String,
        noDefaultLibs: Boolean = true,
        outputDir: File = buildDir,
    ): KLIB = cinteropToLibrary(
        defFile = testDataDir.resolve("$name.def"),
        outputDir = outputDir,
        freeCompilerArgs = TestCInteropArgs("-Xmodule-name", name, "-nopack", *additionalArgs),
        noDefaultLibs = noDefaultLibs,
    ).assertSuccess().resultingArtifact

    private fun KLIB.readManifestProperties(): Properties =
        klibFile.resolve("default/manifest").toPath().readProperties()

    private fun KLIB.readDepends(): String =
        readManifestProperties().propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).sorted().joinToString(" ")

    private fun File.assertExists(): File {
        assertTrue(exists()) { "File or directory does not exist: $this" }
        return this
    }
}
