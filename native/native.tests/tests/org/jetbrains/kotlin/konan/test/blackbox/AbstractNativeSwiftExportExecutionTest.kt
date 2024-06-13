/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.SwiftCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.SimpleTestRunProvider.getTestRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createTestProvider
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.swiftexport.standalone.createDummyLogger
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("swiftexport")
abstract class AbstractNativeSwiftExportExecutionTest : AbstractNativeSwiftExportTest() {
    private val testSuiteDir = File("native/native.tests/testData/framework")

    override fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutput: SwiftExportModule,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
    ) {
        val swiftTestFiles = testPathFull.walk().filter { it.extension == "swift" }.map { testPathFull.resolve(it) }.toList()
        val testExecutable =
            compileTestExecutable(testPathFull.name, swiftTestFiles, swiftModules = swiftModules)
        runExecutableAndVerify(testCase, testExecutable)
    }

    internal fun runExecutableAndVerify(testCase: TestCase, executable: TestExecutable) {
        val testRun = getTestRun(testCase, executable)
        val testRunner = createProperTestRunner(testRun, testRunSettings)
        testRunner.run()
    }

    override fun constructSwiftExportConfig(module: TestModule.Exclusive): SwiftExportConfig {
        val exportResultsPath = buildDir(module.name).toPath().resolve("swift_export_results")
        return SwiftExportConfig(
            settings = mapOf(
                SwiftExportConfig.BRIDGE_MODULE_NAME to SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME,
                SwiftExportConfig.STABLE_DECLARATIONS_ORDER to "true",
            ),
            logger = createDummyLogger(),
            outputPath = exportResultsPath
        )
    }

    private fun compileTestExecutable(
        testName: String,
        testSources: List<File>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
    ): TestExecutable {
        val swiftExtraOpts = swiftModules.flatMap {
            listOf(
                "-I", it.rootDir.absolutePath,
                "-L", it.rootDir.absolutePath,
                "-l${it.moduleName}",
            ) + (it.modulemap?.let { listOf("-Xcc", "-fmodule-map-file=${it.absolutePath}") } ?: emptyList())
        } + listOf("-Xcc", "-fmodule-map-file=${Distribution(KotlinNativePaths.homePath.absolutePath).kotlinRuntimeForSwiftModuleMap}")
        val provider = createTestProvider(buildDir(testName), testSources)
        val success = SwiftCompilation(
            testRunSettings,
            testSources + listOf(
                provider,
                testSuiteDir.resolve("main.swift")
            ),
            TestCompilationArtifact.Executable(buildDir(testName).resolve("swiftTestExecutable")),
            swiftExtraOpts,
            outputFile = { executable -> executable.executableFile }
        ).result.assertSuccess()
        return TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName(testName))
        )
    }
}
