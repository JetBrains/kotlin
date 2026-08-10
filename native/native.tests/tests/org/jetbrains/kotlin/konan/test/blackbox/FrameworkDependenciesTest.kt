/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCInteropArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.TestDataAssertions.assertEqualsToFile
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("cinterop")
class FrameworkDependenciesTest : AbstractNativeSimpleTest() {
    @BeforeEach
    fun setUp() {
        Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().testTarget.family == Family.OSX)
    }

    @Test
    fun testWithDefaultLibs() {
        executeTest(noDefaultLibs = false)
    }

    @Test
    fun testNoDefaultLibs() {
        executeTest(noDefaultLibs = true)
    }

    private fun executeTest(noDefaultLibs: Boolean) {
        val interopKlib = cinteropToLibrary(
            defFile = defFile,
            outputDir = buildDir,
            freeCompilerArgs = TestCInteropArgs("-compiler-options", "-iframework $macosPlatformFrameworksPath",),
            noDefaultLibs = false,
        ).assertSuccess().resultingArtifact

        val testCase = generateTestCaseWithSingleModule(ktFile)
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = TestCompilerArgs("-linker-option", "-F$macosPlatformFrameworksPath")
                .applyIf(noDefaultLibs) { plusCompilerArgs(listOf("-no-default-libs")) },
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = listOf(interopKlib.asLibraryDependency()),
            expectedArtifact = Executable(buildDir.resolve("main.kexe")),
        ).result.assertSuccess()

        val otoolResult = runOtoolAndStripOutput(compilation.resultingArtifact)

        assertEqualsToFile(otoolFile(noDefaultLibs), otoolResult)
    }

    private val testDataDir: File = getAbsoluteFile("native/native.tests/testData/CInterop/frameworkDependencies")
    private val defFile: File = testDataDir.resolve("xctest.def")
    private val ktFile: File = testDataDir.resolve("main.kt")

    private fun otoolFile(noDefaultLibs: Boolean): File {
        val suffix = if (noDefaultLibs) "no-default-libs" else "with-default-libs"
        return testDataDir.resolve("otoolResult.$suffix.txt")
    }

    private val macosPlatformPath: String by lazy {
        runProcess("/usr/bin/xcrun", "--show-sdk-platform-path").stdout
    }

    private val macosPlatformFrameworksPath: String by lazy {
        "$macosPlatformPath/Developer/Library/Frameworks"
    }

    private fun runOtoolAndStripOutput(executable: Executable): String {
        val rawResult = runProcess("/usr/bin/otool", "-L", executable.executableFile.absolutePath).stdout

        val lines = rawResult.split("\n")

        val header = lines[0].removePrefix(buildDir.absolutePath).removePrefix("/")

        val otherLines = lines.drop(1)
            .map { line ->
                val truncatedLine = line.substringBefore(" (")

                val strippedLine = if ("/Versions/" in truncatedLine) {
                    // extract the pure framework name: "ABC.framework"
                    truncatedLine.substringBefore("/Versions/").substringAfterLast('/')
                } else if (truncatedLine.endsWith(".dylib")) {
                    // extract the pure dynamic library name: "ABC.dylib"
                    truncatedLine.substringAfterLast('/').substringBefore('.') + ".dylib"
                } else {
                    truncatedLine
                }

                "\t$strippedLine"
            }.sorted()

        return (listOf(header) + otherLines).joinToString("\n")
    }
}
