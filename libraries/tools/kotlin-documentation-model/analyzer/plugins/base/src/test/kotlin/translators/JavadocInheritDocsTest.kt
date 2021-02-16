package translators

import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class JavadocInheritDocsTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
            }
        }
    }

    @Test
    fun `work when whole description is inherited`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            |* Superclass docs
            |*/
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* {@inheritDoc}
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Superclass docs"))
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `work when inherited part is inside description`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            |* Superclass docs
            |*/
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. Superclass docs End of subclass docs"))
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `work when inherited part is empty`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. End of subclass docs"))
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    @Disabled("This should be enabled when we have proper tag inheritance in javadoc parser")
    fun `work when inherited part is empty in supertype but present in its supertype`() {
        testInline(
            """
            |/src/main/java/sample/SuperSuperclass.java
            |package sample;
            |/**
            |* Super super docs
            |*/
            |public class SuperSuperclass { }
            |/src/main/java/sample/Superclass.java
            |package sample;
            |public class Superclass extends SuperSuperClass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. Super super docs End of subclass docs"))
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    //Original javadoc doesn't treat interfaces as valid candidates for inherit doc
    fun `work with interfaces`() {
        testInline(
            """
            |/src/main/java/sample/SuperInterface.java
            |package sample;
            |/**
            |* Super super docs
            |*/
            |public interface SuperInterface { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public interface Subclass extends SuperInterface { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. End of subclass docs"))
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }


    @Test
    fun `work with multiple supertypes`() {
        testInline(
            """
            |/src/main/java/sample/SuperInterface.java
            |package sample;
            |/**
            |* Super interface docs
            |*/
            |public interface SuperInterface { }
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            |* Super class docs
            |*/
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public class Subclass extends Superclass implements SuperInterface { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. Super class docs End of subclass docs"))
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `work with methods`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |public class Superclass {
            |/**
            |* Sample super method
            |*
            |* @return super string
            |* @throws RuntimeException super throws
            |* @see java.lang.String super see
            |* @deprecated super deprecated
            |*/
            |public String test() {
            |    return "";
            |}
            |}
            |/src/main/java/sample/Subclass.java
            |package sample;
            |public class Subclass extends Superclass {
            |    /**
            |     * Sample sub method. {@inheritDoc}
            |     */
            |    @Override
            |    public String test() {
            |        return super.test();
            |    }
            |}
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.findFunction("sample", "Subclass", "test")
                val descriptionGot = function.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Sample sub method. Sample super method"))
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }
}