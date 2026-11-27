/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package markdown

import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.analyzer.ids.callableNameOf
import org.jetbrains.kotlin.documentation.analyzer.ids.ownerDeclarationIdOf
import org.jetbrains.kotlin.documentation.analyzer.ids.receiverDeclarationId
import org.jetbrains.kotlin.documentation.analyzer.ids.valueParameterDeclarationId
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInAnalysisApi
import utils.name
import utils.text
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@MustRunOnChangesInAnalysisApi
class LinkTest : BaseAbstractTest() {

    internal val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOfNotNull(jvmStdlibPath)
            }
        }
    }

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
            documentablesMergingStage = { module ->
                // a link to a member resolves to the member's own ID
                assertEquals(
                    KdElementId("java.lang/ClassLoader#clearAssertionStatus#KK4ERoLQuv0"),
                    module.getLinkDeclarationIdFrom("test")
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
            documentablesMergingStage = { module ->
                val outer = module.dfs { it.name == "Outer" } as DClass
                val inner = outer.classlikes.single { it.name == "Inner" } as DClass
                val foo = inner.functions.single { it.name == "foo" }

                val fooType = foo.type as TypeParameter
                // the return type carries the ID of the declaration that declares the referenced type parameter
                assertEquals(outer.declarationId, fooType.containerDeclarationId)
                assertEquals(outer.generics.first().name, fooType.name)
            }
        }
    }

    @Test
    fun `link to parameter #238`() {
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
                val stop = module.dfs { it.name == "stop" } as DFunction
                val parameter = stop.parameters.single { it.name == "waitAMinute" }
                val link = stop.documentation.values.single()
                    .dfs { it is DocumentationLink } as DocumentationLink

                // a parameter link points to the parameter's own ID
                assertEquals(valueParameterDeclarationId(stop.declarationId, 1), link.declarationId)
                // while the parameter itself has a synthetic ID hanging off the function's ID
                assertEquals(valueParameterDeclarationId(stop.declarationId, 1), parameter.declarationId)
            }
        }
    }

    @Test
    fun `link to this keyword with receiver to some class`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package example
            |
            |/**
            |* Link to [this]
            |*/
            |fun String.stop() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fn = module.dfs { it.name == "stop" } as DFunction
                val link = fn.documentation.values.single()
                    .dfs { it is DocumentationLink } as DocumentationLink

                assertEquals(receiverDeclarationId(fn.declarationId), link.declarationId)
            }
        }
    }

    @Test
    fun `link to this keyword with receiver to type parameter`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package example
            |
            |/** 
            |* Link to [this]
            |*/
            |fun <T> T.stop() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fn = module.dfs { it.name == "stop" } as DFunction
                val link = fn.documentation.values.single()
                    .dfs { it is DocumentationLink } as DocumentationLink

                // a receiver link points to the receiver documentable of the owner callable
                assertEquals(receiverDeclarationId(fn.declarationId), link.declarationId)
            }
        }
    }

    @Test
    fun `link to this keyword with receiver of dynamic that is prohibited by compiler`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package example
            |
            |/**
            |* Link to [this]
            |*/
            |fun dynamic.stop() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fn = module.dfs { it.name == "stop" } as DFunction
                val link = fn.documentation.values.single()
                    .dfs { it is DocumentationLink } as DocumentationLink

                // the link points to the receiver documentable even for the prohibited `dynamic` receiver
                assertEquals(receiverDeclarationId(fn.declarationId), link.declarationId)
            }
        }
    }

    @Test
    fun `link to this keyword with receiver of DNN-type`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |package example
            |
            |/**
            |* Link to [this]
            |*/
            |fun <T> (T&Any).stop() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fn = module.dfs { it.name == "stop" } as DFunction
                val link = fn.documentation.values.single()
                    .dfs { it is DocumentationLink } as DocumentationLink

                // a receiver link points to the receiver documentable of the owner callable
                assertEquals(receiverDeclarationId(fn.declarationId), link.declarationId)
            }
        }
    }

    @Test
    fun `link with exclamation mark`() {
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
                                        declarationId = KdElementId("example#stop#70vAuuBWtk0#param[1]"),
                                        children = listOf(
                                            Text("waitAMinute")
                                        ),
                                        params = mapOf("href" to "[waitAMinute]")
                                    )
                                )
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expected, functionDocs.children.first())
            }
        }
    }

    @Test
    fun `link to property with exclamation mark`() {
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
                                        declarationId = KdElementId("example/Testing#property#ctE8McQtNZQ"),
                                        children = listOf(
                                            Text("Testing.property")
                                        ),
                                        params = mapOf("href" to "[Testing.property]")
                                    )
                                )
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expected, functionDocs.children.first())
            }
        }
    }

    @Test
    fun `link should be resolved in @constructor section`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            |* @constructor reference in constructor [AllKDocTagsClass]
            | */
            |class AllKDocTagsClass(paramInt: Int, paramStr: String = "100"){}
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val functionDocs = (module.packages.flatMap { it.classlikes }
                    .first() as DClass).constructors.first().documentation.values.first()
                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("reference in constructor "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example/AllKDocTagsClass"),
                                        children = listOf(
                                            Text("AllKDocTagsClass")
                                        ),
                                        params = mapOf("href" to "[AllKDocTagsClass]")
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
    fun `link should lead to List class rather than function`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * ref to [List] or [Set]
            | */
            |fun x(){}
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
                                    Text("ref to "),
                                    DocumentationLink(
                                        declarationId = KdElementId("kotlin.collections/List"),
                                        children = listOf(
                                            Text("List")
                                        ),
                                        params = mapOf("href" to "[List]")
                                    ),
                                    Text(" or "),
                                    DocumentationLink(
                                        declarationId = KdElementId("kotlin.collections/Set"),
                                        children = listOf(
                                            Text("Set")
                                        ),
                                        params = mapOf("href" to "[Set]")
                                    ),
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
    fun `link should lead to a function with a nullable parameter`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * ref to [java.applet.AppletContext.showDocument]
            | */
            |fun x(){}
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
                                    Text("ref to "),
                                    DocumentationLink(
                                        declarationId = KdElementId("java.applet/AppletContext#showDocument#najpYeH8v3E"),
                                        children = listOf(
                                            Text("java.applet.AppletContext.showDocument")
                                        ),
                                        params = mapOf("href" to "[java.applet.AppletContext.showDocument]")
                                    ),
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
    fun `link should lead to function rather than property`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |fun x(p: Int){}
            |val x = 0
            |/**
            | * ref to fun [x]
            | */
            |val x2 = 0
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val propDocs =
                    module.packages.flatMap { it.properties }.first { it.name == "x2" }.documentation.values.first()
                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("ref to fun "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example#x#hDKzvCWNlUY"),
                                        children = listOf(
                                            Text("x")
                                        ),
                                        params = mapOf("href" to "[x]")
                                    )
                                )
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )
                assertEquals(expected, propDocs.children.first())
            }
        }
    }

    @Test
    fun `fully qualified link should lead to function`() {
        // for the test case, there is the only one link candidate in K1 and K2
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * refs to the function [example.fn] and the property [example.x]
            | */
            |val x = 0
            |
            |/**
            | * refs to the function [example.fn] and the property [example.x]
            | */
            |fun fn(p: Int){}
            |
            |/src/main/kotlin/Testing2.kt
            |package example.fn
            |
            |fun fn(p: Int){}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val propDocs =
                    module.packages.flatMap { it.properties }.first { it.name == "x" }.documentation.values.first()

                val fnDocs =
                    module.packages.first { it.name == "example" }.functions.first { it.name == "fn" }.documentation.values.first()

                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("refs to the function "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example#fn#hDKzvCWNlUY"),
                                        children = listOf(
                                            Text("example.fn")
                                        ),
                                        params = mapOf("href" to "[example.fn]")
                                    ),
                                    Text(" and the property "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example#x#ctE8McQtNZQ"),
                                        children = listOf(
                                            Text("example.x")
                                        ),
                                        params = mapOf("href" to "[example.x]")
                                    )
                                )
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )
                assertEquals(expected, propDocs.children.first())
                assertEquals(expected, fnDocs.children.first())
            }
        }
    }


    @Test
    fun `short link should lead to class rather than package`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * refs to the class [example]
            | */
            |val x = 0
            |
            |class example
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val propDocs =
                    module.packages.flatMap { it.properties }.first { it.name == "x" }.documentation.values.first()


                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("refs to the class "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example/example"),
                                        children = listOf(
                                            Text("example")
                                        ),
                                        params = mapOf("href" to "[example]")
                                    ),
                                )
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )
                assertEquals(expected, propDocs.children.first())
            }
        }
    }

    @Test
    fun `short link should lead to function`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * refs to the function [example]
            | */
            |val x = 0
            |
            |fun example() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val propDocs =
                    module.packages.flatMap { it.properties }.first { it.name == "x" }.documentation.values.first()


                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("refs to the function "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example#example#KK4ERoLQuv0"),
                                        children = listOf(
                                            Text("example")
                                        ),
                                        params = mapOf("href" to "[example]")
                                    ),
                                )
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )
                assertEquals(expected, propDocs.children.first())
            }
        }
    }

    @Test
    fun `link should be stable for overloads`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * refs to the overload [f] and [f2]
            | */
            |val x = 0
            |
            |fun f(i: Int) {}
            |fun f(s: String) {}
            |
            |fun f2(i: String) {}
            |fun f2(s: Int) {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val propDocs =
                    module.packages.flatMap { it.properties }.first { it.name == "x" }.documentation.values.first()


                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("refs to the overload "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example#f#hDKzvCWNlUY"),
                                        children = listOf(
                                            Text("f")
                                        ),
                                        params = mapOf("href" to "[f]")
                                    ),

                                    Text(" and "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example#f2#P7n-P2YFLsg"),
                                        children = listOf(
                                            Text("f2")
                                        ),
                                        params = mapOf("href" to "[f2]")
                                    ),
                                )
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )
                assertEquals(expected, propDocs.children.first())
            }
        }
    }

    @Test
    fun `link should be stable for overloads in different files`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * refs to the overload [f]
            | */
            |val x = 0
            |
            |fun f(i: String) {}
            |
            |/src/main/kotlin/Testing2.kt
            |package example
            |
            |fun f(s: Int) {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val propDocs =
                    module.packages.flatMap { it.properties }.first { it.name == "x" }.documentation.values.first()


                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("refs to the overload "),
                                    DocumentationLink(
                                        declarationId = KdElementId("example#f#P7n-P2YFLsg"),
                                        children = listOf(
                                            Text("f")
                                        ),
                                        params = mapOf("href" to "[f]")
                                    ),
                                )
                            )
                        ),
                        name = "MARKDOWN_FILE"
                    )
                )
                assertEquals(expected, propDocs.children.first())
            }
        }
    }

    @Test
    fun `full link should lead to an extension`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |/**
            | * [Bar.bar]
            | */
            |fun usage() {}
            |
            |class Bar
            |fun Bar.bar() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    KdElementId("example#bar#ISBoUF6tO3Y"),
                    module.getLinkDeclarationIdFrom("usage")
                )
            }
        }
    }

    @Test
    fun `K2 full link should lead to an extension with type params`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |fun <T : Number> List<T>.foo() {}
            |
            |interface MyListWithT<T> : List<T>
            |
            |interface MyListWithTNumberBound<T : Number> : List<T>
            |
            |interface MyListWithNumber : List<Number>
            |
            |/**
            | * 1 [List.foo]
            | * 2 [MutableList.foo] is resolved only in K2, but unresolved in K1
            | * 3 [MyListWithT.foo] is resolved only in K2, but unresolved in K1
            | * 4 [MyListWithTNumberBound.foo]
            | * 5 [MyListWithNumber.foo]
            | */
            |fun usage() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fooId = KdElementId("example#foo#slM7akVXc1o")
                assertEquals(
                    listOf(
                        "List.foo" to fooId,
                        "MutableList.foo" to fooId,
                        "MyListWithT.foo" to fooId,
                        "MyListWithTNumberBound.foo" to fooId,
                        "MyListWithNumber.foo" to fooId,
                    ), module.getAllLinkDeclarationIdsFrom("usage")
                )
            }
        }
    }

    // based on https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0385-kdoc-links-to-extensions.md
    @Test
    fun `links to extensions with type parameters should be resolved correctly`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |/**
            | * all should be resolved to extensions on Iterable
            | * - [Iterable.map]
            | * - [Iterable.flatMap]
            | * - [Iterable.flatten]
            | * - [Iterable.min]
            | * - [List.map]
            | * - [List.flatMap]
            | * - [List.flatten]
            | * - [List.min]
            | */
            |fun usage() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val links = module.getAllLinkDeclarationIdsFrom("usage").toMap()

                fun assertResolvesTo(link: String) {
                    // each link resolves to the top-level extension in `kotlin.collections`:
                    // its own callable ID with that package as the owner
                    val id = links.getValue(link)
                    assertEquals(KdElementId("kotlin.collections"), ownerDeclarationIdOf(id))
                    assertEquals(link.substringAfter('.'), callableNameOf(id))
                }

                // there should be all 8 resolved links
                assertEquals(links.size, 8)
                assertResolvesTo("Iterable.map")
                assertResolvesTo("Iterable.flatMap")
                assertResolvesTo("Iterable.flatten")
                assertResolvesTo("Iterable.min")
                assertResolvesTo("List.map")
                assertResolvesTo("List.flatMap")
                assertResolvesTo("List.flatten")
                assertResolvesTo("List.min")
            }
        }
    }

    @Test
    fun `full link should not lead to members of typealias #3521`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |typealias F = String
            |/**
            | * [example.F.length] is unresolved
            | * [String.length] is resolved
            | */
            |val usage = 0
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "String.length" to KdElementId("kotlin/String#length#ctE8McQtNZQ")
                    ),
                    module.getAllLinkDeclarationIdsFrom("usage")
                )
            }
        }
    }

    @Test
    fun `full link should lead to typealias`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |typealias F = String
            |/**
            | * Refs to typealiases [kotlin.collections.ArrayList] and [kotlin.IllegalArgumentException]
            | * and [example.F]
            | */
            |val usage = 0
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "kotlin.collections.ArrayList" to KdElementId("kotlin.collections/ArrayList"),
                        "kotlin.IllegalArgumentException" to KdElementId("kotlin/IllegalArgumentException"),
                        "example.F" to KdElementId("example/F")
                    ),
                    module.getAllLinkDeclarationIdsFrom("usage")
                )
            }
        }
    }

    @Test
    fun `full link should lead to typealias from another file`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * refs to typealias [demos.F]
            | */
            |val usage = 0
            |/src/main/kotlin/demos/F.kt
            |package demos
            |typealias F = String
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(KdElementId("demos/F"), module.getLinkDeclarationIdFrom("usage"))
            }
        }
    }

    @Test
    fun `KDoc link should lead to java annotation methods`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * [Storage.value]
            | */
            |val usage = 0
            |/src/example/Storage.java
            |package example;
            |@interface Storage {
            |  String value() default "";
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    KdElementId("example/Storage#value#ctE8McQtNZQ"),
                    module.getLinkDeclarationIdFrom("usage")
                )
            }
        }
    }

    @Test
    fun `KDoc link should lead to java synthetic properties`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * value: [Storage.value] is unresolved
            | * setValue: [Storage.setValue]
            | * prop: [Storage.prop] is resolve in K2, but unresolved in K1
            | */
            |val usage = 0
            |/src/example/Storage.java
            |package example;
            |class Storage {
            |    void setValue(String value) {}
            |    String getProp() { return null; }
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "Storage.setValue" to KdElementId("example/Storage#setValue#VMudsCHwk_c"),
                        // refers to a Java synthetic property, see https://youtrack.jetbrains.com/issue/KT-86394
                        "Storage.prop" to KdElementId("example/Storage#prop#ctE8McQtNZQ")
                    ),
                    module.getAllLinkDeclarationIdsFrom("usage")
                )
            }
        }
    }

    @Test
    fun `KDoc link should be unresolved to non-existed property with the name of Kotlin getter #3681`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |fun getProperty() = 0
            |/**
            |* [property] is unresolved
            | */
            |fun usage() = 0
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(null, module.getLinkDeclarationIdFrom("usage"))
            }
        }
    }

    @Test
    fun `should have a correct KDoc and external links when there is a newline inside link text`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |/**
            |* Text
            |* [some
            |* link](https://www.google.com/)
            |*
            |* Text:
            |* [some
            |*  declaration][usage]
            |*/
            |fun usage() = 0
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val doc = module.dfs { it.name == "usage" }?.documentation?.values?.single()
                    ?: throw IllegalStateException("Can't find documentation for declaration 'usage'")
                val externalLink = doc.firstMemberOfType<A>()
                assertEquals("some link", externalLink.children.first().text())
                assertEquals("https://www.google.com/", externalLink.params["href"])

                val docLink = doc.firstMemberOfType<DocumentationLink>()
                assertEquals("some declaration", docLink.children.first().text())
                assertEquals(KdElementId("#usage#KK4ERoLQuv0"), docLink.declarationId)
            }
        }
    }

    @Test
    fun `KDoc link should be resolved to two extensions with the same name #3631`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |class C
            |public fun C.ensureActive() {}
            |/**
            | * [C.ensureActive]
            | */
            |class B
            |/**
            | * [C.ensureActive]
            | */
            |public fun B.ensureActive() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val bEnsureActive = module.dfs {
                    it.name == "ensureActive" && (it as? DFunction)?.receiver?.type?.name == "B"
                }
                val link1 = bEnsureActive?.documentation?.values?.single()
                    ?.firstMemberOfTypeOrNull<DocumentationLink>()?.declarationId
                val link2 = module.getLinkDeclarationIdFrom("B")

                // both links point to the top-level extension `C.ensureActive`
                assertEquals(KdElementId("#ensureActive#yMbDuoEfqAI"), link1)
                assertEquals(link1, link2)
            }
        }
    }

    @Test
    fun `KDoc Link to a class with quoted name should be resolved`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |/**
            |* Class: [Quoted Class Name] is unresolved in K2, but resolved in K1
            |* [`Quoted Class Name`] is resolved in K2, but unresolved in K1
            |* [example.`Quoted Class Name`] is resolved in K2 and K1
            |*/
            |class `Quoted Class Name`
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "`Quoted Class Name`" to KdElementId("example/Quoted%20Class%20Name"),
                        "example.`Quoted Class Name`" to KdElementId("example/Quoted%20Class%20Name"),
                    ),
                    module.getAllLinkDeclarationIdsFrom("Quoted Class Name")
                )
            }
        }
    }

    @Test
    fun `should resolve KDoc links in module and package documentation`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    name = "moduleA"
                    sourceRoots = listOf("src/")
                    classpath = listOfNotNull(jvmStdlibPath)
                    includes = listOf("module.md")
                }
            }
        }
        testInline(
            """
            |/module.md
            |# Module root
            |
            |Link to [example.Foo]
            |
            |# Package example
            |
            |Link to [example.Foo] and [Bar]
            |
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |class Foo
            |class Bar
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "example.Foo" to KdElementId("example/Foo"),
                    ),
                    module.getAllLinkDeclarationIdsFrom("root")
                )
                assertEquals(
                    listOf(
                        "example.Foo" to KdElementId("example/Foo"),
                        "Bar" to KdElementId("example/Bar"),
                    ),
                    module.getAllLinkDeclarationIdsFrom("example")
                )
            }
        }
    }

    @Test
    fun `should resolve KDoc links in module and package documentation in source set without sources`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val a = sourceSet {
                    name = "moduleA"
                    classpath = listOfNotNull(jvmStdlibPath)
                    sourceRoots = listOf("src/")
                    includes = listOf("module.md")
                }
                sourceSet {
                    name = "moduleB"
                    classpath = listOfNotNull(jvmStdlibPath)
                    includes = listOf("module.md")
                    dependentSourceSets = setOf(a.value.sourceSetID)
                }
            }
        }
        testInline(
            """
            |/module.md
            |# Module root
            |
            |Link to [example.Foo]
            |
            |# Package example
            |
            |Link to [example.Foo] and [Bar]
            |
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |class Foo
            |class Bar
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = {
                // as `moduleB` has no sources, and so has no declarations it will be filtered out
                // still, as we resolve links earlier in the pipeline,
                // unresolved links will cause unnecessary warning messages in logs,
                // while not affecting the output.
                // so the only way to check that all links were resolved is to check, that there were no `logger.warn` calls
                assertEquals(emptyList(), logger.warnMessages)
            }
        }
    }

    @Test
    fun `should resolve KDoc links in the second line of @param tag`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |interface Call
            |/**
            | * @param text some description with reference.
            | *     But with a few lines and indent [Call]
            | */
            |fun protocol(text: String) {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "Call" to KdElementId("example/Call"),
                    ),
                    module.getAllLinkDeclarationIdsFrom("protocol")
                )
            }
        }
    }

    @Test
    fun `should resolve KDoc links in the second line of @constructor tag`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |interface Call
            |/**
            |* @constructor text some description with reference.
            |*     But with a few lines and indent [Call]
            |*/
            |class A(val text: String)
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "Call" to KdElementId("example/Call"),
                    ),
                    module.getAllLinkDeclarationIdsFrom("A")
                )
            }
        }
    }

    @Test
    fun `should resolve KDoc links in the second level of a list`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |interface IllegalTimeZoneException
            |interface UTC
            |
            |/**
            | * ...
            | * How exactly the time zone is acquired is system-dependent. The current implementation:
            | * - JVM: `java.time.ZoneId.systemDefault()` is queried.
            | * - Kotlin/Native:
            | *     - Darwin: first, `NSTimeZone.resetSystemTimeZone` is called to clear the cache of the system timezone.
            | *       Then, `NSTimeZone.systemTimeZone.name` is used to obtain the up-to-date timezone name.
            | *     - Linux: this function checks the `/etc/localtime` symbolic link.
            | *       If the link is missing, [UTC] is used.
            | *       If the file is not a link but a plain file,
            | *       the contents of `/etc/timezone` are additionally checked for the timezone name.
            | *       [IllegalTimeZoneException] is thrown if the timezone name cannot be determined
            | *       or is invalid.
            | * ...
            | */
            |fun currentSystemDefault(g) {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "UTC" to KdElementId("example/UTC"),
                        "IllegalTimeZoneException" to KdElementId("example/IllegalTimeZoneException"),
                    ),
                    module.getAllLinkDeclarationIdsFrom("currentSystemDefault")
                )
            }
        }
    }

    @Test
    fun `should resolve KDoc links to context parameter`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |/**
            | * [s]
            | */
            |context(s: String)
            |fun saveFromResponse()
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    KdElementId("example#saveFromResponse#GXn-IuN_dvE#context-param[0]"),
                    module.getLinkDeclarationIdFrom("saveFromResponse")
                )
            }
        }
    }

    @Test
    fun `should resolve KDoc links that goes after markdown blocks`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |interface JavaNetCookieJar
            |
            |/**
            | * Markdown syntax ```code```
            | * This references doesn't work: [System.currentTimeMillis] and [JavaNetCookieJar].
            | */
            |fun saveFromResponse(url: String)
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "System.currentTimeMillis" to KdElementId("java.lang/System#currentTimeMillis#jIdvB92_wHY"),
                        "JavaNetCookieJar" to KdElementId("example/JavaNetCookieJar"),
                    ),
                    module.getAllLinkDeclarationIdsFrom("saveFromResponse")
                )
            }
        }
    }

    @Test
    fun `should resolve KDoc link in indented @param tag`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |interface Call
            |
            |/**
            | * @param text some description with reference.
            | *     But with a few lines and indent [Call]
            | */
            |fun protocol(text: String) {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    KdElementId("example/Call"),
                    module.getLinkDeclarationIdFrom("protocol")
                )
            }
        }
    }

    @Test
    fun `should warn about unresolved links`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |
            |/**
            |* [property] is unresolved
            | */
            |fun usage() = 0
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { m ->
                val warn = logger.warnMessages.first()
                val path = m.sourceSets.first().sourceRoots.first().invariantSeparatorsPath

                assertEquals(
                    "Couldn't resolve link: [property] in file:///PATH/main/kotlin/Testing.kt:2:3 (root/main)",
                    warn.replace(path, "PATH")
                )
            }
        }
    }

    // copy-pasted org.jetbrains.dokka.analysis.test.api.util.getResourceAbsolutePath
    private fun getResourceAbsolutePath(resourcePath: String): String {
        val resource = object {}.javaClass.classLoader.getResource(resourcePath)?.file
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        return File(resource).absolutePath
    }

    @Test
    fun `should resolve fq KDoc links from unpacked klib`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/commonMain/kotlin")
                    analysisPlatform = "native"
                    name = "example"
                    classpath = listOf(
                        getResourceAbsolutePath("klibs/example"),
                    )
                }

            }
        }
        testInline(
            """
                |/src/commonMain/kotlin/main.kt
                |package example
                |import com.example.A
                |/**
                | * Links [com.example.A] and [A]
                |*/
                |fun f() = 0
            """,
            configuration = configuration
        ) {
            documentablesMergingStage = { module ->
                val classId = KdElementId("com.example/A")
                val allLinks = module.getAllLinkDeclarationIdsFrom("f")
                assertEquals(
                    listOf(
                        "com.example.A" to classId,
                        "A" to classId,
                    ),
                    allLinks
                )
            }

        }
    }

    @Test
    fun `should resolve KDoc links from classpath`() {
        val coroutines = ClassLoader.getSystemResource("kotlinx/coroutines/MainCoroutineDispatcher.class")
            ?.file
            ?.replace("file:", "")
            ?.replaceAfter(".jar", "") ?: throw IllegalStateException("Coroutines jar not found")

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/jvmMain/kotlin")
                    analysisPlatform = "jvm"
                    name = "jvm-example"
                    classpath = listOf(coroutines)
                }

            }
        }
        testInline(
            """
                |/src/jvmMain/kotlin/main.kt
                |package example
                |import kotlinx.coroutines.MainCoroutineDispatcher
                |/**
                | * Links [MainCoroutineDispatcher.immediate] and [kotlinx.coroutines.MainCoroutineDispatcher.immediate]
                |*/
                |class TestClass
            """,
            configuration = configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "MainCoroutineDispatcher.immediate" to KdElementId("kotlinx.coroutines/MainCoroutineDispatcher#immediate#ctE8McQtNZQ"),
                        "kotlinx.coroutines.MainCoroutineDispatcher.immediate" to KdElementId("kotlinx.coroutines/MainCoroutineDispatcher#immediate#ctE8McQtNZQ"),
                    ),
                    module.getAllLinkDeclarationIdsFrom("TestClass")
                )
            }

        }
    }

    @Test
    fun `KDoc links to companion object, companion block and companion extensions`() {
        testInline(
            """
            |/src/main/kotlin/example/Test.kt
            |package example
            |
            |data class Vector(val x: Double, val y: Double) {
            |    companion object {
            |        val ZeroObject: Vector get() = Vector(0.0, 0.0)
            |    }
            |    companion {
            |        val ZeroBlock: Vector get() = Vector(0.0, 0.0)
            |    }
            |}
            |
            |val Vector.Companion.UnitXObjectExtension get() = Vector(1.0, 0.0)
            |
            |companion val Vector.UnitXExtension get() = Vector(1.0, 0.0)
            |
            |/**
            | * - [Vector.ZeroObject]
            | * - [Vector.Companion.ZeroObject]
            | * - [Vector.ZeroBlock]
            | * - [Vector.Companion.ZeroBlock] is unresolved
            | * - [Vector.UnitXObjectExtension]
            | * - [Vector.UnitXExtension]
            | * - [Vector.Companion.UnitXObjectExtension]
            | * - [Vector.Companion.UnitXExtension] is unresolved
            | */
            |fun usage() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                // a property of the `companion object` - a regular member of `Vector.Companion`
                val zeroObject = KdElementId("example/Vector.Companion#ZeroObject#ctE8McQtNZQ")
                // a property of the `companion { }` block - a member of `Vector`
                val zeroBlock = KdElementId("example/Vector#ZeroBlock#IfqD6rn7vBU")
                val unitXObjectExtension = KdElementId("example#UnitXObjectExtension#XlBjAzG0_ms")
                val unitXExtension = KdElementId("example#UnitXExtension#B2o17J_ttyQ")

                assertEquals(
                    listOf(
                        "Vector.ZeroObject" to zeroObject,
                        "Vector.Companion.ZeroObject" to zeroObject,
                        "Vector.ZeroBlock" to zeroBlock,
                        // [Vector.Companion.ZeroBlock] is intentionally unresolved:
                        // a companion-block member is not a member of the `Companion` object
                        "Vector.UnitXObjectExtension" to unitXObjectExtension,
                        "Vector.UnitXExtension" to unitXExtension,
                        "Vector.Companion.UnitXObjectExtension" to unitXObjectExtension,
                        // [Vector.Companion.UnitXExtension] is intentionally unresolved:
                        // a companion extension extends `Vector`, not `Vector.Companion`
                    ),
                    module.getAllLinkDeclarationIdsFrom("usage")
                )
            }
        }
    }

    private fun DModule.getLinkDeclarationIdFrom(name: String): KdElementId? {
        val doc = this.dfs { it.name == name }?.documentation?.values?.single()
            ?: throw IllegalStateException("Can't find documentation for declaration '$name'")
        val link = doc.firstMemberOfTypeOrNull<DocumentationLink>()
        return link?.declarationId
    }

    private fun DModule.getAllLinkDeclarationIdsFrom(name: String): List<Pair<String, KdElementId>> {
        val result = mutableListOf<Pair<String, KdElementId>>()
        this.dfs { it.name == name }?.documentation?.values?.single()?.dfs {
            if (it is DocumentationLink) result.add(it.textWithCodeInline() to it.declarationId)
            false
        }
        return result
    }

    /**
     * Adapted from [DocTag.text]
     */
    internal fun DocTag.textWithCodeInline(): String = when (val t = this) {
        is Text -> t.body
        is CodeInline -> "`" + t.children.joinToString("\n") { it.textWithCodeInline() } + "`"
        is Code -> t.children.joinToString("\n") { it.textWithCodeInline() }
        is P -> t.children.joinToString("") { it.textWithCodeInline() } + "\n"
        else -> t.children.joinToString("") { it.textWithCodeInline() }
    }
}
