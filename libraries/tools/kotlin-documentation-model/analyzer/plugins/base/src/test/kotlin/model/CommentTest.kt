/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.doc.*
import utils.AbstractModelTest
import utils.assertNotNull
import utils.comments
import utils.docs
import kotlin.test.Test

class CommentTest : AbstractModelTest("/src/main/kotlin/comment/Test.kt", "comment") {

    @Test
    fun codeBlockComment() {
        inlineModelTest(
            """
                |/**
                | * ```brainfuck
                | * ++++++++++[>+++++++>++++++++++>+++>+<<<<-]>++.>+.+++++++..+++.>++.<<+++++++++++++++.>.+++.------.--------.>+.>.
                | * ```
                | */
                |val prop1 = ""
                |
                |
                |/**
                | * ```
                | * a + b - c
                | * ```
                | */
                |val prop2 = ""
            """
        ) {
            with((this / "comment" / "prop1").cast<DProperty>()) {
                name equals "prop1"
                with(this.docs().firstOrNull()?.children?.firstOrNull()?.assertNotNull("Code")) {
                    (this?.children?.firstOrNull() as? Text)
                        ?.body equals "++++++++++[>+++++++>++++++++++>+++>+<<<<-]>++.>+.+++++++..+++.>++.<<+++++++++++++++.>.+++.------.--------.>+.>."

                    this?.params?.get("lang") equals "brainfuck"
                }
            }
            with((this / "comment" / "prop2").cast<DProperty>()) {
                name equals "prop2"
                with(this.docs().firstOrNull()?.children?.firstOrNull()?.assertNotNull("Code")) {
                    (this?.children?.firstOrNull() as? Text)
                        ?.body equals "a + b - c"

                    this?.params?.get("lang") equals null
                }
            }
        }
    }

    @Test
    fun codeBlockWithIndentationComment() {
        inlineModelTest(
            """
                |/**
                | * 1.
                | *    ```
                | *    line 1
                | *    line 2
                | *    ```
                | */
                |val prop1 = ""
            """
        ) {
            with((this / "comment" / "prop1").cast<DProperty>()) {
                name equals "prop1"
                with(this.docs().firstOrNull()?.children?.firstOrNull()?.assertNotNull("Code")) {
                    val codeBlockChildren = ((this?.children?.firstOrNull() as? Li)?.children?.firstOrNull() as? CodeBlock)?.children
                    (codeBlockChildren?.get(0) as? Text)?.body equals " line 1"
                    (codeBlockChildren?.get(1) as? Br) notNull "Br"
                    (codeBlockChildren?.get(2) as? Text)?.body equals " line 2"
                }
            }
        }
    }

    @Test
    fun emptyDoc() {
        inlineModelTest(
            """
            val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                name equals "property"
                comments() equals ""
            }
        }
    }

    @Test
    fun emptyDocButComment() {
        inlineModelTest(
            """
            |/* comment */
            |val property = "test"
            |fun tst() = property
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals ""
            }
        }
    }

    @Test
    fun multilineDoc() {
        inlineModelTest(
            """
            |/**
            | * doc1
            | *
            | * doc2
            | * doc3
            | */
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "doc1\ndoc2 doc3\n"
            }
        }
    }

    @Test
    fun multilineDocWithComment() {
        inlineModelTest(
            """
            |/**
            | * doc1
            | *
            | * doc2
            | * doc3
            | */
            |// comment
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "doc1\ndoc2 doc3\n"
            }
        }
    }

    @Test
    fun oneLineDoc() {
        inlineModelTest(
            """
            |/** doc */
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "doc\n"
            }
        }
    }

    @Test
    fun oneLineDocWithComment() {
        inlineModelTest(
            """
            |/** doc */
            |// comment
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "doc\n"
            }
        }
    }

    @Test
    fun oneLineDocWithEmptyLine() {
        inlineModelTest(
            """
            |/** doc */
            |
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "doc\n"
            }
        }
    }

    @Test
    fun emptySection() {
        inlineModelTest(
            """
            |/**
            | * Summary
            | * @one
            | */
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "Summary\n\none: []"
                with(docs().find { it is CustomTagWrapper && it.name == "one" }.assertNotNull("'one' entry")) {
                    root.children counts 0
                    root.params.keys counts 0
                }
            }
        }
    }

    @Test
    fun quotes() {
        inlineModelTest(
            """
            |/** it's "useful" */
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals """it's "useful"
"""
            }
        }
    }

    @Test
    fun section1() {
        inlineModelTest(
            """
            |/**
            | * Summary
            | * @one section one
            | */
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "Summary\n\none: [section one\n]"
            }
        }
    }


    @Test
    fun section2() {
        inlineModelTest(
            """
            |/**
            | * Summary
            | * @one section one
            | * @two section two
            | */
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "Summary\n\none: [section one\n]\ntwo: [section two\n]"
            }
        }
    }

    @Test
    fun multilineSection() {
        inlineModelTest(
            """
            |/**
            | * Summary
            | * @one
            | *   line one
            | *   line two
            | */
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "Summary\n\none: [line one line two\n]"
            }
        }
    }

    @Test
    fun `should be space between Markdown nodes`() {
        inlineModelTest(
            """
            |/**
            | * Rotates paths by `amount` **radians** around (`x`, `y`).
            | */
            |val property = "test"
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                comments() equals "Rotates paths by amount radians around (x, y).\n"
            }
        }
    }

    @Test
    fun `should remove spaces inside indented code block`() {
        inlineModelTest(
            """
            |/**
            | * Welcome:
            | *
            | * ```kotlin
            | * fun main() {
            | *     println("Hello World!")
            | * }
            | * ```
            | *
            | *     fun thisIsACodeBlock() {
            | *         val butWhy = "per markdown spec, because four-spaces prefix"
            | *     }
            | */
            |class Foo
        """
        ) {
            with((this / "comment" / "Foo").cast<DClass>()) {
                docs()[0].children[2] equals CodeBlock(
                    listOf(
                        Text(
                            "fun thisIsACodeBlock() {\n" +
                                    "    val butWhy = \"per markdown spec, because four-spaces prefix\"\n" +
                                    "}"
                        )
                    )
                )
            }
        }
    }

}
