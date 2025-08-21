/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// TODO: file should be removed after the next bootstrap
import kotlin.reflect.*
import kotlin.reflect.js.internal.*

internal fun createKType(
    classifier: KClassifier,
    arguments: Array<KTypeProjection>,
    isMarkedNullable: Boolean
) =
    KTypeImpl(classifier, arguments.asList(), isMarkedNullable)

internal fun createDynamicKType(): KType = DynamicKType

internal fun markKTypeNullable(kType: KType) = KTypeImpl(kType.classifier!!, kType.arguments, true)

internal fun createKTypeParameter(
    name: String,
    upperBounds: Array<KType>,
    variance: String,
    isReified: Boolean,
): KTypeParameter {
    val kVariance = when (variance) {
        "in" -> KVariance.IN
        "out" -> KVariance.OUT
        else -> KVariance.INVARIANT
    }

    return KTypeParameterImpl(name, upperBounds.asList(), kVariance, isReified, "")
}

internal fun getStarKTypeProjection(): KTypeProjection =
    KTypeProjection.STAR

internal fun createCovariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.covariant(type)

internal fun createInvariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.invariant(type)

internal fun createContravariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.contravariant(type)
