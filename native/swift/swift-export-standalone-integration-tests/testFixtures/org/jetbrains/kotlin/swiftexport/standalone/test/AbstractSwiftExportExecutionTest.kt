/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.Family
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
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.systemToolchainPath
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ClangMode
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClangToStaticLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
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
abstract class AbstractSwiftExportExecutionTest : AbstractSwiftExportWithBinaryCompilationTest() {
    private val testSuiteDir = getAbsoluteFile("native/native.tests/testData/framework")

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
        val objCTestFiles = testPathFull.walk().filter { it.extension == "m" }.map { testPathFull.resolve(it) }.toList()
        val testExecutable = compileTestExecutable(
            testPathFull,
            swiftTestFiles,
            objCTestFiles,
            swiftModules,
            kotlinBinaryLibrary,
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    internal fun runExecutableAndVerify(testCase: TestCase, executable: TestExecutable) {
        val testRun = getTestRun(testCase, executable)
        val testRunner = createProperTestRunner(testRun, testRunSettings)
        testRunner.run()
    }

    /**
     * Compiles the Objective-C sources of a test into a static library, and returns the `swiftc`
     * options that link it in and expose its declarations to Swift through a bridging header.
     *
     * `swiftc` cannot compile `.m` sources itself, so they go through Clang first. The bridging
     * header is the single `.h` next to them.
     */
    private fun objCCompilationOptions(testPathFull: File, objCSources: List<File>): List<String> {
        if (objCSources.isEmpty()) return emptyList()

        val kotlinRuntimeHome = File(Distribution(KotlinNativePaths.homePath.absolutePath).kotlinRuntimeForSwiftHome)
        val staticLibrary = compileWithClangToStaticLibrary(
            testRunSettings = testRunSettings,
            clangMode = ClangMode.C,
            sourceFiles = objCSources,
            outputFile = buildDir(testPathFull).resolve("libObjCTestSources.a"),
            // `KotlinBase.h` lives here; keep modules off so that including it directly does not
            // collide with the `KotlinRuntime` module map sitting in the same directory.
            includeDirectories = listOf(kotlinRuntimeHome) + objCSources.map { it.parentFile }.distinct(),
            additionalClangFlags = listOf("-fno-modules", "-fno-objc-arc"),
        ).assertSuccess().resultingArtifact

        val bridgingHeaders = objCSources.map { it.parentFile }.distinct()
            .flatMap { dir -> dir.listFiles { f: File -> f.extension == "h" }?.toList() ?: emptyList() }
        return listOf(
            "-L", staticLibrary.libraryFile.parentFile.absolutePath,
            "-l${staticLibrary.libraryFile.nameWithoutExtension.removePrefix("lib")}",
            // Swift precompiles the bridging header with Clang, so it needs to find `KotlinBase.h` too.
            "-Xcc", "-I", "-Xcc", kotlinRuntimeHome.absolutePath,
        ) + bridgingHeaders.flatMap { listOf("-import-objc-header", it.absolutePath) }
    }

    private fun compileTestExecutable(
        testPathFull: File,
        testSources: List<File>,
        objCSources: List<File>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ): TestExecutable {
        // todo: KT-81344 Swift Export Execution tests uses 2 different xcode installlation
        val swiftExtraOpts = swiftModules.flatMap {
            listOf(
                "-I", it.rootDir.absolutePath,
                "-L", it.rootDir.absolutePath,
                "-l${it.moduleName}",
            )
        } + listOfNotNull(
            "-Xcc", "-fmodule-map-file=${Distribution(KotlinNativePaths.homePath.absolutePath).kotlinRuntimeForSwiftModuleMap}",
            "-L", kotlinBinaryLibrary.libraryFile.parentFile.absolutePath,
            "-l${kotlinBinaryLibrary.libraryFile.nameWithoutExtension.removePrefix("lib")}",

            "-F", testRunSettings.systemFrameworksPath,
            "-Xlinker", "-rpath", "-Xlinker", testRunSettings.systemFrameworksPath,
            "-framework", "Testing",
            testRunSettings.systemToolchainPath?.let { "-plugin-path" },
            testRunSettings.systemToolchainPath?.let { "${it}/usr/lib/swift/host/plugins/testing/" },
        ) + objCCompilationOptions(testPathFull, objCSources) + extraSwiftCompilerOptions

        val success = SwiftCompilation(
            testRunSettings,
            testSources + listOf(
                testSuiteDir.resolve("main-testing.swift")
            ),
            TestCompilationArtifact.Executable(buildDir(testPathFull).resolve("swiftTestExecutable")),
            swiftExtraOpts,
            outputFile = { executable -> executable.executableFile },
            minOSVersion = minOSVersion,
        ).result.assertSuccess()
        return TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName(testPathFull.name))
        )
    }
}
