package parsers

import com.jetbrains.rd.util.first
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import utils.text

class JavadocParserTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    private fun performJavadocTest(testOperation: (DModule) -> Unit) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Date2.java
            |
            |package docs
            |/**
            | * class level docs
            | */
            |public enum AnEnumType {
            |    /**
            |     * content being refreshed, which can be a result of
            |     * invalidation, refresh that may contain content updates, or the initial load.
            |     */
            |    REFRESH
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = testOperation
        }
    }

    @Test
    fun `correctly parsed list`() {
        performJavadocTest { module ->
            val docs = (module.packages.single().classlikes.single() as DEnum).entries.single().documentation.values.single().children.single().root.text()
            assertEquals("content being refreshed, which can be a result of invalidation, refresh that may contain content updates, or the initial load.", docs.trimEnd())
        }
    }

    @Test
    fun `code tag`() {
        val source = """
            |/src/main/kotlin/test/Test.java
            |package example
            |
            | /**
            | * Identifies calls to {@code assertThat}.
            | *
            | * {@code
            | * Set<String> s;
            | * System.out.println("s1 = " + s);
            | * }
            | * <pre>{@code
            | * Set<String> s2;
            | * System.out
            | *         .println("s2 = " + s2);
            | * }</pre>
            | * 
            | */
            | public class Test {}
            """.trimIndent()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.first().value
                val root = docs.children.first().root

                kotlin.test.assertEquals(
                    listOf(
                        Text(body = "Identifies calls to "),
                        CodeInline(children = listOf(Text(body = "assertThat"))),
                        Text(body = ". "),
                        CodeInline(children = listOf(Text(body = "\nSet<String> s;\nSystem.out.println(\"s1 = \" + s);\n")))
                    ),
                    root.children[0].children
                )
                kotlin.test.assertEquals(
                    CodeBlock(children = listOf(Text(body = "\nSet<String> s2;\nSystem.out\n        .println(\"s2 = \" + s2);\n"))),
                    root.children[1]
                )
            }
        }
    }

    @Test
    fun `literal tag`() {
        val source = """
            |/src/main/kotlin/test/Test.java
            |package example
            |
            | /**
            | * An example of using the literal tag
            | * {@literal @}Entity
            | * public class User {}
            | */
            | public class Test {}
            """.trimIndent()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.first().value
                val root = docs.children.first().root

                kotlin.test.assertEquals(
                    listOf(
                        Text(body = "An example of using the literal tag "),
                        Text(body = "@"),
                        Text(body = "Entity public class User {}"),
                    ),
                    root.children.first().children
                )
            }
        }
    }

    @Test
    fun `literal tag nested under pre tag`() {
        val source = """
            |/src/main/kotlin/test/Test.java
            |package example
            |
            | /**
            | * An example of using the literal tag
            | * <pre>
            | * {@literal @}Entity
            | * public class User {}
            | * </pre>
            | */
            | public class Test  {}
            """.trimIndent()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.first().value
                val root = docs.children.first().root

                kotlin.test.assertEquals(
                    listOf(
                        P(children = listOf(Text(body = "An example of using the literal tag "))),
                        Pre(children =
                            listOf(
                                Text(body = "@"),
                                Text(body = "Entity\npublic class User {}\n")
                            )
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `literal tag containing angle brackets`() {
        val source = """
            |/src/main/kotlin/test/Test.java
            |package example
            |
            | /**
            | * An example of using the literal tag
            | * {@literal a<B>c}
            | */
            | public class Test  {}
            """.trimIndent()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.first().value
                val root = docs.children.first().root

                kotlin.test.assertEquals(
                    listOf(
                        P(children = listOf(
                            Text(body = "An example of using the literal tag "),
                            Text(body = "a<B>c")
                        )),
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `html img tag`() {
        val source = """
            |/src/main/kotlin/test/Test.java
            |package example
            |
            | /**
            | * <img src="/path/to/img.jpg" alt="Alt text"/>
            | */
            | public class Test  {}
            """.trimIndent()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.first().value
                val root = docs.children.first().root

                kotlin.test.assertEquals(
                    listOf(
                        P(
                            children = listOf(
                                Img(
                                    params = mapOf(
                                        "href" to "/path/to/img.jpg",
                                        "alt" to "Alt text"
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
    fun `description list tag`() {
        val source = """
            |/src/main/kotlin/test/Test.java
            |package example
            |
            | /**
            | * <dl>
            | *     <dt>
            | *         <code>name="<i>name</i>"</code>
            | *     </dt>
            | *     <dd>
            | *         A URI path segment. The subdirectory name for this value is contained in the
            | *         <code>path</code> attribute.
            | *     </dd>
            | *     <dt>
            | *         <code>path="<i>path</i>"</code>
            | *     </dt>
            | *     <dd>
            | *         The subdirectory you're sharing. While the <i>name</i> attribute is a URI path
            | *         segment, the <i>path</i> value is an actual subdirectory name. 
            | *     </dd>
            | * </dl>
            | */
            | public class Test  {}
            """.trimIndent()

        val expected = listOf(
            Dl(
                listOf(
                    Dt(
                        listOf(
                            CodeInline(
                                listOf(
                                    Text("name=\""),
                                    I(
                                        listOf(
                                            Text("name")
                                        )
                                    ),
                                    Text("\"")
                                )
                            ),
                        )
                    ),
                    Dd(
                        listOf(
                            Text(" A URI path segment. The subdirectory name for this value is contained in the "),
                            CodeInline(
                                listOf(
                                    Text("path")
                                )
                            ),
                            Text(" attribute. ")
                        )
                    ),

                    Dt(
                        listOf(
                            CodeInline(
                                listOf(
                                    Text("path=\""),
                                    I(
                                        listOf(
                                            Text("path")
                                        )
                                    ),
                                    Text("\"")
                                )
                            )
                        )
                    ),
                    Dd(
                        listOf(
                            Text(" The subdirectory you're sharing. While the "),
                            I(
                                listOf(
                                    Text("name")
                                )
                            ),
                            Text(" attribute is a URI path segment, the "),
                            I(
                                listOf(
                                    Text("path")
                                )
                            ),
                            Text(" value is an actual subdirectory name. ")
                        )
                    )
                )
            )
        )

        testInline(source, configuration) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.first().value
                assertEquals(expected, docs.children.first().root.children)
            }
        }
    }
    
    @Test
    fun `header tags are handled properly`() {
        val source = """
            |/src/main/kotlin/test/Test.java
            |package example
            |
            | /**
            | * An example of using the header tags
            | * <h1>A header</h1>
            | * <h2>A second level header</h2>
            | * <h3>A third level header</h3>
            | */
            | public class Test  {}
            """.trimIndent()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.first().value
                val root = docs.children.first().root

                kotlin.test.assertEquals(
                    listOf(
                        P(children = listOf(Text("An example of using the header tags "))),
                        H1(
                            listOf(
                                Text("A header")
                            )
                        ),
                        H2(
                            listOf(
                                Text("A second level header")
                            )
                        ),
                        H3(
                            listOf(
                                Text("A third level header")
                            )
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `var tag is handled properly`() {
        val source = """
            |/src/main/kotlin/test/Test.java
            |package example
            |
            | /**
            | * An example of using var tag: <var>variable</var>
            | */
            | public class Test  {}
            """.trimIndent()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.first().value
                val root = docs.children.first().root

                kotlin.test.assertEquals(
                    listOf(
                        P(children = listOf(
                            Text("An example of using var tag: "),
                            Var(children = listOf(Text("variable"))),
                        )),
                    ),
                    root.children
                )
            }
        }
    }
}
