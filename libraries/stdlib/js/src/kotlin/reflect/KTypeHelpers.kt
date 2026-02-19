/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.reflect.js.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.reflect.*

@UsedFromCompilerGeneratedCode
internal fun createKType(
    classifier: KClassifier,
    arguments: Array<KTypeProjection>,
    isMarkedNullable: Boolean
) =
    KTypeImpl(classifier, arguments.asList(), isMarkedNullable)

@UsedFromCompilerGeneratedCode
internal fun createDynamicKType(): KType = DynamicKType

@UsedFromCompilerGeneratedCode
internal fun createKTypeParameter(
    name: String,
    upperBounds: Array<KType>,
    variance: String,
    isReified: Boolean,
    container: String,
): KTypeParameter {
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
