package org.jetbrains.dokka.tests

import markdown.KDocTest
import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.model.doc.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ParserTest : KDocTest() {

    @Test
    fun `Simple text`() {
        val kdoc = """
        | This is simple test of string
        | Next line
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(P(listOf(Text("This is simple test of string Next line")))),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Simple text with new line`() {
        val kdoc = """
        | This is simple test of string\
        | Next line
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text("This is simple test of string"),
                                    Br,
                                    Text("Next line")
                                )
                            )
                        ),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Text with Bold and Emphasis decorators`() {
        val kdoc = """
        | This is **simple** test of _string_
        | Next **_line_**
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text("This is "),
                                    B(listOf(Text("simple"))),
                                    Text(" test of "),
                                    I(listOf(Text("string"))),
                                    Text(" Next "),
                                    B(listOf(I(listOf(Text("line")))))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Text with Colon`() {
        val kdoc = """
        | This is simple text with: colon!
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(P(listOf(Text("This is simple text with: colon!")))),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Multilined text`() {
        val kdoc = """
        | Text
        | and
        | String
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(P(listOf(Text("Text and String")))),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Paragraphs`() {
        val kdoc = """
        | Paragraph number
        | one
        |
        | Paragraph\
        | number two
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(listOf(Text("Paragraph number one"))),
                            P(listOf(Text("Paragraph"), Br, Text("number two")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Emphasis with star`() {
        val kdoc = " *text*"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(P(listOf(I(listOf(Text("text")))))),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Underscores that are not Emphasis`() {
        val kdoc = "text_with_underscores"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(P(listOf(Text("text_with_underscores")))),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Emphasis with underscores`() {
        val kdoc = "_text_"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(P(listOf(I(listOf(Text("text")))))),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Stars as italic bounds`() {
        val kdoc = "The abstract syntax tree node for a multiplying expression.  A multiplying\n" +
                "expression is a binary expression where the operator is a multiplying operator\n" +
                "such as \"*\", \"/\", or \"mod\".  A simple example would be \"5*x\"."
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text(
                                        "The abstract syntax tree node for a multiplying expression.  A multiplying " +
                                                "expression is a binary expression where the operator is a multiplying operator " +
                                                "such as \""
                                    ),
                                    I(listOf(Text("\", \"/\", or \"mod\".  A simple example would be \"5"))),
                                    Text("x\".")
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Stars as bold bounds`() {
        val kdoc = "The abstract syntax tree node for a multiplying expression.  A multiplying\n" +
                "expression is a binary expression where the operator is a multiplying operator\n" +
                "such as \"**\", \"/\", or \"mod\".  A simple example would be \"5**x\"."
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text(
                                        "The abstract syntax tree node for a multiplying expression.  A multiplying " +
                                                "expression is a binary expression where the operator is a multiplying operator " +
                                                "such as \""
                                    ),
                                    B(listOf(Text("\", \"/\", or \"mod\".  A simple example would be \"5"))),
                                    Text("x\".")
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Embedded star`() {
        val kdoc = "Embedded*Star"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(listOf(Text("Embedded*Star")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }


    @Test
    fun `Unordered list`() {
        val kdoc = """
        | * list item 1
        | * list item 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ul(
                                listOf(
                                    Li(listOf(P(listOf(Text("list item 1"))))),
                                    Li(listOf(P(listOf(Text("list item 2")))))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Unordered list with multilines`() {
        val kdoc = """
        | * list item 1
        |  continue 1
        | * list item 2\
        | continue 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ul(
                                listOf(
                                    Li(listOf(P(listOf(Text("list item 1 continue 1"))))),
                                    Li(listOf(P(listOf(Text("list item 2"), Br, Text("continue 2")))))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Unordered list with Bold`() {
        val kdoc = """
        | * list **item** 1
        |  continue 1
        | * list __item__ 2
        |  continue 2
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ul(
                                listOf(
                                    Li(
                                        listOf(
                                            P(
                                                listOf(
                                                    Text("list "),
                                                    B(listOf(Text("item"))),
                                                    Text(" 1 continue 1")
                                                )
                                            )
                                        )
                                    ),
                                    Li(
                                        listOf(
                                            P(
                                                listOf(
                                                    Text("list "),
                                                    B(listOf(Text("item"))),
                                                    Text(" 2 continue 2")
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Unordered list with nested bullets`() {
        val kdoc = """
        | * Outer first
        | Outer next line
        | * Outer second
        |     - Middle first
        | Middle next line
        |     - Middle second
        |         + Inner first
        | Inner next line
        |     - Middle third
        | * Outer third
        |
        | New paragraph""".trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ul(
                                listOf(
                                    Li(listOf(P(listOf(Text("Outer first Outer next line"))))),
                                    Li(listOf(P(listOf(Text("Outer second"))))),
                                    Ul(
                                        listOf(
                                            Li(listOf(P(listOf(Text("Middle first Middle next line"))))),
                                            Li(listOf(P(listOf(Text("Middle second"))))),
                                            Ul(
                                                listOf(
                                                    Li(listOf(P(listOf(Text("Inner first Inner next line")))))
                                                )
                                            ),
                                            Li(listOf(P(listOf(Text("Middle third")))))
                                        )
                                    ),
                                    Li(listOf(P(listOf(Text("Outer third")))))
                                )
                            ),
                            P(listOf(Text("New paragraph")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Ordered list`() {
        val kdoc = """
        | 1. list item 1
        | 2. list item 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ol(
                                listOf(
                                    Li(listOf(P(listOf(Text("list item 1"))))),
                                    Li(listOf(P(listOf(Text("list item 2")))))
                                ),
                                mapOf("start" to "1")
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }


    @Test
    fun `Ordered list beginning from other number`() {
        val kdoc = """
        | 9. list item 1
        | 12. list item 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ol(
                                listOf(
                                    Li(listOf(P(listOf(Text("list item 1"))))),
                                    Li(listOf(P(listOf(Text("list item 2")))))
                                ),
                                mapOf("start" to "9")
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Ordered list with multilines`() {
        val kdoc = """
        | 2. list item 1
        |  continue 1
        | 3. list item 2
        | continue 2
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ol(
                                listOf(
                                    Li(listOf(P(listOf(Text("list item 1 continue 1"))))),
                                    Li(listOf(P(listOf(Text("list item 2 continue 2")))))
                                ),
                                mapOf("start" to "2")
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Ordered list with Bold`() {
        val kdoc = """
        | 1. list **item** 1
        |  continue 1
        | 2. list __item__ 2
        |  continue 2
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ol(
                                listOf(
                                    Li(
                                        listOf(
                                            P(
                                                listOf(
                                                    Text("list "),
                                                    B(listOf(Text("item"))),
                                                    Text(" 1 continue 1")
                                                )
                                            )
                                        )
                                    ),
                                    Li(
                                        listOf(
                                            P(
                                                listOf(
                                                    Text("list "),
                                                    B(listOf(Text("item"))),
                                                    Text(" 2 continue 2")
                                                )
                                            )
                                        )
                                    )
                                ),
                                mapOf("start" to "1")
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Ordered list with nested bullets`() {
        val kdoc = """
        | 1. Outer first
        | Outer next line
        | 2. Outer second
        |     1. Middle first
        | Middle next line
        |     2. Middle second
        |         1. Inner first
        | Inner next line
        |     5. Middle third
        | 4. Outer third
        |
        | New paragraph""".trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ol(
                                listOf(
                                    Li(listOf(P(listOf(Text("Outer first Outer next line"))))),
                                    Li(listOf(P(listOf(Text("Outer second"))))),
                                    Ol(
                                        listOf(
                                            Li(listOf(P(listOf(Text("Middle first Middle next line"))))),
                                            Li(listOf(P(listOf(Text("Middle second"))))),
                                            Ol(
                                                listOf(
                                                    Li(listOf(P(listOf(Text("Inner first Inner next line")))))
                                                ),
                                                mapOf("start" to "1")
                                            ),
                                            Li(listOf(P(listOf(Text("Middle third")))))
                                        ),
                                        mapOf("start" to "1")
                                    ),
                                    Li(listOf(P(listOf(Text("Outer third")))))
                                ),
                                mapOf("start" to "1")
                            ),
                            P(listOf(Text("New paragraph")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Ordered nested in Unordered nested in Ordered list`() {
        val kdoc = """
        | 1. Outer first
        | Outer next line
        | 2. Outer second
        |     + Middle first
        | Middle next line
        |     + Middle second
        |         1. Inner first
        | Inner next line
        |     + Middle third
        | 4. Outer third
        |
        | New paragraph""".trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Ol(
                                listOf(
                                    Li(listOf(P(listOf(Text("Outer first Outer next line"))))),
                                    Li(listOf(P(listOf(Text("Outer second"))))),
                                    Ul(
                                        listOf(
                                            Li(listOf(P(listOf(Text("Middle first Middle next line"))))),
                                            Li(listOf(P(listOf(Text("Middle second"))))),
                                            Ol(
                                                listOf(
                                                    Li(listOf(P(listOf(Text("Inner first Inner next line")))))
                                                ),
                                                mapOf("start" to "1")
                                            ),
                                            Li(listOf(P(listOf(Text("Middle third")))))
                                        )
                                    ),
                                    Li(listOf(P(listOf(Text("Outer third")))))
                                ),
                                mapOf("start" to "1")
                            ),
                            P(listOf(Text("New paragraph")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Header and two paragraphs`() {
        val kdoc = """
        | # Header 1
        | Following text
        |
        | New paragraph
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            H1(listOf(Text("Header 1"))),
                            P(listOf(Text("Following text"))),
                            P(listOf(Text("New paragraph")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Disabled //TODO: ATX_2 to ATX_6 and sometimes ATX_1 from jetbrains parser consumes white space. Need to handle it in their library
    @Test
    fun `All headers`() {
        val kdoc = """
        | # Header 1
        | Text 1
        | ## Header 2
        | Text 2
        | ### Header 3
        | Text 3
        | #### Header 4
        | Text 4
        | ##### Header 5
        | Text 5
        | ###### Header 6
        | Text 6
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            H1(listOf(Text("Header 1"))),
                            P(listOf(Text("Text 1"))),
                            H2(listOf(Text("Header 2"))),
                            P(listOf(Text("Text 2"))),
                            H3(listOf(Text("Header 3"))),
                            P(listOf(Text("Text 3"))),
                            H4(listOf(Text("Header 4"))),
                            P(listOf(Text("Text 4"))),
                            H5(listOf(Text("Header 5"))),
                            P(listOf(Text("Text 5"))),
                            H6(listOf(Text("Header 6"))),
                            P(listOf(Text("Text 6")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Bold New Line Bold`() {
        val kdoc = """
        | **line 1**\
        | **line 2**
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    B(listOf(Text("line 1"))),
                                    Br,
                                    B(listOf(Text("line 2")))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Horizontal rule`() {
        val kdoc = """
        | ***
        | text 1
        | ___
        | text 2
        | ***
        | text 3
        | ___
        | text 4
        | ***
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            HorizontalRule,
                            P(listOf(Text("text 1"))),
                            HorizontalRule,
                            P(listOf(Text("text 2"))),
                            HorizontalRule,
                            P(listOf(Text("text 3"))),
                            HorizontalRule,
                            P(listOf(Text("text 4"))),
                            HorizontalRule
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Blockquote`() {
        val kdoc = """
        | > Blockquotes are very handy in email to emulate reply text.
        | > This line is part of the same quote.
        |
        | Quote break.
        |
        | > Quote
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            BlockQuote(
                                listOf(
                                    P(
                                        listOf(
                                            Text("Blockquotes are very handy in email to emulate reply text. This line is part of the same quote.")
                                        )
                                    )
                                )
                            ),
                            P(listOf(Text("Quote break."))),
                            BlockQuote(
                                listOf(
                                    P(listOf(Text("Quote")))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }


    @Test
    fun `Blockquote nested`() {
        val kdoc = """
        | > text 1
        | > text 2
        | >> text 3
        | >> text 4
        | >
        | > text 5
        |
        | Quote break.
        |
        | > Quote
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            BlockQuote(
                                listOf(
                                    P(listOf(Text("text 1 text 2"))),
                                    BlockQuote(
                                        listOf(
                                            P(listOf(Text("text 3 text 4")))
                                        )
                                    ),
                                    P(listOf(Text("text 5")))
                                )
                            ),
                            P(listOf(Text("Quote break."))),
                            BlockQuote(
                                listOf(
                                    P(listOf(Text("Quote")))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Disabled //TODO: Again ATX_1 consumes white space
    @Test
    fun `Blockquote nested with fancy text enhancement`() {
        val kdoc = """
        | > text **1**
        | > text 2
        | >> # text 3
        | >> * text 4
        | >>     * text 5
        | >
        | > text 6
        |
        | Quote break.
        |
        | > Quote
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            BlockQuote(
                                listOf(
                                    P(
                                        listOf(
                                            Text("text "),
                                            B(listOf(Text("1"))),
                                            Text("\ntext 2")
                                        )
                                    ),
                                    BlockQuote(
                                        listOf(
                                            H1(listOf(Text("text 3"))),
                                            Ul(
                                                listOf(
                                                    Li(listOf(P(listOf(Text("text 4"))))),
                                                    Ul(
                                                        listOf(
                                                            Li(listOf(P(listOf(Text("text 5")))))
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    P(listOf(Text("text 6")))
                                )
                            ),
                            P(listOf(Text("Quote break."))),
                            BlockQuote(
                                listOf(
                                    P(listOf(Text("Quote")))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Simple Code Block`() {
        val kdoc = """
        | `Some code`
        | Sample text
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    CodeInline(listOf(Text("Some code"))),
                                    Text(" Sample text")
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Multilined Code Block`() {
        val kdoc = """
        | ```kotlin
        | val x: Int = 0
        | val y: String = "Text"
        |
        |     val z: Boolean = true
        | for(i in 0..10) {
        |     println(i)
        | }
        | ```
        | Sample text
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            CodeBlock(
                                listOf(
                                    Text("val x: Int = 0"), Br,
                                    Text("val y: String = \"Text\""), Br, Br,
                                    Text("    val z: Boolean = true"), Br,
                                    Text("for(i in 0..10) {"), Br,
                                    Text("    println(i)"), Br,
                                    Text("}")
                                ),
                                mapOf("lang" to "kotlin")
                            ),
                            P(listOf(Text("Sample text")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }


    @Test
    fun `Inline link`() {
        val kdoc = """
        | [I'm an inline-style link](https://www.google.com)
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    A(
                                        listOf(Text("I'm an inline-style link")),
                                        mapOf("href" to "https://www.google.com")
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Inline link with title`() {
        val kdoc = """
        | [I'm an inline-style link with title](https://www.google.com "Google's Homepage")
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    A(
                                        listOf(Text("I'm an inline-style link with title")),
                                        mapOf("href" to "https://www.google.com", "title" to "Google's Homepage")
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Full reference link`() {
        val kdoc = """
        | [I'm a reference-style link][Arbitrary case-insensitive reference text]
        |
        | [arbitrary case-insensitive reference text]: https://www.mozilla.org
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    A(
                                        listOf(Text("I'm a reference-style link")),
                                        mapOf("href" to "https://www.mozilla.org")
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Full reference link with number`() {
        val kdoc = """
        | [You can use numbers for reference-style link definitions][1]
        |
        | [1]: http://slashdot.org
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    A(
                                        listOf(Text("You can use numbers for reference-style link definitions")),
                                        mapOf("href" to "http://slashdot.org")
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Short reference link`() {
        val kdoc = """
        | Or leave it empty and use the [link text itself].
        |
        | [link text itself]: http://www.reddit.com
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text("Or leave it empty and use the "),
                                    A(
                                        listOf(Text("link text itself")),
                                        mapOf("href" to "http://www.reddit.com")
                                    ),
                                    Text(".")
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Autolink`() {
        val kdoc = """
        | URLs and URLs in angle brackets will automatically get turned into links.
        | http://www.example.com or <http://www.example.com> and sometimes
        | example.com (but not on Github, for example).
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text("URLs and URLs in angle brackets will automatically get turned into links. http://www.example.com or "),
                                    A(
                                        listOf(Text("http://www.example.com")),
                                        mapOf("href" to "http://www.example.com")
                                    ),
                                    Text(" and sometimes example.com (but not on Github, for example).")
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Various links`() {
        val kdoc = """
        | [I'm an inline-style link](https://www.google.com)
        |
        | [I'm an inline-style link with title](https://www.google.com "Google's Homepage")
        |
        | [I'm a reference-style link][Arbitrary case-insensitive reference text]
        |
        | [You can use numbers for reference-style link definitions][1]
        |
        | Or leave it empty and use the [link text itself].
        |
        | URLs and URLs in angle brackets will automatically get turned into links.
        | http://www.example.com or <http://www.example.com> and sometimes
        | example.com (but not on Github, for example).
        |
        | Some text to show that the reference links can follow later.
        |
        | [arbitrary case-insensitive reference text]: https://www.mozilla.org
        | [1]: http://slashdot.org
        | [link text itself]: http://www.reddit.com
         """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    A(
                                        listOf(Text("I'm an inline-style link")),
                                        mapOf("href" to "https://www.google.com")
                                    )
                                )
                            ),
                            P(
                                listOf(
                                    A(
                                        listOf(Text("I'm an inline-style link with title")),
                                        mapOf("href" to "https://www.google.com", "title" to "Google's Homepage")
                                    )
                                )
                            ),
                            P(
                                listOf(
                                    A(
                                        listOf(Text("I'm a reference-style link")),
                                        mapOf("href" to "https://www.mozilla.org")
                                    )
                                )
                            ),
                            P(
                                listOf(
                                    A(
                                        listOf(Text("You can use numbers for reference-style link definitions")),
                                        mapOf("href" to "http://slashdot.org")
                                    )
                                )
                            ),
                            P(
                                listOf(
                                    Text("Or leave it empty and use the "),
                                    A(
                                        listOf(Text("link text itself")),
                                        mapOf("href" to "http://www.reddit.com")
                                    ),
                                    Text(".")
                                )
                            ),
                            P(
                                listOf(
                                    Text("URLs and URLs in angle brackets will automatically get turned into links. http://www.example.com or "),
                                    A(
                                        listOf(Text("http://www.example.com")),
                                        mapOf("href" to "http://www.example.com")
                                    ),
                                    Text(" and sometimes example.com (but not on Github, for example).")
                                )
                            ),
                            P(listOf(Text("Some text to show that the reference links can follow later.")))
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Windows Carriage Return Line Feed`() {
        val kdoc = "text\r\ntext"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text("text text")
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun image() {
        val kdoc = "![Sample image](https://www.google.pl/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png)"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Img(
                                        emptyList(),
                                        mapOf(
                                            "href" to "https://www.google.pl/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png",
                                            "alt" to "Sample image"
                                        )
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Bold + italic + link`() {
        val kdoc = "It's very easy to make some words **bold** and other words *italic* with Markdown.\n" +
                "You can even [link to Google!](http://google.com)"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text("It's very easy to make some words "),
                                    B(listOf(Text("bold"))),
                                    Text(" and other words "),
                                    I(listOf(Text("italic"))),
                                    Text(" with Markdown. You can even "),
                                    A(listOf(Text("link to Google!")), mapOf("href" to "http://google.com"))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Codeblock from indent`() {
        val kdoc = "Here is some example how to use conditional instructions:\n\n" +
                "    val x = 1\n" +
                "    val y = 2\n" +
                "    if (x == 1) {\n" +
                "        println(y)\n" +
                "    }"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(listOf(Text("Here is some example how to use conditional instructions:"))),
                            CodeBlock(
                                listOf(
                                    Text(
                                        "    val x = 1\n" +
                                                "    val y = 2\n" +
                                                "    if (x == 1) {\n" +
                                                "        println(y)\n" +
                                                "    }"
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Table`() {
        val kdoc = "First Header | Second Header\n" +
                "------------ | -------------\n" +
                "Content from cell 1 | Content from cell 2\n" +
                "Content in the first column | Content in the second column"
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            Table(
                                listOf(
                                    Th(
                                        listOf(
                                            Td(
                                                listOf(
                                                    Text("First Header")
                                                )
                                            ),
                                            Td(
                                                listOf(
                                                    Text("Second Header")
                                                )
                                            )
                                        )
                                    ),
                                    Tr(
                                        listOf(
                                            Td(
                                                listOf(
                                                    Text("Content from cell 1")
                                                )
                                            ),
                                            Td(
                                                listOf(
                                                    Text("Content from cell 2")
                                                )
                                            )
                                        )
                                    ),
                                    Tr(
                                        listOf(
                                            Td(
                                                listOf(
                                                    Text("Content in the first column")
                                                )
                                            ),
                                            Td(
                                                listOf(
                                                    Text("Content in the second column")
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `Text with Strikethrough`() {
        val kdoc = """
        | This is ~~strikethroughed~~
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text("This is "),
                                    Strikethrough(listOf(Text("strikethroughed")))
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `short link without destination`() {
        val kdoc = """
        | This is [link]()
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            P(
                                listOf(
                                    Text("This is "),
                                    A(
                                        listOf(Text("link")),
                                        mapOf("href" to "")
                                    )
                                )
                            )
                        ), name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }

    @Test
    fun `exception thrown by empty header should point to location of a file`() {
        val kdoc = """
        | ###
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(emptyList())
        val exception = runCatching { executeTest(kdoc, expectedDocumentationNode) }.exceptionOrNull()

        assertEquals(
            "Wrong AST Tree. Header does not contain expected content in Test.kt/example.Test, element starts from offset 0 and ends 3: ###",
            exception?.message
        )
    }

    @Test
    fun `should ignore html comments`() {
        val kdoc = """
        | # Example <!--- not visible in header --> Kdoc
        | <!-- not visible alone -->
        | Pre <!--- not visible --> visible
        """.trimMargin()
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        listOf(
                            H1(
                                listOf(
                                    Text("Example "),
                                    Text("<!--- not visible in header -->", params = mapOf("content-type" to "html")),
                                    Text(" Kdoc")
                                )
                            ),
                            Text("<!-- not visible alone -->", params = mapOf("content-type" to "html")),
                            P(
                                listOf(
                                    Text("Pre "),
                                    Text("<!--- not visible -->", params = mapOf("content-type" to "html")),
                                    Text(" visible")
                                )
                            )
                        ),
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(kdoc, expectedDocumentationNode)
    }
}

