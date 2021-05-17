package parsers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import utils.text

class JavadocParserTest : BaseAbstractTest() {

    private fun performJavadocTest(testOperation: (DModule) -> Unit) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Date2.java
            |
            |package docs
            |/**
            | * class level docs
            | */
            |public enum AnEnumType {
            |    /**
            |     * content being refreshed, which can be a result of
            |     * invalidation, refresh that may contain content updates, or the initial load.
            |     */
            |    REFRESH
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = testOperation
        }
    }

    @Test
    fun `correctly parsed list`() {
        performJavadocTest { module ->
            val docs = (module.packages.single().classlikes.single() as DEnum).entries.single().documentation.values.single().children.single().root.text()
            assertEquals("content being refreshed, which can be a result of invalidation, refresh that may contain content updates, or the initial load.", docs.trimEnd())
        }
    }
}
