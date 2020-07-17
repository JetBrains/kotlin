package filter

import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DeprecationFilterTest : AbstractCoreTest() {
    @Test
    fun `function with false global skipDeprecated`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    skipDeprecated = false
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
    fun `deprecated function with false global skipDeprecated`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    skipDeprecated = false
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |@Deprecated("dep")
            |fun testFunction() { }
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
    fun `deprecated function with true global skipDeprecated`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    skipDeprecated = true
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package example
            |
            |@Deprecated("dep")
            |fun testFunction() { }
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
    fun `deprecated function with false global true package skipDeprecated`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    skipDeprecated = false
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl("example",
                            true,
                            false,
                            true,
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
            |@Deprecated("dep")
            |fun testFunction() { }
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
    fun `deprecated function with true global false package skipDeprecated`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    skipDeprecated = true
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
            |@Deprecated("dep")
            |fun testFunction() { }
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
}
