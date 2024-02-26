/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.P
import kotlin.test.Test
import utils.*


class ConstructorsTest : AbstractModelTest("/src/main/kotlin/constructors/Test.kt", "constructors") {

    @Test
    fun `should have documentation for @constructor tag without parameters`() {
        val expectedRootDescription = Description(
            CustomDocTag(
                emptyList(),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )

        val expectedConstructorTag = Constructor(
            CustomDocTag(
                listOf(
                    P(
                        listOf(
                            Text("some doc"),
                        )
                    )
                ),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )
        val expectedDescriptionTag = Description(
            CustomDocTag(
                listOf(
                    P(
                        listOf(
                            Text("some doc"),
                        )
                    )
                ),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )
        inlineModelTest(
            """
               |/**
               |* @constructor some doc
               |*/
               |class A
            """.trimMargin()
        ) {
            val classlike = packages.flatMap { it.classlikes }.first() as DClass
            classlike.name equals "A"
            classlike.documentation.values.single() equals DocumentationNode(listOf(expectedRootDescription, expectedConstructorTag))
            val constructor = classlike.constructors.single()
            constructor.documentation.values.single() equals DocumentationNode(listOf(expectedDescriptionTag))
        }
    }

    @Test
    fun `should have documentation for @constructor tag`() {

        val expectedRootDescription = Description(
            CustomDocTag(
                emptyList(),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )

        val expectedConstructorTag = Constructor(
            CustomDocTag(
                listOf(
                    P(
                        listOf(
                            Text("some doc"),
                        )
                    )
                ),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )
        val expectedDescriptionTag = Description(
            CustomDocTag(
                listOf(
                    P(
                        listOf(
                            Text("some doc"),
                        )
                    )
                ),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )
        inlineModelTest(
            """
               |/**
               |* @constructor some doc
               |*/
               |class A(a: Int)
            """.trimMargin()
        ) {
            val classlike = packages.flatMap { it.classlikes }.first() as DClass
            classlike.name equals "A"
            classlike.documentation.values.single() equals DocumentationNode(listOf(expectedRootDescription, expectedConstructorTag))
            val constructor = classlike.constructors.single()
            constructor.documentation.values.single() equals DocumentationNode(listOf(expectedDescriptionTag))
        }
    }

    @Test
    fun `should ignore documentation in @constructor tag for a secondary constructor`() {
        val expectedRootDescription = Description(
            CustomDocTag(
                emptyList(),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )

        val expectedConstructorTag = Constructor(
            CustomDocTag(
                listOf(
                    P(
                        listOf(
                            Text("some doc"),
                        )
                    )
                ),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )

        inlineModelTest(
            """
               |/**
               |* @constructor some doc
               |*/
               |class A {
               |    constructor(a: Int)
               |}
            """.trimMargin()
        ) {
            val classlike = packages.flatMap { it.classlikes }.first() as DClass
            classlike.name equals "A"
            classlike.documentation.values.single() equals DocumentationNode(listOf(expectedRootDescription, expectedConstructorTag))
            val constructor = classlike.constructors.single()
            constructor.documentation.isEmpty() equals true
        }
    }

    @Test
    fun `should have KMP documentation for @constructor tag`() {
        val expectedDescriptionTag = Description(
            CustomDocTag(
                listOf(
                    P(
                        listOf(
                            Text("some doc"),
                        )
                    )
                ),
                params = emptyMap(),
                name = MARKDOWN_ELEMENT_FILE_NAME
            )
        )
        val configuration = dokkaConfiguration {
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/main/kotlin/common/Test.kt")
                }

                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/main/kotlin/jvm/Test.kt")
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/common/Test.kt
            |package multiplatform
            |
            |expect class A()
            |
            |/src/main/kotlin/jvm/Test.kt
            |package multiplatform
            |/**
            ||* @constructor some doc
            |*/
            |actual class A()
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = {
                val classlike = it.packages.flatMap { it.classlikes }.first() as DClass
                classlike.name equals "A"
                val actualConstructor = classlike.constructors.first { it.sourceSets.single().displayName == "jvm" }
                actualConstructor.documentation.values.single() equals DocumentationNode(listOf(expectedDescriptionTag))

                val expectConstructor = classlike.constructors.first { it.sourceSets.single().displayName == "common" }
                expectConstructor.documentation.isEmpty() equals true
            }

        }
    }

    @Test
    fun `should have actual constructor`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/main/kotlin/common/Test.kt")
                }

                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/main/kotlin/jvm/Test.kt")
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/common/Test.kt
            |package multiplatform
            |
            |expect class A()
            |
            |/src/main/kotlin/jvm/Test.kt
            |package multiplatform
            |actual class A{
            |    actual constructor(){}
            |}
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                val actualClasslike = it.first { it.sourceSets.single().displayName == "common" }.packages.flatMap { it.classlikes }.first() as DClass
                actualClasslike.name equals "A"
                val actualConstructor = actualClasslike.constructors.first { it.sourceSets.single().displayName == "common" }
                actualConstructor.isExpectActual equals true

                val expectClasslike = it.first { it.sourceSets.single().displayName == "jvm" }.packages.flatMap { it.classlikes }.first() as DClass
                expectClasslike.name equals "A"
                val expectConstructor = expectClasslike.constructors.first { it.sourceSets.single().displayName == "jvm" }
                expectConstructor.isExpectActual equals true
            }
            documentablesMergingStage = {
                val classlike = it.packages.flatMap { it.classlikes }.first() as DClass
                classlike.name equals "A"
                val constructor = classlike.constructors.single()
                constructor.isExpectActual equals true
                constructor.sourceSets counts 2
            }
        }
    }
}
