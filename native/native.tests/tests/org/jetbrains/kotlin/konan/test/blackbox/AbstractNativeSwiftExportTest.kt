/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.LoggedData
import org.jetbrains.kotlin.konan.blackboxtest.support.PackageName
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.TestKind
import org.jetbrains.kotlin.konan.blackboxtest.support.TestModule
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.konan.blackboxtest.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.blackboxtest.support.util.getAbsoluteFile
import java.io.File
import java.nio.file.Files

/**
 * Trivial test infrastructure for Swift Export.
 *
 * The current implementation is mostly a Frankenstein monster that does not really
 * support test runners of any kind. Which should be OK for now and could be improved as we go.
 */
abstract class AbstractNativeSwiftExportTest : AbstractNativeSimpleTest() {

    private val testCompilationFactory = TestCompilationFactory()

    protected fun runIntegrationTest(@TestDataFile testDir: String) {
        val testDirectory = getAbsoluteFile(testDir)
        val ktSources = testDirectory.list()!!
            .filter { it.endsWith(".kt") }
            .map { testDirectory.resolve(it) }
        val swiftSources = testDirectory.list()!!
            .filter { it.endsWith(".swift") }
            .map { testDirectory.resolve(it) }

        val testCase = generateSwiftExportTestCase(testDirectory, ktSources)
        val swiftArtifact = testCase.toSwiftArtifact().assertSuccess()
        val outputDir = buildDir.resolve("swift_compilation_result")
        outputDir.mkdirs()
        val outputFile = outputDir.resolve("main")
        val swiftCompilationResult = compileSwiftSources(
            sources = swiftSources + swiftArtifact.resultingArtifact.swiftSources,
            output = outputFile,
            libraries = listOf(swiftArtifact.resultingArtifact.artifactName),
            importedHeaders = swiftArtifact.resultingArtifact.cBridgingHeaders,
            libraryDirectories = listOf(swiftArtifact.resultingArtifact.kotlinBinary.parentFile),
        )
        if (swiftCompilationResult != 0) {
            error("Swift compilation failed with exit code $swiftCompilationResult")
        }
        val testExecutable = TestExecutable(outputFile, LoggedData.NoopCompilerCall(outputFile), emptySet())
        runExecutableAndVerify(testCase, testExecutable)
    }

    protected fun runAPIGenerationTest(@TestDataFile testDir: String) {
        val testDirectory = getAbsoluteFile(testDir)
        val ktSources = testDirectory.list()!!
            .filter { it.endsWith(".kt") }
            .map { testDirectory.resolve(it) }
        val expectedSwiftAPI = testDirectory.list()!!
            .filter { it.endsWith(".swift") }
            .map { testDirectory.resolve(it) }

        val testCase = generateSwiftExportTestCase(testDirectory, ktSources)
        val resultedSwiftAPI = testCase.toSwiftAPI().assertSuccess()

        assert(expectedSwiftAPI.count() == resultedSwiftAPI.resultingArtifact.swiftSources.count()) {
            "expected ${expectedSwiftAPI.count()} swift files in result, got ${resultedSwiftAPI.resultingArtifact.swiftSources.count()}"
        }
        val zipped = expectedSwiftAPI.sorted().zip(resultedSwiftAPI.resultingArtifact.swiftSources.sorted())
        zipped.forEach {
            assert(it.first.readBytes().contentEquals(it.second.readBytes())) { "Generated files were different" }
        }
    }

    private fun TestCase.toSwiftArtifact(): TestCompilationResult<out TestCompilationArtifact.SwiftArtifact> {
        val factory = testCompilationFactory
        toSwiftAPI()
        toSwiftFromKLib()
        val compilation = factory.testCaseToSwiftArtifactCompilation(this, testRunSettings)
        return compilation.result
    }

    private fun TestCase.toSwiftAPI(): TestCompilationResult<out TestCompilationArtifact.SwiftArtifact> {
        val factory = testCompilationFactory
        val compilation = factory.testCaseToSwiftAPIProduction(this, testRunSettings)
        return compilation.result
    }

    private fun TestCase.toSwiftFromKLib(): TestCompilationResult<out TestCompilationArtifact.SwiftArtifact> {
        val factory = testCompilationFactory
        val compilation = factory.testCaseToExtractSwiftFromKLib(this, testRunSettings)
        return compilation.result
    }

    // This method is pretty hacky and there is a big room for improvement (e.g. a proper support for test-runners).
    private fun generateSwiftExportTestCase(testPath: File, sources: List<File>): TestCase {
        val moduleName = testPath.name
        val module = TestModule.Exclusive(DEFAULT_MODULE_NAME, emptySet(), emptySet(), emptySet())
        sources.forEach { module.files += TestFile.createCommitted(it, module) }

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(listOf()),
            nominalPackageName = PackageName(moduleName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            // We don't actually need an entry point here, but this is what infrastructure requires.
            extras = TestCase.NoTestRunnerExtras("unused")
        ).apply {
            initialize(null, null)
        }
    }

    /**
     * Compiles Swift sources using the Swift compiler.
     *
     * @param sources the list of Swift source files to be compiled
     * @param output the output file to generate after compilation
     * @param libraries the list of libraries to link during compilation (default is empty)
     * @param importedHeaders the list of c headers to import as bridging headers (default is empty)
     * @param libraryDirectories the list of directories to search for libraries (default is empty)
     * @return the exit code of the compilation process
     */
    private fun compileSwiftSources(
        sources: List<File>,
        output: File,
        libraries: List<String> = emptyList(),
        importedHeaders: List<File> = emptyList(),
        libraryDirectories: List<File> = emptyList(),
    ): Int {
        val aggregatedHeader = Files.createTempFile("bridge", ".h").toFile().also { bridge ->
            importedHeaders.forEach { header ->
                bridge.appendText("#include <${header.absolutePath}>\n")
            }
        }

        val process = ProcessBuilder(
            // TODO: Use swiftc from a determined toolchain
            "swiftc",
            *libraryDirectories.flatMap { listOf("-L", it.absolutePath) }.toTypedArray(),
            *libraries.map { "-l$it" }.toTypedArray(),
            "-import-objc-header", aggregatedHeader.absolutePath,
            "-o", output.absolutePath,
            *sources.map { it.absolutePath }.toTypedArray(),
        )
            .inheritIO()
            .start()
        return process.waitFor()
    }
}