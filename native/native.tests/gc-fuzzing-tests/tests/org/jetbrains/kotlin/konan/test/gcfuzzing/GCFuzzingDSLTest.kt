/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing

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
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

private fun assertFilesEqual(expected: java.io.File, actual: java.io.File) {
    JUnit5Assertions.assertEqualsToFile(
        expected,
        if (actual.isFile) actual.readText() else "",
        sanitizer = { it }
    )
}

private fun assertDirectoriesEqual(expected: java.io.File, actual: java.io.File) {
    val expectedListing = expected.list().orEmpty().toSet()
    val actualListing = actual.list().orEmpty().toSet()
    (actualListing + expectedListing).forEach { path ->
        val expectedFile = java.io.File(expected, path)
        val actualFile = java.io.File(actual, path)
        when {
            expectedFile.isDirectory -> assertDirectoriesEqual(expectedFile, actualFile)
            else -> assertFilesEqual(expectedFile, actualFile)
        }
    }
}

class GCFuzzingDSLTest : AbstractNativeSimpleTest() {
    private val testDataDir = java.io.File(System.getProperty("kotlin.internal.native.test.testDataDir")).resolve("gcFuzzingDSLTest")
    private val testCompilationFactory = TestCompilationFactory()

    private fun runTest(name: String, program: Program) {
        val output = program.translate()
        val generatedDir = buildDir.resolve("generated").also { root ->
            root.mkdirs()
            output.forEach {
                root.resolve(it.filename).writeText(it.contents)
            }
        }
        val goldenDataDir = testDataDir.resolve(name)
        assertDirectoriesEqual(goldenDataDir, generatedDir)
        val cinterop = cinteropToLibrary(
            testRunSettings.get<KotlinNativeTargets>(),
            generatedDir.resolve(output.find { it.kind == FileKind.DEF }!!.filename),
            buildDir.resolve("cinterop").also { it.mkdirs() },
            freeCompilerArgs = TestCompilerArgs.EMPTY,
        ).assertSuccess()
        val objcFrameworkTestCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR,
            TestCase.NoTestRunnerExtras(),
            "KotlinObjCFramework",
            listOf(generatedDir.resolve(output.find { it.kind == FileKind.KOTLIN }!!.filename)),
            TestCompilerArgs("-Xstatic-framework", "-Xbinary=bundleId=KotlinObjCFramework"),
            setOf(TestModule.Given(cinterop.resultingArtifact.klibFile)),
        )
        val objCFramework = testCompilationFactory.testCaseToObjCFrameworkCompilation(objcFrameworkTestCase, testRunSettings).result.assertSuccess()
        codesign(objCFramework.resultingArtifact.frameworkDir.absolutePath)
        val finalExecutable = compileWithClang(
            sourceFiles = listOf(generatedDir.resolve(output.find { it.kind == FileKind.OBJC_SOURCE }!!.filename)),
            outputFile = buildDir.resolve("main.exe"),
            additionalClangFlags = listOf("-fobjc-arc", "-framework", "KotlinObjCFramework"),
            frameworkDirectories = listOf(buildDir),
            includeDirectories = listOf(buildDir.resolve("KotlinObjCFramework.framework").resolve("Headers"))
        ).assertSuccess()
        val testExecutable = TestExecutable(
            finalExecutable.resultingArtifact,
            finalExecutable.loggedData,
            listOf(TestName(name))
        )
        runExecutableAndVerify(objcFrameworkTestCase, testExecutable)
    }

    private inline fun runTest(testInfo: TestInfo, block: () -> Program) = runTest(testInfo.testMethod.get().name, block())

    @Test
    fun smoke(testInfo: TestInfo) = runTest(testInfo) {
        Program(
            definitions = listOf(
                Definition.Class(
                    TargetLanguage.Kotlin,
                    listOf(Field, Field),
                ),
                Definition.Class(
                    TargetLanguage.ObjC,
                    listOf(Field, Field),
                ),
                Definition.Global(
                    TargetLanguage.Kotlin,
                    Field,
                ),
                Definition.Global(
                    TargetLanguage.ObjC,
                    Field,
                ),
                Definition.Function(
                    TargetLanguage.Kotlin,
                    listOf(Parameter, Parameter),
                    BodyWithReturn(
                        body = Body(
                            listOf(
                                BodyStatement.Store(
                                    StoreExpression.Global(0, Path(listOf())),
                                    LoadExpression.Local(0, Path(listOf())),
                                ),
                                BodyStatement.Store(
                                    StoreExpression.Local(1, Path(listOf(0))),
                                    LoadExpression.Global(0, Path(listOf(1))),
                                ),
                                BodyStatement.SpawnThread(2, listOf(LoadExpression.Local(1, Path(listOf())))),
                            )
                        ),
                        returnExpression = LoadExpression.Default
                    )
                ),
                Definition.Function(
                    TargetLanguage.ObjC,
                    listOf(Parameter, Parameter),
                    BodyWithReturn(
                        body = Body(
                            listOf(
                                BodyStatement.Store(
                                    StoreExpression.Global(0, Path(listOf())),
                                    LoadExpression.Local(0, Path(listOf())),
                                ),
                                BodyStatement.Store(
                                    StoreExpression.Local(1, Path(listOf(0))),
                                    LoadExpression.Global(0, Path(listOf(1))),
                                ),
                                BodyStatement.SpawnThread(3, listOf(LoadExpression.Local(1, Path(listOf())))),
                            )
                        ),
                        returnExpression = LoadExpression.Default
                    )
                ),
                Definition.Function(
                    TargetLanguage.Kotlin,
                    listOf(Parameter),
                    BodyWithReturn(
                        body = Body(
                            listOf(
                                BodyStatement.Alloc(0, listOf()),
                                BodyStatement.Alloc(1, listOf()),
                                BodyStatement.Call(3, listOf(LoadExpression.Local(0, Path(listOf()))))
                            )
                        ),
                        returnExpression = LoadExpression.Local(5, Path(listOf(1, 3, 4)))
                    )
                ),
                Definition.Function(
                    TargetLanguage.ObjC,
                    listOf(Parameter),
                    BodyWithReturn(
                        body = Body(
                            listOf(
                                BodyStatement.Alloc(0, listOf()),
                                BodyStatement.Alloc(1, listOf()),
                                BodyStatement.Call(2, listOf(LoadExpression.Local(0, Path(listOf()))))
                            )
                        ),
                        returnExpression = LoadExpression.Local(5, Path(listOf(1, 3, 4)))
                    )
                ),
            ),
            mainBody = Body(
                listOf(
                    BodyStatement.Alloc(123, listOf()),
                    BodyStatement.Load(
                        LoadExpression.Local(0, Path(listOf())),
                    ),
                    BodyStatement.Call(
                        65, listOf(
                            LoadExpression.Local(0, Path(listOf())),
                            LoadExpression.Local(1, Path(listOf(67))),
                        )
                    )
                )
            )
        )
    }
}