/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestFile
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.SwiftCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createModuleMap
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createTestProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("swiftexport")
abstract class AbstractNativeSwiftExportTest : AbstractNativeSimpleTest() {

    private val testCompilationFactory = TestCompilationFactory()
    private val testSuiteDir = File("native/native.tests/testData/framework")

    protected fun runTest(@TestDataFile testDir: String) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val testPathFull = getAbsoluteFile(testDir)

        val testName = testPathFull.name
        val swiftModuleName = testName.capitalizeAsciiOnly()

        val swiftExportOutput = runSwiftExport(swiftModuleName, testPathFull)

        val kotlinFiles = testPathFull.walk().filter { it.extension == "kt" }.map { testPathFull.resolve(it) }.toList()
        val testCase = generateSwiftExportTestCase(testName, kotlinFiles + swiftExportOutput.kotlinBridges.toFile())
        val kotlinBinaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            testCase, testRunSettings,
            kind = BinaryLibraryKind.DYNAMIC,
        ).result.assertSuccess().resultingArtifact

        val bridgeModuleFile = createModuleMap(buildDir, swiftExportOutput.cHeaderBridges.toFile())
        val swiftModule = compileSwiftModule(
            swiftModuleName,
            listOf(swiftExportOutput.swiftApi.toFile()),
            bridgeModuleFile,
            kotlinBinaryLibrary
        )

        val swiftTestFiles = testPathFull.walk().filter { it.extension == "swift" }.map { testPathFull.resolve(it) }.toList()
        val testExecutable = compileTestExecutable(testName, swiftTestFiles, swiftModule.rootDir, swiftModuleName, bridgeModuleFile)
        runExecutableAndVerify(testCase, testExecutable)
    }

    private fun runSwiftExport(
        moduleName: String,
        testPathFull: File
    ): SwiftExportFiles {
        val swiftExportInput = InputModule.Source(
            name = moduleName,
            path = testPathFull.toPath(),
        )
        val exportResultsPath = buildDir.toPath().resolve("swift_export_results")

        val swiftExportConfig = SwiftExportConfig(
            settings = mapOf(
                SwiftExportConfig.BRIDGE_MODULE_NAME to SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME,
                SwiftExportConfig.STABLE_DECLARATIONS_ORDER to "true",
            ),
            logger = createDummyLogger(),
            outputPath = exportResultsPath
        )

        return runSwiftExport(
            swiftExportInput,
            swiftExportConfig
        ).getOrThrow().first().files
    }

    private fun compileSwiftModule(
        swiftModuleName: String,
        sources: List<File>,
        moduleMap: File,
        binaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ): TestCompilationArtifact.Swift.Module {
        val swiftModuleDir = buildDir.resolve("SwiftModules").resolve(swiftModuleName).also { it.mkdirs() }
        val binaryLibraryName = binaryLibrary.libraryFile.nameWithoutExtension.substringAfter("lib")
        return SwiftCompilation(
            testRunSettings,
            sources = sources,
            TestCompilationArtifact.Swift.Module(
                rootDir = swiftModuleDir,
                moduleName = swiftModuleName
            ),
            swiftExtraOpts = listOf(
                "-Xcc", "-fmodule-map-file=${moduleMap.absolutePath}",
                "-Xcc", "-fmodule-map-file=${Distribution(KotlinNativePaths.homePath.absolutePath).kotlinRuntimeForSwiftModuleMap}",
                "-L", binaryLibrary.libraryFile.parentFile.absolutePath,
                "-l$binaryLibraryName",
                "-emit-module", "-parse-as-library", "-emit-library", "-enable-library-evolution",
                "-module-name", swiftModuleName,
            ),
            outputFile = { it.binaryLibrary },
        ).result.assertSuccess().resultingArtifact
    }

    private fun compileTestExecutable(
        testName: String,
        testSources: List<File>,
        swiftModuleDir: File,
        binaryLibraryName: String,
        moduleMap: File,
    ): TestExecutable {
        val swiftExtraOpts = listOf(
            "-I", swiftModuleDir.absolutePath,
            "-L", swiftModuleDir.absolutePath,
            "-l$binaryLibraryName",
            "-Xcc", "-fmodule-map-file=${moduleMap.absolutePath}",
            "-Xcc", "-fmodule-map-file=${Distribution(KotlinNativePaths.homePath.absolutePath).kotlinRuntimeForSwiftModuleMap}",
        )
        val provider = createTestProvider(buildDir, testSources)
        val success = SwiftCompilation(
            testRunSettings,
            testSources + listOf(
                provider,
                testSuiteDir.resolve("main.swift")
            ),
            TestCompilationArtifact.Executable(buildDir.resolve("swiftTestExecutable")),
            swiftExtraOpts,
            outputFile = { executable -> executable.executableFile }
        ).result.assertSuccess()
        return TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName(testName))
        )
    }

    private fun generateSwiftExportTestCase(testName: String, sources: List<File>): TestCase {
        val module = TestModule.Exclusive(DEFAULT_MODULE_NAME, emptySet(), emptySet(), emptySet())
        sources.forEach { module.files += TestFile.createCommitted(it, module) }

        return TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in", "kotlin.experimental.ExperimentalNativeApi",
                    "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi"
                )
            ),
            nominalPackageName = PackageName(testName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
        ).apply {
            initialize(null, null)
        }
    }
}