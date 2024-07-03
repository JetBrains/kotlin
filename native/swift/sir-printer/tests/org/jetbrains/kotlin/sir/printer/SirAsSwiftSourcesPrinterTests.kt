/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.*
import org.jetbrains.kotlin.sir.providers.utils.updateImports
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
                    kind = SirCallableKind.FUNCTION
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
                    kind = SirCallableKind.FUNCTION
                    visibility = SirVisibility.PUBLIC
                    name = "foo1"
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    kind = SirCallableKind.FUNCTION
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
                    kind = SirCallableKind.FUNCTION
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
                    kind = SirCallableKind.FUNCTION
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
                    kind = SirCallableKind.FUNCTION
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
                            SirParameter(
                                argumentName = "arg8",
                                type = SirNominalType(SirSwiftModule.utf16CodeUnit)
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
                    kind = SirCallableKind.FUNCTION
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
    fun `should print static function`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    kind = SirCallableKind.STATIC_METHOD
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
    fun `should print class function`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    kind = SirCallableKind.CLASS_METHOD
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
            "testData/class_function"
        )
    }

    @Test
    fun `should print DocC comment on function`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    kind = SirCallableKind.FUNCTION
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

    @Test
    fun `should print DocC comment on class`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildClass {
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    name = "Foo"
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
            "testData/commented_class"
        )
    }

    @Test
    fun `should print DocC comment on namespaced class`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildEnum {
                    name = "NAMESPACE"
                    declarations += buildClass {
                        origin = SirOrigin.Unknown
                        visibility = SirVisibility.PUBLIC
                        name = "Foo"
                        documentation = """
                            /**
                             *  demo comment for
                             *  NAMESPACED_CLASS
                             */
                        """.trimIndent()
                    }
                }
            )
        }

        runTest(
            module,
            "testData/commented_namespaced_class"
        )
    }

    @Test
    fun `should print DocC comment on variable`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildVariable {
                    name = "myVariable"
                    type = SirNominalType(SirSwiftModule.bool)
                    getter = buildGetter {
                        kind = SirCallableKind.INSTANCE_METHOD
                    }
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
            "testData/commented_variable"
        )
    }

    @Test
    fun `should print empty class`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildClass {
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    name = "Foo"
                }
            )
        }

        runTest(
            module,
            "testData/empty_class"
        )
    }

    @Test
    fun `should print empty class inside enum`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildEnum {
                    origin = SirOrigin.Unknown
                    name = "MyEnum"

                    declarations.add(
                        buildClass {
                            origin = SirOrigin.Unknown
                            visibility = SirVisibility.PUBLIC
                            name = "Foo"
                        })
                })
        }

        runTest(
            module,
            "testData/empty_class_inside_enum"
        )
    }

    @Test
    fun `should print empty class inside class`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildClass {
                    origin = SirOrigin.Unknown
                    name = "OUTER_CLASS"

                    declarations.add(
                        buildClass {
                            origin = SirOrigin.Unknown
                            visibility = SirVisibility.PUBLIC
                            name = "INNER_CLASS"
                        })
                })
        }

        runTest(
            module,
            "testData/empty_class_inside_class"
        )
    }

    @Test
    fun `should print class with function`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildClass {
                    origin = SirOrigin.Unknown
                    name = "Foo"

                    declarations.add(
                        buildFunction {
                            origin = SirOrigin.Unknown
                            kind = SirCallableKind.INSTANCE_METHOD
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
                                    SirParameter(
                                        argumentName = "arg8",
                                        type = SirNominalType(SirSwiftModule.utf16CodeUnit)
                                    ),
                                )
                            )
                            returnType = SirNominalType(SirSwiftModule.bool)
                        }
                    )

                    declarations.add(
                        buildFunction {
                            origin = SirOrigin.Unknown
                            kind = SirCallableKind.INSTANCE_METHOD
                            visibility = SirVisibility.PUBLIC
                            name = "bar"
                            parameters.addAll(
                                listOf(
                                    SirParameter(
                                        argumentName = "arg1",
                                        type = SirNominalType(SirSwiftModule.uint8)
                                    ),
                                    SirParameter(
                                        argumentName = "arg2",
                                        type = SirNominalType(SirSwiftModule.uint16)
                                    ),
                                    SirParameter(
                                        argumentName = "arg3",
                                        type = SirNominalType(SirSwiftModule.uint32)
                                    ),
                                    SirParameter(
                                        argumentName = "arg4",
                                        type = SirNominalType(SirSwiftModule.uint64)
                                    ),
                                )
                            )
                            returnType = SirNominalType(SirSwiftModule.bool)
                        }
                    )
                }
            )
        }

        runTest(
            module,
            "testData/class_with_function"
        )
    }

    @Test
    fun `should print class with constructor`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildClass {
                    origin = SirOrigin.Unknown
                    name = "Foo"

                    declarations.add(
                        buildInit {
                            origin = SirOrigin.Unknown
                            kind = SirCallableKind.INSTANCE_METHOD
                            initKind = SirInitializerKind.ORDINARY
                            visibility = SirVisibility.PUBLIC
                            isFailable = true
                            isOverride = false
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
                                    SirParameter(
                                        argumentName = "arg8",
                                        type = SirNominalType(SirSwiftModule.utf16CodeUnit)
                                    ),
                                )
                            )
                        }
                    )

                    declarations.add(
                        buildInit {
                            origin = SirOrigin.Unknown
                            kind = SirCallableKind.INSTANCE_METHOD
                            initKind = SirInitializerKind.ORDINARY
                            visibility = SirVisibility.PUBLIC
                            isFailable = false
                            isOverride = false
                            parameters.addAll(
                                listOf(
                                    SirParameter(
                                        argumentName = "arg1",
                                        type = SirNominalType(SirSwiftModule.uint8)
                                    ),
                                    SirParameter(
                                        argumentName = "arg2",
                                        type = SirNominalType(SirSwiftModule.uint16)
                                    ),
                                    SirParameter(
                                        argumentName = "arg3",
                                        type = SirNominalType(SirSwiftModule.uint32)
                                    ),
                                    SirParameter(
                                        argumentName = "arg4",
                                        type = SirNominalType(SirSwiftModule.uint64)
                                    ),
                                )
                            )
                        }
                    )

                    declarations.add(
                        buildInit {
                            origin = SirOrigin.Unknown
                            kind = SirCallableKind.INSTANCE_METHOD
                            initKind = SirInitializerKind.REQUIRED
                            visibility = SirVisibility.PUBLIC
                            isFailable = false
                            isOverride = false
                            parameters.addAll(
                                listOf(
                                    SirParameter(
                                        argumentName = "arg1",
                                        type = SirNominalType(SirSwiftModule.uint8)
                                    ),
                                    SirParameter(
                                        argumentName = "arg2",
                                        type = SirNominalType(SirSwiftModule.uint16)
                                    ),
                                    SirParameter(
                                        argumentName = "arg3",
                                        type = SirNominalType(SirSwiftModule.uint32)
                                    ),
                                    SirParameter(
                                        argumentName = "arg4",
                                        type = SirNominalType(SirSwiftModule.uint64)
                                    ),
                                )
                            )
                        }
                    )

                    declarations.add(
                        buildInit {
                            origin = SirOrigin.Unknown
                            kind = SirCallableKind.INSTANCE_METHOD
                            initKind = SirInitializerKind.CONVENIENCE
                            visibility = SirVisibility.PUBLIC
                            isFailable = false
                            isOverride = false
                            parameters.addAll(
                                listOf(
                                    SirParameter(
                                        argumentName = "arg1",
                                        type = SirNominalType(SirSwiftModule.uint8)
                                    ),
                                    SirParameter(
                                        argumentName = "arg2",
                                        type = SirNominalType(SirSwiftModule.uint16)
                                    ),
                                    SirParameter(
                                        argumentName = "arg3",
                                        type = SirNominalType(SirSwiftModule.uint32)
                                    ),
                                    SirParameter(
                                        argumentName = "arg4",
                                        type = SirNominalType(SirSwiftModule.uint64)
                                    ),
                                )
                            )
                        }
                    )
                }
            )
        }

        runTest(
            module,
            "testData/class_with_init"
        )
    }

    @Test
    fun `should print class with variable`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildClass {
                    origin = SirOrigin.Unknown
                    name = "Foo"

                    declarations.add(
                        buildVariable {
                            name = "my_variable1"
                            type = SirNominalType(SirSwiftModule.bool)
                            getter = buildGetter {
                                kind = SirCallableKind.INSTANCE_METHOD
                            }
                        }
                    )

                    declarations.add(
                        buildVariable {
                            name = "my_variable2"
                            type = SirNominalType(SirSwiftModule.int8)
                            getter = buildGetter {
                                kind = SirCallableKind.INSTANCE_METHOD
                            }
                        }
                    )
                }
            )
        }

        runTest(
            module,
            "testData/class_with_variable"
        )
    }

    @Test
    fun `should print typealias`() {
        val sampleType = buildStruct {
            origin = SirOrigin.ExternallyDefined(name = "Baz.Bar")
            visibility = SirVisibility.PUBLIC
            name = "Bar"
        }

        val `typealias` = buildTypealias {
            origin = SirOrigin.Unknown
            name = "Foo"
            type = SirNominalType(sampleType)
        }

        val module = buildModule {
            name = "Test"
            declarations.add(`typealias`)
            declarations.add(sampleType)
        }.apply {
            `typealias`.parent = this
            sampleType.parent = this
        }

        runTest(
            module,
            "testData/typealias"
        )
    }

    @Test
    fun `should print extensions`() {

        val externalDefinedEnum: SirEnum
        val externalModule = buildModule {
            name = "MyDependencyModule"
            externalDefinedEnum = buildEnum {
                name = "my_external_enum"
                origin = SirOrigin.Unknown
            }
            declarations.add(
                externalDefinedEnum
            )
        }.apply {
            externalDefinedEnum.parent = this
        }

        val enum: SirEnum
        val module = buildModule {
            name = "Test"
            declarations.add(
                buildExtension {
                    origin = SirOrigin.Unknown

                    extendedType = SirNominalType(SirSwiftModule.int32)
                    visibility = SirVisibility.PUBLIC
                }
            )
            declarations.add(
                buildExtension {
                    origin = SirOrigin.Unknown

                    extendedType = SirNominalType(SirSwiftModule.int32)
                    visibility = SirVisibility.PRIVATE
                }
            )
            declarations.add(
                buildExtension {
                    origin = SirOrigin.Unknown

                    extendedType = SirNominalType(SirSwiftModule.int32)
                    visibility = SirVisibility.PUBLIC
                    documentation = """
                        ///
                        /// this is a documented extension 
                        /// (is it even possible? Printer don't actually care)
                        ///
                    """.trimIndent()
                }
            )
            declarations.add(
                buildExtension {
                    origin = SirOrigin.Unknown

                    extendedType = SirNominalType(SirSwiftModule.int32)
                    declarations.add(
                        buildClass {
                            name = "Foo"
                        }
                    )

                    declarations.add(
                        buildFunction {
                            origin = SirOrigin.Unknown
                            kind = SirCallableKind.FUNCTION
                            visibility = SirVisibility.PUBLIC
                            name = "foo"
                            returnType = SirNominalType(SirSwiftModule.bool)
                        }
                    )

                    declarations.add(
                        buildVariable {
                            name = "my_variable1"
                            type = SirNominalType(SirSwiftModule.bool)
                            getter = buildGetter {
                                kind = SirCallableKind.INSTANCE_METHOD
                            }
                        }
                    )
                }
            )

            enum = buildEnum {
                name = "my_enum"
                origin = SirOrigin.Namespace(listOf("my_enum"))
            }
            declarations.add(
                enum
            )
            declarations.add(
                buildExtension {
                    origin = SirOrigin.Unknown

                    extendedType = SirNominalType(enum)
                    declarations.add(
                        buildClass {
                            name = "Foo"
                        }
                    )
                }
            )

            declarations.add(
                buildExtension {
                    origin = SirOrigin.Unknown

                    extendedType = SirNominalType(externalDefinedEnum)
                    declarations.add(
                        buildClass {
                            name = "Foo"
                        }
                    )
                }
            )
        }.apply {
            enum.parent = this
        }

        runTest(
            module,
            "testData/extension"
        )
    }

    @Test
    fun `should print imports`() {
        val module = buildModule {
            name = "Test"
        }

        module.updateImports(
            listOf(
                SirImport(moduleName = "DEMO_PACKAGE"),
                SirImport(moduleName = "ExportedModule", mode = SirImport.Mode.Exported),
                SirImport(moduleName = "PrivateModule", mode = SirImport.Mode.ImplementationOnly),
            )
        )

        runTest(
            module,
            "testData/imports"
        )
    }

    private fun runTest(module: SirModule, goldenDataFile: String) {
        val expectedSwiftSrc = File(KtTestUtil.getHomeDirectory()).resolve("$goldenDataFile.golden.swift")
        val actualSwiftSrc = SirAsSwiftSourcesPrinter.print(
            module,
            stableDeclarationsOrder = false,
            renderDocComments = true,
            emptyBodyStub = SirFunctionBody(listOf("stub()"))
        )
        JUnit5Assertions.assertEqualsToFile(expectedSwiftSrc, actualSwiftSrc)
    }
}
