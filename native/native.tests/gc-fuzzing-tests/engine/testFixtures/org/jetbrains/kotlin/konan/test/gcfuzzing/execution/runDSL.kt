/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.execution

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.cinteropToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.generateObjCFrameworkTestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.codesign
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExecutionTimeout
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.TestFiltering
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Output
import org.junit.jupiter.api.Assumptions
import java.io.File
import kotlin.time.Duration

fun AbstractNativeSimpleTest.resolveTestBuildDir(testName: String): File = buildDir.resolve(testName)
fun AbstractNativeSimpleTest.resolveDslDir(testName: String): File = resolveTestBuildDir(testName).resolve("generated")

fun AbstractNativeSimpleTest.runDSL(
    testName: String,
    dslOutput: Output,
    executionTimeout: Duration
) {
    val baseDir = resolveTestBuildDir(testName)
    val dslGeneratedDir = resolveDslDir(testName)
    Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().hostTarget.family.isAppleFamily)
    val cinterop = cinteropToLibrary(
        dslGeneratedDir.resolve(dslOutput.cinterop.defFilename),
        baseDir,
        freeCompilerArgs = TestCompilerArgs(compilerArgs = emptyList(), cinteropArgs = dslOutput.cinterop.args),
    ).assertSuccess()
    val objcFrameworkTestCase = generateObjCFrameworkTestCase(
        TestKind.STANDALONE_NO_TR,
        TestCase.NoTestRunnerExtras(),
        dslOutput.kotlin.frameworkName,
        listOf(dslGeneratedDir.resolve(dslOutput.kotlin.filename)),
        TestCompilerArgs(dslOutput.kotlin.args),
        setOf(TestModule.Given(cinterop.resultingArtifact.klibFile)),
        checks = TestRunChecks(
            executionTimeoutCheck = ExecutionTimeout.ShouldNotExceed(executionTimeout),
            testFiltering = TestFiltering(TestOutputFilter.NO_FILTERING),
            exitCodeCheck = ExitCode.Expected(0),
            outputDataFile = null,
            outputMatcher = null,
            fileCheckMatcher = null,
        )
    )
    val objCFramework = TestCompilationFactory()
        .testCaseToObjCFrameworkCompilation(
            objcFrameworkTestCase,
            testRunSettings,
            buildDir = baseDir
        )
        .result.assertSuccess()
    codesign(objCFramework.resultingArtifact.frameworkDir.absolutePath)
    val finalExecutable = compileWithClang(
        sourceFiles = listOf(dslGeneratedDir.resolve(dslOutput.objc.filename)),
        outputFile = baseDir.resolve("main.exe"),
        additionalClangFlags = dslOutput.objc.args + listOf("-framework", dslOutput.kotlin.frameworkName),
        frameworkDirectories = listOf(baseDir),
        includeDirectories = listOf(baseDir.resolve("${dslOutput.kotlin.frameworkName}.framework").resolve("Headers"))
    ).assertSuccess()
    val testExecutable = TestExecutable(
        finalExecutable.resultingArtifact,
        finalExecutable.loggedData,
        listOf(TestName(testName))
    )
    runExecutableAndVerify(objcFrameworkTestCase, testExecutable)
}