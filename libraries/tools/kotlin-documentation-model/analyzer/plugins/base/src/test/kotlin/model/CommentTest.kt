package model

import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.junit.jupiter.api.Test
import utils.*

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
}
