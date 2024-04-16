/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// a package is omitted to get declarations directly under the module

// TODO: Remove once JsReflectionAPICallChecker supports more reflection types
@file:Suppress("Unsupported")

import kotlin.reflect.*
import kotlin.reflect.js.internal.*

@JsName("createKType")
internal fun createKType(
    classifier: KClassifier,
    arguments: Array<KTypeProjection>,
    isMarkedNullable: Boolean
) =
    KTypeImpl(classifier, arguments.asList(), isMarkedNullable)

@JsName("createDynamicKType")
internal fun createDynamicKType(): KType = DynamicKType

@JsName("markKTypeNullable")
internal fun markKTypeNullable(kType: KType) = KTypeImpl(kType.classifier!!, kType.arguments, true)

@JsName("createKTypeParameter")
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

    return KTypeParameterImpl(name, upperBounds.asList(), kVariance, isReified)
}

@JsName("getStarKTypeProjection")
internal fun getStarKTypeProjection(): KTypeProjection =
    KTypeProjection.STAR

@JsName("createCovariantKTypeProjection")
internal fun createCovariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.covariant(type)

@JsName("createInvariantKTypeProjection")
internal fun createInvariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.invariant(type)

@JsName("createContravariantKTypeProjection")
internal fun createContravariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.contravariant(type)
