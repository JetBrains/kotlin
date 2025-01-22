/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.SwiftCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.SimpleTestRunProvider.getTestRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.systemFrameworksPath
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import java.io.File

/**
 * Abstract swift export execution test
 *
 * Swift project provides a modern testing framework – swift-testing, integrated into SPM and Xcode.
 * Unfortunately, those integrations aren't a good fit for us, for various reasons. Fortunately, swift-testing provides
 * a separate ABI with a separate entrypoint, which we use here.
 * See more at https://github.com/swiftlang/swift-testing/tree/main/Documentation/ABI.
 * Harness is at native/native.tests/testData/framework/main-testing.swift
 */
abstract class AbstractSwiftExportExecutionTest : AbstractSwiftExportTest() {
    private val testSuiteDir = File("native/native.tests/testData/framework")

    @BeforeEach
    fun checkHost() {
        Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().hostTarget.family.isAppleFamily)
    }

    override fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutputs: Set<SwiftExportModule>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ) {
        runSwiftTests(testPathFull, testCase, swiftModules, kotlinBinaryLibrary)
    }

    fun runSwiftTests(
        testPathFull: File,
        testCase: TestCase,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ) {
        val swiftTestFiles = testPathFull.walk().filter { it.extension == "swift" }.map { testPathFull.resolve(it) }.toList()
        val testExecutable = compileTestExecutable(testPathFull.name, swiftTestFiles, swiftModules, kotlinBinaryLibrary)
        runExecutableAndVerify(testCase, testExecutable)
    }

    internal fun runExecutableAndVerify(testCase: TestCase, executable: TestExecutable) {
        val testRun = getTestRun(testCase, executable)
        val testRunner = createProperTestRunner(testRun, testRunSettings)
        testRunner.run()
    }

    private fun compileTestExecutable(
        testName: String,
        testSources: List<File>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ): TestExecutable {
        val swiftExtraOpts = swiftModules.flatMap {
            listOf(
                "-I", it.rootDir.absolutePath,
                "-L", it.rootDir.absolutePath,
                "-l${it.moduleName}",
            )
        } + listOf(
            "-Xcc", "-fmodule-map-file=${Distribution(KotlinNativePaths.homePath.absolutePath).kotlinRuntimeForSwiftModuleMap}",
            "-L", kotlinBinaryLibrary.libraryFile.parentFile.absolutePath,
            "-l${kotlinBinaryLibrary.libraryFile.nameWithoutExtension.substringAfter("lib")}",

            "-F", testRunSettings.systemFrameworksPath,
            "-Xlinker", "-rpath", "-Xlinker", testRunSettings.systemFrameworksPath,
            "-framework", "Testing"
        )

        val success = SwiftCompilation(
            testRunSettings,
            testSources + listOf(
                testSuiteDir.resolve("main-testing.swift")
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
