/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class ConstraintBuilder(val boundTypeStorage: BoundTypeStorage) {
    private val constraints = mutableListOf<Constraint>()

    internal fun getConstraints(): List<Constraint> = constraints

    inline fun KtExpression.addSubtypeNullabilityConstraint(typeVariable: TypeVariable, cameFrom: ConstraintCameFrom) {
        addSubtypeNullabilityConstraint(TypeVariableBoundType(typeVariable), cameFrom)
    }

    inline fun KtExpression.addEqualsNullabilityConstraint(nullability: Nullability, cameFrom: ConstraintCameFrom) {
        val boundType = boundTypeStorage.boundTypeFor(this).safeAs<TypeVariableBoundType>()
            ?.withForcedNullability(getForcedNullability())
            ?: return
        constraints += EqualConstraint(boundType.bound, LiteralBound(nullability), cameFrom)
    }

    inline fun TypeVariable.addEqualsNullabilityConstraint(other: BoundType, cameFrom: ConstraintCameFrom) {
        TypeVariableBoundType(this).isTheSameType(other, cameFrom)
    }

    inline fun KtExpression.addSubtypeNullabilityConstraint(upperTypeExpression: KtExpression, cameFrom: ConstraintCameFrom) {
        boundTypeStorage.boundTypeFor(this).subtypeOf(boundTypeStorage.boundTypeFor(upperTypeExpression), cameFrom)
    }

    inline fun KtExpression.addSubtypeNullabilityConstraint(upperBoundType: BoundType, cameFrom: ConstraintCameFrom) {
        boundTypeStorage.boundTypeFor(this)
            .withForcedNullability(getForcedNullability())
            .subtypeOf(upperBoundType, cameFrom)
    }

    inline fun TypeVariable.subtypeOf(other: BoundType, cameFrom: ConstraintCameFrom) {
        TypeVariableBoundType(this).subtypeOf(other, cameFrom)
    }

    fun BoundType.subtypeOf(other: BoundType, cameFrom: ConstraintCameFrom) {
        (typeParameters zip other.typeParameters).forEach { (left, right) ->
            val variance = left.variance
            when (variance) {
                Variance.OUT_VARIANCE -> left.boundType.subtypeOf(right.boundType, cameFrom)
                Variance.IN_VARIANCE -> right.boundType.subtypeOf(left.boundType, cameFrom)
                Variance.INVARIANT -> right.boundType.isTheSameType(left.boundType, cameFrom)
            }
        }

        constraints += SubtypeConstraint(bound, other.bound, cameFrom)
    }

    fun BoundType.isTheSameType(other: BoundType, cameFrom: ConstraintCameFrom) {
        (typeParameters zip other.typeParameters).forEach { (left, right) ->
            left.boundType.isTheSameType(right.boundType, cameFrom)
        }
        constraints += EqualConstraint(bound, other.bound, cameFrom)
    }
}