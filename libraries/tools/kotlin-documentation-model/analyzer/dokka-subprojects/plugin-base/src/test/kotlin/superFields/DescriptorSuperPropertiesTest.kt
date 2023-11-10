/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package superFields

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.InheritedMember
import org.jetbrains.dokka.model.IsVar
import org.jetbrains.dokka.model.KotlinVisibility
import utils.OnlyDescriptors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DescriptorSuperPropertiesTest : BaseAbstractTest() {

    private val commonTestConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                name = "jvm"
            }
        }
    }

    @Test
    fun `kotlin inheriting java should append only getter`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   public int getA() { return a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            this.documentablesTransformationStage = { module ->
                val kotlinProperties = module.packages.single().classlikes.single { it.name == "B" }.properties

                val property = kotlinProperties.single { it.name == "a" }
                val propertyInheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), propertyInheritedFrom)

                assertNull(property.setter)
                assertNotNull(property.getter)

                val getterInheritedFrom = property.getter?.extra?.get(InheritedMember)?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), getterInheritedFrom)

                assertNull(property.extra[IsVar])
            }
        }
    }


    @Test
    fun `kotlin inheriting java should ignore setter lookalike for non accessible field`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   
            |   public void setA(int a) { this.a = a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "B" }

                val property = testedClass.properties.firstOrNull { it.name == "a" }
                assertNull(property, "Inherited property `a` should not be visible as it's not accessible")

                val setterLookalike = testedClass.functions.firstOrNull { it.name == "setA" }
                assertNotNull(setterLookalike) {
                    "Expected setA to be a regular function because field `a` is neither var nor val from Kotlin's " +
                            "interop perspective, it's not accessible."
                }
            }
        }
    }


    @Test
    fun `kotlin inheriting java should append getter and setter`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   public int getA() { return a; }
            |   public void setA(int a) { this.a = a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val kotlinProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val property = kotlinProperties.single { it.name == "a" }
                property.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                    assertEquals(
                        DRI(packageName = "test", classNames = "A"),
                        this
                    )
                }

                val getter = property.getter
                assertNotNull(getter)
                assertEquals("getA", getter.name)
                val getterInheritedFrom = getter.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), getterInheritedFrom)

                val setter = property.setter
                assertNotNull(setter)
                assertEquals("setA", setter.name)
                val setterInheritedFrom = setter.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), setterInheritedFrom)

                assertNotNull(property.extra[IsVar])
            }
        }
    }

    @Test
    @OnlyDescriptors("Incorrect test, see https://github.com/Kotlin/dokka/issues/3128")
    fun `should have special getter and setter names for boolean property inherited from java`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private boolean bool = true;
            |   public boolean isBool() { return bool; }
            |   public void setBool(boolean bool) { this.bool = bool; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val kotlinProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val boolProperty = kotlinProperties.single { it.name == "bool" }

                val getter = boolProperty.getter
                assertNotNull(getter)
                assertEquals("isBool", getter.name)

                val setter = boolProperty.setter
                assertNotNull(setter)
                assertEquals("setBool", setter.name)

                assertNotNull(boolProperty.extra[IsVar])
            }
        }
    }

    @OnlyDescriptors("Incorrect test, see https://github.com/Kotlin/dokka/issues/3128")
    @Test
    fun `kotlin inheriting java should not append anything since field is public api`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "jvm"
                    name = "jvm"
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
        }

        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   protected int a = 1;
            |   public int getA() { return a; }
            |   public void setA(int a) { this.a = a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "B" }
                val property = testedClass.properties.single { it.name == "a" }

                assertNull(property.getter)
                assertNull(property.setter)
                assertEquals(2, testedClass.functions.size)

                assertEquals("getA", testedClass.functions[0].name)
                assertEquals("setA", testedClass.functions[1].name)

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)

                assertNotNull(property.extra[IsVar])
            }
        }
    }

    @Test
    fun `should inherit property visibility from getter`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "jvm"
                    name = "jvm"
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
        }

        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   protected int getA() { return a; }
            |   protected void setA(int a) { this.a = a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "B" }
                assertEquals(0, testedClass.functions.size)

                val property = testedClass.properties.single { it.name == "a" }

                assertNotNull(property.getter)
                assertNotNull(property.setter)

                val propertyVisibility = property.visibility.values.single()
                assertEquals(KotlinVisibility.Protected, propertyVisibility)

                val getterVisibility = property.getter?.visibility?.values?.single()
                assertEquals(KotlinVisibility.Protected, getterVisibility)

                val setterVisibility = property.setter?.visibility?.values?.single()
                assertEquals(KotlinVisibility.Protected, setterVisibility)

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)

                assertNotNull(property.extra[IsVar])
            }
        }
    }

    @Test // checking for mapping between kotlin and java visibility
    fun `should resolve inherited java protected field as protected`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "jvm"
                    name = "jvm"
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
        }

        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   protected int protectedProperty = 0;
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "B" }
                assertEquals(0, testedClass.functions.size)

                val property = testedClass.properties.single { it.name == "protectedProperty" }

                assertNull(property.getter)
                assertNull(property.setter)

                val propertyVisibility = property.visibility.values.single()
                assertEquals(KotlinVisibility.Protected, propertyVisibility)

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)

                assertNotNull(property.extra[IsVar])
            }
        }
    }

    @Test
    fun `should mark final property inherited from java as val`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   public final int a = 1;
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val kotlinProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val property = kotlinProperties.single { it.name == "a" }

                assertNull(property.extra[IsVar])
            }
        }
    }
}
