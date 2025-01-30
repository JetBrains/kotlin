/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.util.flatMapToSet
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.assertAll
import java.io.File
import kotlin.io.path.div
import kotlin.io.path.readText

abstract class AbstractKlibBasedSwiftRunnerTest : AbstractSwiftExportTest() {

    protected val tmpdir = FileUtil.createTempDirectory("SwiftExportIntegrationTests", null, false)

    override fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutputs: Set<SwiftExportModule>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    ) {
        validateSwiftExportOutput(testPathFull, swiftExportOutputs)
    }

    /**
     * Check that [swiftExportOutputs] are the same as in [goldenData].
     */
    fun validateSwiftExportOutput(
        goldenData: File,
        swiftExportOutputs: Set<SwiftExportModule>,
        validateKotlinBridge: Boolean = true,
    ) {
        val flattenModules = swiftExportOutputs.flatMapToSet { it.dependencies.toSet() + it }

        assertAll(flattenModules.flatMap {
            when (it) {
                is SwiftExportModule.BridgesToKotlin -> {
                    val files = it.files

                    val expectedFiles = goldenData.toPath() / "golden_result/"
                    val expectedSwift = expectedFiles / it.name / "${it.name}.swift"
                    val expectedCHeader = expectedFiles / it.name / "${it.name}.h"
                    val expectedKotlinBridge = expectedFiles / it.name / "${it.name}.kt"

                    buildList {
                        add { KotlinTestUtils.assertEqualsToFile(expectedSwift, files.swiftApi.readText()) }
                        add { KotlinTestUtils.assertEqualsToFile(expectedCHeader, files.cHeaderBridges.readText()) }
                        if (validateKotlinBridge) {
                            add { KotlinTestUtils.assertEqualsToFile(expectedKotlinBridge, files.kotlinBridges.readText()) }
                        }
                    }
                }
                is SwiftExportModule.SwiftOnly -> {
                    when (it.kind) {
                        SwiftExportModule.SwiftOnly.Kind.KotlinPackages -> {
                            val expectedFiles = goldenData.toPath() / "golden_result/"
                            val expectedSwift = expectedFiles / it.name / "${it.name}.swift"

                            listOf { KotlinTestUtils.assertEqualsToFile(expectedSwift, it.swiftApi.readText()) }
                        }
                        SwiftExportModule.SwiftOnly.Kind.KotlinRuntimeSupport -> {
                            // No need to verify predefined files.
                            emptyList()
                        }
                    }

                }
                else -> emptyList()
            }
        })
    }
}
