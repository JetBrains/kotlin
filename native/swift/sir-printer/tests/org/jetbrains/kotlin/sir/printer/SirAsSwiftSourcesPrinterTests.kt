/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import org.junit.jupiter.api.Test
import java.io.File

class SirAsSwiftSourcesPrinterTests {

    @Test
    fun `should print simple function`() {
        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = false
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
        }

        runTest(
            module,
            "testData/simple_function"
        )
    }

    @Test
    fun `should print multiple functions`() {
        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = false
                    visibility = SirVisibility.PUBLIC
                    name = "foo1"
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = false
                    visibility = SirVisibility.PUBLIC
                    name = "foo2"
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
        }

        runTest(
            module,
            "testData/simple_multiple_function"
        )
    }

    @Test
    fun `should print single argument`() {
        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = false
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    parameters.add(
                        SirParameter(
                            argumentName = "arg1",
                            type = SirNominalType(SirSwiftModule.int32)
                        )
                    )
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
        }

        runTest(
            module,
            "testData/single_argument"
        )
    }

    @Test
    fun `should print two argument`() {
        val module = buildModule {
            name = "Test"

            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = false
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    parameters.add(
                        SirParameter(
                            argumentName = "arg1",
                            type = SirNominalType(SirSwiftModule.int32)
                        )
                    )
                    parameters.add(
                        SirParameter(
                            argumentName = "arg2",
                            type = SirNominalType(SirSwiftModule.double)
                        )
                    )
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
        }

        runTest(
            module,
            "testData/two_arguments"
        )
    }

    @Test
    fun `should all types as parameter be handled`() {
        val module = buildModule {
            name = "Test"

            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = false
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    parameters.addAll(
                        listOf(
                            SirParameter(
                                argumentName = "arg1",
                                type = SirNominalType(SirSwiftModule.bool)
                            ),
                            SirParameter(
                                argumentName = "arg2",
                                type = SirNominalType(SirSwiftModule.int8)
                            ),
                            SirParameter(
                                argumentName = "arg3",
                                type = SirNominalType(SirSwiftModule.int16)
                            ),
                            SirParameter(
                                argumentName = "arg4",
                                type = SirNominalType(SirSwiftModule.int32)
                            ),
                            SirParameter(
                                argumentName = "arg5",
                                type = SirNominalType(SirSwiftModule.int64)
                            ),
                            SirParameter(
                                argumentName = "arg6",
                                type = SirNominalType(SirSwiftModule.double)
                            ),
                            SirParameter(
                                argumentName = "arg7",
                                type = SirNominalType(SirSwiftModule.float)
                            ),
                        )
                    )
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
        }

        runTest(
            module,
            "testData/all_types_argument"
        )
    }

    @Test
    fun `should print non-empty bodies`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = false
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    parameters.add(
                        SirParameter(
                            argumentName = "arg1",
                            type = SirNominalType(SirSwiftModule.int32)
                        )
                    )
                    returnType = SirNominalType(SirSwiftModule.bool)
                    body = SirFunctionBody(listOf("return foo_wrapped(arg1)"))
                }
            )
        }

        runTest(
            module,
            "testData/non_empty_body"
        )
    }

    @Test
    fun `should print static`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = true
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    parameters.add(
                        SirParameter(
                            argumentName = "arg1",
                            type = SirNominalType(SirSwiftModule.int32)
                        )
                    )
                    returnType = SirNominalType(SirSwiftModule.bool)
                    body = SirFunctionBody(listOf("return foo_wrapped(arg1)"))
                }
            )
        }

        runTest(
            module,
            "testData/static_function"
        )
    }

    @Test
    fun `should print DocC comment`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    isStatic = false
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    parameters.add(
                        SirParameter(
                            argumentName = "p",
                            type = SirNominalType(SirSwiftModule.int64)
                        )
                    )
                    returnType = SirNominalType(SirSwiftModule.bool)
                    documentation = """
                            /// Function foo description.
                            /// - Parameters:
                            ///   - p: first Integer to consume
                            /// - Returns: Bool
                        """.trimIndent()
                }
            )
        }

        runTest(
            module,
            "testData/commented_function"
        )
    }

    private fun runTest(module: SirModule, goldenDataFile: String) {
        val expectedSwiftSrc = File(KtTestUtil.getHomeDirectory()).resolve("$goldenDataFile.golden.swift")

        val actualSwiftSrc = SirAsSwiftSourcesPrinter().print(module)
        JUnit5Assertions.assertEqualsToFile(expectedSwiftSrc, actualSwiftSrc)
    }
}