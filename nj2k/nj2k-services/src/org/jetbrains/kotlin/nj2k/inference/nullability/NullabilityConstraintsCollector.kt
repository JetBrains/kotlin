/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.nullability

import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.nj2k.inference.common.collectors.ConstraintsCollector
import org.jetbrains.kotlin.psi.*

class NullabilityConstraintsCollector : ConstraintsCollector() {
    override fun ConstraintBuilder.collectConstraints(
        element: KtElement,
        boundTypeCalculator: BoundTypeCalculator,
        inferenceContext: InferenceContext,
        resolutionFacade: ResolutionFacade
    ) = with(boundTypeCalculator) {
        when {
            element is KtBinaryExpression &&
                    (element.left?.isNullExpression() == true
                            || element.right?.isNullExpression() == true) -> {
                val notNullOperand =
                    if (element.left?.isNullExpression() == true) element.right
                    else element.left
                notNullOperand?.isTheSameTypeAs(State.UPPER, ConstraintPriority.COMPARE_WITH_NULL)
            }
            element is KtQualifiedExpression -> {
                element.receiverExpression.isTheSameTypeAs(State.LOWER, ConstraintPriority.USE_AS_RECEIVER)
            }

            element is KtForExpression -> {
                element.loopRange?.isTheSameTypeAs(State.LOWER, ConstraintPriority.USE_AS_RECEIVER)
            }

            element is KtWhileExpressionBase -> {
                element.condition?.isTheSameTypeAs(State.LOWER, ConstraintPriority.USE_AS_RECEIVER)
            }

            element is KtIfExpression -> {
                element.condition?.isTheSameTypeAs(State.LOWER, ConstraintPriority.USE_AS_RECEIVER)
            }
            element is KtValueArgument && element.isSpread -> {
                element.getArgumentExpression()?.isTheSameTypeAs(State.LOWER, ConstraintPriority.USE_AS_RECEIVER)
            }
            element is KtBinaryExpression && !KtPsiUtil.isAssignment(element) -> {
                element.left?.isTheSameTypeAs(State.LOWER, ConstraintPriority.USE_AS_RECEIVER)
                element.right?.isTheSameTypeAs(State.LOWER, ConstraintPriority.USE_AS_RECEIVER)
            }
        }
        Unit
    }
}