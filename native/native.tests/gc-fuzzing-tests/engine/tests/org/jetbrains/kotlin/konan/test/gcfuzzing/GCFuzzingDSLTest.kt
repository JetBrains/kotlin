/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.*
import org.jetbrains.kotlin.konan.test.gcfuzzing.execution.*
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

private fun assertFilesEqual(expected: java.io.File, actual: java.io.File) {
    JUnit5Assertions.assertEqualsToFile(
        expected, if (actual.isFile) actual.readText() else "", sanitizer = { it })
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

// These tests are not stress tests, they merely check the adequacy of generated code.
@EnforcedProperty(property = ClassLevelProperty.EXECUTION_TIMEOUT, propertyValue = "10m")
class GCFuzzingDSLTest : AbstractNativeSimpleTest() {
    private val testDataDir =
        ForTestCompileRuntime.transformTestDataPath("native/native.tests/gc-fuzzing-tests/engine/testData/gcFuzzingDSLTest").absoluteFile

    private fun runTest(name: String, program: Program) {
        val output = program.translate()
        val dslGeneratedDir = resolveDslDir(name)
        output.save(dslGeneratedDir)
        val goldenDataDir = testDataDir.resolve(name)
        assertDirectoriesEqual(goldenDataDir, dslGeneratedDir)
        runDSL(name, output, testRunSettings.get<Timeouts>().executionTimeout)
    }

    private inline fun runTest(testInfo: TestInfo, block: () -> Program) = runTest(testInfo.testMethod.get().name, block())

    @Test
    fun empty(testInfo: TestInfo) = runTest(testInfo) {
        Program(definitions = [], mainBody = Body(statements = []))
    }

    @Test
    fun noDefinitions(testInfo: TestInfo) = runTest(testInfo) {
        val storeExpressions = [
            StoreExpression.Global(0, Path([])),
            StoreExpression.Global(0, Path([0, 0, 0])),
            StoreExpression.Local(0, Path([])),
            StoreExpression.Local(0, Path([0, 0, 0])),
        ]
        val loadExpressions = [
            LoadExpression.Global(0, Path([])),
            LoadExpression.Global(0, Path([0, 0, 0])),
            LoadExpression.Local(0, Path([])),
            LoadExpression.Local(0, Path([0, 0, 0])),
        ]
        Program(
            definitions = [], mainBody = Body(
                statements = buildList {
                    storeExpressions.forEach { store ->
                        loadExpressions.forEach { load ->
                            add(BodyStatement.Store(store, load))
                        }
                    }
                    add(BodyStatement.SpawnThread(0, []))
                })
        )
    }

    @Test
    fun differentIds(testInfo: TestInfo) = runTest(testInfo) {
        val ids = [0, -1, 1, 511, Int.MIN_VALUE, Int.MAX_VALUE]
        val paths = [Path([]), Path(ids)]
        val loadExpressions = buildList {
            ids.forEach { id ->
                paths.forEach { path ->
                    add(LoadExpression.Global(id, path))
                    add(LoadExpression.Local(id, path))
                }
            }
        }
        val storeExpressions = buildList {
            ids.forEach { id ->
                paths.forEach { path ->
                    add(StoreExpression.Global(id, path))
                    add(StoreExpression.Local(id, path))
                }
            }
        }
        val argss = buildList {
            add([])
            loadExpressions.forEach {
                add([it])
            }
            add(loadExpressions)
        }
        val body = BodyWithReturn(
            body = Body(
                statements = buildList {
                    ids.forEach { id ->
                        argss.forEach { args ->
                            add(BodyStatement.Call(id, args))
                        }
                    }
                    ids.forEach { id ->
                        argss.forEach { args ->
                            add(BodyStatement.SpawnThread(id, args))
                        }
                    }
                    ids.forEach { id ->
                        argss.forEach { args ->
                            add(BodyStatement.Alloc(id, args))
                        }
                    }
                    loadExpressions.forEach { from ->
                        add(BodyStatement.Load(from))
                    }
                    storeExpressions.forEach { to ->
                        loadExpressions.forEach { from ->
                            add(BodyStatement.Store(to, from))
                        }
                    }
                }), returnExpression = LoadExpression.Default
        )
        Program(
            definitions = [
                Definition.Class(
                    TargetLanguage.Kotlin,
                    [Field.StrongRef, Field.StrongRef],
                ),
                Definition.Class(
                    TargetLanguage.ObjC,
                    [Field.StrongRef, Field.StrongRef],
                ),
                Definition.Global(
                    TargetLanguage.Kotlin,
                    Field.StrongRef,
                ),
                Definition.Global(
                    TargetLanguage.ObjC,
                    Field.StrongRef,
                ),
                Definition.Function(
                    TargetLanguage.Kotlin, parameters = [Parameter, Parameter], body = body
                ),
                Definition.Function(
                    TargetLanguage.ObjC, parameters = [Parameter, Parameter], body = body
                ),
            ],
            mainBody = Body(
                [
                    BodyStatement.Call(0, []),
                    BodyStatement.Call(1, []),
                ]
            ),
        )
    }

    @Test
    fun referenceLocals(testInfo: TestInfo) = runTest(testInfo) {
        fun local(id: EntityId) = LoadExpression.Local(id, Path([]))
        fun localL(id: EntityId) = StoreExpression.Local(id, Path([]))

        val bodies = [buildList {
            add(BodyStatement.Alloc(0, [local(0)]))
            add(BodyStatement.Alloc(0, [local(1)]))
            add(BodyStatement.Alloc(0, [local(3)]))
        }, buildList {
            add(BodyStatement.Call(0, [local(0)]))
            add(BodyStatement.Call(0, [local(1)]))
            add(BodyStatement.Call(0, [local(3)]))
        }, buildList {
            add(BodyStatement.Load(local(0)))
            add(BodyStatement.Load(local(1)))
            add(BodyStatement.Load(local(3)))
        }, buildList {
            add(BodyStatement.Load(LoadExpression.Default))
            add(BodyStatement.Store(localL(0), LoadExpression.Default))
            add(BodyStatement.Store(localL(1), LoadExpression.Default))
            add(BodyStatement.Store(localL(2), LoadExpression.Default))
        }]
        Program(
            definitions = listOf(
                Definition.Class(
                    TargetLanguage.Kotlin,
                    [Field.StrongRef],
                ), Definition.Global(
                    TargetLanguage.Kotlin,
                    Field.StrongRef,
                ), Definition.Global(
                    TargetLanguage.ObjC,
                    Field.StrongRef,
                ), *bodies.flatMap { statements ->
                    [
                        Definition.Function(
                            TargetLanguage.Kotlin, parameters = [Parameter], body = BodyWithReturn(
                                body = Body(statements), returnExpression = LoadExpression.Default
                            )
                        ),
                        Definition.Function(
                            TargetLanguage.ObjC, parameters = [Parameter], body = BodyWithReturn(
                                body = Body(statements), returnExpression = LoadExpression.Default
                            )
                        ),
                    ]
                }.toTypedArray()
            ),
            mainBody = Body(
                [
                    BodyStatement.Call(0, []),
                    BodyStatement.Call(1, []),
                ]
            ),
        )
    }

    @Test
    fun setFields(testInfo: TestInfo) = runTest(testInfo) {
        val setFieldsBody = Body(
            [
                BodyStatement.Alloc(0, []),
                BodyStatement.Alloc(1, []),
                BodyStatement.Store(StoreExpression.Local(0, Path([0])), LoadExpression.Local(1, Path([1]))),
                BodyStatement.Store(StoreExpression.Local(0, Path([1])), LoadExpression.Local(1, Path([0]))),
                BodyStatement.Store(StoreExpression.Local(1, Path([0])), LoadExpression.Local(0, Path([1]))),
                BodyStatement.Store(StoreExpression.Local(1, Path([1])), LoadExpression.Local(0, Path([0]))),
            ]
        )
        Program(
            definitions = [
                Definition.Class(
                    TargetLanguage.Kotlin,
                    [Field.StrongRef, Field.StrongRef],
                ),
                Definition.Class(
                    TargetLanguage.ObjC,
                    [Field.StrongRef, Field.StrongRef],
                ),
                Definition.Function(
                    TargetLanguage.Kotlin,
                    [],
                    body = BodyWithReturn(body = setFieldsBody, returnExpression = LoadExpression.Default)
                ),
                Definition.Function(
                    TargetLanguage.ObjC, [], body = BodyWithReturn(body = setFieldsBody, returnExpression = LoadExpression.Default)
                ),
            ], mainBody = Body(
                [
                    BodyStatement.Call(0, [LoadExpression.Default, LoadExpression.Default]),
                    BodyStatement.Call(1, [LoadExpression.Default, LoadExpression.Default])
                ]
            )
        )
    }

    @Test
    fun weakRefs(testInfo: TestInfo) = runTest(testInfo) {
        val testFunBody = Body(
            [
                BodyStatement.Alloc(
                    0, [
                        LoadExpression.Global(0, Path([])),
                        LoadExpression.Global(1, Path([])),
                    ]
                ),
                BodyStatement.Store(
                    StoreExpression.Local(0, Path([0])),
                    LoadExpression.Local(0, Path([1]))
                ),
                BodyStatement.Store(
                    StoreExpression.Local(0, Path([1])),
                    LoadExpression.Local(0, Path([0]))
                ),
                BodyStatement.Store(
                    StoreExpression.Local(1, Path([0])),
                    LoadExpression.Local(1, Path([1]))
                ),
                BodyStatement.Store(
                    StoreExpression.Local(1, Path([1])),
                    LoadExpression.Local(1, Path([0]))
                ),
            ]
        )
        Program(
            definitions = [
                Definition.Global(
                    TargetLanguage.Kotlin,
                    Field.WeakRef,
                ),
                Definition.Global(
                    TargetLanguage.ObjC,
                    Field.WeakRef,
                ),
                Definition.Class(
                    TargetLanguage.Kotlin,
                    [Field.StrongRef, Field.WeakRef],
                ),
                Definition.Class(
                    TargetLanguage.ObjC,
                    [Field.StrongRef, Field.WeakRef],
                ),
                Definition.Function(
                    TargetLanguage.Kotlin,
                    [],
                    body = BodyWithReturn(body = testFunBody, returnExpression = LoadExpression.Default)
                ),
                Definition.Function(
                    TargetLanguage.ObjC,
                    [],
                    body = BodyWithReturn(body = testFunBody, returnExpression = LoadExpression.Default)
                ),
            ], mainBody = Body(
                [
                    BodyStatement.Alloc(0, []),
                    BodyStatement.Store(
                        StoreExpression.Global(0, Path([])),
                        LoadExpression.Local(0, Path([]))
                    ),
                    BodyStatement.Store(
                        StoreExpression.Global(1, Path([])),
                        LoadExpression.Local(0, Path([]))
                    ),
                    BodyStatement.Call(0, [LoadExpression.Default, LoadExpression.Default]),
                    BodyStatement.Call(1, [LoadExpression.Default, LoadExpression.Default])
                ]
            )
        )
    }

    @Test
    fun smoke(testInfo: TestInfo) = runTest(testInfo) {
        Program(
            definitions = [
                Definition.Class(
                    TargetLanguage.Kotlin,
                    [Field.StrongRef, Field.StrongRef],
                ),
                Definition.Class(
                    TargetLanguage.ObjC,
                    [Field.StrongRef, Field.StrongRef],
                ),
                Definition.Global(
                    TargetLanguage.Kotlin,
                    Field.StrongRef,
                ),
                Definition.Global(
                    TargetLanguage.ObjC,
                    Field.StrongRef,
                ),
                Definition.Function(
                    TargetLanguage.Kotlin, [Parameter, Parameter], BodyWithReturn(
                        body = Body(
                            [
                                BodyStatement.Store(
                                    StoreExpression.Global(0, Path([])),
                                    LoadExpression.Local(0, Path([])),
                                ),
                                BodyStatement.Store(
                                    StoreExpression.Local(1, Path([0])),
                                    LoadExpression.Global(0, Path([1])),
                                ),
                                BodyStatement.SpawnThread(2, [LoadExpression.Local(1, Path([]))]),
                            ]
                        ), returnExpression = LoadExpression.Default
                    )
                ),
                Definition.Function(
                    TargetLanguage.ObjC, [Parameter, Parameter], BodyWithReturn(
                        body = Body(
                            [
                                BodyStatement.Store(
                                    StoreExpression.Global(0, Path([])),
                                    LoadExpression.Local(0, Path([])),
                                ),
                                BodyStatement.Store(
                                    StoreExpression.Local(1, Path([0])),
                                    LoadExpression.Global(0, Path([1])),
                                ),
                                BodyStatement.SpawnThread(3, [LoadExpression.Local(1, Path([]))]),
                            ]
                        ), returnExpression = LoadExpression.Default
                    )
                ),
                Definition.Function(
                    TargetLanguage.Kotlin, [Parameter], BodyWithReturn(
                        body = Body(
                            [
                                BodyStatement.Alloc(0, []),
                                BodyStatement.Alloc(1, []),
                                BodyStatement.Call(3, [LoadExpression.Local(0, Path([]))])
                            ]
                        ), returnExpression = LoadExpression.Local(5, Path([1, 3, 4]))
                    )
                ),
                Definition.Function(
                    TargetLanguage.ObjC, [Parameter], BodyWithReturn(
                        body = Body(
                            [
                                BodyStatement.Alloc(0, []),
                                BodyStatement.Alloc(1, []),
                                BodyStatement.Call(2, [LoadExpression.Local(0, Path([]))])
                            ]
                        ), returnExpression = LoadExpression.Local(5, Path([1, 3, 4]))
                    )
                ),
            ], mainBody = Body(
                [
                    BodyStatement.Alloc(123, []), BodyStatement.Load(
                    LoadExpression.Local(0, Path([])),
                ), BodyStatement.Call(
                    65, [
                        LoadExpression.Local(0, Path([])),
                        LoadExpression.Local(1, Path([67])),
                    ]
                ), BodyStatement.Call(
                    6, []
                )
                ]
            )
        )
    }

    @Test
    fun oom(testInfo: TestInfo) = runTest(testInfo) {
        val fieldsCount = 10
        val listsLengthCount = 100
        fun defineNode(targetLanguage: TargetLanguage) = Definition.Class(targetLanguage, Array(fieldsCount) { Field.StrongRef }.toList())
        fun populateNode(classId: EntityId) = buildList {
            (0 until fieldsCount).forEach {
                add(BodyStatement.Alloc(classId, []))
                add(BodyStatement.Store(StoreExpression.Local(0, Path([it])), LoadExpression.Local(it + 1, Path([]))))
            }
        }

        fun definePopulateNodeAndReturnNext(targetLanguage: TargetLanguage, classId: EntityId) = Definition.Function(
            targetLanguage, parameters = [
                Parameter
            ], body = BodyWithReturn(Body(populateNode(classId)), returnExpression = LoadExpression.Local(0, Path([fieldsCount - 1])))
        )

        Program(
            definitions = [
                defineNode(TargetLanguage.Kotlin),
                defineNode(TargetLanguage.ObjC),
                definePopulateNodeAndReturnNext(TargetLanguage.Kotlin, 0),
                definePopulateNodeAndReturnNext(TargetLanguage.ObjC, 1),
            ],
            mainBody = Body(
                buildList {
                    add(BodyStatement.Alloc(0, []))
                    add(BodyStatement.Alloc(1, []))
                    (0 until listsLengthCount).forEach {
                        add(BodyStatement.Call(0, [LoadExpression.Local(2 * it, Path([]))]))
                        add(BodyStatement.Call(1, [LoadExpression.Local(2 * it + 1, Path([]))]))
                    }
                }
            )
        )
    }
}
