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
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.cinteropArgs
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.defFilename
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.kotlinFilename
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.kotlinFrameworkArgs
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.kotlinFrameworkName
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.objcSourceArgs
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.objcSourceFilename
import org.junit.jupiter.api.Assumptions
import java.io.File
import kotlin.time.Duration

val AbstractNativeSimpleTest.dslGeneratedDir: File
    get() = buildDir.resolve("generated")

fun AbstractNativeSimpleTest.runDSL(
    testName: String,
    dslOutput: Output,
    executionTimeout: Duration
) {
    Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().hostTarget.family.isAppleFamily)
    val cinterop = cinteropToLibrary(
        testRunSettings.get<KotlinNativeTargets>(),
        dslGeneratedDir.resolve(dslOutput.defFilename),
        buildDir,
        freeCompilerArgs = TestCompilerArgs(compilerArgs = emptyList(), cinteropArgs = dslOutput.cinteropArgs),
    ).assertSuccess()
    val objcFrameworkTestCase = generateObjCFrameworkTestCase(
        TestKind.STANDALONE_NO_TR,
        TestCase.NoTestRunnerExtras(),
        dslOutput.kotlinFrameworkName,
        listOf(dslGeneratedDir.resolve(dslOutput.kotlinFilename)),
        TestCompilerArgs(dslOutput.kotlinFrameworkArgs),
        setOf(TestModule.Given(cinterop.resultingArtifact.klibFile)),
        checks = TestRunChecks(
            executionTimeoutCheck = ExecutionTimeout.MayExceed(executionTimeout),
            testFiltering = TestFiltering(TestOutputFilter.NO_FILTERING),
            exitCodeCheck = ExitCode.Expected(0),
            outputDataFile = null,
            outputMatcher = null,
            fileCheckMatcher = null,
        )
    )
    val objCFramework = TestCompilationFactory().testCaseToObjCFrameworkCompilation(objcFrameworkTestCase, testRunSettings).result.assertSuccess()
    codesign(objCFramework.resultingArtifact.frameworkDir.absolutePath)
    val finalExecutable = compileWithClang(
        sourceFiles = listOf(dslGeneratedDir.resolve(dslOutput.objcSourceFilename)),
        outputFile = buildDir.resolve("main.exe"),
        additionalClangFlags = dslOutput.objcSourceArgs + listOf("-framework", dslOutput.kotlinFrameworkName),
        frameworkDirectories = listOf(buildDir),
        includeDirectories = listOf(buildDir.resolve("${dslOutput.kotlinFrameworkName}.framework").resolve("Headers"))
    ).assertSuccess()
    val testExecutable = TestExecutable(
        finalExecutable.resultingArtifact,
        finalExecutable.loggedData,
        listOf(TestName(testName))
    )
    runExecutableAndVerify(objcFrameworkTestCase, testExecutable)
}