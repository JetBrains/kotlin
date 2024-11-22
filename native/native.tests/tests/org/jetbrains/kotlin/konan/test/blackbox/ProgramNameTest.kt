/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.testProcessExecutor
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ClangDistribution
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.native.executors.runProcess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@Tag("program-name")
class ProgramNameTest : AbstractNativeSimpleTest() {

    @Test
    fun programNameTest() {
        // The С part of the test relies on execv which is not available on tvOS and watchOS.
        Assumptions.assumeTrue(targets.testTarget.family != Family.TVOS)
        Assumptions.assumeTrue(targets.testTarget.family != Family.WATCHOS)
        // 1. Compile kotlinPrintEntryPoint.kt to kotlinPrintEntryPoint.kexe

        val kotlinCompilation = compileToExecutableInOneStage(
            generateTestCaseWithSingleFile(
                sourceFile = sourceDir.resolve("kotlinPrintEntryPoint.kt"),
                testKind = TestKind.STANDALONE_NO_TR,
                extras = TestCase.NoTestRunnerExtras("main")
            )
        ).assertSuccess()

        // 2. Compile main.c to main.cexe

        val cExecutable = buildDir.resolve("main.cexe")
        compileWithClang(
            clangDistribution = ClangDistribution.Llvm,
            sourceFiles = listOf(sourceDir.resolve("main.c")),
            outputFile = cExecutable,
            additionalClangFlags = listOf("-Wall", "-Werror"),
        ).assertSuccess()

        // 3. run main.cexe (with different parameters) to call kotlin executable

        fun validate(expected: String, vararg args: String) {
            val binaryName = kotlinCompilation.resultingArtifact.executableFile.path
            val result = testRunSettings.testProcessExecutor.runProcess(cExecutable.absolutePath, binaryName, *args) {
                timeout = 60.seconds
            }
            val sanitizedStdout = result.stdout.replace("\r\n", "\n") // Ignore if we have unix or windows line endings
            Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
                assertEquals("calling exec...\n$expected", sanitizedStdout)
                assertEquals("", result.stderr)
            }
        }

        // kotlinPrintEntryPoint removes .kexe
        validate("programName: app\nargs:", "app.kexe")

        // Simulate a custom program name, see e.g. https://busybox.net/downloads/BusyBox.html#usage
        validate("programName: customProgramName\nargs:", "customProgramName")
        validate("programName: customProgramName\nargs: firstArg, secondArg", "customProgramName", "firstArg", "secondArg")

        if (targets.testTarget.family == Family.MINGW) {
            // With MinGW, `execv` in `main.c` fails with errno=22 (EINVAL) in the tests below.
            // Let's just skip those corner cases.
            return
        }

        // No program name - this would not be POSIX compliant, see https://pubs.opengroup.org/onlinepubs/9699919799/functions/exec.html:
        // "[...] requires a Strictly Conforming POSIX Application to pass at least one argument to the exec function"
        // However, we should not crash the Kotlin runtime because of this.
        validate("programName: null\nargs:")

        /*
        An empty programName is treated as "no program name", because of the following reasoning:

        When providing no program name (see above validation), both macOS and windows result in an empty argv (argv=[]).
        However, linux behaves differently and sets argv=[""].

        The C standard in section "5.1.2.2.1 Program startup" states:
        "argv[0][0] shall be the null character if the program name is not available from the host environment".

        It is discussable if en empty program name is considered as "not available from the host environment",
        however in the sake of consistency across platforms we decided to treat it as such.

        Therefore, the following test with an empty program name is expected to result in programName=null.
        */

        validate("programName: null\nargs:", "")
    }

    companion object {
        private val sourceDir = File("native/native.tests/testData/programName")
    }
}