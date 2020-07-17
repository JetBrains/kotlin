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
                with(this.docs().firstOrNull()?.root.assertNotNull("Code")) {
                    (children.firstOrNull() as? Text)
                        ?.body equals "++++++++++[>+++++++>++++++++++>+++>+<<<<-]>++.>+.+++++++..+++.>++.<<+++++++++++++++.>.+++.------.--------.>+.>."

                    params["lang"] equals "brainfuck"
                }
            }
            with((this / "comment" / "prop2").cast<DProperty>()) {
                name equals "prop2"
                comments() equals "a + b - c"
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
            val p = this
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
                comments() equals "doc1\ndoc2 doc3"
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
                comments() equals "doc1\ndoc2 doc3"
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
                comments() equals "doc"
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
                comments() equals "doc"
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
                comments() equals "doc"
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
                comments() equals "Summary\none: []"
                docs().find { it is CustomTagWrapper && it.name == "one" }.let {
                    with(it.assertNotNull("'one' entry")) {
                        root.children counts 0
                        root.params.keys counts 0
                    }
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
                comments() equals """it's "useful""""
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
                comments() equals "Summary\none: [section one]"
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
                comments() equals "Summary\none: [section one]\ntwo: [section two]"
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
                comments() equals "Summary\none: [line one line two]"
            }
        }
    }

//    @Test todo
    fun directive() {
        inlineModelTest(
            """
            |/**
            | * Summary
            | *
            | * @sample example1
            | * @sample example2
            | * @sample X.example3
            | * @sample X.Y.example4
            | */
            |val property = "test"
            |
            |fun example1(node: String) = if (true) {
            |    println(property)
            |}
            |
            |fun example2(node: String) {
            |    if (true) {
            |        println(property)
            |    }
            |}
            |
            |class X {
            |    fun example3(node: String) {
            |        if (true) {
            |            println(property)
            |        }
            |    }
            |
            |    class Y {
            |        fun example4(node: String) {
            |            if (true) {
            |                println(property)
            |            }
            |        }
            |    }
            |}
        """
        ) {
            with((this / "comment" / "property").cast<DProperty>()) {
                this
            }
        }
    }


//    @Test fun directive() {
//        checkSourceExistsAndVerifyModel("testdata/comments/directive.kt", defaultModelConfig) { model ->
//            with(model.members.single().members.first()) {
//                assertEquals("Summary", content.summary.toTestString())
//                with (content.description) {
//                    assertEqualsIgnoringSeparators("""
//                        |[code lang=kotlin]
//                        |if (true) {
//                        |    println(property)
//                        |}
//                        |[/code]
//                        |[code lang=kotlin]
//                        |if (true) {
//                        |    println(property)
//                        |}
//                        |[/code]
//                        |[code lang=kotlin]
//                        |if (true) {
//                        |    println(property)
//                        |}
//                        |[/code]
//                        |[code lang=kotlin]
//                        |if (true) {
//                        |    println(property)
//                        |}
//                        |[/code]
//                        |""".trimMargin(), toTestString())
//                }
//            }
//        }
//    }

}