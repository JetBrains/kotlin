package filter

import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EmptyPackagesFilterTest : AbstractCoreTest() {
    @Test
    fun `empty package with false skipEmptyPackages`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    skipEmptyPackages = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesFirstTransformationStep = {
                Assertions.assertTrue(
                    it.component2().packages.isNotEmpty()
                )
            }
        }
    }
    @Test
    fun `empty package with true skipEmptyPackages`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    skipEmptyPackages = true
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesFirstTransformationStep = {
                Assertions.assertTrue(
                    it.component2().packages.isEmpty()
                )
            }
        }
    }
}
