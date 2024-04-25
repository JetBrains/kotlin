/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.modifiers
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import utils.OnlyDescriptors
import utils.text
import kotlin.test.*

class DefaultDescriptorToDocumentableTranslatorTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        suppressObviousFunctions = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
                classpath = listOf(commonStdlibPath!!, jvmStdlibPath!!)
            }
        }
    }

    @Suppress("DEPRECATION") // for includeNonPublic
    val javaConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
                includeNonPublic = true
            }
        }
    }

    @Test
    fun `data class kdocs over generated methods`() {
        testInline(
            """
            |/src/main/kotlin/sample/XD.kt
            |package sample
            |/**
            | * But the fat Hobbit, he knows. Eyes always watching.
            | */
            |data class XD(val xd: String) {
            |   /**
            |    * But the fat Hobbit, he knows. Eyes always watching.
            |    */
            |   fun custom(): String = ""
            |
            |   /**
            |    * Memory is not what the heart desires. That is only a mirror.
            |    */
            |   override fun equals(other: Any?): Boolean = true
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals("", module.documentationOf("XD", "copy"))
                assertEquals(
                    "Memory is not what the heart desires. That is only a mirror.",
                    module.documentationOf(
                        "XD",
                        "equals"
                    )
                )
                assertEquals("", module.documentationOf("XD", "hashCode"))
                assertEquals("", module.documentationOf("XD", "toString"))
                assertEquals("But the fat Hobbit, he knows. Eyes always watching.", module.documentationOf("XD", "custom"))
            }
        }
    }

    @Test
    fun `simple class kdocs`() {
        testInline(
            """
            |/src/main/kotlin/sample/XD.kt
            |package sample
            |/**
            | * But the fat Hobbit, he knows. Eyes always watching.
            | */
            |class XD(val xd: String) {
            |   /**
            |    * But the fat Hobbit, he knows. Eyes always watching.
            |    */
            |   fun custom(): String = ""
            |
            |   /**
            |    * Memory is not what the heart desires. That is only a mirror.
            |    */
            |   override fun equals(other: Any?): Boolean = true
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals("But the fat Hobbit, he knows. Eyes always watching.", module.documentationOf("XD", "custom"))
                assertEquals(
                    "Memory is not what the heart desires. That is only a mirror.",
                    module.documentationOf(
                        "XD",
                        "equals"
                    )
                )
            }
        }
    }

    @Test
    fun `kdocs with code block`() {
        testInline(
            """
            |/src/main/kotlin/sample/TestForCodeInDocs.kt
            |package sample
            |/**
            | * Utility for building a String that represents an XML document.
            | * The XmlBlob object is immutable and the passed values are copied where it makes sense.
            | *
            | * Note the XML Declaration is not output as part of the XmlBlob
            | *
            | * 
            | *    val soapAttrs = attrs("soap-env" to "http://www.w3.org/2001/12/soap-envelope",
            | *        "soap-env:encodingStyle" to "http://www.w3.org/2001/12/soap-encoding")
            | *    val soapXml = node("soap-env:Envelope", soapAttrs,
            | *        node("soap-env:Body", attrs("xmlns:m" to "http://example"),
            | *            node("m:GetExample",
            | *                node("m:GetExampleName", "BasePair")
            | *            )
            | *        )
            | *    )
            | *
            | *
            | */
            |class TestForCodeInDocs {
            |}
        """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val description = module.descriptionOf("TestForCodeInDocs")
                val expected = listOf(
                    P(
                        children = listOf(Text("Utility for building a String that represents an XML document. The XmlBlob object is immutable and the passed values are copied where it makes sense."))
                    ),
                    P(
                        children = listOf(Text("Note the XML Declaration is not output as part of the XmlBlob"))
                    ),
                    CodeBlock(
                        children = listOf(
                            Text(
                                """val soapAttrs = attrs("soap-env" to "http://www.w3.org/2001/12/soap-envelope",
    "soap-env:encodingStyle" to "http://www.w3.org/2001/12/soap-encoding")
val soapXml = node("soap-env:Envelope", soapAttrs,
    node("soap-env:Body", attrs("xmlns:m" to "http://example"),
        node("m:GetExample",
            node("m:GetExampleName", "BasePair")
        )
    )
)"""
                            )
                        )
                    )
                )
                assertEquals(expected, description?.root?.children)
            }
        }
    }

    private fun runTestSuitesAgainstGivenClasses(classlikes: List<DClasslike>, testSuites: List<List<TestSuite>>) {
        classlikes.zip(testSuites).forEach { (classlike, testSuites) ->
            testSuites.forEach { testSuite ->
                when (testSuite) {
                    is TestSuite.PropertyDoesntExist -> assertEquals(
                        null,
                        classlike.properties.firstOrNull { it.name == testSuite.propertyName },
                        "Test for class ${classlike.name} failed"
                    )
                    is TestSuite.PropertyExists -> classlike.properties.single { it.name == testSuite.propertyName }
                        .run {
                            assertEquals(
                                testSuite.modifier,
                                modifier.values.single(),
                                "Test for class ${classlike.name} with property $name failed"
                            )
                            assertEquals(
                                testSuite.visibility,
                                visibility.values.single(),
                                "Test for class ${classlike.name} with property $name failed"
                            )
                            assertEquals(
                                testSuite.additionalModifiers,
                                extra[AdditionalModifiers]?.content?.values?.single() ?: emptySet<ExtraModifiers>(),
                                "Test for class ${classlike.name} with property $name failed"
                            )
                        }
                    is TestSuite.FunctionDoesntExist -> assertEquals(
                        null,
                        classlike.functions.firstOrNull { it.name == testSuite.propertyName },
                        "Test for class ${classlike.name} failed"
                    )
                    is TestSuite.FunctionExists -> classlike.functions.single { it.name == testSuite.propertyName }
                        .run {
                            assertEquals(
                                testSuite.modifier,
                                modifier.values.single(),
                                "Test for class ${classlike.name} with function $name failed"
                            )
                            assertEquals(
                                testSuite.visibility,
                                visibility.values.single(),
                                "Test for class ${classlike.name} with function $name failed"
                            )
                            assertEquals(
                                testSuite.additionalModifiers,
                                extra[AdditionalModifiers]?.content?.values?.single() ?: emptySet<ExtraModifiers>(),
                                "Test for class ${classlike.name} with function $name failed"
                            )
                        }
                }
            }
        }
    }

    @Test
    fun `derived properties with non-public code included`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PRIVATE,
                        DokkaConfiguration.Visibility.PROTECTED,
                        DokkaConfiguration.Visibility.INTERNAL,
                    )
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/sample/XD.kt
            |package sample
            |
            |open class A {
            |    private val privateProperty: Int = 1
            |    protected val protectedProperty: Int = 2
            |    internal val internalProperty: Int = 3
            |    val publicProperty: Int = 4
            |    open val propertyToOverride: Int = 5
            |
            |    private fun privateFun(): Int = 6
            |    protected fun protectedFun(): Int = 7
            |    internal fun internalFun(): Int = 8
            |    fun publicFun(): Int = 9
            |    open fun funToOverride(): Int = 10
            |}
            |
            |open class B : A() {
            |    override val propertyToOverride: Int = 11
            |
            |    override fun funToOverride(): Int = 12
            |}
            |class C : B()
            """.trimIndent(),
            configuration
        ) {

            documentablesMergingStage = { module ->
                val classes = module.packages.single().classlikes.sortedBy { it.name }

                val testSuites: List<List<TestSuite>> = listOf(
                    listOf(
                        TestSuite.PropertyExists(
                            "privateProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Private,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "protectedProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Protected,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "internalProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Internal,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "publicProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "privateFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Private,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "protectedFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Protected,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "internalFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Internal,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "publicFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "funToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            emptySet()
                        )
                    ),
                    listOf(
                        TestSuite.PropertyExists(
                            "privateProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Private,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "protectedProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Protected,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "internalProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Internal,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "publicProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        ),
                        TestSuite.FunctionExists(
                            "privateFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Private,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "protectedFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Protected,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "internalFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Internal,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "publicFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "funToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        )
                    ),
                    listOf(
                        TestSuite.PropertyExists(
                            "privateProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Private,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "protectedProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Protected,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "internalProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Internal,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "publicProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        ),
                        TestSuite.FunctionExists(
                            "privateFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Private,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "protectedFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Protected,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "internalFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Internal,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "publicFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "funToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        )
                    )
                )

                runTestSuitesAgainstGivenClasses(classes, testSuites)
            }
        }
    }


    @Test
    fun `derived properties with only public code`() {

        @Suppress("DEPRECATION") // for includeNonPublic
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                    includeNonPublic = false
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/sample/XD.kt
            |package sample
            |
            |open class A {
            |    private val privateProperty: Int = 1
            |    protected val protectedProperty: Int = 2
            |    internal val internalProperty: Int = 3
            |    val publicProperty: Int = 4
            |    open val propertyToOverride: Int = 5
            |    open val propertyToOverrideButCloseMeanwhile: Int = 6
            |
            |    private fun privateFun(): Int = 7
            |    protected fun protectedFun(): Int = 8
            |    internal fun internalFun(): Int = 9
            |    fun publicFun(): Int = 10
            |    open fun funToOverride(): Int = 11
            |    open fun funToOverrideButCloseMeanwhile(): Int = 12
            |}
            |
            |open class B : A() {
            |    override val propertyToOverride: Int = 13
            |    final override val propertyToOverrideButCloseMeanwhile: Int = 14
            |
            |    override fun funToOverride(): Int = 15
            |    final override fun funToOverrideButCloseMeanwhile(): Int = 16
            |}
            |class C : B()
            """.trimIndent(),
            configuration
        ) {

            documentablesMergingStage = { module ->
                val classes = module.packages.single().classlikes.sortedBy { it.name }

                val testSuites: List<List<TestSuite>> = listOf(
                    listOf(
                        TestSuite.PropertyDoesntExist("privateProperty"),
                        TestSuite.PropertyDoesntExist("protectedProperty"),
                        TestSuite.PropertyDoesntExist("internalProperty"),
                        TestSuite.PropertyExists(
                            "publicProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverrideButCloseMeanwhile",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionDoesntExist("privateFun"),
                        TestSuite.FunctionDoesntExist("protectedFun"),
                        TestSuite.FunctionDoesntExist("internalFun"),
                        TestSuite.FunctionExists(
                            "publicFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "funToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "funToOverrideButCloseMeanwhile",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            emptySet()
                        )
                    ),
                    listOf(
                        TestSuite.PropertyDoesntExist("privateProperty"),
                        TestSuite.PropertyDoesntExist("protectedProperty"),
                        TestSuite.PropertyDoesntExist("internalProperty"),
                        TestSuite.PropertyExists(
                            "publicProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverrideButCloseMeanwhile",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        ),
                        TestSuite.FunctionDoesntExist("privateFun"),
                        TestSuite.FunctionDoesntExist("protectedFun"),
                        TestSuite.FunctionDoesntExist("internalFun"),
                        TestSuite.FunctionExists(
                            "publicFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "funToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        ),
                        TestSuite.FunctionExists(
                            "funToOverrideButCloseMeanwhile",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        )
                    ),
                    listOf(
                        TestSuite.PropertyDoesntExist("privateProperty"),
                        TestSuite.PropertyDoesntExist("protectedProperty"),
                        TestSuite.PropertyDoesntExist("internalProperty"),
                        TestSuite.PropertyExists(
                            "publicProperty",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        ),
                        TestSuite.PropertyExists(
                            "propertyToOverrideButCloseMeanwhile",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        ),
                        TestSuite.FunctionDoesntExist("privateFun"),
                        TestSuite.FunctionDoesntExist("protectedFun"),
                        TestSuite.FunctionDoesntExist("internalFun"),
                        TestSuite.FunctionExists(
                            "publicFun",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            emptySet()
                        ),
                        TestSuite.FunctionExists(
                            "funToOverride",
                            KotlinModifier.Open,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        ),
                        TestSuite.FunctionExists(
                            "funToOverrideButCloseMeanwhile",
                            KotlinModifier.Final,
                            KotlinVisibility.Public,
                            setOf(ExtraModifiers.KotlinOnlyModifiers.Override)
                        )
                    )
                )

                runTestSuitesAgainstGivenClasses(classes, testSuites)
            }
        }
    }

    @Ignore // The compiler throws away annotations on unresolved types upstream
    @Test
    fun `Can annotate unresolved type`() {
        testInline(
            """
            |/src/main/java/sample/FooLibrary.kt
            |package sample;
            |@MustBeDocumented
            |@Target(AnnotationTarget.TYPE)
            |annotation class Hello()
            |fun bar(): @Hello() TypeThatDoesntResolve
            """.trimMargin(),
            javaConfiguration
        ) {
            documentablesMergingStage = { module ->
                val type = module.packages.single().functions.single().type as GenericTypeConstructor
                assertEquals(
                    Annotations.Annotation(DRI("sample", "Hello"), emptyMap()),
                    type.extra[Annotations]?.directAnnotations?.values?.single()?.single()
                )
            }
        }
    }

    /**
     * Kotlin Int becomes java int. Java int cannot be annotated in source, but Kotlin Int can be.
     * This is paired with KotlinAsJavaPluginTest.`Java primitive annotations work`()
     */
    @Test
    fun `Java primitive annotations work`() {
        testInline(
            """
            |/src/main/java/sample/FooLibrary.kt
            |package sample;
            |@MustBeDocumented
            |@Target(AnnotationTarget.TYPE)
            |annotation class Hello()
            |fun bar(): @Hello() Int
            """.trimMargin(),
            javaConfiguration
        ) {
            documentablesMergingStage = { module ->
                val type = module.packages.single().functions.single().type as GenericTypeConstructor
                assertEquals(
                    Annotations.Annotation(DRI("sample", "Hello"), emptyMap()),
                    type.extra[Annotations]?.directAnnotations?.values?.single()?.single()
                )
                assertEquals("kotlin/Int///PointingToDeclaration/", type.dri.toString())
            }
        }
    }

    @Test
    fun `should preserve regular functions that look like accessors, but are not accessors`() {
        testInline(
            """
            |/src/main/kotlin/A.kt
            |package test
            |class A {
            |    private var v: Int = 0
            |    
            |    // not accessors because declared separately, just functions
            |    fun setV(new: Int) { v = new }
            |    fun getV(): Int = v
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testClass = module.packages.single().classlikes.single { it.name == "A" }
                val setterLookalike = testClass.functions.firstOrNull { it.name == "setV" }
                assertNotNull(setterLookalike) {
                    "Expected regular function not found, wrongly categorized as setter?"
                }

                val getterLookalike = testClass.functions.firstOrNull { it.name == "getV" }
                assertNotNull(getterLookalike) {
                    "Expected regular function not found, wrongly categorized as getter?"
                }
            }
        }
    }

    @Test
    fun `should correctly add IsVar extra for properties`() {
        testInline(
            """
            |/src/main/kotlin/A.kt
            |package test
            |class A {
            |    public var mutable: Int = 0
            |    public val immutable: Int = 0
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testClass = module.packages.single().classlikes.single { it.name == "A" }
                assertEquals(2, testClass.properties.size)

                val mutable = testClass.properties[0]
                assertEquals("mutable", mutable.name)
                assertNotNull(mutable.extra[IsVar])

                val immutable = testClass.properties[1]
                assertEquals("immutable", immutable.name)
                assertNull(immutable.extra[IsVar])
            }
        }
    }

    @Test
    fun `should correctly parse multiple see tags with static function and property links`() {
        testInline(
            """
            |/src/main/kotlin/com/example/package/CollectionExtensions.kt
            |package com.example.util
            |
            |object CollectionExtensions {
            |    val property = "Hi"
            |
            |    fun emptyList() {}
            |    fun emptyMap() {}
            |    fun emptySet() {}
            |}
            |
            |/src/main/kotlin/com/example/foo.kt
            |package com.example
            |
            |import com.example.util.CollectionExtensions.emptyMap
            |import com.example.util.CollectionExtensions.emptyList
            |import com.example.util.CollectionExtensions.emptySet
            |import com.example.util.CollectionExtensions.property
            |
            |/**
            | * @see [List] stdlib list
            | * @see [Map] stdlib map
            | * @see [emptyMap] static emptyMap
            | * @see [emptyList] static emptyList
            | * @see [emptySet] static emptySet
            | * @see [property] static property
            | */
            |fun foo() {}
            """.trimIndent(),
            configuration
        ) {
            fun assertSeeTag(tag: TagWrapper, expectedName: String, expectedDescription: String) {
                assertTrue(tag is See)
                assertEquals(expectedName, tag.name)
                val description = tag.children.joinToString { it.text().trim() }
                assertEquals(expectedDescription, description)
            }

            documentablesMergingStage = { module ->
                val testFunction = module.packages.find { it.name == "com.example" }
                    ?.functions
                    ?.single { it.name == "foo" }
                assertNotNull(testFunction)

                val documentationTags = testFunction.documentation.values.single().children
                assertEquals(7, documentationTags.size)

                val descriptionTag = documentationTags[0]
                assertTrue(descriptionTag is Description, "Expected first tag to be empty description")
                assertTrue(descriptionTag.children.isEmpty(), "Expected first tag to be empty description")

                assertSeeTag(
                    tag = documentationTags[1],
                    expectedName = "kotlin.collections.List",
                    expectedDescription = "stdlib list"
                )
                assertSeeTag(
                    tag = documentationTags[2],
                    expectedName = "kotlin.collections.Map",
                    expectedDescription = "stdlib map"
                )
                assertSeeTag(
                    tag = documentationTags[3],
                    expectedName = "com.example.util.CollectionExtensions.emptyMap",
                    expectedDescription = "static emptyMap"
                )
                assertSeeTag(
                    tag = documentationTags[4],
                    expectedName = "com.example.util.CollectionExtensions.emptyList",
                    expectedDescription = "static emptyList"
                )
                assertSeeTag(
                    tag = documentationTags[5],
                    expectedName = "com.example.util.CollectionExtensions.emptySet",
                    expectedDescription = "static emptySet"
                )
                assertSeeTag(
                    tag = documentationTags[6],
                    expectedName = "com.example.util.CollectionExtensions.property",
                    expectedDescription = "static property"
                )
            }
        }
    }

    @Test
    fun `should have documentation for synthetic Enum values functions`() {
        testInline(
            """
            |/src/main/kotlin/test/KotlinEnum.kt
            |package test
            |
            |enum class KotlinEnum {
            |    FOO, BAR;
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val kotlinEnum = module.packages.find { it.name == "test" }
                    ?.classlikes
                    ?.single { it.name == "KotlinEnum" }
                assertNotNull(kotlinEnum)
                val valuesFunction = kotlinEnum.functions.single { it.name == "values" }

                val expectedValuesType = GenericTypeConstructor(
                    dri = DRI(
                        packageName = "kotlin",
                        classNames = "Array"
                    ),
                    projections = listOf(
                        Invariance(
                            GenericTypeConstructor(
                                dri = DRI(
                                    packageName = "test",
                                    classNames = "KotlinEnum"
                                ),
                                projections = emptyList()
                            )
                        )
                    )
                )
                assertEquals(expectedValuesType, valuesFunction.type)

                val expectedDocumentation = DocumentationNode(listOf(
                    Description(
                        CustomDocTag(
                            children = listOf(
                                P(listOf(
                                    Text(
                                        "Returns an array containing the constants of this enum type, in the order " +
                                                "they're declared."
                                    ),
                                )),
                                P(listOf(
                                    Text("This method may be used to iterate over the constants.")
                                ))
                            ),
                            name = MARKDOWN_ELEMENT_FILE_NAME
                        )
                    )
                ))
                assertEquals(expectedDocumentation, valuesFunction.documentation.values.single())
            }
        }
    }

    @Test
    fun `should have documentation for synthetic Enum entries property`() {
        testInline(
            """
            |/src/main/kotlin/test/KotlinEnum.kt
            |package test
            |
            |enum class KotlinEnum {
            |    FOO, BAR;
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val kotlinEnum = module.packages.find { it.name == "test" }
                    ?.classlikes
                    ?.single { it.name == "KotlinEnum" }

                assertNotNull(kotlinEnum)

                val entriesProperty = kotlinEnum.properties.single { it.name == "entries" }
                val expectedEntriesType = GenericTypeConstructor(
                    dri = DRI(
                        packageName = "kotlin.enums",
                        classNames = "EnumEntries"
                    ),
                    projections = listOf(
                        Invariance(
                            GenericTypeConstructor(
                                dri = DRI(
                                    packageName = "test",
                                    classNames = "KotlinEnum"
                                ),
                                projections = emptyList()
                            )
                        )
                    )
                )
                assertEquals(expectedEntriesType, entriesProperty.type)

                val expectedDocumentation = DocumentationNode(listOf(
                    Description(
                        CustomDocTag(
                            children = listOf(
                                P(listOf(
                                    Text(
                                        "Returns a representation of an immutable list of all enum entries, " +
                                                "in the order they're declared."
                                    ),
                                )),
                                P(listOf(
                                    Text("This method may be used to iterate over the enum entries.")
                                ))
                            ),
                            name = MARKDOWN_ELEMENT_FILE_NAME
                        )
                    )
                ))
                assertEquals(expectedDocumentation, entriesProperty.documentation.values.single())
            }
        }
    }

    @Test
    fun `should have documentation for synthetic Enum valueOf functions`() {
        testInline(
            """
            |/src/main/kotlin/test/KotlinEnum.kt
            |package test
            |
            |enum class KotlinEnum {
            |    FOO, BAR;
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val kotlinEnum = module.packages.find { it.name == "test" }
                    ?.classlikes
                    ?.single { it.name == "KotlinEnum" }
                assertNotNull(kotlinEnum)

                val expectedValueOfType = GenericTypeConstructor(
                    dri = DRI(
                        packageName = "test",
                        classNames = "KotlinEnum"
                    ),
                    projections = emptyList()
                )

                val expectedDocumentation = DocumentationNode(listOf(
                    Description(
                        CustomDocTag(
                            children = listOf(
                                P(listOf(
                                    Text(
                                        "Returns the enum constant of this type with the specified name. " +
                                            "The string must match exactly an identifier used to declare an enum " +
                                            "constant in this type. (Extraneous whitespace characters are not permitted.)"
                                    )
                                ))
                            ),
                            name = MARKDOWN_ELEMENT_FILE_NAME
                        )
                    ),
                    Throws(
                        root = CustomDocTag(
                            children = listOf(
                                P(listOf(
                                    Text("if this enum type has no constant with the specified name")
                                ))
                            ),
                            name = MARKDOWN_ELEMENT_FILE_NAME
                        ),
                        name = "kotlin.IllegalArgumentException",
                        exceptionAddress = DRI(
                            packageName = "kotlin",
                            classNames = "IllegalArgumentException",
                            target = PointingToDeclaration
                        ),
                    )
                ))

                val valueOfFunction = kotlinEnum.functions.single { it.name == "valueOf" }
                assertEquals(expectedDocumentation, valueOfFunction.documentation.values.single())
                assertEquals(expectedValueOfType, valueOfFunction.type)

                val valueOfParamDRI = (valueOfFunction.parameters.single().type as GenericTypeConstructor).dri
                assertEquals(DRI(packageName = "kotlin", classNames = "String"), valueOfParamDRI)
            }
        }
    }

    @Test
    fun `should add data modifier to data objects`() {
        testInline(
            """
            |/src/main/kotlin/test/KotlinDataObject.kt
            |package test
            |
            |data object KotlinDataObject {}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pckg = module.packages.single { it.name == "test" }

                val dataObject = pckg.classlikes.single { it.name == "KotlinDataObject" }
                assertTrue(dataObject is DObject)

                val modifiers = dataObject.modifiers().values.flatten()
                assertEquals(1, modifiers.size)
                assertEquals(ExtraModifiers.KotlinOnlyModifiers.Data, modifiers[0])
            }
        }
    }

    @Test
    @OnlyDescriptors("In K2 the types of recursive typealias is resolved")
    fun `a translator should not fail for a recursive typealias A = A #3565`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    name = "androidJvm"
                    analysisPlatform = Platform.common.key // an androidJvm source set has a common platform
                    sourceRoots = listOf("src/main/kotlin")
                    classpath = listOf(commonStdlibPath!!)
                }
            }
        }
        // `java.io.File` is unavailable in a common platform
        // so `typealias File = File` is recursive
        testInline(
            """
            |/src/main/kotlin/test/typealias.jvmAndAndroid.kt
            |package test
            |
            |import java.io.File
            |typealias File = File
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val ta = module.dfs { it.name == "File" } as DTypeAlias
                assertTrue { ta.type is UnresolvedBound }
            }
        }
    }
}

private sealed class TestSuite {
    abstract val propertyName: String

    data class PropertyDoesntExist(
        override val propertyName: String
    ) : TestSuite()


    data class PropertyExists(
        override val propertyName: String,
        val modifier: KotlinModifier,
        val visibility: KotlinVisibility,
        val additionalModifiers: Set<ExtraModifiers.KotlinOnlyModifiers>
    ) : TestSuite()

    data class FunctionDoesntExist(
        override val propertyName: String,
    ) : TestSuite()

    data class FunctionExists(
        override val propertyName: String,
        val modifier: KotlinModifier,
        val visibility: KotlinVisibility,
        val additionalModifiers: Set<ExtraModifiers.KotlinOnlyModifiers>
    ) : TestSuite()
}
