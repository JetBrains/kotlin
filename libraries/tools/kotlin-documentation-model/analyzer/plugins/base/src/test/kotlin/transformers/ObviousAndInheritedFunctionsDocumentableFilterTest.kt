package transformers

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import testApi.testRunner.dokkaConfiguration
import kotlin.test.assertEquals

class ObviousAndInheritedFunctionsDocumentableFilterTest : BaseAbstractTest() {
    companion object {
        @JvmStatic
        fun suppressingObviousConfiguration() = listOf(dokkaConfiguration {
            suppressInheritedMembers = false
            suppressObviousFunctions = true
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                }
            }
        })

        @JvmStatic
        fun nonSuppressingObviousConfiguration() = listOf(dokkaConfiguration {
            suppressObviousFunctions = false
            suppressInheritedMembers = false
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                }
            }
        })

        @JvmStatic
        fun suppressingInheritedConfiguration() = listOf(dokkaConfiguration {
            suppressInheritedMembers = true
            suppressObviousFunctions = false
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                }
            }
        })

        @JvmStatic
        fun nonSuppressingInheritedConfiguration() = listOf(dokkaConfiguration {
            suppressObviousFunctions = false
            suppressInheritedMembers = false
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                }
            }
        })
    }


    @ParameterizedTest
    @MethodSource(value = ["suppressingObviousConfiguration"])
    fun `should suppress toString, equals and hashcode`(suppressingConfiguration: DokkaConfigurationImpl) {
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

    @ParameterizedTest
    @MethodSource(value = ["suppressingObviousConfiguration", "suppressingInheritedConfiguration"])
    fun `should suppress toString, equals and hashcode for interface`(suppressingConfiguration: DokkaConfigurationImpl) {
        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            interface Suppressed
            """.trimIndent(),
            suppressingConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(0, functions.size)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = ["suppressingObviousConfiguration", "suppressingInheritedConfiguration"])
    fun `should suppress toString, equals and hashcode in Java`(suppressingConfiguration: DokkaConfigurationImpl) {
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

    @ParameterizedTest
    @MethodSource(value = ["suppressingObviousConfiguration"])
    fun `should suppress toString, equals and hashcode but keep custom ones`(suppressingConfiguration: DokkaConfigurationImpl) {
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

    @ParameterizedTest
    @MethodSource(value = ["suppressingObviousConfiguration", "suppressingInheritedConfiguration"])
    fun `should suppress toString, equals and hashcode but keep custom ones in Java`(suppressingConfiguration: DokkaConfigurationImpl) {
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

    @ParameterizedTest
    @MethodSource(value = ["nonSuppressingObviousConfiguration", "nonSuppressingInheritedConfiguration"])
    fun `should not suppress toString, equals and hashcode if custom config is provided`(nonSuppressingConfiguration: DokkaConfigurationImpl) {
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

    @ParameterizedTest
    @MethodSource(value = ["nonSuppressingObviousConfiguration", "nonSuppressingInheritedConfiguration"])
    fun `not should suppress toString, equals and hashcode for interface if custom config is provided`(nonSuppressingConfiguration: DokkaConfigurationImpl) {
        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            interface Suppressed
            """.trimIndent(),
            nonSuppressingConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(listOf("equals", "hashCode", "toString").sorted(), functions.map { it.name }.sorted())
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = ["nonSuppressingObviousConfiguration", "nonSuppressingInheritedConfiguration"])
    fun `should not suppress toString, equals and hashcode if custom config is provided in Java`(nonSuppressingConfiguration: DokkaConfigurationImpl) {
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
                    setOf(
                        "equals",
                        "hashCode",
                        "toString",
                        "notify",
                        "notifyAll"
                    ).intersect(functions.map { it.name }).size
                )
            }
        }
    }
}