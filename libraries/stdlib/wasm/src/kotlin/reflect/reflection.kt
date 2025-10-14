/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.*
import kotlin.internal.UsedFromCompilerGeneratedCode

internal expect fun <T : Any> getKClassForObject(obj: Any): KClass<T>

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <T : Any> getKClass(): KClass<T> =
    implementedAsIntrinsic

@Suppress("UNCHECKED_CAST")
@UsedFromCompilerGeneratedCode
internal fun <T : Any> getKClassFromExpression(e: T): KClass<T> =
    getKClassForObject(e)

@UsedFromCompilerGeneratedCode
internal fun createKType(classifier: KClassifier, arguments: Array<KTypeProjection>, isMarkedNullable: Boolean): KType =
    KTypeImpl(classifier, arguments.asList(), isMarkedNullable)

@UsedFromCompilerGeneratedCode
internal fun createKTypeParameter(name: String, upperBounds: Array<KType>, variance: String, isReified: Boolean, container: String): KTypeParameter {
    val kVariance = when (variance) {
        "in" -> KVariance.IN
        "out" -> KVariance.OUT
        else -> KVariance.INVARIANT
    }
    return KTypeParameterImpl(name, upperBounds.asList(), kVariance, isReified, container)
}

@UsedFromCompilerGeneratedCode
internal fun getStarKTypeProjection(): KTypeProjection =
    KTypeProjection.STAR

@UsedFromCompilerGeneratedCode
internal fun createCovariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.covariant(type)

@UsedFromCompilerGeneratedCode
internal fun createInvariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.invariant(type)

@UsedFromCompilerGeneratedCode
internal fun createContravariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.contravariant(type)
