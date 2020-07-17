package filter

import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VisibilityFilterTest : AbstractCoreTest() {
    @Test
    fun `public function with false global includeNonPublic`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesFirstTransformationStep = {
                Assertions.assertTrue(
                    it.component2().packages.first().functions.size == 1
                )
            }
        }
    }
    @Test
    fun `private function with false global includeNonPublic`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesFirstTransformationStep = {
                Assertions.assertTrue(
                    it.component2().packages.first().functions.size == 0
                )
            }
        }
    }
    @Test
    fun `private function with true global includeNonPublic`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    includeNonPublic = true
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesFirstTransformationStep = {
                Assertions.assertTrue(
                    it.component2().packages.first().functions.size == 1
                )
            }
        }
    }
    @Test
    fun `private function with false global true package includeNonPublic`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    includeNonPublic = false
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl("example",
                            true,
                            false,
                            false,
                            false)
                    )
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesFirstTransformationStep = {
                Assertions.assertTrue(
                    it.component2().packages.first().functions.size == 1
                )
            }
        }
    }
    @Test
    fun `private function with true global false package includeNonPublic`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    includeNonPublic = true
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl("example",
                            false,
                            false,
                            false,
                            false)
                    )
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |private fun testFunction() { }
            |
            |
            |
        """.trimMargin(),
            configuration
        ) {
            documentablesFirstTransformationStep = {
                 Assertions.assertTrue(
                     it.component2().packages.first().functions.size == 0
                 )
             }
        }
    }
}
