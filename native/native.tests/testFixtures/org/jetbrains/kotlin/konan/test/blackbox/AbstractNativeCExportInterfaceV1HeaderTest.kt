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
        createTestFiles(path, module).forEach { module.files += it }

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

    /**
     * Splits the test data file into individual source files by `// FILE: <name>` markers. This is needed to place
     * declarations in several packages (a package can be declared only once per file). A file without any marker is
     * used as a single source file as-is; text before the first marker (if any) is treated as a preamble and dropped.
     */
    private fun createTestFiles(path: Path, module: TestModule.Exclusive): List<TestFile<TestModule.Exclusive>> {
        val marker = "// FILE: "
        val lines = path.toFile().readLines()
        if (lines.none { it.startsWith(marker) }) {
            return listOf(TestFile.createCommitted(path.toFile(), module))
        }

        val sourcesDir = testRunSettings.get<Binaries>().testBinariesDir.resolve(path.nameWithoutExtension + "-sources")
        val files = mutableListOf<TestFile<TestModule.Exclusive>>()
        var currentName: String? = null
        val currentText = StringBuilder()
        fun flush() {
            val name = currentName ?: return
            files += TestFile.createUncommitted(sourcesDir.resolve(name), module, currentText.toString())
            currentText.clear()
        }
        for (line in lines) {
            if (line.startsWith(marker)) {
                flush()
                currentName = line.removePrefix(marker).trim()
            } else if (currentName != null) {
                currentText.appendLine(line)
            }
        }
        flush()
        return files
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
abstract class AbstractNativeCExportInterfaceV1HeaderIrTest : AbstractNativeCExportInterfaceV1HeaderTest() {
    override val additionalCompilerArgs: List<String>
        get() = listOf("-Xbinary=cExportUseIrDiscovery=true")
}
