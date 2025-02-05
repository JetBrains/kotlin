/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseStandardTestCaseGroupProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.util.flatMapToSet
import org.jetbrains.kotlin.konan.test.testLibraryAKlibFile
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.swiftexport.standalone.runSwiftExport
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

@TestMetadata("native/swift/swift-export-standalone-integration-tests/external/testData/execution")
@TestDataPath("\$PROJECT_ROOT")
@FirPipeline
@UseStandardTestCaseGroupProvider
class ExternalProjectExecutionTests : AbstractSwiftExportExecutionTest() {

    @Test
    fun `smoke test against testLibraryA`() {
        val testPath = testDataDir.resolve("testLibraryA_smoke").absoluteFile
        val klibSettings = KlibExportSettings(
            path = testLibraryAKlibFile,
            swiftModuleName = "LibraryA",
            rootPackage = "org.jetbrains.a",
        )
        runTestsAgainstKlib(klibSettings, testPath)
    }

    private fun runTestsAgainstKlib(klibSettings: KlibExportSettings, testPath: File) {
        val testModule = TestModule.Given(klibSettings.path.toFile())
        val config = klibSettings.createConfig(
            exportResults = buildDir(testModule.name).toPath().resolve("swift_export_results")
        )
        val swiftExportResult = runSwiftExport(setOf(klibSettings.createInputModule(config))).getOrThrow()
        val kotlinBridgeFiles = swiftExportResult.filterIsInstance<SwiftExportModule.BridgesToKotlin>().map { it.files.kotlinBridges.toFile() }
        val testCase = generateSwiftExportTestCase(
            testPathFull = testPath,
            sources = kotlinBridgeFiles,
            dependencies = setOf(testModule),
        )

        val kotlinBinaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            testCase, testRunSettings,
            kind = BinaryLibraryKind.STATIC,
        ).result.assertSuccess().resultingArtifact

        val swiftModules = swiftExportResult.flatMapToSet { it.compile(testPath, swiftExportResult) }
        runSwiftTests(testPath, testCase, swiftModules, kotlinBinaryLibrary)
    }
}

private val testDataDir = File("native/swift/swift-export-standalone-integration-tests/external/testData/execution")