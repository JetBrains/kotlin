package basic

import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.Nullable
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DRITest : BaseAbstractTest() {
    @Test
    fun issue634() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package toplevel
            |
            |inline fun <T, R : Comparable<R>> Array<out T>.mySortBy(
            |    crossinline selector: (T) -> R?): Array<out T> = TODO()
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val expected = TypeConstructor(
                    "kotlin.Function1", listOf(
                        TypeParam(listOf(Nullable(TypeConstructor("kotlin.Any", emptyList())))),
                        Nullable(TypeParam(listOf(TypeConstructor("kotlin.Comparable", listOf(RecursiveType(0))))))
                    )
                )
                val actual = module.packages.single()
                    .functions.single()
                    .dri.callable?.params?.single()
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun issue634WithImmediateNullableSelf() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package toplevel
            |
            |fun <T : Comparable<T>> Array<T>.doSomething(t: T?): Array<T> = TODO()
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val expected = Nullable(TypeParam(listOf(TypeConstructor("kotlin.Comparable", listOf(RecursiveType(0))))))
                val actual = module.packages.single()
                    .functions.single()
                    .dri.callable?.params?.single()
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun issue634WithGenericNullableReceiver() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package toplevel
            |
            |fun <T : Comparable<T>> T?.doSomethingWithNullable() = TODO()
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val expected = Nullable(TypeParam(listOf(TypeConstructor("kotlin.Comparable", listOf(RecursiveType(0))))))
                val actual = module.packages.single()
                    .functions.single()
                    .dri.callable?.receiver
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun issue642WithStarAndAny() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    analysisPlatform = "js"
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt
            |
            |open class Bar<Z>
            |class ReBarBar : Bar<StringBuilder>()
            |class Foo<out T : Comparable<*>, R : List<Bar<*>>>
            |
            |fun <T : Comparable<Any?>> Foo<T, *>.qux(): String = TODO()
            |fun <T : Comparable<*>> Foo<T, *>.qux(): String = TODO()
            |
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = { module ->
                // DRI(//qux/Foo[TypeParam(bounds=[kotlin.Comparable[kotlin.Any?]]),*]#/PointingToFunctionOrClasslike/)
                val expectedDRI = DRI(
                    "",
                    null,
                    Callable(
                        "qux", TypeConstructor(
                            "Foo", listOf(
                                TypeParam(
                                    listOf(
                                        TypeConstructor(
                                            "kotlin.Comparable", listOf(
                                                Nullable(TypeConstructor("kotlin.Any", emptyList()))
                                            )
                                        )
                                    )
                                ),
                                StarProjection
                            )
                        ),
                        emptyList()
                    )
                )

                val driCount = module
                    .withDescendants()
                    .filterIsInstance<ContentPage>()
                    .sumBy { it.dri.count { dri -> dri == expectedDRI } }

                assertEquals(1, driCount)
            }
        }
    }

    @Test
    fun driForGenericClass(){
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
            |class Sample<S>(first: S){ }
            |
            |
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = { module ->
                val sampleClass = module.dfs { it.name == "Sample" } as ClasslikePageNode
                val classDocumentable = sampleClass.documentables.firstOrNull() as DClass

                assertEquals( "example/Sample///PointingToDeclaration/", sampleClass.dri.first().toString())
                assertEquals("example/Sample///PointingToGenericParameters(0)/", classDocumentable.generics.first().dri.toString())
            }
        }
    }

    @Test
    fun driForGenericFunction(){
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    classpath = listOfNotNull(jvmStdlibPath)
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package example
            |
            |class Sample<S>(first: S){
            |    fun <T> genericFun(param1: String): Tuple<S,T> = TODO()
            |}
            |
            |
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = { module ->
                val sampleClass = module.dfs { it.name == "Sample" } as ClasslikePageNode
                val functionNode = sampleClass.children.first { it.name == "genericFun" } as MemberPageNode
                val functionDocumentable = functionNode.documentables.firstOrNull() as DFunction
                val parameter = functionDocumentable.parameters.first()

                assertEquals("example/Sample/genericFun/#kotlin.String/PointingToDeclaration/", functionNode.dri.first().toString())

                assertEquals(1, functionDocumentable.parameters.size)
                assertEquals("example/Sample/genericFun/#kotlin.String/PointingToCallableParameters(0)/", parameter.dri.toString())
                //1 since from the function's perspective there is only 1 new generic declared
                //The other one is 'inherited' from class
                assertEquals( 1, functionDocumentable.generics.size)
                assertEquals( "T", functionDocumentable.generics.first().name)
                assertEquals( "example/Sample/genericFun/#kotlin.String/PointingToGenericParameters(0)/", functionDocumentable.generics.first().dri.toString())
            }
        }
    }

    @Test
    fun driForFunctionNestedInsideInnerClass() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    classpath = listOfNotNull(jvmStdlibPath)
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package example
            |
            |class Sample<S>(first: S){
            |    inner class SampleInner {
            |       fun foo(): S = TODO()
            |    }
            |}
            |
            |
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = { module ->
                val sampleClass = module.dfs { it.name == "Sample" } as ClasslikePageNode
                val sampleInner = sampleClass.children.first { it.name == "SampleInner" } as ClasslikePageNode
                val foo = sampleInner.children.first { it.name == "foo" } as MemberPageNode
                val documentable = foo.documentables.firstOrNull() as DFunction

                val generics = (sampleClass.documentables.firstOrNull() as WithGenerics).generics
                assertEquals(generics.first().dri.toString(), (documentable.type as TypeParameter).dri.toString())
                assertEquals(0, documentable.generics.size)
            }
        }
    }

    @Test
    fun driForGenericExtensionFunction(){
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
            | fun <T> List<T>.extensionFunction(): String = ""
            |
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = { module ->
                val extensionFunction = module.dfs { it.name == "extensionFunction" } as MemberPageNode
                val documentable = extensionFunction.documentables.firstOrNull() as DFunction

                assertEquals(
                    "example//extensionFunction/kotlin.collections.List[TypeParam(bounds=[kotlin.Any?])]#/PointingToDeclaration/",
                    extensionFunction.dri.first().toString()
                )
                assertEquals(1, documentable.generics.size)
                assertEquals("T", documentable.generics.first().name)
                assertEquals(
                    "example//extensionFunction/kotlin.collections.List[TypeParam(bounds=[kotlin.Any?])]#/PointingToGenericParameters(0)/",
                     documentable.generics.first().dri.toString()
                )

            }
        }
    }

    @Test
    fun `deep recursive typebound #1342`() {
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
            | fun <T, S, R> recursiveBound(t: T, s: S, r: R) where T: List<S>, S: List<R>, R: List<S> = Unit
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.dfs { it.name == "recursiveBound" }
                assertEquals(
                    "example//recursiveBound/#TypeParam(bounds=[kotlin.collections.List[TypeParam(bounds=[kotlin.collections.List[TypeParam(bounds=[kotlin.collections.List[^^]])]])]])#TypeParam(bounds=[kotlin.collections.List[TypeParam(bounds=[kotlin.collections.List[^]])]])#TypeParam(bounds=[kotlin.collections.List[TypeParam(bounds=[kotlin.collections.List[^]])]])/PointingToDeclaration/",
                    function?.dri?.toString(),
                )
            }
        }
    }
}
