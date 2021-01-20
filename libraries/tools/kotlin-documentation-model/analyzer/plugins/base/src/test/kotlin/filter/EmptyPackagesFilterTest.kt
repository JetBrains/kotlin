package filter

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EmptyPackagesFilterTest : BaseAbstractTest() {
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
                    it.first().packages.isNotEmpty()
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
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            | class ThisShouldBePresent { }
            |/src/main/kotlin/empty/TestEmpty.kt
            |package empty
        """.trimMargin(),
            configuration
        ) {
            documentablesFirstTransformationStep = { modules ->
                modules.forEach { module ->
                    assertEquals(listOf("example"), module.packages.map { it.name })
                }
            }
        }
    }
}
