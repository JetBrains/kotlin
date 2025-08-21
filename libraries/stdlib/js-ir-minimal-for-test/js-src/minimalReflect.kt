/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.js.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

internal class KTypeImpl(
    override val classifier: KClassifier,
    override val arguments: List<KTypeProjection>,
    override val isMarkedNullable: Boolean
) : KType

internal object DynamicKType : KType {
    override val classifier: KClassifier? = null
    override val isMarkedNullable: Boolean = false
    override fun toString(): String = "dynamic"
    override lateinit var arguments: List<KTypeProjection>
}

@UsedFromCompilerGeneratedCode
internal fun createKType(
    classifier: KClassifier,
    arguments: Array<KTypeProjection>,
    isMarkedNullable: Boolean
) =
    KTypeImpl(classifier, arguments.unsafeCast<List<KTypeProjection>>(), isMarkedNullable)

@UsedFromCompilerGeneratedCode
internal fun createDynamicKType(): KType = DynamicKType

internal fun markKTypeNullable(kType: KType) = KTypeImpl(kType.classifier!!, kType.arguments, true)

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

    return KTypeParameterImpl(name, upperBounds.unsafeCast<List<KType>>(), kVariance, isReified, container)
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
