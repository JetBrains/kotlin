/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestFile
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import java.io.File

/**
 * A minimal `// MODULE:` / `// FILE:` reader for the C and Objective-C export **header** tests.
 *
 * These tests do not run anything: each module becomes its own KLIB, the KLIBs are `-Xinclude`d into a single
 * library or framework, and only the generated header is compared against a golden file. That is why they do
 * not go through [org.jetbrains.kotlin.konan.test.blackbox.support.group.TestCaseGroupProvider] and the
 * [org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives] machinery, which builds a full runnable
 * test case (entry points, test runner, test dump) out of the test data.
 *
 * [parse] is the single place that understands the test-data format; it classifies a fixture into one of the
 * [HeaderTest] shapes. Which compilation entry point to use and which artifact to produce is left to the caller.
 */
object HeaderTestModuleMarkers {
    private val MODULE_HEADER = Regex("""// MODULE:\s*(\w+)\s*(?:\(([^)]*)\))?\s*""")
    private const val MODULE_MARKER = "// MODULE:"
    private const val FILE_MARKER = "// FILE: "

    class ParsedModule(val name: String, val dependencies: Set<String>) {
        val files: MutableList<ParsedFile> = mutableListOf()
    }

    class ParsedFile(val name: String, val text: String)

    /**
     * The shape of a header-test fixture, as classified by [parse]. The three shapes correspond one-to-one to the
     * three ways a fixture can be written; how each is compiled is the caller's decision.
     */
    sealed interface HeaderTest {
        /** No markers at all: the test data [file] is compiled as-is. */
        data class SingleFile(val file: File) : HeaderTest

        /** `// FILE:` markers only: one module whose sources are the split [files]. */
        data class SplitFiles(val files: List<ParsedFile>) : HeaderTest

        /** `// MODULE:` markers: several ordered [modules], each optionally split by `// FILE:`. */
        data class MultiModule(val modules: List<ParsedModule>) : HeaderTest
    }

    /**
     * Reads [file] and classifies it:
     *  - a `// MODULE:` marker anywhere -> [HeaderTest.MultiModule];
     *  - otherwise a `// FILE:` marker anywhere -> [HeaderTest.SplitFiles];
     *  - otherwise -> [HeaderTest.SingleFile].
     */
    fun parse(file: File): HeaderTest {
        val lines = file.readLines()
        return when {
            lines.any { it.startsWith(MODULE_MARKER) } -> HeaderTest.MultiModule(parseModules(lines))
            lines.any { it.startsWith(FILE_MARKER) } -> HeaderTest.SplitFiles(splitFiles(lines, "${file.nameWithoutExtension}.kt"))
            else -> HeaderTest.SingleFile(file)
        }
    }

    /**
     * Materializes a parsed [test] into [TestModule.Exclusive]s ready to be compiled, writing any generated sources
     * under [sourcesDir]. [moduleName] names the single module of the [HeaderTest.SingleFile] / [HeaderTest.SplitFiles]
     * shapes (it is ignored for [HeaderTest.MultiModule], whose modules are named by their `// MODULE:` markers).
     * Modules are returned **in declaration order**; same-named files in different modules stay distinct because
     * each module gets its own source directory.
     */
    fun toTestModules(test: HeaderTest, moduleName: String, sourcesDir: File): List<TestModule.Exclusive> = when (test) {
        is HeaderTest.SingleFile -> listOf(
            newModule(moduleName).apply { files += TestFile.createCommitted(test.file, this) }
        )
        is HeaderTest.SplitFiles -> listOf(
            newModule(moduleName).apply {
                test.files.forEach { files += TestFile.createUncommitted(sourcesDir.resolve(it.name), this, it.text) }
            }
        )
        is HeaderTest.MultiModule -> test.modules.map { parsed ->
            newModule(parsed.name, parsed.dependencies).apply {
                parsed.files.forEach { sourceFile ->
                    files += TestFile.createUncommitted(
                        sourcesDir.resolve(parsed.name).resolve(sourceFile.name),
                        this,
                        sourceFile.text,
                    )
                }
            }
        }
    }

    private fun newModule(name: String, dependencies: Set<String> = emptySet()) =
        TestModule.Exclusive(name, dependencies, emptySet(), emptySet())

    /**
     * Builds the [TestCase] that owns [modules], and initializes it.
     *
     * A header test never runs its test case -- it only needs the KLIBs and the generated header. The case still
     * has to exist and be initialized, though: [TestCase.initialize] is what resolves each module's dependency
     * *names* into module references and links each module back to its case. Skip it and
     * `TestModule.Exclusive.directRegularDependencies` stays uninitialized, so the first dependency lookup during
     * compilation fails with `UninitializedPropertyAccessException` rather than anything self-explanatory.
     */
    fun createInitializedHeaderTestCase(
        testName: String,
        modules: Set<TestModule.Exclusive>,
        freeCompilerArgs: TestCompilerArgs,
        checks: TestRunChecks,
    ): TestCase = TestCase(
        id = TestCaseId.Named(testName),
        kind = TestKind.STANDALONE_NO_TR,
        modules = modules,
        freeCompilerArgs = freeCompilerArgs,
        nominalPackageName = PackageName(testName),
        checks = checks,
        extras = TestCase.NoTestRunnerExtras(),
    ).apply { initialize(null, null) }

    /**
     * Resolves the golden file for [pathToTestFile], preferring a target-specific `<test>.<target>.<extension>`
     * over the shared `<test>.<extension>`.
     */
    fun resolveTargetSpecificGoldenDataFile(pathToTestFile: File, testTarget: String, extension: String): File {
        val testName = pathToTestFile.nameWithoutExtension
        val parentDirectory = pathToTestFile.parentFile
        val targetSpecificFile = parentDirectory.resolve("$testName.$testTarget.$extension")
        return if (targetSpecificFile.exists()) targetSpecificFile else parentDirectory.resolve("$testName.$extension")
    }

    /**
     * Splits one module's worth of [lines] into files -- the single `// FILE:` reader shared by every shape:
     *  - with `// FILE:` markers: each marked section is a file; text before the first marker is dropped;
     *  - without markers: the whole (non-blank) block becomes a single file named [defaultFileName].
     */
    private fun splitFiles(lines: List<String>, defaultFileName: String): List<ParsedFile> {
        if (lines.none { it.startsWith(FILE_MARKER) }) {
            return if (lines.all { it.isBlank() }) emptyList()
            else listOf(ParsedFile(defaultFileName, buildString { lines.forEach { appendLine(it) } }))
        }

        // Lines before the first `// FILE:` are dropped; keep that intentional but visible — only blank or comment
        // lines are allowed there, so a stray declaration placed before the first marker fails loudly.
        val preamble = lines.takeWhile { !it.startsWith(FILE_MARKER) }
        require(preamble.all { it.isBlank() || it.trimStart().startsWith("//") }) {
            "Content before the first `// FILE:` marker is dropped; it must be blank or a comment. Offending lines:\n" +
                    preamble.filterNot { it.isBlank() || it.trimStart().startsWith("//") }.joinToString("\n")
        }

        val files = mutableListOf<ParsedFile>()
        var currentName: String? = null
        val currentText = StringBuilder()

        fun flush() {
            val name = currentName ?: return
            files += ParsedFile(name, currentText.toString())
            currentText.clear()
            currentName = null
        }

        for (line in lines) {
            if (line.startsWith(FILE_MARKER)) {
                flush()
                currentName = line.removePrefix(FILE_MARKER).trim()
            } else if (currentName != null) {
                currentText.appendLine(line)
            }
        }
        flush()
        return files
    }

    /**
     * Parses `// MODULE: name(dep1, dep2)` blocks, each split into files by [splitFiles] (a module with no
     * `// FILE:` marker becomes a single `<module>.kt` file). Only regular dependencies are supported. Module
     * declaration order is preserved. Any text before the first `// MODULE:` marker is treated as a preamble and
     * dropped.
     */
    private fun parseModules(lines: List<String>): List<ParsedModule> {
        val modules = mutableListOf<ParsedModule>()
        var current: ParsedModule? = null
        val currentLines = mutableListOf<String>()

        fun flushModule() {
            val module = current ?: run { currentLines.clear(); return }
            module.files += splitFiles(currentLines, "${module.name}.kt")
            currentLines.clear()
        }

        for (line in lines) {
            val moduleMatch = MODULE_HEADER.matchEntire(line.trim())
            if (moduleMatch != null) {
                flushModule()
                val name = moduleMatch.groupValues[1]
                val deps = moduleMatch.groupValues[2].split(',').map(String::trim).filter(String::isNotEmpty).toSet()
                current = ParsedModule(name, deps).also { modules += it }
            } else {
                currentLines += line
            }
        }
        flushModule()
        return modules
    }
}
