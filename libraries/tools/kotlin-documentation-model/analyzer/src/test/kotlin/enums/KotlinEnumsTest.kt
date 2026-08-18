/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package enums

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.dfs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinEnumsTest : BaseAbstractTest() {

    @Test
    fun `should preserve enum source ordering for documentables`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   ZERO,
            |   ONE,
            |   TWO,
            |   THREE,
            |   FOUR,
            |   FIVE,
            |   SIX,
            |   SEVEN,
            |   EIGHT,
            |   NINE
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val testPackage = module.packages[0]
                assertEquals("testpackage", testPackage.name)

                val testEnum = testPackage.classlikes[0] as DEnum
                assertEquals("TestEnum", testEnum.name)

                val enumEntries = testEnum.entries
                assertEquals(10, enumEntries.count())

                assertEquals("ZERO", enumEntries[0].name)
                assertEquals("ONE", enumEntries[1].name)
                assertEquals("TWO", enumEntries[2].name)
                assertEquals("THREE", enumEntries[3].name)
                assertEquals("FOUR", enumEntries[4].name)
                assertEquals("FIVE", enumEntries[5].name)
                assertEquals("SIX", enumEntries[6].name)
                assertEquals("SEVEN", enumEntries[7].name)
                assertEquals("EIGHT", enumEntries[8].name)
                assertEquals("NINE", enumEntries[9].name)
            }
        }
    }

    @Test
    fun `should handle companion object within enum`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   E1,
            |   E2;
            |   companion object {}
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { m ->
                m.packages.let { p ->
                    assertTrue(p.isNotEmpty(), "Package list cannot be empty")
                    p.first().classlikes.let { c ->
                        assertTrue(c.isNotEmpty(), "Classlikes list cannot be empty")

                        val enum = c.first() as DEnum
                        assertNotNull(enum.companion)
                    }
                }
            }
        }
    }

    @Test
    fun enumWithMethods() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    classpath = listOfNotNull(jvmStdlibPath)
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/TestEnum.kt
            |package testpackage
            |
            |
            |interface Sample {
            |    fun toBeImplemented(): String
            |}
            |
            |enum class TestEnum: Sample {
            |    E1 {
            |        override fun toBeImplemented(): String = "e1"
            |    }
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { m ->
                m.packages.let { p ->
                    p.first().classlikes.let { c ->
                        val enum = c.first { it is DEnum } as DEnum
                        val first = enum.entries.first()

                        assertNotNull(first.functions.find { it.name == "toBeImplemented" })
                    }
                }
            }
        }
    }

    @Test
    fun `enum should have functions on page`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/TestEnum.kt
            |package testpackage
            |
            |
            |interface Sample {
            |    fun toBeImplemented(): String
            |}
            |
            |enum class TestEnum: Sample {
            |    E1 {
            |        override fun toBeImplemented(): String = "e1"
            |    }
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val enum = module.dfs { it.name == "TestEnum" } as DEnum
                assertEquals(
                    listOf("toBeImplemented", "valueOf", "values"),
                    enum.functions.map { it.name }.sorted()
                )

                val entry = enum.entries.single { it.name == "E1" }
                assertEquals(
                    listOf("toBeImplemented"),
                    entry.functions.map { it.name }
                )
            }
        }
    }

    // Synthetic methods (values, valueOf) should still resolve a source location so that source links
    // can be generated for them without failing the build.
    // Initially reported for Java, making sure it doesn't fail for Kotlin either
    // https://github.com/Kotlin/dokka/issues/2544
    @Test
    fun `kotlin enum synthetic methods should resolve source line numbers`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/KotlinEnum.kt
            |package testpackage
            |
            |/**
            |* Doc
            |*/
            |enum class KotlinEnum {
            |    ONE, TWO, THREE
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val enum = module.packages.single()
                    .classlikes.single() as DEnum

                val enumSource = enum.sources.values.single()
                assertEquals("KotlinEnum.kt", enumSource.path.substringAfterLast('/'))
                assertEquals(6, enumSource.computeLineNumber())

                // synthetic methods (values, valueOf) fall back to the enum declaration line
                val syntheticMethods = enum.functions.filter { it.name == "values" || it.name == "valueOf" }
                assertEquals(2, syntheticMethods.size)
                syntheticMethods.forEach { method ->
                    assertEquals(6, method.sources.values.single().computeLineNumber())
                }
            }
        }
    }
}
