/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@Suppress("unused")
class ConstraintBuilder(
    private val inferenceContext: InferenceContext,
    private val boundTypeCalculator: BoundTypeCalculator
) : BoundTypeCalculator by boundTypeCalculator {
    private val constraints = mutableListOf<Constraint>()

    fun TypeVariable.isSubtypeOf(supertype: BoundType, priority: ConstraintPriority) {
        asBoundType().isSubtypeOf(supertype, priority)
    }

    fun TypeVariable.isSubtypeOf(supertype: TypeVariable, priority: ConstraintPriority) {
        asBoundType().isSubtypeOf(supertype.asBoundType(), priority)
    }

    fun KtExpression.isSubtypeOf(supertype: KtExpression, priority: ConstraintPriority) {
        boundType().isSubtypeOf(supertype.boundType(), priority)
    }

    fun KtExpression.isSubtypeOf(supertype: TypeVariable, priority: ConstraintPriority) {
        boundType().isSubtypeOf(supertype.asBoundType(), priority)
    }

    fun KtExpression.isSubtypeOf(supertype: BoundType, priority: ConstraintPriority) {
        boundType().isSubtypeOf(supertype, priority)
    }

    fun KtExpression.isTheSameTypeAs(other: State, priority: ConstraintPriority) {
        boundType().label.safeAs<TypeVariableLabel>()?.typeVariable?.let { typeVariable ->
            constraints += EqualsConstraint(typeVariable.constraintBound, other.constraintBound, priority)
        }
    }

    fun TypeVariable.isTheSameTypeAs(other: BoundType, priority: ConstraintPriority) {
        asBoundType().isTheSameTypeAs(other, priority)
    }

    fun TypeVariable.isTheSameTypeAs(
        other: TypeVariable,
        priority: ConstraintPriority,
        ignoreTypeVariables: Set<TypeVariable> = emptySet()
    ) {
        asBoundType().isTheSameTypeAs(other.asBoundType(), priority, ignoreTypeVariables)
    }

    fun KtTypeElement.isTheSameTypeAs(
        other: KtTypeElement,
        priority: ConstraintPriority,
        ignoreTypeVariables: Set<TypeVariable> = emptySet()
    ) {
        inferenceContext.typeElementToTypeVariable[this]
            ?.asBoundType()
            ?.isTheSameTypeAs(
                inferenceContext.typeElementToTypeVariable[other]?.asBoundType() ?: return,
                priority,
                ignoreTypeVariables
            )
    }

    fun TypeVariable.isTheSameTypeAs(
        other: KtTypeElement,
        priority: ConstraintPriority,
        ignoreTypeVariables: Set<TypeVariable> = emptySet()
    ) {
        asBoundType().isTheSameTypeAs(
            inferenceContext.typeElementToTypeVariable[other]?.asBoundType() ?: return,
            priority,
            ignoreTypeVariables
        )
    }

    fun BoundType.isTheSameTypeAs(
        other: KtTypeElement,
        priority: ConstraintPriority,
        ignoreTypeVariables: Set<TypeVariable> = emptySet()
    ) {
        isTheSameTypeAs(
            inferenceContext.typeElementToTypeVariable[other]?.asBoundType() ?: return,
            priority,
            ignoreTypeVariables
        )
    }

    fun BoundType.isTheSameTypeAs(
        other: BoundType,
        priority: ConstraintPriority,
        ignoreTypeVariables: Set<TypeVariable> = emptySet()
    ) {
        (typeParameters zip other.typeParameters).forEach { (left, right) ->
            left.boundType.isTheSameTypeAs(right.boundType, priority, ignoreTypeVariables)
        }

        if (typeVariable !in ignoreTypeVariables && other.typeVariable !in ignoreTypeVariables) {
            constraints += EqualsConstraint(
                constraintBound ?: return,
                other.constraintBound ?: return,
                priority
            )
        }
    }

    fun BoundType.isSubtypeOf(supertype: BoundType, priority: ConstraintPriority) {
        (typeParameters zip supertype.typeParameters).forEach { (left, right) ->
            when (left.variance) {
                Variance.OUT_VARIANCE -> left.boundType.isSubtypeOf(right.boundType, priority)
                Variance.IN_VARIANCE -> right.boundType.isSubtypeOf(left.boundType, priority)
                Variance.INVARIANT -> right.boundType.isTheSameTypeAs(left.boundType, priority)
            }
        }

        constraints += SubtypeConstraint(
            constraintBound ?: return,
            supertype.constraintBound ?: return,
            priority
        )
    }

    fun KtExpression.boundType() = with(boundTypeCalculator) {
        this@boundType.boundType(inferenceContext)
    }

    val collectedConstraints: List<Constraint>
        get() = constraints
}