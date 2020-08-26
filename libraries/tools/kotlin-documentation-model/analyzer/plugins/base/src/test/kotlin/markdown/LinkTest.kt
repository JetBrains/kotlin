package markdown

import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LinkTest : AbstractCoreTest() {
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
                                node.address.toString() == "parser//test/#java.lang.ClassLoader/PointingToDeclaration/"}
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
                val destinationDri = (root.documentable as WithGenerics).generics.first().dri.toString()

                assertEquals(destinationDri, "/Outer///PointingToGenericParameters(0)/")
                assertNotNull(foo.content.dfs { it is ContentDRILink && it.address.toString() == destinationDri } )
            }
        }
    }
}
