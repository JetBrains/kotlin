/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package filter

import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.PackageOptionsImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DeprecationFilterTest : BaseAbstractTest() {

    @Test
    fun `should skip hidden deprecated level regardless of skipDeprecated`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                    classpath = listOfNotNull(jvmStdlibPath)
                    skipDeprecated = false
                    perPackageOptions = mutableListOf(
                        PackageOptionsImpl(
                            "example.*",
                            true,
                            false,
                            false,
                            false,
                            DokkaDefaults.documentedVisibilities
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
            |@Deprecated("dep", level = DeprecationLevel.HIDDEN)
            |fun testFunction() { }
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                assertTrue(
                    it.first().packages.first().functions.isEmpty()
                )
            }
        }
    }

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
            preMergeDocumentablesTransformationStage = {
                assertTrue(
                    it.first().packages.first().functions.size == 1
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
            preMergeDocumentablesTransformationStage = {
                assertTrue(
                    it.first().packages.first().functions.size == 1
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
            preMergeDocumentablesTransformationStage = {
                assertTrue(
                    it.first().packages.first().functions.isEmpty()
                )
            }
        }
    }

    @Test
    fun `should skip deprecated companion object`() {
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
            |class Test {
            |    @Deprecated("dep")
            |    companion object {
            |        fun method() {}
            |    }
            |}
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                assertTrue(
                    it.first().packages.first().classlikes.first().classlikes.isEmpty()
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
                        PackageOptionsImpl(
                            "example.*",
                            true,
                            false,
                            true,
                            false,
                            DokkaDefaults.documentedVisibilities
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
            |@Deprecated("dep")
            |fun testFunction() { }
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                assertTrue(
                    it.first().packages.first().functions.isEmpty()
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
                            false,
                            DokkaDefaults.documentedVisibilities
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
            |@Deprecated("dep")
            |fun testFunction() { }
            |
            |
        """.trimMargin(),
            configuration
        ) {
            preMergeDocumentablesTransformationStage = {
                assertTrue(
                    it.first().packages.first().functions.size == 1
                )
            }
        }
    }
}
