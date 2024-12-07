/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.*

internal expect fun <T : Any> getKClassForObject(obj: Any): KClass<T>

@ExcludedFromCodegen
internal fun <T : Any> getKClass(): KClass<T> =
    implementedAsIntrinsic

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> getKClassFromExpression(e: T): KClass<T> =
    getKClassForObject(e)

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
internal inline fun <reified T : Any> wasmGetKClass(): KClass<T> =
    KClassImpl(getTypeInfoTypeDataByPtr(wasmTypeId<T>()))

internal fun createKType(classifier: KClassifier, arguments: Array<KTypeProjection>, isMarkedNullable: Boolean): KType =
    KTypeImpl(classifier, arguments.asList(), isMarkedNullable)

internal fun createKTypeParameter(name: String, upperBounds: Array<KType>, variance: String, isReified: Boolean): KTypeParameter {
    val kVariance = when (variance) {
        "in" -> KVariance.IN
        "out" -> KVariance.OUT
        else -> KVariance.INVARIANT
    }
    return KTypeParameterImpl(name, upperBounds.asList(), kVariance, isReified)
}

internal fun getStarKTypeProjection(): KTypeProjection =
    KTypeProjection.STAR

internal fun createCovariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.covariant(type)

internal fun createInvariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.invariant(type)

internal fun createContravariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.contravariant(type)
