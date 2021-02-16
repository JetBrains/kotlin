package filter

import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VisibilityFilterTest : BaseAbstractTest() {

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
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 1
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
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 0
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
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 1
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
                        PackageOptionsImpl(
                            "example",
                            true,
                            false,
                            false,
                            false
                        )
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
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 1
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
                        PackageOptionsImpl(
                            "example",
                            false,
                            false,
                            false,
                            false
                        )
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
            preMergeDocumentablesTransformationStage = {
                Assertions.assertTrue(
                    it.first().packages.first().functions.size == 0
                )
            }
        }
    }

    @Test
    fun `private typealias should be skipped`() {
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
            |private typealias ABC = Int
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                assertEquals(0, it.first().packages.first().typealiases.size)
            }
        }
    }
}
