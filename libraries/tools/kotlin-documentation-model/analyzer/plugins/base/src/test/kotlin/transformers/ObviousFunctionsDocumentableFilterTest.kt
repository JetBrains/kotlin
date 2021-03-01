package transformers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ObviousFunctionsDocumentableFilterTest : BaseAbstractTest() {
    val suppressingConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
            }
        }
    }

    val nonSuppressingConfiguration = dokkaConfiguration {
        suppressObviousFunctions = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
            }
        }
    }

    @Test
    fun `should suppress toString, equals and hashcode`() {
        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            data class Suppressed(val x: String)
            """.trimIndent(),
            suppressingConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(0, functions.size)
            }
        }
    }

    @Test
    fun `should suppress toString, equals and hashcode in Java`() {
        testInline(
            """
            /src/suppressed/Suppressed.java
            package suppressed;
            public class Suppressed {
            }
            """.trimIndent(),
            suppressingConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(0, functions.size)
            }
        }
    }

    @Test
    fun `should suppress toString, equals and hashcode but keep custom ones`() {
        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            data class Suppressed(val x: String) {
                override fun toString(): String {
                    return "custom"
                }
            }
            """.trimIndent(),
            suppressingConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(listOf("toString"), functions.map { it.name })
            }
        }
    }

    @Test
    fun `should suppress toString, equals and hashcode but keep custom ones in Java`() {
        testInline(
            """
            /src/suppressed/Suppressed.java
            package suppressed;
            public class Suppressed {
                @Override
                public String toString() {
                    return "";
                }
            }
            """.trimIndent(),
            suppressingConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(listOf("toString"), functions.map { it.name })
            }
        }
    }

    @Test
    fun `should not suppress toString, equals and hashcode if custom config is provided`() {
        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            data class Suppressed(val x: String)
            """.trimIndent(),
            nonSuppressingConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(
                    listOf("copy", "equals", "toString", "component1", "hashCode").sorted(),
                    functions.map { it.name }.sorted()
                )
            }
        }
    }

    @Test
    fun `should not suppress toString, equals and hashcode if custom config is provided in Java`() {
        testInline(
            """
            /src/suppressed/Suppressed.java
            package suppressed;
            public class Suppressed {
            }
            """.trimIndent(),
            nonSuppressingConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                //I would normally just assert names but this would make it JDK dependent, so this is better
                assertEquals(
                    5,
                    setOf("equals", "hashCode", "toString", "notify", "notifyAll").intersect(functions.map { it.name }).size
                )
            }
        }
    }
}