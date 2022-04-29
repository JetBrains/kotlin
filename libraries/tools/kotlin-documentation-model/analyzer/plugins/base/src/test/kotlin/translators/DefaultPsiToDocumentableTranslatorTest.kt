package translators

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.firstMemberOfType
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import utils.assertNotNull

class DefaultPsiToDocumentableTranslatorTest : BaseAbstractTest() {
    @Suppress("DEPRECATION") // for includeNonPublic
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
                includeNonPublic = true
            }
        }
    }

    @Test
    fun `method overriding two documented classes picks closest class documentation`() {
        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample;
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/BaseClass2.java
            |package sample;
            |public class BaseClass2 extends BaseClass1 {
            |    /** B2 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/X.java
            |package sample;
            |public class X extends BaseClass2 {
            |    void x() { }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val documentationOfFunctionX = module.documentationOf("X", "x")
                assertTrue(
                    "B2" in documentationOfFunctionX,
                    "Expected nearest super method documentation to be parsed as documentation. " +
                            "Documentation: $documentationOfFunctionX"
                )
            }
        }
    }

    @Test
    fun `method overriding class and interface picks class documentation`() {
        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample;
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/Interface1.java
            |package sample;
            |public interface Interface1 {
            |    /** I1 */
            |    void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample;
            |public class X extends BaseClass1 implements Interface1 {
            |    void x() { }
            |}
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val documentationOfFunctionX = module.documentationOf("X", "x")
                assertTrue(
                    "B1" in documentationOfFunctionX,
                    "Expected documentation of superclass being prioritized over interface " +
                            "Documentation: $documentationOfFunctionX"
                )
            }
        }
    }

    @Test
    fun `method overriding two classes picks closest documented class documentation`() {
        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample;
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/BaseClass2.java
            |package sample;
            |public class BaseClass2 extends BaseClass1 {
            |    void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample;
            |public class X extends BaseClass2 {
            |    void x() { }
            |}
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val documentationOfFunctionX = module.documentationOf("X", "x")
                assertTrue(
                    "B1" in documentationOfFunctionX,
                    "Expected Documentation \"B1\", found: \"$documentationOfFunctionX\""
                )
            }
        }
    }

    @Test
    fun `java package-info package description`() {
        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample;
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/BaseClass2.java
            |package sample;
            |public class BaseClass2 extends BaseClass1 {
            |    void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample;
            |public class X extends BaseClass2 {
            |    void x() { }
            |}
            |
            |/src/main/java/sample/package-info.java
            |/**
            | * Here comes description from package-info
            | */
            |package sample;
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val documentationOfPackage = module.packages.single().documentation.values.single().children.single()
                    .firstMemberOfType<Text>().body
                assertEquals(
                    "Here comes description from package-info", documentationOfPackage
                )
            }
        }
    }

    @Test
    fun `java package-info package annotations`() {
        testInline(
            """
            |/src/main/java/sample/PackageAnnotation.java
            |package sample;
            |@java.lang.annotation.Target(java.lang.annotation.ElementType.PACKAGE)
            |public @interface PackageAnnotation {
            |}
            |
            |/src/main/java/sample/package-info.java
            |@PackageAnnotation
            |package sample;
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    Annotations.Annotation(DRI("sample", "PackageAnnotation"), emptyMap()),
                    module.packages.single().extra[Annotations]?.directAnnotations?.values?.single()?.single()
                )
            }
        }
    }

    class OnlyPsiPlugin : DokkaPlugin() {
        private val dokkaBase by lazy { plugin<DokkaBase>() }

        @Suppress("unused")
        val psiOverrideDescriptorTranslator by extending {
            (dokkaBase.psiToDocumentableTranslator
                    override dokkaBase.descriptorToDocumentableTranslator)
        }
    }

    // for Kotlin classes from DefaultPsiToDocumentableTranslator
    @Test
    fun `should resolve ultralight class`() {
        val configurationWithNoJVM = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/example/Test.kt
            |package example
            |
            |open class KotlinSubClass {
            |    fun kotlinSubclassFunction(bar: String): String {
            |       return "KotlinSubClass"
            |    }
            |}
            |
            |/src/main/java/example/JavaLeafClass.java
            |package example;
            |
            |public class JavaLeafClass extends KotlinSubClass {
            |    public String javaLeafClassFunction(String baz) {
            |        return "JavaLeafClass";
            |    }
            |}
        """.trimMargin(),
            configurationWithNoJVM,
            pluginOverrides = listOf(OnlyPsiPlugin()) // suppress a descriptor translator because of psi and descriptor translators work in parallel
        ) {
            documentablesMergingStage = { module ->
                val kotlinSubclassFunction =
                    module.packages.single().classlikes.find { it.name == "JavaLeafClass" }?.functions?.find { it.name == "kotlinSubclassFunction" }
                        .assertNotNull("kotlinSubclassFunction ")

                assertEquals(
                    "String",
                    (kotlinSubclassFunction.type as? TypeConstructor)?.dri?.classNames
                )
                assertEquals(
                    "String",
                    (kotlinSubclassFunction.parameters.firstOrNull()?.type as? TypeConstructor)?.dri?.classNames
                )
            }
        }
    }
}
