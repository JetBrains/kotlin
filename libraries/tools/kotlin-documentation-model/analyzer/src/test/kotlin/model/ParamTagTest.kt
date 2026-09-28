/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.doc.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalDokkaApi::class)
class ParamTagTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOfNotNull(jvmStdlibPath)
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `context parameter in param tag of a function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @param scope context parameter
            |  * @param arg parameter
            |  */
            |context(scope: String)
            |fun foo(arg: Int) {}
        """.trimIndent(), testConfiguration
        ) {
            documentablesMergingStage = { module ->
                val function = module.packages.flatMap { it.functions }.single { it.name == "foo" }

                assertEquals(
                    DocumentationNode(
                        children = listOf(
                            Description(CustomDocTag(name = MARKDOWN_ELEMENT_FILE_NAME)),
                            Param(
                                root = CustomDocTag(
                                    children = listOf(P(children = listOf(Text(body = "context parameter")))),
                                    name = MARKDOWN_ELEMENT_FILE_NAME
                                ),
                                name = "scope",
                                address = DRI(
                                    packageName = "test",
                                    callable = Callable(
                                        name = "foo",
                                        params = listOf(TypeConstructor("kotlin.Int", emptyList())),
                                        contextParameters = listOf(TypeConstructor("kotlin.String", emptyList()))
                                    ),
                                    target = PointingToContextParameters(0)
                                ),
                            ),
                            Param(
                                root = CustomDocTag(
                                    children = listOf(P(children = listOf(Text(body = "parameter")))),
                                    name = MARKDOWN_ELEMENT_FILE_NAME
                                ),
                                name = "arg",
                                address = DRI(
                                    packageName = "test",
                                    callable = Callable(
                                        name = "foo",
                                        params = listOf(TypeConstructor("kotlin.Int", emptyList())),
                                        contextParameters = listOf(TypeConstructor("kotlin.String", emptyList()))
                                    ),
                                    target = PointingToCallableParameters(0)
                                ),
                            )
                        )
                    ),
                    function.documentation.values.single()
                )
            }
        }
    }

    @Test
    fun `context parameter in param tag with brackets of a function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @param[scope] context parameter
            |  * @param[arg] parameter
            |  */
            |context(scope: String)
            |fun foo(arg: Int) {}
        """.trimIndent(), testConfiguration
        ) {
            documentablesMergingStage = { module ->
                val function = module.packages.flatMap { it.functions }.single { it.name == "foo" }

                assertEquals(
                    DocumentationNode(
                        children = listOf(
                            Description(CustomDocTag(name = MARKDOWN_ELEMENT_FILE_NAME)),
                            Param(
                                root = CustomDocTag(
                                    children = listOf(P(children = listOf(Text(body = "context parameter")))),
                                    name = MARKDOWN_ELEMENT_FILE_NAME
                                ),
                                name = "scope",
                                address = DRI(
                                    packageName = "test",
                                    callable = Callable(
                                        name = "foo",
                                        params = listOf(TypeConstructor("kotlin.Int", emptyList())),
                                        contextParameters = listOf(TypeConstructor("kotlin.String", emptyList()))
                                    ),
                                    target = PointingToContextParameters(0)
                                ),
                            ),
                            Param(
                                root = CustomDocTag(
                                    children = listOf(P(children = listOf(Text(body = "parameter")))),
                                    name = MARKDOWN_ELEMENT_FILE_NAME
                                ),
                                name = "arg",
                                address = DRI(
                                    packageName = "test",
                                    callable = Callable(
                                        name = "foo",
                                        params = listOf(TypeConstructor("kotlin.Int", emptyList())),
                                        contextParameters = listOf(TypeConstructor("kotlin.String", emptyList()))
                                    ),
                                    target = PointingToCallableParameters(0)
                                ),
                            )
                        )
                    ),
                    function.documentation.values.single()
                )
            }
        }
    }

    @Test
    fun `context parameter in param tag of a property`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @param scope context parameter
            |  */
            |context(scope: String)
            |val foo: String = ""
        """.trimIndent(), testConfiguration
        ) {
            documentablesMergingStage = { module ->
                val property = module.packages.flatMap { it.properties }.single { it.name == "foo" }

                assertEquals(
                    DocumentationNode(
                        children = listOf(
                            Description(CustomDocTag(name = MARKDOWN_ELEMENT_FILE_NAME)),
                            Param(
                                root = CustomDocTag(
                                    children = listOf(P(children = listOf(Text(body = "context parameter")))),
                                    name = MARKDOWN_ELEMENT_FILE_NAME
                                ),
                                name = "scope",
                                address = DRI(
                                    packageName = "test",
                                    callable = Callable(
                                        name = "foo",
                                        params = emptyList(),
                                        contextParameters = listOf(TypeConstructor("kotlin.String", emptyList())),
                                        isProperty = true
                                    ),
                                    target = PointingToContextParameters(0)
                                ),
                            )
                        )
                    ),
                    property.documentation.values.single()
                )
            }
        }
    }

    @Test
    fun `context parameter in param tag with brackets of a property`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @param[scope] context parameter
            |  */
            |context(scope: String)
            |val foo: String = ""
        """.trimIndent(), testConfiguration
        ) {
            documentablesMergingStage = { module ->
                val property = module.packages.flatMap { it.properties }.single { it.name == "foo" }

                assertEquals(
                    DocumentationNode(
                        children = listOf(
                            Description(CustomDocTag(name = MARKDOWN_ELEMENT_FILE_NAME)),
                            Param(
                                root = CustomDocTag(
                                    children = listOf(P(children = listOf(Text(body = "context parameter")))),
                                    name = MARKDOWN_ELEMENT_FILE_NAME
                                ),
                                name = "scope",
                                address = DRI(
                                    packageName = "test",
                                    callable = Callable(
                                        name = "foo",
                                        params = emptyList(),
                                        contextParameters = listOf(TypeConstructor("kotlin.String", emptyList())),
                                        isProperty = true
                                    ),
                                    target = PointingToContextParameters(0)
                                ),
                            )
                        )
                    ),
                    property.documentation.values.single()
                )
            }
        }
    }

}
