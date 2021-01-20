package content.annotations

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.StringValue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class FileLevelJvmNameTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                classpath += jvmStdlibPath!!
            }
        }
    }

    companion object {
        private const val functionTest =
            """
            |/src/main/kotlin/test/source.kt
            |@file:JvmName("CustomJvmName")
            |package test
            |
            |fun function(abc: String): String {
            |    return "Hello, " + abc
            |}
        """

        private const val extensionFunctionTest =
            """
            |/src/main/kotlin/test/source.kt
            |@file:JvmName("CustomJvmName")
            |package test
            |
            |fun String.function(abc: String): String {
            |    return "Hello, " + abc
            |}
        """

        private const val propertyTest =
            """
            |/src/main/kotlin/test/source.kt
            |@file:JvmName("CustomJvmName")
            |package test
            |
            |val property: String
            |   get() = ""
        """

        private const val extensionPropertyTest =
            """
            |/src/main/kotlin/test/source.kt
            |@file:JvmName("CustomJvmName")
            |package test
            |
            |val String.property: String
            |   get() = ""
        """
    }

    @ParameterizedTest
    @ValueSource(strings = [functionTest, extensionFunctionTest])
    fun `jvm name should be included in functions extra`(query: String) {
        testInline(
            query.trimIndent(), testConfiguration
        ) {
            documentablesCreationStage = { modules ->
                val expectedAnnotation = Annotations.Annotation(
                    dri = DRI("kotlin.jvm", "JvmName"),
                    params = mapOf("name" to StringValue("CustomJvmName")),
                    scope = Annotations.AnnotationScope.FILE,
                    mustBeDocumented = true
                )
                val function = modules.flatMap { it.packages }.first().functions.first()
                val annotation = function.extra[Annotations]?.fileLevelAnnotations?.entries?.first()?.value?.single()
                assertEquals(emptyMap(), function.extra[Annotations]?.directAnnotations)
                assertEquals(expectedAnnotation, annotation)
                assertEquals(expectedAnnotation.scope, annotation?.scope)
                assertEquals(expectedAnnotation.mustBeDocumented, annotation?.mustBeDocumented)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [propertyTest, extensionPropertyTest])
    fun `jvm name should be included in properties extra`(query: String) {
        testInline(
            query.trimIndent(), testConfiguration
        ) {
            documentablesCreationStage = { modules ->
                val expectedAnnotation = Annotations.Annotation(
                    dri = DRI("kotlin.jvm", "JvmName"),
                    params = mapOf("name" to StringValue("CustomJvmName")),
                    scope = Annotations.AnnotationScope.FILE,
                    mustBeDocumented = true
                )
                val properties = modules.flatMap { it.packages }.first().properties.first()
                val annotation = properties.extra[Annotations]?.fileLevelAnnotations?.entries?.first()?.value?.single()
                assertEquals(emptyMap(), properties.extra[Annotations]?.directAnnotations)
                assertEquals(expectedAnnotation, annotation)
                assertEquals(expectedAnnotation.scope, annotation?.scope)
                assertEquals(expectedAnnotation.mustBeDocumented, annotation?.mustBeDocumented)
            }
        }
    }
}