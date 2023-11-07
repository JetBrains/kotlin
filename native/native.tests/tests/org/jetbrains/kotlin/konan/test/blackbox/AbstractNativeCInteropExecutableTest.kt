/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import java.io.File

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
abstract class AbstractNativeCInteropExecutableTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testDataDir: String) {
        val defFile = File(testDataDir, "library.def")
        muteCInteropTestIfNecessary(defFile, targets.testTarget)

        val mFile = File(testDataDir, "library.m")
        val ktFile = File(testDataDir, "usage.kt")

        val library = cinteropToLibrary(
            targets = targets,
            defFile = defFile,
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-compiler-option", "-I$testDataDir",
                    "-Xcompile-source", mFile.absolutePath,
                    "-Xsource-compiler-option", "-DNS_FORMAT_ARGUMENT(A)=",
                )
            )
        ).assertSuccess().resultingArtifact

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = ktFile,
            freeCompilerArgs = TestCompilerArgs(testRunSettings.get<PipelineType>().compilerFlags),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras("main")
        )
        val compilationResult = compileToExecutable(testCase, library.asLibraryDependency()).assertSuccess()
        val testExecutable = TestExecutable.fromCompilationResult(testCase, compilationResult)
        runExecutableAndVerify(testCase, testExecutable)
    }
}
