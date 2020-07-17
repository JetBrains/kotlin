package translators

import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DefaultDescriptorToDocumentableTranslatorTest : AbstractCoreTest() {

    @Test
    fun `data class kdocs over generated methods`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/sample/XD.kt
            |package sample
            |/**
            | * But the fat Hobbit, he knows. Eyes always watching.
            | */
            |data class XD(val xd: String) {
            |   /**
            |    * But the fat Hobbit, he knows. Eyes always watching.
            |    */
            |   fun custom(): String = ""
            |
            |   /**
            |    * Memory is not what the heart desires. That is only a mirror.
            |    */
            |   override fun equals(other: Any?): Boolean = true
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assert(module.documentationOf("XD", "copy") == "")
                assert(module.documentationOf("XD", "equals") == "Memory is not what the heart desires. That is only a mirror.")
                assert(module.documentationOf("XD", "hashCode") == "")
                assert(module.documentationOf("XD", "toString") == "")
                assert(module.documentationOf("XD", "custom") == "But the fat Hobbit, he knows. Eyes always watching.")
            }
        }
    }

    @Test
    fun `simple class kdocs`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/sample/XD.kt
            |package sample
            |/**
            | * But the fat Hobbit, he knows. Eyes always watching.
            | */
            |class XD(val xd: String) {
            |   /**
            |    * But the fat Hobbit, he knows. Eyes always watching.
            |    */
            |   fun custom(): String = ""
            |
            |   /**
            |    * Memory is not what the heart desires. That is only a mirror.
            |    */
            |   override fun equals(other: Any?): Boolean = true
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assert(module.documentationOf("XD", "custom") == "But the fat Hobbit, he knows. Eyes always watching.")
                assert(module.documentationOf("XD", "equals") == "Memory is not what the heart desires. That is only a mirror.")
            }
        }
    }
}