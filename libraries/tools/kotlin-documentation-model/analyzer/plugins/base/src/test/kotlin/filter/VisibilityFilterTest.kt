package filter

import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DEnum
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `private setter with false global includeNonPublic`() {
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
            |var property: Int = 0
            |private set 
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                Assertions.assertNull(
                    it.first().packages.first().properties.first().setter
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

    @Test
    fun `internal property from enum should be skipped`() {
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
            |package enums
            |
            |enum class Test(internal val value: Int) {
            |    A(0) {
            |        override fun testFun(): Float = 0.05F
            |    },
            |    B(1) {
            |        override fun testFun(): Float = 0.1F
            |    };
            | 
            |    internal open fun testFun(): Float = 0.5F
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val enum = module.packages.flatMap { it.classlikes }.filterIsInstance<DEnum>().first()
                val entry = enum.entries.first()

                assertFalse("testFun" in entry.functions.map { it.name })
                assertFalse("value" in entry.properties.map { it.name })
                assertFalse("testFun" in enum.functions.map { it.name })
            }
        }
    }

    @Test
    fun `internal property from enum`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    includeNonPublic = true
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package enums
            |
            |enum class Test(internal val value: Int) {
            |    A(0) {
            |        override fun testFun(): Float = 0.05F
            |    },
            |    B(1) {
            |        override fun testFun(): Float = 0.1F
            |    };
            | 
            |    internal open fun testFun(): Float = 0.5F
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val enum = module.packages.flatMap { it.classlikes }.filterIsInstance<DEnum>().first()
                val entry = enum.entries.first()

                assertTrue("testFun" in entry.functions.map { it.name })
                assertTrue("value" in entry.properties.map { it.name })
                assertTrue("testFun" in enum.functions.map { it.name })
            }
        }
    }
}
