package org.jetbrains.uast.kotlin.evaluation

import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UastPostfixOperator
import org.jetbrains.uast.evaluation.AbstractEvaluatorExtension
import org.jetbrains.uast.evaluation.UEvaluationInfo
import org.jetbrains.uast.evaluation.UEvaluationState
import org.jetbrains.uast.kotlin.KotlinBinaryOperators
import org.jetbrains.uast.kotlin.KotlinPostfixOperators
import org.jetbrains.uast.values.*
import org.jetbrains.uast.evaluation.to

class KotlinEvaluatorExtension : AbstractEvaluatorExtension(KotlinLanguage.INSTANCE) {

    private data class Range(val from: UValue, val to: UValue) {
        override fun toString() = "$from..$to"
    }

    private class UClosedRangeConstant(override val value: Range, override val source: UBinaryExpression?) : UAbstractConstant() {
        constructor(from: UValue, to: UValue, source: UBinaryExpression): this(Range(from, to), source)
    }

    override fun evaluatePostfix(
            operator: UastPostfixOperator,
            operandValue: UValue,
            state: UEvaluationState
    ): UEvaluationInfo {
        return when (operator) {
            KotlinPostfixOperators.EXCLEXCL -> when (operandValue.toConstant()) {
                UNullConstant -> UValue.UNREACHABLE
                is UConstant -> operandValue
                else -> UUndeterminedValue
            } to state
            else -> return super.evaluatePostfix(operator, operandValue, state)
        }
    }

    private fun UValue.contains(value: UValue): UValue {
        val range = (this as? UClosedRangeConstant)?.value ?: return UUndeterminedValue
        return (value greaterOrEquals range.from) and (value lessOrEquals range.to)
    }

    override fun evaluateBinary(
            binaryExpression: UBinaryExpression,
            leftValue: UValue,
            rightValue: UValue,
            state: UEvaluationState
    ): UEvaluationInfo {
        return when (binaryExpression.operator) {
            KotlinBinaryOperators.IN -> rightValue.contains(leftValue)
            KotlinBinaryOperators.NOT_IN -> !rightValue.contains(leftValue)
            KotlinBinaryOperators.RANGE_TO -> UClosedRangeConstant(leftValue, rightValue, binaryExpression)
            else -> UUndeterminedValue
        } to state
    }
}