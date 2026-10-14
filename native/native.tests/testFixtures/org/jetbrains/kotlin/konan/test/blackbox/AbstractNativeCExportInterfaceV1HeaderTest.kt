/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.BinaryLibraryCompilation
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

    protected fun runTest(@TestDataFile testFile: String) {
        val path = File(ForTestCompileRuntime.transformTestDataPath(testFile).path)
        val goldenDataHeaderFile = HeaderTestModuleMarkers.resolveTargetSpecificGoldenDataFile(
            path, targets.testTarget.toString(), extension = "h"
        )

        val testName: String = path.nameWithoutExtension
        val lines = path.readLines()

        // A test data file with `// MODULE:` markers exercises the several-inter-dependent-included-libraries
        // scenario; without them it is a single-module test, compiled exactly as before.
        val compilation = if (HeaderTestModuleMarkers.hasModuleMarkers(lines)) {
            multiModuleBinaryLibrary(testName, lines)
        } else {
            singleModuleBinaryLibrary(path, testName)
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
    ))

    /** A single-module header test: the whole test data file becomes one library. */
    private fun singleModuleBinaryLibrary(path: File, moduleName: String): BinaryLibraryCompilation {
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
        module.files += TestFile.createCommitted(path, module)

        val testCase = TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = freeCompilerArgs(),
            nominalPackageName = PackageName(moduleName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.NoTestRunnerExtras()
        ).apply {
            initialize(null, null)
        }
        return testCompilationFactory.testCaseToBinaryLibrary(
            testCase,
            testRunSettings,
            kind = testRunSettings.get<BinaryLibraryKind>(),
        )
    }

    /**
     * A multi-module header test (`// MODULE: name(deps)` markers): each module is compiled to its own KLIB and then
     * `-Xinclude`d into one library, in declaration order. Declaring a dependent module *before* its dependency makes
     * the CLI include order contradict the topological order; C export must still emit the exported declarations in a
     * stable dependency-first order, so emitting them in CLI order instead would diverge from the shared golden.
     */
    private fun multiModuleBinaryLibrary(testName: String, lines: List<String>): BinaryLibraryCompilation {
        val sourcesDir = testRunSettings.get<Binaries>().testBinariesDir.resolve("$testName-sources")
        val orderedModules = HeaderTestModuleMarkers.createOrderedTestModules(
            HeaderTestModuleMarkers.parseHeaderTestModules(lines), sourcesDir
        )

        val testCase = HeaderTestModuleMarkers.createInitializedHeaderTestCase(
            testName,
            orderedModules.toSet(),
            freeCompilerArgs(),
            TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        )
        return testCompilationFactory.includedModulesToBinaryLibrary(
            orderedModules,
            testCase.freeCompilerArgs,
            testRunSettings,
            kind = testRunSettings.get<BinaryLibraryKind>(),
        )
    }
}
