/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.SwiftCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.SimpleTestRunProvider.getTestRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInNative
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import java.io.File

/**
 * Imports the test Swift module, compiles it together with the generated Swift bridges into a dynamic library,
 * and runs a Kotlin/Native executable that is linked against this library.
 *
 * Besides Swift sources, a test data directory `<name>` contains:
 * - `<name>.kt`: Kotlin sources with the `main` function that calls the imported declarations;
 * - `<name>.out` (optional): expected stdout of the executable;
 * - `exitCode` (optional): expected exit code of the executable, `!0` means any non-zero exit code.
 */
@MustRunOnChangesInNative
abstract class AbstractSwiftImportExecutionTest : AbstractSwiftImportTest() {
    private val testCompilationFactory = TestCompilationFactory()

    @BeforeEach
    fun checkTarget() {
        Assumptions.assumeTrue(targets.testTarget == KonanTarget.MACOS_ARM64)
    }

    protected fun runTest(@TestDataFile testDir: String) {
        val testPathFull = getAbsoluteFile(testDir)
        val output = importSwiftModule(testPathFull)
        val moduleName = output.moduleName

        val outputDir = buildDir(testPathFull).resolve("swift_import_results").apply { mkdirs() }
        val kotlinBridges = outputDir.resolve("$moduleName.kt").apply { writeText(output.kotlinSources) }
        val swiftBridges = outputDir.resolve("${moduleName}Bridges.swift").apply { writeText(output.swiftBridges) }
        outputDir.resolve("$moduleName.h").writeText(output.cHeaderBridges)

        val swiftLibrary = compileSwiftLibrary(testPathFull, moduleName, testPathFull.swiftSources() + swiftBridges)

        val testCase = generateSwiftImportTestCase(
            testPathFull,
            sources = listOf(kotlinBridges, testPathFull.resolve("${testPathFull.name}.kt")),
            swiftLibrary = swiftLibrary,
            swiftModuleName = moduleName,
        )
        val compilationResult = testCompilationFactory.testCasesToExecutable(listOf(testCase), testRunSettings).result.assertSuccess()
        val executable = TestExecutable(
            compilationResult.resultingArtifact,
            compilationResult.loggedData,
            listOf(TestName(testPathFull.name)),
        )

        createProperTestRunner(getTestRun(testCase, executable), testRunSettings).run()
    }

    private fun compileSwiftLibrary(
        testPathFull: File,
        moduleName: String,
        sources: List<File>,
    ): TestCompilationArtifact.BinaryLibrary {
        val libraryDir = buildDir(testPathFull).resolve("SwiftLibrary").apply { mkdirs() }
        return SwiftCompilation(
            testRunSettings = testRunSettings,
            sources = sources,
            expectedArtifact = TestCompilationArtifact.BinaryLibrary(libraryDir.resolve("lib$moduleName.dylib")),
            swiftExtraOpts = listOf(
                // A dynamic library brings its dependencies on the Swift runtime with it,
                // so the Kotlin/Native executable doesn't have to link the Swift runtime explicitly.
                "-parse-as-library", "-emit-library",
                "-module-name", moduleName,
                // Swift bridges are declared as `package` functions.
                "-package-name", "SwiftImportTests",
            ),
            outputFile = { it.libraryFile },
        ).result.assertSuccess().resultingArtifact
    }

    private fun generateSwiftImportTestCase(
        testPathFull: File,
        sources: List<File>,
        swiftLibrary: TestCompilationArtifact.BinaryLibrary,
        swiftModuleName: String,
    ): TestCase {
        val testName = testPathFull.name
        val module = TestModule.newDefaultModule()
        sources.forEach { module.files += TestFile.createCommitted(it, module) }

        val expectedOutput = testPathFull.resolve("$testName.out").takeIf { it.exists() }
        val exitCode = testPathFull.resolve("exitCode").takeIf { it.exists() }?.readText()?.trim()
        val swiftLibraryDir = swiftLibrary.libraryFile.parentFile.absolutePath

        return TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in", "kotlin.native.internal.InternalForKotlinNative", // for @ImportedBridge
                    "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in", "kotlin.experimental.ExperimentalNativeApi",
                    "-linker-option", "-L$swiftLibraryDir",
                    "-linker-option", "-l$swiftModuleName",
                    "-linker-option", "-rpath",
                    "-linker-option", swiftLibraryDir,
                )
            ),
            nominalPackageName = PackageName(testName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).run {
                copy(
                    outputDataFile = expectedOutput?.let { TestRunCheck.OutputDataFile(TestRunCheck.Output.STDOUT, it) },
                    exitCodeCheck = exitCode?.let {
                        if (it == "!0") TestRunCheck.ExitCode.AnyNonZero else TestRunCheck.ExitCode.Expected(it.toInt())
                    } ?: exitCodeCheck,
                )
            },
            extras = TestCase.NoTestRunnerExtras(entryPoint = "main"),
        ).apply {
            initialize(givenModules = null, findSharedModule = null)
        }
    }
}
