package translators

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DefaultDescriptorToDocumentableTranslatorTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        suppressObviousFunctions = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
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
                assert(module.documentationOf("XD", "copy") == "")
                assert(
                    module.documentationOf(
                        "XD",
                        "equals"
                    ) == "Memory is not what the heart desires. That is only a mirror."
                )
                assert(module.documentationOf("XD", "hashCode") == "")
                assert(module.documentationOf("XD", "toString") == "")
                assert(module.documentationOf("XD", "custom") == "But the fat Hobbit, he knows. Eyes always watching.")
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
                assert(module.documentationOf("XD", "custom") == "But the fat Hobbit, he knows. Eyes always watching.")
                assert(
                    module.documentationOf(
                        "XD",
                        "equals"
                    ) == "Memory is not what the heart desires. That is only a mirror."
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
                                """    val soapAttrs = attrs("soap-env" to "http://www.w3.org/2001/12/soap-envelope",
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
                                extra[AdditionalModifiers]?.content?.values?.single(),
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
                                extra[AdditionalModifiers]?.content?.values?.single(),
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

    @Suppress("DEPRECATION") // for includeNonPublic
    val javaConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
                includeNonPublic = true
            }
        }
    }

    @Disabled // The compiler throws away annotations on unresolved types upstream
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
}
