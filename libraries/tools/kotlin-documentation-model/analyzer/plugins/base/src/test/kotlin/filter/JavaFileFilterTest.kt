package filter

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JavaFileFilterTest : BaseAbstractTest() {
    @Test
    fun `java file should be included`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    skipEmptyPackages = false
                    sourceRoots = listOf("src/main/java/basic/Test.java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/basic/Test.java
            |package example;
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.isNotEmpty()
                )
            }
        }
    }
}
