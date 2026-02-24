/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package parsers.javadoc

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.A
import org.jetbrains.dokka.model.doc.BlockQuote
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.Em
import org.jetbrains.dokka.model.doc.H1
import org.jetbrains.dokka.model.doc.H2
import org.jetbrains.dokka.model.doc.H3
import org.jetbrains.dokka.model.doc.H4
import org.jetbrains.dokka.model.doc.H5
import org.jetbrains.dokka.model.doc.H6
import org.jetbrains.dokka.model.doc.Li
import org.jetbrains.dokka.model.doc.Ol
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Strong
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.dokka.model.doc.Table
import org.jetbrains.dokka.model.doc.TBody
import org.jetbrains.dokka.model.doc.Td
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.doc.Th
import org.jetbrains.dokka.model.doc.THead
import org.jetbrains.dokka.model.doc.Tr
import org.jetbrains.dokka.model.doc.Ul
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavadocMarkdownTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `markdown version of javadoc for hashCode`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// Returns a hash code value for the object. This method is
            | /// supported for the benefit of hash tables such as those provided by
            | /// [java.util.HashMap].
            | ///
            | /// The general contract of `hashCode` is:
            | ///
            | ///   - Whenever it is invoked on the same object more than once during
            | ///     an execution of a Java application, the `hashCode` method
            | ///     must consistently return the same integer, provided no information
            | ///     used in `equals` comparisons on the object is modified.
            | ///     This integer need not remain consistent from one execution of an
            | ///     application to another execution of the same application.
            | ///   - If two objects are equal according to the
            | ///     [equals][#equals(Object)] method, then calling the
            | ///     `hashCode` method on each of the two objects must produce the
            | ///     same integer result.
            | ///   - It is _not_ required that if two objects are unequal
            | ///     according to the [equals][#equals(Object)] method, then
            | ///     calling the `hashCode` method on each of the two objects
            | ///     must produce distinct integer results.  However, the programmer
            | ///     should be aware that producing distinct integer results for
            | ///     unequal objects may improve the performance of hash tables.
            | ///
            | /// @return  a hash code value for this object.
            | /// @see     java.lang.Object#equals(java.lang.Object)
            | /// @see     java.lang.System#identityHashCode
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                // Description
                val descriptionRoot = getFirstClassDocumentationNodeChildren(modules).root

                assertEquals(
                    listOf(
                        P(
                            children = listOf(
                                Text("Returns a hash code value for the object. This method is supported for the benefit of hash tables such as those provided by "),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.util",
                                        classNames = "HashMap"
                                    ),
                                    children = listOf(Text("java.util.HashMap"))
                                ),
                                Text(".")
                            )
                        ),
                        P(
                            children = listOf(
                                Text("The general contract of "),
                                CodeInline(
                                    children = listOf(Text("hashCode")),
                                    params = mapOf("lang" to "java")
                                ),
                                Text(" is:")
                            )
                        ),
                        Ul(
                            children = listOf(
                                Li(
                                    children = listOf(
                                        Text("Whenever it is invoked on the same object more than once during an execution of a Java application, the "),
                                        CodeInline(
                                            children = listOf(Text("hashCode")),
                                            params = mapOf("lang" to "java")
                                        ),
                                        Text(" method must consistently return the same integer, provided no information used in "),
                                        CodeInline(
                                            children = listOf(Text("equals")),
                                            params = mapOf("lang" to "java")
                                        ),
                                        Text(" comparisons on the object is modified. This integer need not remain consistent from one execution of an application to another execution of the same application.")
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        Text("If two objects are equal according to the "),
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "Object",
                                                callable = Callable(
                                                    name = "equals",
                                                    params = listOf(JavaClassReference("java.lang.Object"))
                                                )
                                            ),
                                            children = listOf(Text("equals"))
                                        ),
                                        Text(" method, then calling the "),
                                        CodeInline(
                                            children = listOf(Text("hashCode")),
                                            params = mapOf("lang" to "java")
                                        ),
                                        Text(" method on each of the two objects must produce the same integer result.")
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        Text("It is "),
                                        Em(
                                            children = listOf(Text("not"))
                                        ),
                                        Text(" required that if two objects are unequal according to the "),
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "Object",
                                                callable = Callable(
                                                    name = "equals",
                                                    params = listOf(JavaClassReference("java.lang.Object"))
                                                )
                                            ),
                                            children = listOf(Text("equals"))
                                        ),
                                        Text(" method, then calling the "),
                                        CodeInline(
                                            children = listOf(Text("hashCode")),
                                            params = mapOf("lang" to "java")
                                        ),
                                        Text(" method on each of the two objects must produce distinct integer results. However, the programmer should be aware that producing distinct integer results for unequal objects may improve the performance of hash tables.")
                                    )
                                )
                            )
                        )
                    ),
                    descriptionRoot.children
                )

                // @return
                val returnRoot = getFirstClassDocumentationNodeChildren(modules, 1).root
                assertEquals(
                    listOf(
                        P(
                            children = listOf(
                                Text("a hash code value for this object.")
                            )
                        )
                    ),
                    returnRoot.children
                )

                // @see first
                val see1Root = getFirstClassDocumentationNodeChildren(modules, 2)
                assertTrue { see1Root is See }
                assertEquals(
                    DRI(
                        packageName = "java.lang",
                        classNames = "Object",
                        callable = Callable(
                            name = "equals",
                            params = listOf(JavaClassReference("java.lang.Object"))
                        )
                    ),
                    (see1Root as See).address
                )

                // @see second
                val see2Root = getFirstClassDocumentationNodeChildren(modules, 3)
                assertTrue { see2Root is See }
                assertEquals(
                    DRI(
                        packageName = "java.lang",
                        classNames = "System",
                        callable = Callable(
                            name = "identityHashCode",
                            params = listOf(JavaClassReference("java.lang.Object"))
                        )
                    ),
                    (see2Root as See).address
                )
            }
        }
    }

    @Test
    fun `markdown code block`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// Inline `code` block.
            | /// Traditional code block:
            | /// ```
            | /// /** Hello World! */
            | /// public class HelloWorld {
            | ///     public static void main(String... args) {
            | ///         System.out.println("Hello World!"); // the traditional example
            | ///     }
            | /// }
            | /// ```
            | /// Code block with specified language:
            | /// ```kotlin
            | /// val sum: (Int, Int) -> Int = { x: Int, y: Int -> x + y }
            | /// ```
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocumentationNodeChildren(modules).root

                assertEquals(
                    listOf(
                        P(
                            children = listOf(
                                Text("Inline "),
                                CodeInline(
                                    children = listOf(Text("code")),
                                    params = mapOf("lang" to "java")
                                ),
                                Text(" block. Traditional code block:")
                            )
                        ),
                        CodeBlock(
                            children = listOf(
                                Text("/** Hello World! */\npublic class HelloWorld {\n    public static void main(String... args) {\n        System.out.println(\"Hello World!\"); // the traditional example\n    }\n}")
                            ),
                            params = mapOf("lang" to "java")
                        ),
                        P(
                            children = listOf(
                                Text("Code block with specified language:")
                            )
                        ),
                        CodeBlock(
                            children = listOf(
                                Text("val sum: (Int, Int) -> Int = { x: Int, y: Int -> x + y }")
                            ),
                            params = mapOf("lang" to "kotlin")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `markdown reference link`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// - a package [java.util]
            | /// - a class [String]
            | /// - a field [String#CASE_INSENSITIVE_ORDER]
            | /// - a method [String#isEmpty()]
            | /// - [the java.util package][java.util]
            | /// - [a class][String]
            | /// - [a field][String#CASE_INSENSITIVE_ORDER]
            | /// - [a method][String#isEmpty()]
            | /// - escaped square brackets in reference [String#copyValueOf(char\[\])]
            | /// - [jetbrains](https://www.jetbrains.com/)
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocumentationNodeChildren(modules).root

                assertEquals(
                    listOf(
                        Ul(
                            children = listOf(
                                Li(
                                    children = listOf(
                                        Text("a package "),
                                        DocumentationLink(
                                            dri = DRI(packageName = "java.util"),
                                            children = listOf(Text("java.util"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        Text("a class "),
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "String"
                                            ),
                                            children = listOf(Text("String"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        Text("a field "),
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "String",
                                                callable = Callable(
                                                    name = "CASE_INSENSITIVE_ORDER",
                                                    params = emptyList(),
                                                    isProperty = true
                                                )
                                            ),
                                            children = listOf(Text("String#CASE_INSENSITIVE_ORDER"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        Text("a method "),
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "String",
                                                callable = Callable(
                                                    name = "isEmpty",
                                                    params = emptyList()
                                                )
                                            ),
                                            children = listOf(Text("String#isEmpty()"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        DocumentationLink(
                                            dri = DRI(packageName = "java.util"),
                                            children = listOf(Text("the java.util package"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "String"
                                            ),
                                            children = listOf(Text("a class"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "String",
                                                callable = Callable(
                                                    name = "CASE_INSENSITIVE_ORDER",
                                                    params = emptyList(),
                                                    isProperty = true
                                                )
                                            ),
                                            children = listOf(Text("a field"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "String",
                                                callable = Callable(
                                                    name = "isEmpty",
                                                    params = emptyList()
                                                )
                                            ),
                                            children = listOf(Text("a method"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        Text("escaped square brackets in reference "),
                                        DocumentationLink(
                                            dri = DRI(
                                                packageName = "java.lang",
                                                classNames = "String",
                                                callable = Callable(
                                                    name = "copyValueOf",
                                                    params = listOf(JavaClassReference("char[]"))
                                                )
                                            ),
                                            children = listOf(Text("String#copyValueOf(char[])"))
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        A(
                                            children = listOf(Text("jetbrains")),
                                            params = mapOf("href" to "https://www.jetbrains.com/")
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `basic text formatting`() {
        // those tags are currently not supported in HtmlToDocTagConverter:
        // - <sub>Subscript</sub> is created using HTML sub tags
        // - <sup>Superscript</sup> is created using HTML sup tags
        // ## Horizontal Rule
        // A horizontal rule can be created using three or more asterisks or underscores:
        // ***
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// ## Inline Formatting
            | /// - **Bold text** is created using double asterisks or double underscores
            | /// - *Italic text* is created using single asterisks or single underscores
            | /// - ***Bold and italic*** is created using triple asterisks
            | /// - `Inline code` is created using backticks
            | ///
            | /// ## Escaping Special Characters
            | /// You can escape special characters using a backslash:
            | /// \*This text is not in italics\*
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocumentationNodeChildren(modules).root

                assertEquals(
                    listOf(
                        H2(
                            children = listOf(Text("Inline Formatting"))
                        ),
                        Ul(
                            children = listOf(
                                Li(
                                    children = listOf(
                                        Strong(
                                            children = listOf(Text("Bold text"))
                                        ),
                                        Text(" is created using double asterisks or double underscores")
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        Em(
                                            children = listOf(Text("Italic text"))
                                        ),
                                        Text(" is created using single asterisks or single underscores")
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        Em(
                                            children = listOf(
                                                Strong(
                                                    children = listOf(Text("Bold and italic"))
                                                )
                                            )
                                        ),
                                        Text(" is created using triple asterisks")
                                    )
                                ),
                                Li(
                                    children = listOf(
                                        CodeInline(
                                            children = listOf(Text("Inline code")),
                                            params = mapOf("lang" to "java")
                                        ),
                                        Text(" is created using backticks")
                                    )
                                )
                            )
                        ),
                        H2(
                            children = listOf(Text("Escaping Special Characters"))
                        ),
                        P(
                            children = listOf(
                                Text("You can escape special characters using a backslash: *This text is not in italics*")
                            )
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun headings() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// # Heading 1
            | /// ## Heading 2
            | /// ### Heading 3
            | /// #### Heading 4
            | /// ##### Heading 5
            | /// ###### Heading 6
            | ///
            | /// Alternatively, you can use the following syntax for heading 1 and 2:
            | ///
            | /// Heading 1
            | /// =========
            | ///
            | /// Heading 2
            | /// ---------
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocumentationNodeChildren(modules).root

                assertEquals(
                    listOf(
                        H1(
                            children = listOf(Text("Heading 1"))
                        ),
                        H2(
                            children = listOf(Text("Heading 2"))
                        ),
                        H3(
                            children = listOf(Text("Heading 3"))
                        ),
                        H4(
                            children = listOf(Text("Heading 4"))
                        ),
                        H5(
                            children = listOf(Text("Heading 5"))
                        ),
                        H6(
                            children = listOf(Text("Heading 6"))
                        ),
                        P(
                            children = listOf(
                                Text("Alternatively, you can use the following syntax for heading 1 and 2:")
                            )
                        ),
                        H1(
                            children = listOf(Text("Heading 1"))
                        ),
                        H2(
                            children = listOf(Text("Heading 2"))
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun lists() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// ## Unordered Lists
            | /// - Item 1
            | /// - Item 2
            | ///   - Subitem 2.1
            | ///   - Subitem 2.2
            | /// - Item 3
            | ///
            | /// You can also use asterisks or plus signs:
            | /// * Item A
            | /// * Item B
            | /// + Item X
            | /// + Item Y
            | ///
            | /// ## Ordered Lists
            | /// 1. First item
            | /// 2. Second item
            | ///    1. Subitem 2.1
            | ///    2. Subitem 2.2
            | /// 3. Third item
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocumentationNodeChildren(modules).root

                assertEquals(
                    listOf(
                        H2(
                            children = listOf(Text("Unordered Lists"))
                        ),
                        Ul(
                            children = listOf(
                                Li(
                                    children = listOf(Text("Item 1"))
                                ),
                                Li(
                                    children = listOf(
                                        Text("Item 2"),
                                        Ul(
                                            children = listOf(
                                                Li(children = listOf(Text("Subitem 2.1"))),
                                                Li(children = listOf(Text("Subitem 2.2")))
                                            )
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(Text("Item 3"))
                                )
                            )
                        ),
                        P(
                            children = listOf(Text("You can also use asterisks or plus signs:"))
                        ),
                        Ul(
                            children = listOf(
                                Li(children = listOf(Text("Item A"))),
                                Li(children = listOf(Text("Item B")))
                            )
                        ),
                        Ul(
                            children = listOf(
                                Li(children = listOf(Text("Item X"))),
                                Li(children = listOf(Text("Item Y")))
                            )
                        ),
                        H2(
                            children = listOf(Text("Ordered Lists"))
                        ),
                        Ol(
                            children = listOf(
                                Li(
                                    children = listOf(Text("First item"))
                                ),
                                Li(
                                    children = listOf(
                                        Text("Second item"),
                                        Ol(
                                            children = listOf(
                                                Li(children = listOf(Text("Subitem 2.1"))),
                                                Li(children = listOf(Text("Subitem 2.2")))
                                            )
                                        )
                                    )
                                ),
                                Li(
                                    children = listOf(Text("Third item"))
                                )
                            )
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun tables() {
        // table alignment is currently not supported in Dokka
        // /// ## Alignment
        // ///
        // /// | Left-aligned | Center-aligned | Right-aligned |
        // /// |:-------------|:-------------:|-------------:|
        // /// | Left | Center | Right |
        // /// | Left | Center | Right |
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// ## Basic Table
            | ///
            | /// | Header 1 | Header 2 | Header 3 |
            | /// |----------|----------|----------|
            | /// | Row 1, Col 1 | Row 1, Col 2 | Row 1, Col 3 |
            | /// | Row 2, Col 1 | Row 2, Col 2 | Row 2, Col 3 |
            | /// | Row 3, Col 1 | Row 3, Col 2 | Row 3, Col 3 |
            | ///
            | /// ## Complex Table
            | ///
            | /// | Method | Description | Return Type | Throws |
            | /// |--------|-------------|-------------|--------|
            | /// | `get(Object key)` | Returns the value to which the specified key is mapped | `V` | `ClassCastException`, `NullPointerException` |
            | /// | `put(K key, V value)` | Associates the specified value with the specified key | `V` | `ClassCastException`, `NullPointerException`, `UnsupportedOperationException` |
            | /// | `remove(Object key)` | Removes the mapping for a key from this map if it is present | `V` | `UnsupportedOperationException`, `ClassCastException`, `NullPointerException` |
            | ///
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocumentationNodeChildren(modules).root

                assertEquals(
                    listOf(
                        H2(children = listOf(Text("Basic Table"))),
                        Table(
                            children = listOf(
                                THead(
                                    children = listOf(
                                        Tr(
                                            children = listOf(
                                                Th(children = listOf(Text("Header 1"))),
                                                Th(children = listOf(Text("Header 2"))),
                                                Th(children = listOf(Text("Header 3")))
                                            )
                                        )
                                    )
                                ),
                                TBody(
                                    children = listOf(
                                        Tr(
                                            children = listOf(
                                                Td(children = listOf(Text("Row 1, Col 1"))),
                                                Td(children = listOf(Text("Row 1, Col 2"))),
                                                Td(children = listOf(Text("Row 1, Col 3")))
                                            )
                                        ),
                                        Tr(
                                            children = listOf(
                                                Td(children = listOf(Text("Row 2, Col 1"))),
                                                Td(children = listOf(Text("Row 2, Col 2"))),
                                                Td(children = listOf(Text("Row 2, Col 3")))
                                            )
                                        ),
                                        Tr(
                                            children = listOf(
                                                Td(children = listOf(Text("Row 3, Col 1"))),
                                                Td(children = listOf(Text("Row 3, Col 2"))),
                                                Td(children = listOf(Text("Row 3, Col 3")))
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        H2(children = listOf(Text("Complex Table"))),
                        Table(
                            children = listOf(
                                THead(
                                    children = listOf(
                                        Tr(
                                            children = listOf(
                                                Th(children = listOf(Text("Method"))),
                                                Th(children = listOf(Text("Description"))),
                                                Th(children = listOf(Text("Return Type"))),
                                                Th(children = listOf(Text("Throws")))
                                            )
                                        )
                                    )
                                ),
                                TBody(
                                    children = listOf(
                                        Tr(
                                            children = listOf(
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("get(Object key)")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                ),
                                                Td(children = listOf(Text("Returns the value to which the specified key is mapped"))),
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("V")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                ),
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("ClassCastException")),
                                                            params = mapOf("lang" to "java")
                                                        ),
                                                        Text(", "),
                                                        CodeInline(
                                                            children = listOf(Text("NullPointerException")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                )
                                            )
                                        ),
                                        Tr(
                                            children = listOf(
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("put(K key, V value)")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                ),
                                                Td(children = listOf(Text("Associates the specified value with the specified key"))),
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("V")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                ),
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("ClassCastException")),
                                                            params = mapOf("lang" to "java")
                                                        ),
                                                        Text(", "),
                                                        CodeInline(
                                                            children = listOf(Text("NullPointerException")),
                                                            params = mapOf("lang" to "java")
                                                        ),
                                                        Text(", "),
                                                        CodeInline(
                                                            children = listOf(Text("UnsupportedOperationException")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                )
                                            )
                                        ),
                                        Tr(
                                            children = listOf(
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("remove(Object key)")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                ),
                                                Td(children = listOf(Text("Removes the mapping for a key from this map if it is present"))),
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("V")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                ),
                                                Td(
                                                    children = listOf(
                                                        CodeInline(
                                                            children = listOf(Text("UnsupportedOperationException")),
                                                            params = mapOf("lang" to "java")
                                                        ),
                                                        Text(", "),
                                                        CodeInline(
                                                            children = listOf(Text("ClassCastException")),
                                                            params = mapOf("lang" to "java")
                                                        ),
                                                        Text(", "),
                                                        CodeInline(
                                                            children = listOf(Text("NullPointerException")),
                                                            params = mapOf("lang" to "java")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun blockquotes() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /// ## Basic Blockquote
            | /// > Blockquotes are very handy in email to emulate reply text.
            | /// > This line is part of the same quote.
            | /// 
            | /// Quote break.
            | /// 
            | /// > Quote
            | /// 
            | /// ## Nested Blockquote
            | /// > text 1
            | /// > text 2
            | /// >> text 3
            | /// >> text 4
            | /// >
            | /// > text 5
            | /// 
            | /// Quote break.
            | /// 
            | /// > Quote
            | /// 
            | /// ## Blockquote Right After Text
            | /// text
            | /// > quote
            | /// 
            | /// ## Blockquote Right After Text Inside Code Block
            | /// ```
            | /// text
            | /// > quote
            | /// > quote
            | /// ```
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocumentationNodeChildren(modules).root

                assertEquals(
                    listOf(
                        H2(children = listOf(Text("Basic Blockquote"))),
                        BlockQuote(
                            children = listOf(
                                P(children = listOf(Text("Blockquotes are very handy in email to emulate reply text. This line is part of the same quote.")))
                            )
                        ),
                        P(children = listOf(Text("Quote break."))),
                        BlockQuote(
                            children = listOf(
                                P(children = listOf(Text("Quote")))
                            )
                        ),
                        H2(children = listOf(Text("Nested Blockquote"))),
                        BlockQuote(
                            children = listOf(
                                P(children = listOf(Text("text 1 text 2"))),
                                BlockQuote(
                                    children = listOf(
                                        P(children = listOf(Text("text 3 text 4")))
                                    )
                                ),
                                P(children = listOf(Text("text 5")))
                            )
                        ),
                        P(children = listOf(Text("Quote break."))),
                        BlockQuote(
                            children = listOf(
                                P(children = listOf(Text("Quote")))
                            )
                        ),
                        H2(children = listOf(Text("Blockquote Right After Text"))),
                        P(children = listOf(Text("text"))),
                        BlockQuote(
                            children = listOf(
                                P(children = listOf(Text("quote")))
                            )
                        ),
                        H2(children = listOf(Text("Blockquote Right After Text Inside Code Block"))),
                        CodeBlock(
                            children = listOf(
                                Text("text\n> quote\n> quote")
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    private fun getFirstClassDocumentationNodeChildren(modules: List<DModule>, childrenNumber: Int = 0) =
        modules.filterNot { it.packages.isEmpty() }.single().packages.first().classlikes.single().documentation.values.first().children[childrenNumber]
}
