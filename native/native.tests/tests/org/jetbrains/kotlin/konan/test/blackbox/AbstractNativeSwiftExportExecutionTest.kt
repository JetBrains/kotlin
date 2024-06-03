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
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createTestProvider
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.swiftexport.standalone.createDummyLogger
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.io.path.div

@Tag("swiftexport")
abstract class AbstractNativeSwiftExportExecutionTest : AbstractNativeSwiftExportTest() {
    private val testSuiteDir = File("native/native.tests/testData/framework")

    override fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutput: SwiftExportModule,
        swiftModule: TestCompilationArtifact.Swift.Module,
    ) {
        val swiftTestFiles = testPathFull.walk().filter { it.extension == "swift" }.map { testPathFull.resolve(it) }.toList()
        val testExecutable =
            compileTestExecutable(testPathFull.name, swiftTestFiles, swiftModule.rootDir, swiftModule.moduleName, swiftModule.modulemap)
        runExecutableAndVerify(testCase, testExecutable)
    }

    override fun constructSwiftInput(
        testPathFull: File,
    ): InputModule.Binary {
        val testName = testPathFull.name
        val swiftModuleName = testName.capitalizeAsciiOnly()

        return InputModule.Binary(
            name = swiftModuleName,
            path = compileToNativeKLib(testPathFull.toPath()),
        )
    }

    override fun constructSwiftExportConfig(
        testPathFull: File,
    ): SwiftExportConfig {
        val exportResultsPath = buildDir.toPath().resolve("swift_export_results")
        return SwiftExportConfig(
            settings = mapOf(
                SwiftExportConfig.BRIDGE_MODULE_NAME to SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME,
                SwiftExportConfig.STABLE_DECLARATIONS_ORDER to "true",
            ),
            logger = createDummyLogger(),
            outputPath = exportResultsPath
        )
    }

    override fun collectKotlinFiles(testPathFull: File): List<File> =
        testPathFull.walk().filter { it.extension == "kt" }.map { testPathFull.resolve(it) }.toList()

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
}
