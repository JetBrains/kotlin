package superFields

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.InheritedMember
import org.junit.jupiter.api.Test
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
            }
        }
    }

    @Test
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
            }
        }
    }

    @Test
    fun `kotlin inheriting java should not append anything since field is public`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   public int a = 1;
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

                assertNull(property.getter)
                assertNull(property.setter)

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)
            }
        }
    }

    @Test
    fun `should preserve regular functions that look like accessors, but are not accessors`() {
        testInline(
            """
            |/src/test/A.kt
            |package test
            |class A {
            |    val v = 0
            |    fun setV() { println(10) } // no arg
            |    fun getV(): String { return "s" } // wrong return type
            |}
        """.trimIndent(),
            commonTestConfiguration
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
}
