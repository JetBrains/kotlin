/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model.annotations

import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.annotations
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.utilities.cast
import utils.AbstractModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `type parameter annotations should be visible even if type declaration has none`() {
        inlineModelTest(
            """
             |@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
             |annotation class Hello
             |
             |fun <T> foo(param: List<@Hello T>) {}
            """.trimIndent()
        ) {
            with((this / "annotations" / "foo").cast<DFunction>()) {
                val paramAnnotations = parameters.first()
                    .type.cast<GenericTypeConstructor>()
                    .projections
                    .first().cast<Invariance<TypeParameter>>()
                    .inner.cast<TypeParameter>()
                    .annotations()
                    .values
                    .flatten()

                assertEquals(1, paramAnnotations.size)
                assertEquals(DRI("annotations", "Hello"), paramAnnotations[0].dri)
            }
        }
    }

    @Test
    fun `type parameter annotations should not be propagated from resolved type`() {
        inlineModelTest(
            """
             |@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
             |annotation class Hello
             |
             |fun <@Hello T> foo(param: List<T>) {}
            """.trimIndent()
        ) {
            with((this / "annotations" / "foo").cast<DFunction>()) {
                val paramAnnotations = parameters.first()
                    .type.cast<GenericTypeConstructor>()
                    .projections.first().cast<Invariance<TypeParameter>>()
                    .inner.cast<TypeParameter>()
                    .annotations()

                assertTrue(paramAnnotations.isEmpty())
            }
        }
    }
}
