/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// KT-90068: an explicit smallBinary must make the AArch64 MachineOutliner run on Kotlin code.
@TestDataPath("\$PROJECT_ROOT")
@EnforcedProperty(ClassLevelProperty.OPTIMIZATION_MODE, "OPT")
@EnforcedProperty(ClassLevelProperty.CACHE_MODE, "NO")
class SmallBinaryOutlinerTest : AbstractNativeSimpleTest() {
    @Test
    fun testExplicitSmallBinaryEnablesOutliner() {
        val outlined = outlinedFunctions("explicit", "-Xbinary=smallBinary=true")
        val notOutlined = outlinedFunctions("disabled", "-Xbinary=smallBinary=false")
        assertTrue(outlined > notOutlined, "Expected more outlined functions with smallBinary: $outlined vs $notOutlined")
    }

    @Test
    fun testExplicitOutlinerSettingIsKept() {
        val target = targets.testTarget.name
        assertEquals(
            0,
            outlinedFunctions(
                "overridden",
                "-Xbinary=smallBinary=true",
                "-Xoverride-konan-properties=clangOptFlags.$target=-O3 -mllvm -enable-machine-outliner=never",
            )
        )
    }

    private fun outlinedFunctions(name: String, vararg compilerArgs: String): Int {
        Assumptions.assumeTrue(targets.testTarget.architecture == Architecture.ARM64)

        val rootDir = ForTestCompileRuntime.transformTestDataPath("native/native.tests/testData/compilerOutput/smallBinaryOutliner")
        val tempDir = buildDir.resolve("$name-temp").apply { mkdirs() }
        val testCase = generateTestCaseWithSingleFile(
            rootDir.resolve("main.kt"),
            freeCompilerArgs = TestCompilerArgs(listOf("-Xtemporary-files-dir=${tempDir.absolutePath}") + compilerArgs),
            extras = TestCase.NoTestRunnerExtras("main"),
            testKind = TestKind.STANDALONE_NO_TR,
        )
        ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve(name)),
        ).result.assertSuccess()

        val objectFile = tempDir.walk().single { it.extension == "o" }
        return Regex("OUTLINED_FUNCTION_\\d+").findAll(objectFile.readText(Charsets.ISO_8859_1)).map { it.value }.toSet().size
    }
}
