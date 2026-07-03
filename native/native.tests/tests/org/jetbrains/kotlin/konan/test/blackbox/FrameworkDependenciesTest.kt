/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCInteropArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.TestDataAssertions.assertEqualsToFile
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("cinterop")
class FrameworkDependenciesTest : AbstractNativeSimpleTest() {
    @Test
    fun testSimple() {
        executeTest("simple", noDefaultLibs = false)
    }

    @Test
    fun noDefaultLibs() {
        executeTest("noDefaultLibs", noDefaultLibs = true)
    }

    private fun executeTest(name: String, noDefaultLibs: Boolean) {
        val testDataDir = testDataDir(name)
        val interopKlib = cinteropToLibrary(
            defFile = testDataDir.resolve("xctest.def"),
            outputDir = buildDir,
            freeCompilerArgs = TestCInteropArgs(
                "-compiler-options",
                "-iframework /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/Library/Frameworks",
            ),
            noDefaultLibs = false,
        ).assertSuccess().resultingArtifact

        val testCase = generateTestCaseWithSingleModule(testDataDir.resolve("main.kt"))
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = TestCompilerArgs(
                "-linker-option",
                "-F/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/Library/Frameworks",
            ).applyIf(noDefaultLibs) { plusCompilerArgs(listOf("-no-default-libs")) },
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = listOf(interopKlib.asLibraryDependency()),
            expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("main.kexe")),
        ).result.assertSuccess()
        val executable = compilation.resultingArtifact

        val otoolResult = runProcess("/usr/bin/otool", "-L", executable.executableFile.absolutePath).output
        assertEqualsToFile(testDataDir.resolve("otoolResult.txt"), otoolResult)
    }

    private fun testDataDir(scenario: String): File =
        getAbsoluteFile("native/native.tests/testData/CInterop/frameworkDependencies/$scenario")
}
