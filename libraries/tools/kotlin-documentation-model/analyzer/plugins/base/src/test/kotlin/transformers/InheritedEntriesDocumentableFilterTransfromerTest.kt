package transformers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InheritedEntriesDocumentableFilterTransformerTest : BaseAbstractTest() {
    val suppressingInheritedConfiguration = dokkaConfiguration {
        suppressInheritedMembers = true
        suppressObviousFunctions = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
            }
        }
    }

    val nonSuppressingInheritedConfiguration = dokkaConfiguration {
        suppressObviousFunctions = false
        suppressInheritedMembers = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
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
            suppressingInheritedConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(listOf("toString", "copy", "component1").sorted(), functions.map { it.name }.sorted())
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
            suppressingInheritedConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val functions = modules.flatMap { it.packages }.flatMap { it.classlikes }.flatMap { it.functions }
                assertEquals(listOf("copy", "component1").sorted(), functions.map { it.name }.sorted())
            }
        }
    }

    @Test
    fun `should also suppress properites`(){
        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            open class Parent {
                val parentValue = "String"
            }
            
            class Child : Parent {
                
            }
            """.trimIndent(),
            suppressingInheritedConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val properties = modules.flatMap { it.packages }.flatMap { it.classlikes }.first { it.name == "Child" }.properties
                assertEquals(0, properties.size)
            }
        }
    }

    @Test
    fun `should not suppress properites if config says so`(){
        testInline(
            """
            /src/suppressed/Suppressed.kt
            package suppressed
            open class Parent {
                val parentValue = "String"
            }
            
            class Child : Parent {
                
            }
            """.trimIndent(),
            nonSuppressingInheritedConfiguration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val properties = modules.flatMap { it.packages }.flatMap { it.classlikes }.first { it.name == "Child" }.properties
                assertEquals(listOf("parentValue"), properties.map { it.name })
            }
        }
    }
}