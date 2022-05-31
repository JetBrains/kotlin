package superFields

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.InheritedMember
import org.jetbrains.dokka.model.isJvmField
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class PsiSuperFieldsTest : BaseAbstractTest() {

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
    fun `java inheriting java`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |    public int a = 1;
            |}
            |
            |/src/test/B.java
            |package test;
            |public class B extends A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val inheritorProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val property = inheritorProperties.single { it.name == "a" }

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)
            }
        }
    }

    @Test
    fun `java inheriting kotlin common case`() {
        testInline(
            """
            |/src/test/A.kt
            |package test
            |open class A {
            |    var a: Int = 1
            |}
            |
            |/src/test/B.java
            |package test;
            |public class B extends A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val inheritorProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val property = inheritorProperties.single { it.name == "a" }

                assertNotNull(property.getter)
                assertNotNull(property.setter)

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)
            }
        }
    }

    @Test
    fun `java inheriting kotlin with boolean property`() {
        testInline(
            """
            |/src/test/A.kt
            |package test
            |open class A {
            |    var isActive: Boolean = true
            |}
            |
            |/src/test/B.java
            |package test;
            |public class B extends A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val inheritorProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val property = inheritorProperties.single { it.name == "isActive" }

                assertNotNull(property.getter)
                assertEquals("isActive", property.getter?.name)

                assertNotNull(property.setter)
                assertEquals("setActive", property.setter?.name)

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)
            }
        }
    }

    @Test
    fun `java inheriting kotlin with @JvmField should not inherit accessors`() {
        testInline(
            """
            |/src/test/A.kt
            |package test
            |open class A {
            |    @kotlin.jvm.JvmField
            |    var a: Int = 1
            |}
            |
            |/src/test/B.java
            |package test;
            |public class B extends A {}
        """.trimIndent(),
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/")
                        analysisPlatform = "jvm"
                        name = "jvm"
                        classpath += jvmStdlibPath!! // needed for JvmField
                    }
                }
            }
        ) {
            documentablesMergingStage = { module ->
                val inheritorProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val property = inheritorProperties.single { it.name == "a" }

                assertNull(property.getter)
                assertNull(property.setter)

                val jvmFieldAnnotation = property.extra[Annotations]?.directAnnotations?.values?.single()?.find {
                    it.isJvmField()
                }
                assertNotNull(jvmFieldAnnotation)

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)
            }
        }
    }

    @Test
    fun `should preserve regular functions that look like accessors, but are not accessors`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |    public int a = 1;
            |    public void setA() { } // no arg
            |    public String getA() { return "s"; } // wrong return type
            |}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val testClass = module.packages.single().classlikes.single { it.name == "A" }

                val setterLookalike = testClass.functions.firstOrNull { it.name == "setA" }
                assertNotNull(setterLookalike) {
                    "Expected regular function not found, wrongly categorized as setter?"
                }

                val getterLookalike = testClass.functions.firstOrNull { it.name == "getA" }
                assertNotNull(getterLookalike) {
                    "Expected regular function not found, wrongly categorized as getter?"
                }
            }
        }
    }
}
