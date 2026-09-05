/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package transformers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import kotlin.test.*

class SuppressedByAnnotationsDocumentableFilterTransformerTest : BaseAbstractTest() {

    @Test
    fun `should suppress class by annotation`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressAnnotatedWith = setOf("test.SuppressMe")
                }
            }
        }

        testInline(
            """
            /src/test/Annotated.kt
            package test
            annotation class SuppressMe

            @SuppressMe
            class Annotated

            class NotAnnotated
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                val classNames = pkg.classlikes.map { it.name }
                // SuppressMe itself should be present, but Annotated should be suppressed
                assertEquals(setOf("SuppressMe", "NotAnnotated"), classNames.toSet())
                assertNull(pkg.classlikes.find { it.name == "Annotated" })
            }
        }
    }

    @Test
    fun `should suppress function by annotation`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressAnnotatedWith = setOf("test.SuppressMe")
                }
            }
        }

        testInline(
            """
            /src/test/Functions.kt
            package test
            annotation class SuppressMe

            class Container {
                @SuppressMe
                fun annotated() {}

                fun notAnnotated() {}
            }
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                val container = pkg.classlikes.single { it.name == "Container" } as DClass
                val functionNames = container.functions.map { it.name }
                assertEquals(listOf("notAnnotated"), functionNames)
            }
        }
    }

    @Test
    fun `should suppress by multiple annotations`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressAnnotatedWith = setOf("test.SuppressMe1", "test.SuppressMe2")
                }
            }
        }

        testInline(
            """
            /src/test/Multiple.kt
            package test
            annotation class SuppressMe1
            annotation class SuppressMe2

            @SuppressMe1
            class Suppressed1

            @SuppressMe2
            class Suppressed2

            class NotSuppressed
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                val classNames = pkg.classlikes.map { it.name }
                assertEquals(setOf("SuppressMe1", "SuppressMe2", "NotSuppressed"), classNames.toSet())
            }
        }
    }

    @Test
    fun `should handle empty suppressAnnotatedWith`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressAnnotatedWith = emptySet<String>()
                }
            }
        }

        testInline(
            """
            /src/test/Empty.kt
            package test
            annotation class SuppressMe

            @SuppressMe
            class Annotated
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                assertNotNull(pkg.classlikes.find { it.name == "Annotated" })
            }
        }
    }

    @Test
    fun `should filter per source set in KMP with expect actual - jvm suppressed native not`() {
        // This test verifies the corner case where an actual implementation is suppressed on one platform
        // but should remain on other platforms. The filtering happens per source set in pre-merge phase.

        val configuration = dokkaConfiguration {
            sourceSets {
                val common = sourceSet {
                    sourceRoots = listOf("src/common")
                    analysisPlatform = "common"
                    name = "common"
                    displayName = "common"
                }
                sourceSet {
                    sourceRoots = listOf("src/jvm")
                    analysisPlatform = "jvm"
                    name = "jvm"
                    displayName = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                    suppressAnnotatedWith = setOf("test.SuppressMe")
                }
                sourceSet {
                    sourceRoots = listOf("src/native")
                    analysisPlatform = "native"
                    name = "native"
                    displayName = "native"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
            }
        }

        testInline(
            """
            /src/common/test.kt
            package test
            annotation class SuppressMe
            expect fun f(): String
            
            /src/jvm/test.kt
            package test
            @SuppressMe
            actual fun f(): String = "jvm"
            
            /src/native/test.kt
            package test
            actual fun f(): String = "native"
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                val suppressMe = pkg.classlikes.find { it.name == "SuppressMe" }
                assertNotNull(suppressMe, "SuppressMe annotation class should be present")

                val allF = pkg.functions.filter { it.name == "f" }

                assertTrue(pkg.classlikes.any { it.name == "SuppressMe" }, "SuppressMe annotation should be present")

                val fSourceSets = allF.flatMap { it.sourceSets.map { it.sourceSetID.sourceSetName } }.toSet()
                assertEquals(
                    setOf("common", "native"),
                    fSourceSets,
                    "f should be present in common and native"
                )
            }
        }
    }
}
