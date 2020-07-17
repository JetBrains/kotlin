package translators

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultPsiToDocumentableTranslatorTest : AbstractCoreTest() {

    @Test
    fun `method overriding two documented classes picks closest class documentation`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/BaseClass2.java
            |package sample
            |public class BaseClass2 extends BaseClass1 {
            |    /** B2 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/X.java
            |package sample
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
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/Interface1.java
            |package sample
            |public interface Interface1 {
            |    /** I1 */
            |    void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample
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
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/BaseClass2.java
            |package sample
            |public class BaseClass2 extends BaseClass1 {
            |    void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample
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
}
