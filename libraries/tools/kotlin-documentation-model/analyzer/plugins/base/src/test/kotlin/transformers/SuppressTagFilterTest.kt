package transformers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.WithCompanion
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SuppressTagFilterTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
            }
        }
    }

    @Test
    fun `should filter classes with suppress tag`() {
        testInline(
            """
            |/src/suppressed/NotSuppressed.kt
            |/**
            | * sample docs
            |*/
            |class NotSuppressed 
            |/src/suppressed/Suppressed.kt
            |/**
            | * sample docs
            | * @suppress
            |*/
            |class Suppressed 
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(
                    "NotSuppressed",
                    modules.flatMap { it.packages }.flatMap { it.classlikes }.singleOrNull()?.name
                )
            }
        }
    }

    @Test
    fun `should filter functions with suppress tag`() {
        testInline(
            """
            |/src/suppressed/Suppressed.kt
            |class Suppressed {
            |   /**
            |    * sample docs
            |    * @suppress
            |    */
            |   fun suppressedFun(){ }
            |}
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertNull(modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                    .firstOrNull { it.name == "suppressedFun" })
            }
        }
    }

    @Test
    fun `should filter top level functions`() {
        testInline(
            """
            |/src/suppressed/Suppressed.kt
            |/**
            | * sample docs
            | * @suppress
            | */
            |fun suppressedFun(){ }
            |
            |/**
            | * Sample
            | */
            |fun notSuppressedFun() { }
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertNull(modules.flatMap { it.packages }.flatMap { it.functions }
                    .firstOrNull { it.name == "suppressedFun" })
            }
        }
    }

    @Test
    fun `should filter top level type aliases`() {
        testInline(
            """
            |/src/suppressed/suppressed.kt
            |/**
            | * sample docs
            | * @suppress
            | */
            |typealias suppressedTypeAlias = String
            |
            |/**
            | * Sample
            | */
            |typealias notSuppressedTypeAlias = String
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertNull(modules.flatMap { it.packages }.flatMap { it.typealiases }
                    .firstOrNull { it.name == "suppressedTypeAlias" })
                assertNotNull(modules.flatMap { it.packages }.flatMap { it.typealiases }
                    .firstOrNull { it.name == "notSuppressedTypeAlias" })
            }
        }
    }

    @Test
    fun `should filter companion object`() {
        testInline(
            """
            |/src/suppressed/Suppressed.kt
            |class Suppressed {
            |/**
            | * @suppress
            | */
            |companion object {
            |    val x = 1
            |}
            |}
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertNull((modules.flatMap { it.packages }.flatMap { it.classlikes }
                    .firstOrNull { it.name == "Suppressed" } as? WithCompanion)?.companion)
            }
        }
    }

    @Test
    fun `should suppress inner classlike`() {
        testInline(
            """
            |/src/suppressed/Testing.kt
            |class Testing {
            |    /**
            |     * Sample
            |     * @suppress
            |     */
            |    inner class Suppressed {
            |        val x = 1
            |    }
            |}
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val testingClass = modules.flatMap { it.packages }.flatMap { it.classlikes }.single()
                assertNull(testingClass.classlikes.firstOrNull())
            }
        }
    }

    @Test
    fun `should suppress enum entry`() {
        testInline(
            """
            |/src/suppressed/Testing.kt
            |enum class Testing {
            |    /**
            |     * Sample
            |     * @suppress
            |     */
            |    SUPPRESSED,
            | 
            |   /**
            |     * Not suppressed
            |     */
            |    NOT_SUPPRESSED
            |}
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val testingClass = modules.flatMap { it.packages }.flatMap { it.classlikes }.single() as DEnum
                assertEquals(listOf("NOT_SUPPRESSED"), testingClass.entries.map { it.name })
            }
        }
    }
}