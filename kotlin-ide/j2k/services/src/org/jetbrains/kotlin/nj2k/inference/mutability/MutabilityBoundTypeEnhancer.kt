/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.mutability

import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isFlexible

class MutabilityBoundTypeEnhancer : BoundTypeEnhancer() {
    override fun enhance(
        expression: KtExpression,
        boundType: BoundType,
        inferenceContext: InferenceContext
    ): BoundType = boundType

    override fun enhanceKotlinType(
        type: KotlinType,
        boundType: BoundType,
        allowLowerEnhancement: Boolean,
        inferenceContext: InferenceContext
    ): BoundType {
        if (type.isFlexible()) return boundType
        val fqName = type.constructor.declarationDescriptor?.fqNameOrNull()
        val enhancement = when {
            fqName in MutabilityStateUpdater.mutableToImmutable && allowLowerEnhancement -> State.LOWER
            fqName in MutabilityStateUpdater.immutableToMutable -> State.UPPER
            else -> null
        }
        return boundType.enhanceWith(enhancement)
    }
}