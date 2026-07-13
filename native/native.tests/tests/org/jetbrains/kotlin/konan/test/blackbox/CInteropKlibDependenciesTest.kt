/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.io.propertyList
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCInteropArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties

// The golden files were generated with the old library resolution logic, see KT-84665.
@Tag("cinterop")
class CInteropKlibDependenciesTest : AbstractNativeSimpleTest() {

    @Test
    fun testOnlyStdlibInDepends() {
        val testDataDir = testDataDir("onlyStdlib")
        val singleKlib = cinterop(testDataDir, "single")
        assertEqualsToFile(testDataDir.resolve("single.depends.txt"), singleKlib.readDepends())
    }

    @Test
    fun testPlatformLibsInDepends() {
        Assumptions.assumeTrue(targets.testTarget.family == Family.OSX)

        val testDataDir = testDataDir("platformLibs")
        val platformUserKlib = cinterop(testDataDir, "platformUser", noDefaultLibs = false)
        assertEqualsToFile(testDataDir.resolve("platformUser.depends.txt"), platformUserKlib.readDepends())
    }

    // Only a should be in c's depends.
    // B (although linked) is not used anywhere in c, so cinterop tools drops it from depends.
    @Test
    fun testCliLibrariesInDepends() {
        val testDataDir = testDataDir("cliLibraries")
        val aKlib = cinterop(testDataDir, "a")
        val bKlib = cinterop(testDataDir, "b")
        val cKlib = cinterop(testDataDir, "c", "-l", aKlib.klibFile.canonicalPath, "-l", bKlib.klibFile.canonicalPath)

        assertEqualsToFile(testDataDir.resolve("c.depends.txt"), cKlib.readDepends())
    }

    private fun testDataDir(scenario: String): File =
        getAbsoluteFile("native/native.tests/testData/CInterop/klibDependencies/$scenario")

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
}
