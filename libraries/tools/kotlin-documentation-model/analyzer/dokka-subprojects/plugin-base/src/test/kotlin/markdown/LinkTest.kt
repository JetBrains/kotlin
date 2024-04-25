/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package markdown

import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.MemberPageNode
import utils.OnlyDescriptors
import utils.OnlySymbols
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LinkTest : BaseAbstractTest() {

    val configuration = dokkaConfiguration {
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
    @OnlySymbols("#3207 - In Dokka K1 [this] has an incorrect link that leads to a page of containing package")
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

                assertEquals(DRI("kotlin", "String"), link.dri)
            }
        }
    }

    @Test
    @OnlySymbols("#3207 In Dokka K1 [this] has an incorrect link that leads to a page of containing package")
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

                assertEquals(fn.generics.first().dri, link.dri)
            }
        }
    }

    @Test
    @OnlySymbols("#3207 In Dokka K1 [this] has an incorrect link that leads to a page of containing package")
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

                assertEquals(DRI(packageName = "", classNames = "<ERROR CLASS> dynamic"), link.dri)
            }
        }
    }

    @Test
    @OnlySymbols("#3207 In Dokka K1 [this] has an incorrect link that leads to a page of containing package")
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

                assertEquals(fn.generics.first().dri, link.dri)
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
                val functionDocs = (module.packages.flatMap { it.classlikes }.first() as DClass).constructors.first().documentation.values.first()
                val expected = Description(
                    root = CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    Text("reference in constructor "),
                                    DocumentationLink(
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = "AllKDocTagsClass",
                                            target = PointingToDeclaration
                                        ),
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
                                        dri = DRI(
                                            packageName = "kotlin.collections",
                                            classNames = "List",
                                            target = PointingToDeclaration
                                        ),
                                        children = listOf(
                                            Text("List")
                                        ),
                                        params = mapOf("href" to "[List]")
                                    ),
                                    Text(" or "),
                                    DocumentationLink(
                                        dri = DRI(
                                            packageName = "kotlin.collections",
                                            classNames = "Set",
                                            target = PointingToDeclaration
                                        ),
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
                                        dri = DRI(
                                            packageName = "java.applet",
                                            classNames = "AppletContext",
                                            callable = Callable(
                                                name = "showDocument",
                                                params = listOf(TypeConstructor("java.net.URL", emptyList()))
                                            ),
                                            target = PointingToDeclaration
                                        ),
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
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = null,
                                            target = PointingToDeclaration,
                                            callable = Callable(
                                                "x",
                                                params = listOf(TypeConstructor("kotlin.Int", emptyList()))
                                            )
                                        ),
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
    fun `fully qualified link should lead to package`() {
        // for the test case, there is the only one link candidate in K1 and K2
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * refs to the package [example.fn] and the property [example.x]
            | */
            |val x = 0
            |
            |/**
            | * refs to the package [example.fn] and the property [example.x]
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
                                    Text("refs to the package "),
                                    DocumentationLink(
                                        dri = DRI(
                                            packageName = "example.fn",
                                            classNames = null,
                                            target = PointingToDeclaration,
                                        ),
                                        children = listOf(
                                            Text("example.fn")
                                        ),
                                        params = mapOf("href" to "[example.fn]")
                                    ),
                                    Text(" and the property "),
                                    DocumentationLink(
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = null,
                                            target = PointingToDeclaration,
                                            callable = Callable(
                                                "x",
                                                params = emptyList()
                                            )
                                        ),
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
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = "example",
                                            target = PointingToDeclaration,
                                        ),
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
    fun `short link should lead to package rather than function`() {
        testInline(
            """
            |/src/main/kotlin/Testing.kt
            |package example
            |
            |/**
            | * refs to the package [example]
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
                                    Text("refs to the package "),
                                    DocumentationLink(
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = null,
                                            target = PointingToDeclaration,
                                        ),
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
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = null,
                                            target = PointingToDeclaration,
                                            callable = Callable(
                                                "f",
                                                params = listOf(TypeConstructor("kotlin.Int", emptyList()))
                                            )
                                        ),
                                        children = listOf(
                                            Text("f")
                                        ),
                                        params = mapOf("href" to "[f]")
                                    ),

                                    Text(" and "),
                                    DocumentationLink(
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = null,
                                            target = PointingToDeclaration,
                                            callable = Callable(
                                                "f2",
                                                params = listOf(TypeConstructor("kotlin.String", emptyList()))
                                            )
                                        ),
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
    @OnlyDescriptors("due to #3250 a result DRI is unstable")
    fun `K1 - link should be stable for overloads in different files`() {
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
                                        dri = DRI(
                                            packageName = "example",
                                            classNames = null,
                                            target = PointingToDeclaration,
                                            callable = Callable(
                                                "f",
                                                params = listOf(TypeConstructor("kotlin.String", emptyList()))
                                            )
                                        ),
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
                assertEquals(DRI("example", null, callable = Callable("bar", receiver = TypeConstructor("example.Bar", params = emptyList()), params = emptyList())), module.getLinkDRIFrom("usage"))
            }
        }
    }

    @Test
    @OnlyDescriptors("#3555")
    fun `K1 full link should lead to an extension with type params`() {
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
            | * 1 [List.foo] is resolved
            | * 2 [MutableList.foo] is unresolved
            | * 3 [MyListWithT.foo] is unresolved
            | * 4 [MyListWithTNumberBound.foo] is resolved in K1
            | * 5 [MyListWithNumber.foo] is resolved in K1
            | */
            |fun usage() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fooDRI = DRI(
                    "example",
                    null,
                    callable = Callable(
                        "foo",
                        receiver = TypeConstructor("kotlin.collections.List", params = listOf(TypeParam(listOf(TypeConstructor("kotlin.Number", emptyList()))))),
                        params = emptyList()
                    )
                )
                assertEquals(
                    listOf(
                        "List.foo" to fooDRI,
                        "MyListWithTNumberBound.foo" to fooDRI,
                        "MyListWithNumber.foo" to DRI(
                            "example",
                            null,
                            callable = Callable(
                                "foo",
                                receiver = TypeConstructor(
                                    "kotlin.collections.List",
                                    params = listOf((TypeConstructor("kotlin.Number", emptyList())))
                                ),
                                params = emptyList()
                            )
                        )
                    ), module.getAllLinkDRIFrom("usage")
                )
            }
        }
    }

    @Test
    @OnlySymbols("#3555")
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
            | * 1 [List.foo] is resolved
            | * 2 [MutableList.foo] is unresolved
            | * 3 [MyListWithT.foo] is unresolved
            | * 4 [MyListWithTNumberBound.foo] is unresolved in K2
            | * 5 [MyListWithNumber.foo] is unresolved in K2
            | */
            |fun usage() {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val fooDRI = DRI(
                    "example",
                    null,
                    callable = Callable(
                        "foo",
                        receiver = TypeConstructor("kotlin.collections.List", params = listOf(TypeParam(listOf(TypeConstructor("kotlin.Number", emptyList()))))),
                        params = emptyList()
                    )
                )
                assertEquals(
                    listOf(
                        "List.foo" to fooDRI
                    ), module.getAllLinkDRIFrom("usage")
                )
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
                        "String.length" to DRI(
                            "kotlin",
                            "String",
                            Callable(name = "length", receiver = null, params = emptyList())
                        )
                    ),
                    module.getAllLinkDRIFrom("usage")
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
                        "kotlin.collections.ArrayList" to DRI("kotlin.collections", "ArrayList"),
                        "kotlin.IllegalArgumentException" to DRI("kotlin", "IllegalArgumentException"),
                        "example.F" to DRI("example", "F")
                    ),
                    module.getAllLinkDRIFrom("usage")
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
                assertEquals(DRI("demos", "F"), module.getLinkDRIFrom("usage"))
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
                assertEquals(DRI("example", "Storage", Callable("value", null, emptyList())), module.getLinkDRIFrom("usage"))
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
            | * prop: [Storage.prop]
            | */
            |val usage = 0            
            |/src/example/Storage.java
            |package example;
            |class Storage {
            |    void prop() {}
            |    void setValue(String value) {}
            |    String getProp() { return null; }
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    listOf(
                        "Storage.setValue" to DRI("example", "Storage", Callable("setValue", null, listOf(TypeConstructor("kotlin.String", emptyList())))),
                        "Storage.prop" to DRI("example", "Storage", Callable("prop", null, emptyList()))
                    ),
                    module.getAllLinkDRIFrom("usage"))
            }
        }
    }

    private fun DModule.getLinkDRIFrom(name: String): DRI? {
        val link =  this.dfs { it.name == name }?.documentation?.values?.single()?.firstMemberOfTypeOrNull<DocumentationLink>()
        return link?.dri
    }

    private fun DModule.getAllLinkDRIFrom(name: String): List<Pair<String, DRI>> {
        val result = mutableListOf<Pair<String, DRI>>()
        this.dfs { it.name == name }?.documentation?.values?.single()?.dfs {
            if (it is DocumentationLink) result.add(it.firstChildOfType<Text>().body to it.dri)
            false
        }
        return result
    }
}
