/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.flatMapToSet
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import java.io.File

abstract class AbstractExternalProjectWithBinaryCompilationTest : AbstractExternalProjectTest(), SwiftExportValidator {

    @BeforeEach
    fun checkHost() {
        // Swift export compilation/execution requires an Apple host (swiftc/Xcode). The *target* gate
        // lives in @EnabledOnNativeTargets on this class (see AbstractSwiftExportTest.assumeTestTargetEnabled).
        Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().hostTarget.family.isAppleFamily)
    }

    override fun runTest(
        modules: Set<TestModule.Given>,
        testPathFull: File,
        swiftExportOutputs: Set<SwiftExportModule>,
    ) {
        val kotlinBridgeFiles = swiftExportOutputs.filterIsInstance<SwiftExportModule.BridgesToKotlin>().map {
            it.files.kotlinBridges.toFile()
        }
        val testCase = generateSwiftExportTestCase(
            testPathFull = testPathFull,
            sources = kotlinBridgeFiles,
            dependencies = modules,
        )

        val kotlinBinaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            testCase, testRunSettings,
            kind = BinaryLibraryKind.STATIC,
        ).result.assertSuccess().resultingArtifact

        val swiftModules = swiftExportOutputs.flatMapToSet { it.compile(testPathFull, swiftExportOutputs) }
        runCompiledTest(testPathFull, testCase, swiftExportOutputs, swiftModules, kotlinBinaryLibrary)
    }

    protected open fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutputs: Set<SwiftExportModule>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ) {
    }
}
