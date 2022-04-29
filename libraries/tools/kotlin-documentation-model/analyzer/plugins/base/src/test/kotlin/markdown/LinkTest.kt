package markdown

import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.doc.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LinkTest : BaseAbstractTest() {
    @Test
    fun linkToClassLoader() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/parser")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/parser/Test.kt
            |package parser
            |
            | /**
            | * Some docs that link to [ClassLoader.clearAssertionStatus]
            | */
            |fun test(x: ClassLoader) = x.clearAssertionStatus()
            |
        """.trimMargin(),
            configuration
        ) {
            renderingStage = { rootPageNode, _ ->
                assertNotNull((rootPageNode.children.single().children.single() as MemberPageNode)
                    .content
                    .dfs { node ->
                        node is ContentDRILink &&
                                node.address.toString() == "parser//test/#java.lang.ClassLoader/PointingToDeclaration/"
                    }
                )
            }
        }
    }

    @Test
    fun returnTypeShouldHaveLinkToOuterClassFromInner() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                    displayName = "JVM"
                }
            }
        }
        //This does not contain a package to check for situation when the package has to be artificially generated
        testInline(
            """
            |/src/main/kotlin/parser/Test.kt
            |
            |class Outer<OUTER> {
            |   inner class Inner<INNER> {
            |       fun foo(): OUTER = TODO()
            |   }
            |}
        """.trimMargin(),
            configuration
        ) {
            renderingStage = { rootPageNode, _ ->
                val root = rootPageNode.children.single().children.single() as ClasslikePageNode
                val innerClass = root.children.first { it is ClasslikePageNode }
                val foo = innerClass.children.first { it.name == "foo" } as MemberPageNode
                val destinationDri = (root.documentables.firstOrNull() as WithGenerics).generics.first().dri.toString()

                assertEquals(destinationDri, "/Outer///PointingToGenericParameters(0)/")
                assertNotNull(foo.content.dfs { it is ContentDRILink && it.address.toString() == destinationDri })
            }
        }
    }

    @Test
    fun `link to parameter #238`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package example
            |
            |/** 
            |* Link to [waitAMinute]
            |*/
            |fun stop(hammerTime: String, waitAMinute: String) {}
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val parameter = module.dfs { it.name == "waitAMinute" }
                val link = module.dfs { it.name == "stop" }!!.documentation.values.single()
                    .dfs { it is DocumentationLink } as DocumentationLink

                assertEquals(parameter!!.dri, link.dri)
            }
        }
    }

    @Test
    fun `link with exclamation mark`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package example
            |
            |/** 
            |* Link to ![waitAMinute]
            |*/
            |fun stop(hammerTime: String, waitAMinute: String) {}
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val functionDocs = module.packages.flatMap { it.functions }.first().documentation.values.first()
                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("Link to !"),
                                    DocumentationLink(
                                        dri = DRI(
                                            packageName = "example",
                                            callable = Callable(
                                                "stop",
                                                receiver = null,
                                                params = listOf(
                                                    TypeConstructor("kotlin.String", emptyList()),
                                                    TypeConstructor("kotlin.String", emptyList())
                                                )
                                            ),
                                            target = PointingToCallableParameters(1)
                                        ),
                                        children = listOf(
                                            Text("waitAMinute")
                                        ),
                                        params = mapOf("href" to "[waitAMinute]")
                                    )
                                )
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expected, functionDocs.children.first())
            }
        }
    }

    @Test
    fun `link to property with exclamation mark`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/** 
            |* Link to ![Testing.property]
            |*/
            |class Testing {
            |   var property = ""
            |}
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val functionDocs = module.packages.flatMap { it.classlikes }.first().documentation.values.first()
                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("Link to !"),
                                    DocumentationLink(
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = "Testing",
                                            callable = Callable("property", null, emptyList()),
                                            target = PointingToDeclaration
                                        ),
                                        children = listOf(
                                            Text("Testing.property")
                                        ),
                                        params = mapOf("href" to "[Testing.property]")
                                    )
                                )
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )

                assertEquals(expected, functionDocs.children.first())
            }
        }
    }
}
