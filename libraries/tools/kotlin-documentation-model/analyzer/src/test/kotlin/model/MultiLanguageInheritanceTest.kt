/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.utilities.firstIsInstanceOrNull
import translators.documentationOf
import utils.docs
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiLanguageInheritanceTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        suppressObviousFunctions = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `should not try to find javadoc for non JVM source set`() {
        // Bug #3209 is actual for Dokka K1
        // Technical note: `KtPropertyAccessor`, i.e. `<get-withHintType>`,  is not KtCallableDeclaration so `findKDoc` returns null
        // Meanwhile, `getJavaDocs()` for KtPropertyAccessor tries to unexpectedly parse the KDoc documentation of property, i.e. `withHintType`

        val nonJvmConfiguration = dokkaConfiguration {
            suppressObviousFunctions = false
            sourceSets {
                sourceSet {
                    analysisPlatform = Platform.common.key
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/sample/Parent.kt
            |package sample
            |
            |/**
            | * Sample description from parent
            | */
            |interface Parent {
            |    /**
            |    * Sample description from parent
            |    */  
            |    val withHintType: String
            |}
            |
            |/src/main/kotlin/sample/Child.kt
            |package sample
            |public class Child : Parent {
            |   override val withHintType: String 
            |    get() = ""
            |}
            """.trimIndent(),
            nonJvmConfiguration
        ) { }
    }

    @Test
    fun `from java to kotlin`() {
        testInline(
            """
            |/src/main/kotlin/sample/Parent.java
            |package sample;
            |
            |/**
            | * Sample description from parent
            | */
            |public class Parent {
            |    /**
            |     * parent function docs
            |     * @see java.lang.String for details
            |     */
            |    public void parentFunction(){
            |    }
            |}
            |
            |/src/main/kotlin/sample/Child.kt
            |package sample
            |public class Child : Parent() {
            |   override fun parentFunction(){
            |
            |   }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.packages.flatMap { it.classlikes }
                    .find { it.name == "Child" }?.functions?.find { it.name == "parentFunction" }
                val seeTag = function?.documentation?.values?.first()?.children?.firstIsInstanceOrNull<See>()

                assertEquals("", module.documentationOf("Child"))
                assertEquals("parent function docs", module.documentationOf("Child", "parentFunction"))
                assertEquals("for details", (seeTag?.root?.dfs { it is Text } as Text).body)
                assertEquals("java.lang.String", seeTag.name)
            }
        }
    }

    @Test
    fun `from kotlin to java`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentInKotlin.kt
            |package sample
            |
            |/**
            | * Sample description from parent
            | */
            |public open class ParentInKotlin {
            |   /**
            |    * parent `function docs`
            |    *
            |    * ```
            |    * code block
            |    * ```
            |    * @see java.lang.String for details
            |    */
            |    public open fun parentFun(){
            |
            |    }
            |}
            |
            |
            |/src/main/kotlin/sample/ChildInJava.java
            |package sample;
            |public class ChildInJava extends ParentInKotlin {
            |    @Override
            |    public void parentFun() {
            |        super.parentFun();
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.packages.flatMap { it.classlikes }
                    .find { it.name == "ChildInJava" }?.functions?.find { it.name == "parentFun" }
                val seeTag = function?.documentation?.values?.first()?.children?.firstIsInstanceOrNull<See>()

                val expectedDocs = CustomDocTag(
                    children = listOf(
                        P(
                            listOf(
                                Text("parent "),
                                CodeInline(
                                    listOf(Text("function docs"))
                                )
                            )
                        ),
                        CodeBlock(
                            listOf(Text("code block"))
                        )

                    ),
                    params = emptyMap(),
                    name = MARKDOWN_ELEMENT_FILE_NAME
                )

                assertEquals("", module.documentationOf("ChildInJava"))
                assertEquals(expectedDocs, function?.docs()?.firstIsInstanceOrNull<Description>()?.root)
                assertEquals("for details", (seeTag?.root?.dfs { it is Text } as Text).body)
                assertEquals("java.lang.String", seeTag.name)
            }
        }
    }

    @Test
    fun `inherit doc on method`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentInKotlin.kt
            |package sample
            |
            |/**
            | * Sample description from parent
            | */
            |public open class ParentInKotlin {
            |   /**
            |    * parent `function docs` with a link to [defaultString][java.lang.String]
            |    *
            |    * ```
            |    * code block
            |    * ```
            |    */
            |    public open fun parentFun(){
            |
            |    }
            |}
            |
            |
            |/src/main/kotlin/sample/ChildInJava.java
            |package sample;
            |public class ChildInJava extends ParentInKotlin {
            |   /**
            |    * {@inheritDoc}
            |    */
            |    @Override
            |    public void parentFun() {
            |        super.parentFun();
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.packages.flatMap { it.classlikes }
                    .find { it.name == "ChildInJava" }?.functions?.find { it.name == "parentFun" }

                val expectedDocs = CustomDocTag(
                    children = listOf(
                        P(
                            listOf(
                                P(
                                    listOf(
                                        Text("parent "),
                                        CodeInline(
                                            listOf(Text("function docs"))
                                        ),
                                        Text(" with a link to "),
                                        DocumentationLink(
                                            DRI("java.lang", "String", null, PointingToDeclaration),
                                            listOf(Text("defaultString")),
                                            params = mapOf("href" to "[java.lang.String]")
                                        )
                                    )
                                ),
                                CodeBlock(
                                    listOf(Text("code block"))
                                )
                            )
                        )
                    ),
                    params = emptyMap(),
                    name = MARKDOWN_ELEMENT_FILE_NAME
                )

                assertEquals("", module.documentationOf("ChildInJava"))
                assertEquals(expectedDocs, function?.docs()?.firstIsInstanceOrNull<Description>()?.root)
            }
        }
    }

    @Test
    fun `inline inherit doc on method`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentInKotlin.kt
            |package sample
            |
            |/**
            | * Sample description from parent
            | */
            |public open class ParentInKotlin {
            |   /**
            |    * parent function docs
            |    * @see java.lang.String string
            |    */
            |    public open fun parentFun(){
            |
            |    }
            |}
            |
            |
            |/src/main/kotlin/sample/ChildInJava.java
            |package sample;
            |public class ChildInJava extends ParentInKotlin {
            |   /**
            |    * Start {@inheritDoc} end
            |    */
            |    @Override
            |    public void parentFun() {
            |        super.parentFun();
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.packages.flatMap { it.classlikes }
                    .find { it.name == "ChildInJava" }?.functions?.find { it.name == "parentFun" }?.documentation?.values?.first()?.children?.first()
                assertEquals("", module.documentationOf("ChildInJava"))
                assertEquals("Start parent function docs end", function?.root?.withDescendants()?.filter { it is Text }?.toList()?.joinToString("") { (it as Text).body })
            }
        }
    }

    @Test
    fun `inherit doc on multiple throws`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentInKotlin.kt
            |package sample
            |
            |/**
            | * Sample description from parent
            | */
            |public open class ParentInKotlin {
            |   /**
            |    * parent function docs
            |    * @throws java.lang.RuntimeException runtime
            |    * @throws java.lang.Exception exception
            |    */
            |    public open fun parentFun(){
            |
            |    }
            |}
            |
            |
            |/src/main/kotlin/sample/ChildInJava.java
            |package sample;
            |public class ChildInJava extends ParentInKotlin {
            |   /**
            |    * Start {@inheritDoc} end
            |    * @throws java.lang.RuntimeException Testing {@inheritDoc}
            |    */
            |    @Override
            |    public void parentFun() {
            |        super.parentFun();
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.packages.flatMap { it.classlikes }
                    .find { it.name == "ChildInJava" }?.functions?.find { it.name == "parentFun" }
                val docs = function?.documentation?.values?.first()?.children?.first()
                val throwsTag = function?.documentation?.values?.first()?.children?.firstIsInstanceOrNull<Throws>()

                assertEquals("", module.documentationOf("ChildInJava"))
                assertEquals("Start parent function docs end", docs?.root?.withDescendants()?.filter { it is Text }?.toList()?.joinToString("") { (it as Text).body })
                assertEquals("Testing runtime", throwsTag?.root?.withDescendants()?.filter { it is Text }?.toList()?.joinToString("") { (it as Text).body })
                assertEquals("RuntimeException", throwsTag?.exceptionAddress?.classNames)
            }
        }
    }

    @Test
    fun `inherit doc on params`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentInKotlin.kt
            |package sample
            |
            |/**
            | * Sample description from parent
            | */
            |public open class ParentInKotlin {
            |   /**
            |    * parent function docs
            |    * @param fst first docs
            |    * @param snd second docs
            |    */
            |    public open fun parentFun(fst: String, snd: Int){
            |
            |    }
            |}
            |
            |
            |/src/main/kotlin/sample/ChildInJava.java
            |package sample;
            |
            |import org.jetbrains.annotations.NotNull;
            |
            |public class ChildInJava extends ParentInKotlin {
            |   /**
            |    * @param fst start {@inheritDoc} end
            |    * @param snd start {@inheritDoc} end
            |    */
            |    @Override
            |    public void parentFun(@NotNull String fst, int snd) {
            |        super.parentFun();
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.packages.flatMap { it.classlikes }
                    .find { it.name == "ChildInJava" }?.functions?.find { it.name == "parentFun" }
                val params = function?.documentation?.values?.first()?.children?.filterIsInstance<Param>()

                val fst = params?.first { it.name == "fst" }
                val snd = params?.first { it.name == "snd" }

                assertEquals("", module.documentationOf("ChildInJava"))
                assertEquals("", module.documentationOf("ChildInJava", "parentFun"))
                assertEquals("start first docs end", fst?.root?.withDescendants()?.filter { it is Text }?.toList()?.joinToString("") { (it as Text).body })
                assertEquals("start second docs end", snd?.root?.withDescendants()?.filter { it is Text }?.toList()?.joinToString("") { (it as Text).body })
            }
        }
    }
}
