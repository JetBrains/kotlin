package model.annotations

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import kotlin.test.assertEquals

class KotlinAnnotationsForParametersTest : AbstractModelTest("/src/main/kotlin/annotations/Test.kt", "annotations") {
    @Test
    fun `generic receiver with annotations`() {
        inlineModelTest(
            """
             |@Target(AnnotationTarget.TYPE_PARAMETER)
             |annotation class Hello(val bar: String)
             |fun <@Hello("abc") T> foo(arg: String): List<T> = TODO()
            """.trimIndent()
        ) {
            with((this / "annotations" / "foo").cast<DFunction>()) {
                val annotations = generics.first().extra[Annotations]?.directAnnotations?.flatMap { it.value }
                val driOfHello = DRI("annotations", "Hello")
                val annotationsValues = annotations?.flatMap { it.params.values }?.map { it.toString() }?.toList()

                assertEquals(listOf(driOfHello), annotations?.map { it.dri })
                assertEquals(listOf("abc"), annotationsValues)
            }
        }
    }

    @Test
    fun `generic receiver with annotated bounds`() {
        inlineModelTest(
            """
             |@Target(AnnotationTarget.TYPE_PARAMETER)
             |annotation class Hello(val bar: String)
             |fun <T: @Hello("abc") String> foo(arg: String): List<T> = TODO()
            """.trimIndent()
        ) {
            with((this / "annotations" / "foo").cast<DFunction>()) {
                val annotations = (generics.first().bounds.first() as GenericTypeConstructor)
                    .extra[Annotations]?.directAnnotations?.flatMap { it.value }
                val driOfHello = DRI("annotations", "Hello")
                val annotationsValues = annotations?.flatMap { it.params.values }?.map { it.toString() }?.toList()

                assertEquals(listOf(driOfHello), annotations?.map { it.dri })
                assertEquals(listOf("abc"), annotationsValues)
            }
        }
    }
}