/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.flatMapToSet
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.test.backend.handlers.UpdateTestDataSupport
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(UpdateTestDataSupport::class)
abstract class AbstractSwiftExportWithBinaryCompilationTest : AbstractSwiftExportTest() {
    protected open fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutputs: Set<SwiftExportModule>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ) {
    }

    @BeforeEach
    fun checkHost() {
        Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().hostTarget.family.isAppleFamily)
        // TODO: KT-75530
        Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().testTarget.family == Family.OSX)
    }

    protected fun runTest(@TestDataFile testDir: String) {
        Assumptions.assumeFalse(isTestIgnored(testDir))

        val (swiftExportOutputs, resultingTestCase) = runConvertToSwift(testDir)

        // TODO: we don't need to compile Kotlin binary for generation tests.
        val kotlinBinaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            resultingTestCase, testRunSettings,
            kind = BinaryLibraryKind.STATIC,
        ).result.assertSuccess().resultingArtifact
        val testPathFull = getAbsoluteFile(testDir)

        // compile swift into binary
        val swiftModules = swiftExportOutputs.flatMapToSet {
            it.compile(
                testPathFull,
                swiftExportOutputs
            )
        }

        // at this point we know that the generated code from SwiftExport can be compiled into library
        // and we are ready to perform other checks
        runCompiledTest(
            testPathFull,
            resultingTestCase,
            swiftExportOutputs,
            swiftModules,
            kotlinBinaryLibrary
        )
    }
}

object KonanHome {
    private const val KONAN_HOME_PROPERTY_KEY = "kotlin.internal.native.test.nativeHome"

    val konanHomePath: String
        get() = System.getProperty(KONAN_HOME_PROPERTY_KEY)
            ?: error("Missing System property: '$KONAN_HOME_PROPERTY_KEY'")
}
