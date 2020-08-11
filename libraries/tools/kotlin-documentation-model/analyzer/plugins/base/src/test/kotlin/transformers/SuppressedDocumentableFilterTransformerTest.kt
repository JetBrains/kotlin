package transformers

import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test

class SuppressedDocumentableFilterTransformerTest : AbstractCoreTest() {

    @Test
    fun `class filtered by package options`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    perPackageOptions = listOf(
                        packageOptions(prefix = "suppressed", suppress = true),
                        packageOptions(prefix = "default", suppress = false)
                    )
                }
            }
        }

        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            class Suppressed
            
            /src/default/Default.kt
            package default
            class Default.kt
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(1, module.children.size, "Expected just a single package in module")
                assertEquals(1, module.packages.size, "Expected just a single package in module")

                val pkg = module.packages.single()
                assertEquals("default", pkg.dri.packageName, "Expected 'default' package in module")
                assertEquals(1, pkg.children.size, "Expected just a single child in 'default' package")
                assertEquals(1, pkg.classlikes.size, "Expected just a single child in 'default' package")

                val classlike = pkg.classlikes.single()
                assertEquals(DRI("default", "Default"), classlike.dri, "Expected 'Default' class in 'default' package")
            }
        }
    }

    @Test
    fun `class filtered by more specific package options`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    perPackageOptions = listOf(
                        packageOptions(prefix = "parent.some", suppress = false),
                        packageOptions(prefix = "parent.some.suppressed", suppress = true),

                        packageOptions(prefix = "parent.other", suppress = true),
                        packageOptions(prefix = "parent.other.default", suppress = false)
                    )
                }
            }
        }

        testInline(
            """
            /src/parent/some/Some.kt
            package parent.some
            class Some
            
            /src/parent/some/suppressed/Suppressed.kt
            package parent.some.suppressed
            class Suppressed
            
            /src/parent/other/Other.kt
            package parent.other
            class Other
            
            /src/parent/other/default/Default.kt
            package parent.other.default
            class Default
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(2, module.packages.size, "Expected two packages in module")
                assertIterableEquals(
                    listOf(DRI("parent.some"), DRI("parent.other.default")).sortedBy { it.packageName },
                    module.packages.map { it.dri }.sortedBy { it.packageName },
                    "Expected 'parent.some' and 'parent.other.default' packages to be not suppressed"
                )
            }
        }
    }

    @Test
    fun `class filtered by parent file path`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressedFiles = listOf("src/suppressed")
                }
            }
        }

        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            class Suppressed
            
            /src/default/Default.kt
            package default
            class Default.kt
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(1, module.children.size, "Expected just a single package in module")
                assertEquals(1, module.packages.size, "Expected just a single package in module")

                val pkg = module.packages.single()
                assertEquals("default", pkg.dri.packageName, "Expected 'default' package in module")
                assertEquals(1, pkg.children.size, "Expected just a single child in 'default' package")
                assertEquals(1, pkg.classlikes.size, "Expected just a single child in 'default' package")

                val classlike = pkg.classlikes.single()
                assertEquals(DRI("default", "Default"), classlike.dri, "Expected 'Default' class in 'default' package")
            }
        }
    }

    @Test
    fun `class filtered by exact file path`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressedFiles = listOf("src/suppressed/Suppressed.kt")
                }
            }
        }

        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            class Suppressed
            
            /src/default/Default.kt
            package default
            class Default.kt
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(1, module.children.size, "Expected just a single package in module")
                assertEquals(1, module.packages.size, "Expected just a single package in module")

                val pkg = module.packages.single()
                assertEquals("default", pkg.dri.packageName, "Expected 'default' package in module")
                assertEquals(1, pkg.children.size, "Expected just a single child in 'default' package")
                assertEquals(1, pkg.classlikes.size, "Expected just a single child in 'default' package")

                val classlike = pkg.classlikes.single()
                assertEquals(DRI("default", "Default"), classlike.dri, "Expected 'Default' class in 'default' package")
            }
        }
    }

    private fun packageOptions(
        prefix: String,
        suppress: Boolean
    ) = PackageOptionsImpl(
        prefix = prefix,
        suppress = suppress,
        includeNonPublic = true,
        reportUndocumented = false,
        skipDeprecated = false
    )

}
