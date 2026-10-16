/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.HeaderTestModuleMarkers
import org.jetbrains.kotlin.test.TestDataAssertions
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("cexport")
abstract class AbstractNativeCExportInterfaceV1HeaderTest() : AbstractNativeSimpleTest() {

    private val testCompilationFactory = TestCompilationFactory()

    /** Extra compiler args for subclasses, e.g. to exercise the IR-based discovery mode. */
    protected open val additionalCompilerArgs: List<String> get() = emptyList()

    protected fun runTest(@TestDataFile testFile: String) {
        val path = File(ForTestCompileRuntime.transformTestDataPath(testFile).path)
        val goldenDataHeaderFile = HeaderTestModuleMarkers.resolveTargetSpecificGoldenDataFile(
            path, targets.testTarget.toString(), extension = "h"
        )

        val testName = path.nameWithoutExtension
        val sourcesDir = testRunSettings.get<Binaries>().testBinariesDir.resolve("$testName-sources")

        val test = HeaderTestModuleMarkers.parse(path)
        val modules = HeaderTestModuleMarkers.toTestModules(test, testName, sourcesDir)
        val testCase = HeaderTestModuleMarkers.createInitializedHeaderTestCase(
            testName,
            modules.toSet(),
            freeCompilerArgs(),
            TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        )

        // A single-module fixture compiles straight to a library; a `// MODULE:` fixture compiles each module to a
        // KLIB and `-Xinclude`s them in declaration order -- the include order the ordering fixtures pin down.
        val compilation = when (test) {
            is HeaderTestModuleMarkers.HeaderTest.MultiModule -> testCompilationFactory.includedModulesToBinaryLibrary(
                modules,
                testCase.freeCompilerArgs,
                testRunSettings,
                kind = testRunSettings.get<BinaryLibraryKind>(),
            )
            else -> testCompilationFactory.testCaseToBinaryLibrary(
                testCase,
                testRunSettings,
                kind = testRunSettings.get<BinaryLibraryKind>(),
            )
        }

        val binaryLibrary = compilation.result.assertSuccess().resultingArtifact
        val headerFile = binaryLibrary.headerFile
            ?: error("No header file found for $testName")

        TestDataAssertions.assertEqualsToFile(goldenDataHeaderFile, headerFile.readText())
    }

    private fun freeCompilerArgs() = TestCompilerArgs(listOf(
        "-opt-in", "kotlin.experimental.ExperimentalNativeApi",
        "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi",
        "-opt-in", "kotlin.native.internal.InternalForKotlinNative",
        "-opt-in", "kotlin.experimental.ExperimentalObjCRefinement",
        "-XXLanguage:+CompanionBlocks",
        "-XXLanguage:+CompanionExtensions",
        "-Xbinary=cInterfaceMode=v1",
    ) + additionalCompilerArgs)
}

/**
 * Same as [AbstractNativeCExportInterfaceV1HeaderTest], but with the model built from the IR instead
 * of K1 descriptors (`-Xbinary=cExportUseIrDiscovery=true`).
 * The IR mode must produce byte-identical output to the descriptor mode, so both share the same
 * golden `.h` files.
 */
abstract class AbstractNativeCExportInterfaceV1HeaderIrTest : AbstractNativeCExportInterfaceV1HeaderTest() {
    override val additionalCompilerArgs: List<String>
        get() = listOf("-Xbinary=cExportUseIrDiscovery=true")
}
