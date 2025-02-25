/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseStandardTestCaseGroupProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.util.flatMapToSet
import org.jetbrains.kotlin.konan.test.testLibraryAKlibFile
import org.jetbrains.kotlin.konan.test.testLibraryBKlibFile
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.runSwiftExport
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test
import java.io.File

@TestMetadata("native/swift/swift-export-standalone-integration-tests/external/testData/execution")
@TestDataPath("\$PROJECT_ROOT")
@UseStandardTestCaseGroupProvider
class ExternalProjectExecutionTests : AbstractSwiftExportExecutionTest() {

    @Test
    fun `smoke test against testLibraryA`() {
        val testPath = testDataDir.resolve("testLibraryA_smoke").absoluteFile
        val klibSettings = KlibExportSettings(
            path = testLibraryAKlibFile,
            konanTarget = targets.testTarget,
            swiftModuleName = "LibraryA",
            rootPackage = "org.jetbrains.a",
        )
        runTestsAgainstKlib(setOf(klibSettings), testPath)
    }

    @Test
    fun `smoke test against 2 libraries combined`() {
        val testPath = testDataDir.resolve("testLibraryA_testLibraryB_combined").absoluteFile
        val klibSettingsA = KlibExportSettings(
            path = testLibraryAKlibFile,
            konanTarget = targets.testTarget,
            swiftModuleName = "LibraryA",
            rootPackage = "org.jetbrains.a",
        )
        val klibSettingsB = KlibExportSettings(
            path = testLibraryBKlibFile,
            konanTarget = targets.testTarget,
            swiftModuleName = "LibraryB",
            rootPackage = "org.jetbrains.b",
        )
        runTestsAgainstKlib(setOf(klibSettingsA, klibSettingsB), testPath)
    }

    private fun runTestsAgainstKlib(klibSettings: Set<KlibExportSettings>, testPath: File) {
        val testModules = klibSettings.map { TestModule.Given(it.path.toFile()) }.toSet()
        val inputModules = klibSettings.map {
            it.createInputModule(SwiftModuleConfig(rootPackage = it.rootPackage))
        }.toSet()

        val swiftConfig = SwiftExportConfig(
            outputPath = buildDir(testPath.name).toPath().resolve("swift_export_results"),
            konanTarget = targets.testTarget
        )

        val swiftExportResult = runSwiftExport(inputModules, swiftConfig).getOrThrow()
        val kotlinBridgeFiles =
            swiftExportResult.filterIsInstance<SwiftExportModule.BridgesToKotlin>().map { it.files.kotlinBridges.toFile() }
        val testCase = generateSwiftExportTestCase(
            testPathFull = testPath,
            sources = kotlinBridgeFiles,
            dependencies = testModules,
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