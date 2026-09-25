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

        val testName: String = path.nameWithoutExtension
        val lines = path.toFile().readLines()

        // A test data file with `// MODULE:` markers exercises the several-inter-dependent-included-libraries
        // scenario; without them it is a single-module test, compiled exactly as before.
        val compilation = if (lines.none { it.startsWith("// MODULE:") }) {
            singleModuleBinaryLibrary(path, testName)
        } else {
            multiModuleBinaryLibrary(testName, lines)
        }

        val binaryLibrary = compilation.result.assertSuccess().resultingArtifact
        val headerFile = binaryLibrary.headerFile
            ?: error("No header file found for $testName")

        TestDataAssertions.assertEqualsToFile(goldenDataHeaderFile.toFile(), headerFile.readText())
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

    /** A single-module header test: the whole test data file becomes one library. */
    private fun singleModuleBinaryLibrary(path: Path, moduleName: String): BinaryLibraryCompilation {
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
        createTestFiles(path, module).forEach { module.files += it }

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
        val parsedModules = parseModules(lines)

        val modulesByName = parsedModules.associate { parsed ->
            parsed.name to TestModule.Exclusive(parsed.name, parsed.dependencies, emptySet(), emptySet())
        }
        val orderedModules = parsedModules.map { modulesByName.getValue(it.name) }

        parsedModules.forEach { parsed ->
            val module = modulesByName.getValue(parsed.name)
            parsed.files.forEach { sourceFile ->
                module.files += TestFile.createUncommitted(sourcesDir.resolve(parsed.name).resolve(sourceFile.name), module, sourceFile.text)
            }
        }

        val testCase = TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = orderedModules.toSet(),
            freeCompilerArgs = freeCompilerArgs(),
            nominalPackageName = PackageName(testName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.NoTestRunnerExtras()
        ).apply {
            initialize(null, null)
        }
        return testCompilationFactory.includedModulesToBinaryLibrary(
            orderedModules,
            testCase.freeCompilerArgs,
            testRunSettings,
            kind = testRunSettings.get<BinaryLibraryKind>(),
        )
    }

    private class ParsedModule(val name: String, val dependencies: Set<String>) {
        val files = mutableListOf<SourceFile>()
    }

    private class SourceFile(val name: String, val text: String)

    /**
     * Parses `// MODULE: name(dep1, dep2)` blocks, each optionally split into `// FILE: <name>` source files (a module
     * with no `// FILE:` marker becomes a single `<module>.kt` file). Only regular dependencies are supported. Module
     * declaration order is preserved — it is the order in which the modules are `-Xinclude`d. Any text before the first
     * `// MODULE:` marker is treated as a preamble and dropped.
     */
    private fun parseModules(lines: List<String>): List<ParsedModule> {
        val moduleHeader = Regex("""// MODULE:\s*(\w+)\s*(?:\(([^)]*)\))?\s*""")
        val fileMarker = "// FILE: "

        val modules = mutableListOf<ParsedModule>()
        var currentFileName: String? = null
        val currentText = StringBuilder()

        fun flushFile() {
            val module = modules.lastOrNull() ?: run { currentText.clear(); return }
            val name = currentFileName ?: if (currentText.isBlank()) return else "${module.name}.kt"
            module.files += SourceFile(name, currentText.toString())
            currentText.clear()
            currentFileName = null
        }

        for (line in lines) {
            val moduleMatch = moduleHeader.matchEntire(line.trim())
            when {
                moduleMatch != null -> {
                    flushFile()
                    val name = moduleMatch.groupValues[1]
                    val deps = moduleMatch.groupValues[2].split(',').map(String::trim).filter(String::isNotEmpty).toSet()
                    modules += ParsedModule(name, deps)
                }
                line.startsWith(fileMarker) -> {
                    flushFile()
                    currentFileName = line.removePrefix(fileMarker).trim()
                }
                else -> currentText.appendLine(line)
            }
        }
        flushFile()
        return modules
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
