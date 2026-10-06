/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.ObjCFramework
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.HeaderTestModuleMarkers
import org.jetbrains.kotlin.test.TestDataAssertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("objcexport")
abstract class AbstractNativeObjCExportHeaderTest : AbstractNativeSimpleTest() {
    private val targets: KotlinNativeTargets get() = testRunSettings.get<KotlinNativeTargets>()
    private val testCompilationFactory = TestCompilationFactory()

    protected fun runTest(@TestDataFile testFile: String) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)

        val path = File(ForTestCompileRuntime.transformTestDataPath(testFile).path)
        val testName = path.nameWithoutExtension
        val lines = path.readLines()

        require(HeaderTestModuleMarkers.hasModuleMarkers(lines)) {
            "$testName has no `// MODULE:` markers. These tests exist to pin the order in which declarations from " +
                    "several included libraries reach the header, which a single-module fixture cannot express."
        }

        val sourcesDir = testRunSettings.get<Binaries>().testBinariesDir.resolve("$testName-sources")
        val orderedModules = HeaderTestModuleMarkers.createOrderedTestModules(
            HeaderTestModuleMarkers.parseHeaderTestModules(lines), sourcesDir
        )

        val testCase = HeaderTestModuleMarkers.createInitializedHeaderTestCase(
            testName,
            orderedModules.toSet(),
            freeCompilerArgs(),
            TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        )

        val framework: ObjCFramework = testCompilationFactory.includedModulesToObjCFramework(
            orderedModules,
            testCase.freeCompilerArgs,
            testRunSettings,
            frameworkName = testName,
        ).result.assertSuccess().resultingArtifact

        val goldenDataHeaderFile = HeaderTestModuleMarkers.resolveTargetSpecificGoldenDataFile(
            path, targets.testTarget.toString(), extension = "h"
        )
        TestDataAssertions.assertEqualsToFile(goldenDataHeaderFile, framework.mainHeader.readText())
    }

    private fun freeCompilerArgs() = TestCompilerArgs(
        listOf(
            "-opt-in", "kotlin.experimental.ExperimentalNativeApi",
            "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi",
            "-opt-in", "kotlin.experimental.ExperimentalObjCRefinement",
        )
    )
}
