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

}
