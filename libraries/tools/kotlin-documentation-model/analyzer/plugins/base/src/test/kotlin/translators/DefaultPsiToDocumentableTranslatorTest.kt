package translators

import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.firstMemberOfType
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultPsiToDocumentableTranslatorTest : BaseAbstractTest() {
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
}
