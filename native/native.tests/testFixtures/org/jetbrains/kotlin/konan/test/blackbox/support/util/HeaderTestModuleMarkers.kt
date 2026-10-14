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
 * The one thing these tests need from the test data that a single-module fixture cannot express is the
 * **order** of the modules: declaration order is the order the modules are passed to `-Xinclude`, and
 * declaring a dependent module before its dependency is what makes the CLI order contradict the topological
 * order. [parseHeaderTestModules] therefore preserves declaration order, and same-named files in different
 * modules stay distinct because each module gets its own source directory.
 */
object HeaderTestModuleMarkers {
    private val MODULE_HEADER = Regex("""// MODULE:\s*(\w+)\s*(?:\(([^)]*)\))?\s*""")
    private const val FILE_MARKER = "// FILE: "

    class ParsedModule(val name: String, val dependencies: Set<String>) {
        val files: MutableList<ParsedFile> = mutableListOf()
    }

    class ParsedFile(val name: String, val text: String)

    /**
     * Parses `// MODULE: name(dep1, dep2)` blocks, each optionally split into `// FILE: <name>` source files (a
     * module with no `// FILE:` marker becomes a single `<module>.kt` file). Only regular dependencies are
     * supported. Module declaration order is preserved. Any text before the first `// MODULE:` marker is
     * treated as a preamble and dropped.
     */
    fun parseHeaderTestModules(lines: List<String>): List<ParsedModule> {
        val modules = mutableListOf<ParsedModule>()
        var currentFileName: String? = null
        val currentText = StringBuilder()

        fun flushFile() {
            val module = modules.lastOrNull() ?: run { currentText.clear(); return }
            val name = currentFileName ?: if (currentText.isBlank()) return else "${module.name}.kt"
            module.files += ParsedFile(name, currentText.toString())
            currentText.clear()
            currentFileName = null
        }

        for (line in lines) {
            val moduleMatch = MODULE_HEADER.matchEntire(line.trim())
            when {
                moduleMatch != null -> {
                    flushFile()
                    val name = moduleMatch.groupValues[1]
                    val deps = moduleMatch.groupValues[2].split(',').map(String::trim).filter(String::isNotEmpty).toSet()
                    modules += ParsedModule(name, deps)
                }
                line.startsWith(FILE_MARKER) -> {
                    flushFile()
                    currentFileName = line.removePrefix(FILE_MARKER).trim()
                }
                else -> currentText.appendLine(line)
            }
        }
        flushFile()
        return modules
    }

    /** `true` if [lines] use `// MODULE:` markers, i.e. describe a multi-module fixture. */
    fun hasModuleMarkers(lines: List<String>): Boolean = lines.any { it.startsWith("// MODULE:") }

    /**
     * Turns parsed modules into [TestModule.Exclusive]s **in declaration order**, writing each module's sources
     * under its own directory below [sourcesDir] so that same-named files in different modules do not collide.
     */
    fun createOrderedTestModules(parsedModules: List<ParsedModule>, sourcesDir: File): List<TestModule.Exclusive> {
        val modulesByName = parsedModules.associate { parsed ->
            parsed.name to TestModule.Exclusive(parsed.name, parsed.dependencies, emptySet(), emptySet())
        }

        parsedModules.forEach { parsed ->
            val module = modulesByName.getValue(parsed.name)
            parsed.files.forEach { sourceFile ->
                module.files += TestFile.createUncommitted(
                    sourcesDir.resolve(parsed.name).resolve(sourceFile.name),
                    module,
                    sourceFile.text,
                )
            }
        }

        return parsedModules.map { modulesByName.getValue(it.name) }
    }

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
}
