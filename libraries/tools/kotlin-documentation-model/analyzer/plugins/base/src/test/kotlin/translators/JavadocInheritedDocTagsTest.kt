package translators

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.jetbrains.dokka.model.doc.Deprecated as DokkaDeprecatedTag
import org.jetbrains.dokka.model.doc.Throws as DokkaThrowsTag

class JavadocInheritedDocTagsTest : BaseAbstractTest() {
    @Suppress("DEPRECATION") // for includeNonPublic
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
                includeNonPublic = true
            }
        }
    }

    private fun performTagsTest(test: (DModule) -> Unit) {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            |* @author super author
            |*/
            |class Superclass {
            |    /**
            |     * Sample super method
            |     *
            |     * @return super string
            |     * @throws RuntimeException super throws
            |     * @see java.lang.String super see
            |     * @deprecated super deprecated
            |     */
            |    public String test(){
            |        return "";
            |    }
            |
            |     /**
            |      *
            |      * @param xd String superclass
            |      * @param asd Integer superclass
            |      */
            |    public void test2(String xd, Integer asd){
            |    }
            |}
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* @author Ja, {@inheritDoc}
            |*/
            |class Subclass extends Superclass {
            |/**
            | * Sample sub method. {@inheritDoc}
            | *
            | * @return "sample string". {@inheritDoc}
            | * @throws RuntimeException because i can, {@inheritDoc}
            | * @throws IllegalStateException this should be it {@inheritDoc}
            | * @see java.lang.String string, {@inheritDoc}
            | * @deprecated do not use, {@inheritDoc}
            | */
            | @Override
            | public String test() {
            |    return super.test();
            | }
            |
            |    /**
            |     *
            |     * @param asd2 integer subclass, {@inheritDoc}
            |     * @param xd2 string subclass, {@inheritDoc}
            |     */
            |    public void test2(String xd2, Integer asd2){
            |    }
            |}
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = test
        }
    }

    @Test
    fun `work with return`() {
        performTagsTest { module ->
            val function = module.findFunction("sample", "Subclass", "test")
            val renderedTag = function.documentation.values.first().children.firstIsInstance<Return>()
            val expectedTag = Return(
                CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(Text("\"sample string\". super string"))
                        )
                    ),
                    name = "MARKDOWN_FILE"
                )
            )

            assertEquals(expectedTag, renderedTag)
        }
    }

    @Test
    fun `work with throws`() {
        performTagsTest { module ->
            val function = module.findFunction("sample", "Subclass", "test")
            val renderedTag =
                function.documentation.values.first().children.first { it is DokkaThrowsTag && it.name == "java.lang.RuntimeException" }
            val expectedTag = DokkaThrowsTag(
                CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(Text("because i can, super throws"))
                        )
                    ),
                    name = "MARKDOWN_FILE"
                ),
                "java.lang.RuntimeException",
                DRI("java.lang", "RuntimeException", target = PointingToDeclaration)
            )

            assertEquals(expectedTag, renderedTag)
        }
    }

    @Test
    fun `work with throws when exceptions are different`() {
        performTagsTest { module ->
            val function = module.findFunction("sample", "Subclass", "test")
            val renderedTag =
                function.documentation.values.first().children.first { it is DokkaThrowsTag && it.name == "java.lang.IllegalStateException" }
            val expectedTag = DokkaThrowsTag(
                CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(Text("this should be it"))
                        )
                    ),
                    name = "MARKDOWN_FILE"
                ),
                "java.lang.IllegalStateException",
                DRI("java.lang", "IllegalStateException", target = PointingToDeclaration)
            )

            assertEquals(expectedTag, renderedTag)
        }
    }

    @Test
    fun `work with deprecated`() {
        performTagsTest { module ->
            val function = module.findFunction("sample", "Subclass", "test")
            val renderedTag = function.documentation.values.first().children.firstIsInstance<DokkaDeprecatedTag>()
            val expectedTag = DokkaDeprecatedTag(
                CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(Text("do not use, Sample super method"))
                        )
                    ),
                    name = "MARKDOWN_FILE"
                ),
            )

            assertEquals(expectedTag, renderedTag)
        }
    }

    @Test
    fun `work with see also`() {
        performTagsTest { module ->
            val function = module.findFunction("sample", "Subclass", "test")
            val renderedTag = function.documentation.values.first().children.firstIsInstance<See>()
            val expectedTag = See(
                CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(Text("string,"))
                        )
                    ),
                    name = "MARKDOWN_FILE"
                ),
                "java.lang.String",
                DRI("java.lang", "String")
            )

            assertEquals(expectedTag, renderedTag)
        }
    }

    @Test
    fun `work with author`() {
        performTagsTest { module ->
            val classlike = module.findClasslike("sample", "Subclass")
            val renderedTag = classlike.documentation.values.first().children.firstIsInstance<Author>()
            val expectedTag = Author(
                CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(Text("Ja, super author"))
                        )
                    ),
                    name = "MARKDOWN_FILE"
                ),
            )

            assertEquals(expectedTag, renderedTag)
        }
    }

    @Test
    fun `work with params`() {
        performTagsTest { module ->
            val function = module.findFunction("sample", "Subclass", "test2")
            val (asdTag, xdTag) = function.documentation.values.first().children.filterIsInstance<Param>()
                .sortedBy { it.name }

            val expectedAsdTag = Param(
                CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(Text("integer subclass, Integer superclass"))
                        )
                    ),
                    name = "MARKDOWN_FILE"
                ),
                "asd2"
            )
            val expectedXdTag = Param(
                CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(Text("string subclass, String superclass"))
                        )
                    ),
                    name = "MARKDOWN_FILE"
                ),
                "xd2"
            )
            assertEquals(expectedAsdTag, asdTag)
            assertEquals(expectedXdTag, xdTag)
        }
    }
}