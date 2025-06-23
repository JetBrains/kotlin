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

// These tests are not stress tests, they merely check the adequacy of generated code. Limit their execution time, these tests tolerate timeouts.
@EnforcedProperty(property = ClassLevelProperty.EXECUTION_TIMEOUT, propertyValue = "1m")
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
        Program(definitions = emptyList(), mainBody = Body(statements = emptyList()))
    }

    @Test
    fun noDefinitions(testInfo: TestInfo) = runTest(testInfo) {
        val storeExpressions = listOf(
            StoreExpression.Global(0, Path(emptyList())),
            StoreExpression.Global(0, Path(listOf(0, 0, 0))),
            StoreExpression.Local(0, Path(emptyList())),
            StoreExpression.Local(0, Path(listOf(0, 0, 0))),
        )
        val loadExpressions = listOf(
            LoadExpression.Global(0, Path(emptyList())),
            LoadExpression.Global(0, Path(listOf(0, 0, 0))),
            LoadExpression.Local(0, Path(emptyList())),
            LoadExpression.Local(0, Path(listOf(0, 0, 0))),
        )
        Program(
            definitions = emptyList(), mainBody = Body(
                statements = buildList {
                    storeExpressions.forEach { store ->
                        loadExpressions.forEach { load ->
                            add(BodyStatement.Store(store, load))
                        }
                    }
                    add(BodyStatement.SpawnThread(0, emptyList()))
                })
        )
    }

    @Test
    fun differentIds(testInfo: TestInfo) = runTest(testInfo) {
        val ids = listOf(0, -1, 1, 511, Int.MIN_VALUE, Int.MAX_VALUE)
        val paths = listOf(Path(emptyList()), Path(ids))
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
            add(emptyList())
            loadExpressions.forEach {
                add(listOf(it))
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
                    TargetLanguage.Kotlin, parameters = listOf(Parameter, Parameter), body = body
                ),
                Definition.Function(
                    TargetLanguage.ObjC, parameters = listOf(Parameter, Parameter), body = body
                ),
            ),
            mainBody = Body(
                listOf(
                    BodyStatement.Call(0, emptyList()),
                    BodyStatement.Call(1, emptyList()),
                )
            ),
        )
    }

    @Test
    fun referenceLocals(testInfo: TestInfo) = runTest(testInfo) {
        fun local(id: EntityId) = LoadExpression.Local(id, Path(emptyList()))
        fun localL(id: EntityId) = StoreExpression.Local(id, Path(emptyList()))

        val bodies = listOf(buildList {
            add(BodyStatement.Alloc(0, listOf(local(0))))
            add(BodyStatement.Alloc(0, listOf(local(1))))
            add(BodyStatement.Alloc(0, listOf(local(3))))
        }, buildList {
            add(BodyStatement.Call(0, listOf(local(0))))
            add(BodyStatement.Call(0, listOf(local(1))))
            add(BodyStatement.Call(0, listOf(local(3))))
        }, buildList {
            add(BodyStatement.Load(local(0)))
            add(BodyStatement.Load(local(1)))
            add(BodyStatement.Load(local(3)))
        }, buildList {
            add(BodyStatement.Load(LoadExpression.Default))
            add(BodyStatement.Store(localL(0), LoadExpression.Default))
            add(BodyStatement.Store(localL(1), LoadExpression.Default))
            add(BodyStatement.Store(localL(2), LoadExpression.Default))
        })
        Program(
            definitions = listOf(
                Definition.Class(
                    TargetLanguage.Kotlin,
                    listOf(Field),
                ), Definition.Global(
                    TargetLanguage.Kotlin,
                    Field,
                ), Definition.Global(
                    TargetLanguage.ObjC,
                    Field,
                ), *bodies.flatMap { statements ->
                    listOf(
                        Definition.Function(
                            TargetLanguage.Kotlin, parameters = listOf(Parameter), body = BodyWithReturn(
                                body = Body(statements), returnExpression = LoadExpression.Default
                            )
                        ),
                        Definition.Function(
                            TargetLanguage.ObjC, parameters = listOf(Parameter), body = BodyWithReturn(
                                body = Body(statements), returnExpression = LoadExpression.Default
                            )
                        ),
                    )
                }.toTypedArray()
            ),
            mainBody = Body(
                listOf(
                    BodyStatement.Call(0, emptyList()),
                    BodyStatement.Call(1, emptyList()),
                )
            ),
        )
    }

    @Test
    fun setFields(testInfo: TestInfo) = runTest(testInfo) {
        val setFieldsBody = Body(
            listOf(
                BodyStatement.Alloc(0, emptyList()),
                BodyStatement.Alloc(1, emptyList()),
                BodyStatement.Store(StoreExpression.Local(0, Path(listOf(0))), LoadExpression.Local(1, Path(listOf(1)))),
                BodyStatement.Store(StoreExpression.Local(0, Path(listOf(1))), LoadExpression.Local(1, Path(listOf(0)))),
                BodyStatement.Store(StoreExpression.Local(1, Path(listOf(0))), LoadExpression.Local(0, Path(listOf(1)))),
                BodyStatement.Store(StoreExpression.Local(1, Path(listOf(1))), LoadExpression.Local(0, Path(listOf(0)))),
            )
        )
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
                Definition.Function(
                    TargetLanguage.Kotlin,
                    emptyList(),
                    body = BodyWithReturn(body = setFieldsBody, returnExpression = LoadExpression.Default)
                ),
                Definition.Function(
                    TargetLanguage.ObjC, emptyList(), body = BodyWithReturn(body = setFieldsBody, returnExpression = LoadExpression.Default)
                ),
            ), mainBody = Body(
                listOf(
                    BodyStatement.Call(0, listOf(LoadExpression.Default, LoadExpression.Default)),
                    BodyStatement.Call(1, listOf(LoadExpression.Default, LoadExpression.Default))
                )
            )
        )
    }

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
                    TargetLanguage.Kotlin, listOf(Parameter, Parameter), BodyWithReturn(
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
                        ), returnExpression = LoadExpression.Default
                    )
                ),
                Definition.Function(
                    TargetLanguage.ObjC, listOf(Parameter, Parameter), BodyWithReturn(
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
                        ), returnExpression = LoadExpression.Default
                    )
                ),
                Definition.Function(
                    TargetLanguage.Kotlin, listOf(Parameter), BodyWithReturn(
                        body = Body(
                            listOf(
                                BodyStatement.Alloc(0, listOf()),
                                BodyStatement.Alloc(1, listOf()),
                                BodyStatement.Call(3, listOf(LoadExpression.Local(0, Path(listOf()))))
                            )
                        ), returnExpression = LoadExpression.Local(5, Path(listOf(1, 3, 4)))
                    )
                ),
                Definition.Function(
                    TargetLanguage.ObjC, listOf(Parameter), BodyWithReturn(
                        body = Body(
                            listOf(
                                BodyStatement.Alloc(0, listOf()),
                                BodyStatement.Alloc(1, listOf()),
                                BodyStatement.Call(2, listOf(LoadExpression.Local(0, Path(listOf()))))
                            )
                        ), returnExpression = LoadExpression.Local(5, Path(listOf(1, 3, 4)))
                    )
                ),
            ), mainBody = Body(
                listOf(
                    BodyStatement.Alloc(123, listOf()), BodyStatement.Load(
                        LoadExpression.Local(0, Path(listOf())),
                    ), BodyStatement.Call(
                        65, listOf(
                            LoadExpression.Local(0, Path(listOf())),
                            LoadExpression.Local(1, Path(listOf(67))),
                        )
                    ), BodyStatement.Call(
                        6, listOf()
                    )
                )
            )
        )
    }

    @Test
    fun oom(testInfo: TestInfo) = runTest(testInfo) {
        val fieldsCount = 10
        val listsLengthCount = 100
        fun defineNode(targetLanguage: TargetLanguage) = Definition.Class(targetLanguage, Array(fieldsCount) { Field }.toList())
        fun populateNode(classId: EntityId) = buildList {
            (0 until fieldsCount).forEach {
                add(BodyStatement.Alloc(classId, emptyList()))
                add(BodyStatement.Store(StoreExpression.Local(0, Path(listOf(it))), LoadExpression.Local(it + 1, Path(emptyList()))))
            }
        }

        fun definePopulateNodeAndReturnNext(targetLanguage: TargetLanguage, classId: EntityId) = Definition.Function(
            targetLanguage, parameters = listOf(
                Parameter
            ), body = BodyWithReturn(Body(populateNode(classId)), returnExpression = LoadExpression.Local(0, Path(listOf(fieldsCount - 1))))
        )

        Program(
            definitions = listOf(
                defineNode(TargetLanguage.Kotlin),
                defineNode(TargetLanguage.ObjC),
                definePopulateNodeAndReturnNext(TargetLanguage.Kotlin, 0),
                definePopulateNodeAndReturnNext(TargetLanguage.ObjC, 1),
            ),
            mainBody = Body(
                buildList {
                    add(BodyStatement.Alloc(0, emptyList()))
                    add(BodyStatement.Alloc(1, emptyList()))
                    (0 until listsLengthCount).forEach {
                        add(BodyStatement.Call(0, listOf(LoadExpression.Local(2 * it, Path(emptyList())))))
                        add(BodyStatement.Call(1, listOf(LoadExpression.Local(2 * it + 1, Path(emptyList())))))
                    }
                }
            )
        )
    }
}