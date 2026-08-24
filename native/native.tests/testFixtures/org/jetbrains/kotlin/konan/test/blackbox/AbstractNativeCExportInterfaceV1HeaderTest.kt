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
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.test.TestDataAssertions
import org.junit.jupiter.api.Tag
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

@Tag("cexport")
abstract class AbstractNativeCExportInterfaceV1HeaderTest() : AbstractNativeSimpleTest() {

    private val testCompilationFactory = TestCompilationFactory()

    /** Extra compiler args for subclasses, e.g. to exercise the IR-based discovery mode. */
    protected open val additionalCompilerArgs: List<String> get() = emptyList()

    protected fun runTest(@TestDataFile testFile: String) {
        val path = ForTestCompileRuntime.transformTestDataPath(testFile).toPath()
        val goldenDataHeaderFile = resolveTargetSpecificGoldenDataFile(path)

        val moduleName: String = path.nameWithoutExtension
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
        module.files += TestFile.createCommitted(path.toFile(), module)

        val testCase = TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(listOf(
                "-opt-in", "kotlin.experimental.ExperimentalNativeApi",
                "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in", "kotlin.native.internal.InternalForKotlinNative",
                "-opt-in", "kotlin.experimental.ExperimentalObjCRefinement",
                "-XXLanguage:+CompanionBlocks",
                "-XXLanguage:+CompanionExtensions",
                "-Xbinary=cInterfaceMode=v1",
            ) + additionalCompilerArgs),
            nominalPackageName = PackageName(moduleName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.NoTestRunnerExtras()
        ).apply {
            initialize(null, null)
        }
        val binaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            testCase,
            testRunSettings,
            kind = testRunSettings.get<BinaryLibraryKind>(),
        ).result.assertSuccess().resultingArtifact

        val headerFile = binaryLibrary.headerFile
            ?: error("No header file found for ${moduleName}")

        TestDataAssertions.assertEqualsToFile(goldenDataHeaderFile.toFile(), headerFile.readText())
    }

    private fun resolveTargetSpecificGoldenDataFile(pathToTestFile: Path): Path {
        val testName = pathToTestFile.nameWithoutExtension
        val parentDirectory = pathToTestFile.parent
        val targetSpecificFile = parentDirectory.resolve("$testName.${targets.testTarget}.h")
        val commonFile = parentDirectory.resolve("$testName.h")
        return if (targetSpecificFile.exists()) targetSpecificFile else commonFile
    }
}

/**
 * Same as [AbstractNativeCExportInterfaceV1HeaderTest], but with the model built from the IR instead
 * of K1 descriptors (`-Xbinary=cExportUseIrDiscovery=true`).
 * The IR mode must produce byte-identical output to the descriptor mode, so both share the same
 * golden `.h` files.
 */
@Tag("cexport")
abstract class AbstractNativeCExportInterfaceV1HeaderIrTest : AbstractNativeCExportInterfaceV1HeaderTest() {
    override val additionalCompilerArgs: List<String>
        get() = listOf("-Xbinary=cExportUseIrDiscovery=true")
}
