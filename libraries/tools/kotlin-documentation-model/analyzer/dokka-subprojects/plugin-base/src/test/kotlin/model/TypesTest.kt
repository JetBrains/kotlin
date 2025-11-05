/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import utils.AbstractModelTest
import utils.OnlySymbols
import utils.assertIsInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalDokkaApi::class)
class TypesTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "types") {

    @Test
    fun `type with typealias to functional type with parameter`() {
        inlineModelTest(
            """
            |typealias HttpExceptionCallback<T> = String.(T) -> String
            |fun <T> exception(callback: HttpExceptionCallback<T>){}"""
        ) {
            with((this / "types" / "HttpExceptionCallback").cast<DTypeAlias>()) {
                name equals "HttpExceptionCallback"
                assertTrue(type is GenericTypeConstructor)
                (type as GenericTypeConstructor).projections counts 1
                underlyingType.values.first().driOrNull equals DRI("kotlin", "Function2")
            }
            with((this / "types" / "exception").cast<DFunction>()) {
                name equals "exception"
                val parameterType = parameters.first().type
                assertTrue(parameterType is TypeAliased)
                with(parameterType) {
                    assertTrue(typeAlias is GenericTypeConstructor)
                    (typeAlias as GenericTypeConstructor).projections counts 1
                    assertTrue(inner is FunctionalTypeConstructor)
                    (inner as FunctionalTypeConstructor).dri equals DRI("kotlin", "Function2")
                    (inner as FunctionalTypeConstructor).projections counts 3
                }
            }
        }
    }

    @Test
    fun `type with typealias to functional type`() {
        inlineModelTest(
            """
            |typealias CompletionHandler = (cause: Throwable?) -> Unit
            |fun exception(callback: CompletionHandler){}"""
        ) {
            with((this / "types" / "CompletionHandler").cast<DTypeAlias>()) {
                assertTrue(type is GenericTypeConstructor)
                (type as GenericTypeConstructor).projections counts 0
                name equals "CompletionHandler"
                underlyingType.values.first().driOrNull equals DRI("kotlin", "Function1")
            }
            with((this / "types" / "exception").cast<DFunction>()) {
                name equals "exception"
                val parameterType = parameters.first().type
                assertTrue(parameterType is TypeAliased)
                with(parameterType) {
                    assertTrue(typeAlias is GenericTypeConstructor)
                    assertTrue(inner is FunctionalTypeConstructor)
                    (inner as FunctionalTypeConstructor).dri equals DRI("kotlin", "Function1")
                    (inner as FunctionalTypeConstructor).projections counts 2
                }
            }
        }
    }

    @Test
    @OnlySymbols("context parameters")
    fun `type with typealias to functional type with context parameters`() {
        inlineModelTest(
            """
            |typealias CompletionHandler = context(a: String, _: Any) (cause: Throwable) -> Unit
            |fun exception(callback: CompletionHandler){}"""
        ) {
            with((this / "types" / "CompletionHandler").cast<DTypeAlias>()) {
                assertTrue(type is GenericTypeConstructor)
                (type as GenericTypeConstructor).projections counts 0
                name equals "CompletionHandler"
                underlyingType.values.first().driOrNull equals DRI("kotlin", "Function3")
            }
            with((this / "types" / "exception").cast<DFunction>()) {
                name equals "exception"
                val parameterType = parameters.first().type
                assertTrue(parameterType is TypeAliased)
                with(parameterType) {
                    assertTrue(typeAlias is GenericTypeConstructor)
                    assertTrue(inner is FunctionalTypeConstructor)
                    val inner = (inner as FunctionalTypeConstructor)
                    inner.dri equals DRI("kotlin", "Function3")
                    inner.projections counts 4
                    inner.contextParametersCount equals 2
                    val classNamesOfProjections =
                        inner.projections.map { ((it as Invariance<*>).inner as GenericTypeConstructor).dri.classNames }
                    classNamesOfProjections equals listOf("String", "Any", "Throwable", "Unit")
                }
            }
        }
    }

    @Test
    fun `function types with a parameter name should have implicit @ParameterName annotations with mustBeDocumented=false`() {
        inlineModelTest(
            """
            |val nF: (param: Int) -> String = { _ -> "" }"""
        ) {
            with((this / "types" / "nF").cast<DProperty>()) {
                assertTrue(type is FunctionalTypeConstructor)
                val parameterType =
                    ((type as FunctionalTypeConstructor).projections[0] as Invariance<*>).inner as GenericTypeConstructor
                val annotation = parameterType.extra[Annotations]?.directAnnotations?.values?.single()?.single()
                assertEquals(
                    Annotations.Annotation(
                        dri = DRI("kotlin", "ParameterName"),
                        params = mapOf("name" to StringValue("param")),
                        mustBeDocumented = false
                    ),
                    annotation
                )
            }
        }
    }

    @Test
    fun `explicit @ParameterName annotations should have mustBeDocumented=true`() {
        inlineModelTest(
            """
            |val nF:  (@ParameterName(name="param") Int) -> String = { _ -> "" }"""
        ) {
            with((this / "types" / "nF").cast<DProperty>()) {
                assertTrue(type is FunctionalTypeConstructor)
                val parameterType =
                    ((type as FunctionalTypeConstructor).projections[0] as Invariance<*>).inner as GenericTypeConstructor
                val annotation = parameterType.extra[Annotations]?.directAnnotations?.values?.single()?.single()
                assertEquals(
                    Annotations.Annotation(
                        dri = DRI("kotlin", "ParameterName"),
                        params = mapOf("name" to StringValue("param")),
                        mustBeDocumented = true
                    ),
                    annotation
                )
            }
        }
    }

    @Test
    @OnlySymbols("context parameters")
    @OptIn(ExperimentalDokkaApi::class)
    fun `functional type with context parameters and receiver`() {
        inlineModelTest(
            """
            |val nF:  context(String, Double) Boolean.(Int) -> String = { _ -> "" }"""
        ) {
            with((this / "types" / "nF").cast<DProperty>()) {
                assertTrue(type is FunctionalTypeConstructor)
                with(type as FunctionalTypeConstructor) {
                    projections counts 5
                    isExtensionFunction equals true
                    contextParametersCount equals  2
                    val classNamesOfProjections =
                        projections.map { ((it as Invariance<*>).inner as GenericTypeConstructor).dri.classNames }
                    classNamesOfProjections equals listOf("String", "Double", "Boolean", "Int", "String")
                }
            }
        }
    }

    @Test
    fun `type with typealias to nullable type`() {
        inlineModelTest(
            """
            |typealias Nullable = String?
            |typealias NonNullable = String
            |fun nonNullableParameters(nullable: Nullable, nonNullable: NonNullable) {}
            |fun nullableParameters(nullable: Nullable?, nonNullable: NonNullable?) {}"""
        ) {
            with((this / "types" / "Nullable").cast<DTypeAlias>()) {
                val type = type
                type.assertIsInstance<GenericTypeConstructor>()
                type.projections counts 0

                name equals "Nullable"
                assertTrue(underlyingType.values.first() is Nullable)
                underlyingType.values.first().driOrNull equals DRI("kotlin", "String")
            }

            with((this / "types" / "NonNullable").cast<DTypeAlias>()) {
                val type = type
                type.assertIsInstance<GenericTypeConstructor>()
                type.projections counts 0

                name equals "NonNullable"
                assertTrue(underlyingType.values.first() is GenericTypeConstructor)
                underlyingType.values.first().driOrNull equals DRI("kotlin", "String")
            }

            with((this / "types" / "nonNullableParameters").cast<DFunction>()) {
                name equals "nonNullableParameters"
                parameters.size equals 2
                val (nullable, nonNullable) = parameters

                with(nullable.type) {
                    assertIsInstance<TypeAliased>()

                    typeAlias.assertIsInstance<GenericTypeConstructor>()
                    typeAlias.driOrNull equals DRI("types", "Nullable")

                    inner.assertIsInstance<Nullable>()
                    inner.driOrNull equals DRI("kotlin", "String")
                }

                with(nonNullable.type) {
                    assertIsInstance<TypeAliased>()

                    typeAlias.assertIsInstance<GenericTypeConstructor>()
                    typeAlias.driOrNull equals DRI("types", "NonNullable")

                    inner.assertIsInstance<GenericTypeConstructor>()
                    inner.driOrNull equals DRI("kotlin", "String")
                }
            }

            with((this / "types" / "nullableParameters").cast<DFunction>()) {
                name equals "nullableParameters"
                parameters.size equals 2
                val (nullable, nonNullable) = parameters

                with(nullable.type) {
                    assertIsInstance<TypeAliased>()

                    typeAlias.assertIsInstance<Nullable>()
                    typeAlias.driOrNull equals DRI("types", "Nullable")

                    inner.assertIsInstance<Nullable>()
                    inner.driOrNull equals DRI("kotlin", "String")
                }

                // nullability is propagated from the typealias to the underlying type
                with(nonNullable.type) {
                    assertIsInstance<TypeAliased>()

                    typeAlias.assertIsInstance<Nullable>()
                    typeAlias.driOrNull equals DRI("types", "NonNullable")

                    inner.assertIsInstance<Nullable>()
                    inner.driOrNull equals DRI("kotlin", "String")
                }
            }
        }
    }
}
