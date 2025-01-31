/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.printer

import com.intellij.util.containers.addAllIfNotNull
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.*
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.addChild
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
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
        }.attachDeclarations()

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
                    visibility = SirVisibility.PUBLIC
                    name = "foo1"
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    name = "foo2"
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
        }.attachDeclarations()

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
        }.attachDeclarations()

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
        }.attachDeclarations()

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
                            SirParameter(
                                argumentName = "arg9",
                                type = SirNominalType(SirSwiftModule.bool).optional()
                            ),
                        )
                    )
                    returnType = SirNominalType(SirSwiftModule.bool)
                }
            )
        }.attachDeclarations()

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
        }.attachDeclarations()

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
                buildClass {
                    name = "Foo"
                    declarations.add(
                        buildFunction {
                            origin = SirOrigin.Unknown
                            isInstance = false
                            modality = SirModality.FINAL
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
                }.attachDeclarations()
            )
        }.attachDeclarations()

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
                buildClass {
                    name = "Foo"
                    declarations.add(
                        buildFunction {
                            origin = SirOrigin.Unknown
                            isInstance = false
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
                }.attachDeclarations()
            )
        }.attachDeclarations()

        runTest(
            module,
            "testData/class_function"
        )
    }

    @Test
    fun `should print throwing functions`() {
        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    name = "nothrow"
                    returnType = SirType.void
                    errorType = SirType.never
                }
            )
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    name = "throwsAny"
                    returnType = SirType.void
                    errorType = SirType.any
                }
            )
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    name = "throwsVoid"
                    returnType = SirType.void
                    errorType = SirType.void
                }
            )
        }.attachDeclarations()

        runTest(
            module,
            "testData/throwing_functions"
        )
    }

    @Test
    fun `should print throwing inits`() {
        val module = buildModule {
            name = "Test"
            declarations.add(
                buildStruct {
                    origin = SirOrigin.Unknown
                    name = "Foo"

                    declarations.add(
                        buildInit {
                            origin = SirOrigin.Unknown
                            visibility = SirVisibility.PUBLIC
                            isFailable = false
                            errorType = SirType.never
                        }
                    )
                    declarations.add(
                        buildInit {
                            origin = SirOrigin.Unknown
                            visibility = SirVisibility.PUBLIC
                            isFailable = false
                            errorType = SirType.any
                        }
                    )
                    declarations.add(
                        buildInit {
                            origin = SirOrigin.Unknown
                            visibility = SirVisibility.PUBLIC
                            isFailable = false
                            errorType = SirType.void
                        }
                    )
                }
            )
        }.attachDeclarations()

        runTest(
            module,
            "testData/throwing_inits"
        )
    }

    @Test
    fun `should print throwing accessors`() {
        val module = buildModule {
            name = "Test"
            declarations.add(
                buildVariable {
                    origin = SirOrigin.Unknown
                    name = "nonThrowing"
                    type = SirType.void
                    getter = buildGetter {
                        errorType = SirType.never
                    }
                    setter = buildSetter {
                        errorType = SirType.never
                    }

                }
            )
            declarations.add(
                buildVariable {
                    origin = SirOrigin.Unknown
                    name = "throwingAny"
                    type = SirType.void
                    getter = buildGetter {
                        errorType = SirType.any
                    }
                    setter = buildSetter {
                        errorType = SirType.any
                    }
                }
            )
            declarations.add(
                buildVariable {
                    origin = SirOrigin.Unknown
                    name = "throwingVoid"
                    type = SirType.void
                    getter = buildGetter {
                        errorType = SirType.void
                    }
                    setter = buildSetter {
                        errorType = SirType.void
                    }
                }
            )
        }.attachDeclarations()

        runTest(
            module,
            "testData/throwing_accessors"
        )
    }

    @Test
    fun `should print DocC comment on function`() {

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
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
        }.attachDeclarations()

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
                    getter = buildGetter {}
                    documentation = """
                            /// Function foo description.
                            /// - Parameters:
                            ///   - p: first Integer to consume
                            /// - Returns: Bool
                        """.trimIndent()
                }
            )
        }.attachDeclarations()

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
                }.attachDeclarations()
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
                            isRequired = true
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
                            isConvenience = true
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
                            getter = buildGetter {}
                        }
                    )

                    declarations.add(
                        buildVariable {
                            name = "my_variable2"
                            type = SirNominalType(SirSwiftModule.int8)
                            getter = buildGetter {}
                        }
                    )
                    declarations.add(
                        buildVariable {
                            name = "my_variable3"
                            type = SirNominalType(
                                SirSwiftModule.int32,
                            ).optional()
                            getter = buildGetter {}
                        }
                    )
                }.attachDeclarations()
            )
        }.attachDeclarations()

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

            declarations.add(
                buildTypealias {
                    origin = SirOrigin.Unknown
                    name = "OptionalInt"
                    type = SirNominalType(
                        SirSwiftModule.int32,
                    ).optional()
                }
            )
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
        }.attachDeclarations()

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
                            visibility = SirVisibility.PUBLIC
                            name = "foo"
                            returnType = SirNominalType(SirSwiftModule.bool)
                        }
                    )

                    declarations.add(
                        buildVariable {
                            name = "my_variable1"
                            type = SirNominalType(SirSwiftModule.bool)
                            getter = buildGetter {}
                        }
                    )
                }.attachDeclarations()
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
                }.attachDeclarations()
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
                }.attachDeclarations()
            )
        }.apply {
            enum.parent = this
        }.attachDeclarations()

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

        module.imports.addAll(
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

    @Test
    fun `should elide extra visibility modifiers when modality spedified`() {
        val module = buildModule {
            name = "Test"

            declarations.addAllIfNotNull(
                buildClass {
                    name = "OPEN_PUBLIC"
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    modality = SirModality.OPEN
                },
                buildClass {
                    name = "FINAL_PUBLIC"
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    modality = SirModality.FINAL
                },
                buildClass {
                    name = "UNSPECIDIED_PUBLIC"
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                },
                buildClass {
                    name = "OPEN_INTERNAL"
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.INTERNAL
                    modality = SirModality.OPEN
                },
                buildClass {
                    name = "FINAL_INTERNAL"
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.INTERNAL
                    modality = SirModality.FINAL
                },
                buildClass {
                    name = "UNSPECIFIED_INTERNAL"
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.INTERNAL
                },
            )
        }

        runTest(
            module,
            "testData/modality"
        )
    }

    @Test
    fun `should print attributes`() {

        val clazz = buildClass {
            name = "OPEN_INTERNAL"
            origin = SirOrigin.Unknown
            attributes += SirAttribute.Available(message = "Deprecated class", deprecated = true, obsoleted = false)
            declarations += buildFunction {
                origin = SirOrigin.Unknown
                visibility = SirVisibility.PUBLIC
                name = "method"
                returnType = SirNominalType(SirSwiftModule.bool)
                documentation = "// Check that nested attributes handled properly"
                attributes += SirAttribute.Available(message = "Deprecated method", deprecated = true, obsoleted = false)
            }
        }.attachDeclarations()

        val module = buildModule {
            name = "Test"
        }.apply {
            addChild {
                buildFunction {
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    returnType = SirNominalType(SirSwiftModule.bool)
                    attributes += SirAttribute.Available(message = "Oh no", deprecated = true, obsoleted = true)
                }
            }
            addChild {
                buildVariable {
                    name = "myVariable"
                    type = SirNominalType(SirSwiftModule.bool)
                    getter = buildGetter {}
                    documentation = """
                            /// Example docstring
                        """.trimIndent()
                    attributes += SirAttribute.Available(message = "Obsolete variable", deprecated = false, obsoleted = true)
                }
            }
            addChild {
                buildTypealias {
                    name = "myVariable"
                    type = SirNominalType(SirSwiftModule.bool)
                    documentation = """
                            /// Example docstring
                        """.trimIndent()
                    attributes += SirAttribute.Available(message = "Unavailable typealias", unavailable = true)
                }
            }
            addChild {
                clazz
            }
        }.attachDeclarations()

        runTest(
            module,
            "testData/attributes"
        )
    }

    @Test
    fun `should escape identifiers`() {
        val identifiers = listOf(
            "simple0",
            "", // empty
            "_", // underscore
            "a", // single character
            "0", // single digit
            "∞", // single unicode symbol 221e
            "\u221E", // single unicode symbol 221e
            "0startsWithDigit",
            "~invalidSymbol~",
            "unicode∞symbol221e",
            "with space",
            "() -> Function",
            "+", // operator
            "class", // keyword
            "Class", // almost keyword
            "with\textensive\r\nwhite spacing",
            "\t\r\n", // just whitespacing
            "\b\\\$" // more escapes
        )

        val module = buildModule {
            name = "Test"
        }.apply {
            for (identifier in identifiers) {
                val doc = identifier.split('\n').joinToString(separator = "\n") { "// $it" }
                val decl = buildStruct {
                    origin = SirOrigin.Unknown
                    name = identifier
                }.also { addChild { it } }

                addChild {
                    buildFunction {
                        origin = SirOrigin.Unknown
                        name = identifier
                        returnType = SirNominalType(decl)
                        documentation = doc
                    }
                }
                addChild {
                    buildVariable {
                        origin = SirOrigin.Unknown
                        name = identifier
                        type = SirNominalType(decl)
                        documentation = doc
                        getter = buildGetter()
                    }
                }
                addChild {
                    buildTypealias {
                        origin = SirOrigin.Unknown
                        name = identifier
                        type = SirNominalType(decl)
                        documentation = doc
                    }
                }
            }
        }.attachDeclarations()

        runTest(
            module,
            "testData/identifiers"
        )
    }

    @Test
    fun `should choose appropriate string literals`() {
        val messages = listOf(
            "simple",
            "", // empty
            "∞", // single unicode symbol 221e
            "\u221E", // single unicode symbol 221e
            "unicode∞symbol221e",
            "with space",
            "with\textensive\r\nwhite spacing",
            "\t\r\n", // just whitespacing
            "\b\\\$", // more escapes
            "\"doubly-quoted\"",
            "'singly-quoted'",
            "`backticked`",
            "\"#unescaped",
        )

        val module = buildModule {
            name = "Test"
        }.apply {
            // At the time of writing, attributes is the easiest way to test literal strings
            for (message in messages) {
                addChild {
                    buildStruct {
                        origin = SirOrigin.Unknown
                        name = "test"
                        attributes.add(SirAttribute.Available(deprecated = true, message = message))
                        documentation = message.split('\n').joinToString(separator = "\n") { "// $it" }
                    }
                }
            }
        }.attachDeclarations()

        runTest(
            module,
            "testData/string_literals"
        )
    }

    @Test
    fun `function returns nullable type`() {
        val module = buildModule {
            name = "Test"
            declarations.add(
                buildFunction {
                    origin = SirOrigin.Unknown
                    visibility = SirVisibility.PUBLIC
                    name = "foo"
                    returnType = SirNominalType(
                        SirSwiftModule.bool
                    ).optional()
                }
            )
        }.attachDeclarations()

        runTest(
            module,
            "testData/simple_function_returns_nullable"
        )
    }

    @Test
    fun `should print protocol declarations`() {
        val proto1 = buildProtocol {
            name = "Fooable"
        }.apply {
            parent = kotlinRuntimeModule
        }

        val proto2 = buildProtocol {
            name = "Barable"
        }.apply {
            parent = kotlinRuntimeModule
        }

        val module = buildModule {
            name = "Test"
            declarations.add(
                buildProtocol {
                    name = "Foo"
                    superClass = SirNominalType(kotlinBase)
                    protocols.addAll(listOf(proto1, proto2))

                    declarations.add(
                        buildFunction {
                            origin = SirOrigin.Unknown
                            visibility = SirVisibility.PUBLIC
                            name = "foo"
                            returnType = SirNominalType(
                                SirSwiftModule.bool
                            )
                            body = SirFunctionBody(listOf("<SHOULD NOT BE VISIBLE>"))
                        }
                    )

                    declarations.add(
                        buildVariable {
                            origin = SirOrigin.Unknown
                            visibility = SirVisibility.PUBLIC
                            name = "bar"
                            type = SirNominalType(
                                SirSwiftModule.bool
                            )
                            getter = buildGetter {
                                body = SirFunctionBody(listOf("<SHOULD NOT BE VISIBLE>"))
                            }
                            setter = buildSetter {
                                body = SirFunctionBody(listOf("<SHOULD NOT BE VISIBLE>"))
                            }
                        }
                    )

                    declarations.add(
                        buildInit {
                            origin = SirOrigin.Unknown
                            visibility = SirVisibility.PUBLIC
                            isFailable = false
                            body = SirFunctionBody(listOf("<SHOULD NOT BE VISIBLE>"))
                        }
                    )
                }.attachDeclarations()
            )
        }.attachDeclarations()

        runTest(
            module,
            "testData/protocol_declarations"
        )
    }

    companion object {
        val kotlinRuntimeModule = buildModule {
            name = "KotlinRuntime"
        }
        val kotlinBase = kotlinRuntimeModule.addChild {
            buildClass {
                name = "KotlinBase"
            }
        }
    }
}

private fun <T : SirDeclarationContainer> T.attachDeclarations(): T = also { declarations.forEach { it.parent = this } }