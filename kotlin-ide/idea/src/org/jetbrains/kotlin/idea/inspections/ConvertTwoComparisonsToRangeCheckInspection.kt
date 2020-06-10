/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.evaluatesTo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType
import org.jetbrains.kotlin.util.OperatorNameConventions

class ConvertTwoComparisonsToRangeCheckInspection :
    AbstractApplicabilityBasedInspection<KtBinaryExpression>(KtBinaryExpression::class.java) {
    override fun inspectionText(element: KtBinaryExpression) = KotlinBundle.message("two.comparisons.should.be.converted.to.a.range.check")

    override val defaultFixText get() = KotlinBundle.message("convert.to.a.range.check")

    override fun isApplicable(element: KtBinaryExpression): Boolean {
        val rangeData = generateRangeExpressionData(element) ?: return false
        val function = element.getStrictParentOfType<KtNamedFunction>()
        if (function != null && function.hasModifier(KtTokens.OPERATOR_KEYWORD) && function.nameAsName == OperatorNameConventions.CONTAINS) {
            val context = element.analyze(BodyResolveMode.PARTIAL)
            val functionDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, function]
            val newExpression = rangeData.createExpression()
            val newContext = newExpression.analyzeAsReplacement(element, context)
            if (newExpression.operationReference.getResolvedCall(newContext)?.resultingDescriptor == functionDescriptor) return false
        }
        return true
    }

    override fun applyTo(element: KtBinaryExpression, project: Project, editor: Editor?) {
        val rangeData = generateRangeExpressionData(element) ?: return
        val replaced = element.replace(rangeData.createExpression())
        (replaced as? KtBinaryExpression)?.right?.let {
            ReplaceRangeToWithUntilInspection.applyFixIfApplicable(it)
        }
    }

    private data class RangeExpressionData(val value: KtExpression, val min: String, val max: String) {
        fun createExpression(): KtBinaryExpression {
            val factory = KtPsiFactory(value)
            return factory.createExpressionByPattern(
                "$0 in $1..$2", value, factory.createExpression(min), factory.createExpression(max)
            ) as KtBinaryExpression
        }
    }

    private fun generateRangeExpressionData(condition: KtBinaryExpression): RangeExpressionData? {
        if (condition.operationToken != KtTokens.ANDAND) return null
        val firstCondition = condition.left as? KtBinaryExpression ?: return null
        val secondCondition = condition.right as? KtBinaryExpression ?: return null
        val firstOpToken = firstCondition.operationToken
        val secondOpToken = secondCondition.operationToken
        val firstLeft = firstCondition.left ?: return null
        val firstRight = firstCondition.right ?: return null
        val secondLeft = secondCondition.left ?: return null
        val secondRight = secondCondition.right ?: return null

        fun IElementType.isStrictComparison() = this == KtTokens.GT || this == KtTokens.LT

        val firstStrict = firstOpToken.isStrictComparison()
        val secondStrict = secondOpToken.isStrictComparison()

        fun IElementType.orderLessAndGreater(left: KtExpression, right: KtExpression): Pair<KtExpression, KtExpression>? = when (this) {
            KtTokens.GTEQ, KtTokens.GT -> right to left
            KtTokens.LTEQ, KtTokens.LT -> left to right
            else -> null
        }

        val (firstLess, firstGreater) = firstOpToken.orderLessAndGreater(firstLeft, firstRight) ?: return null
        val (secondLess, secondGreater) = secondOpToken.orderLessAndGreater(secondLeft, secondRight) ?: return null

        return generateRangeExpressionData(firstLess, firstGreater, firstStrict, secondLess, secondGreater, secondStrict)
    }

    private fun KtExpression.isSimple() = this is KtConstantExpression || this is KtNameReferenceExpression

    private fun generateRangeExpressionData(
        firstLess: KtExpression, firstGreater: KtExpression, firstStrict: Boolean,
        secondLess: KtExpression, secondGreater: KtExpression, secondStrict: Boolean
    ) = when {
        firstGreater !is KtConstantExpression && firstGreater.evaluatesTo(secondLess) ->
            generateRangeExpressionData(
                firstGreater,
                min = firstLess,
                max = secondGreater,
                incrementMinByOne = firstStrict,
                decrementMaxByOne = secondStrict
            )
        firstLess !is KtConstantExpression && firstLess.evaluatesTo(secondGreater) ->
            generateRangeExpressionData(
                firstLess,
                min = secondLess,
                max = firstGreater,
                incrementMinByOne = secondStrict,
                decrementMaxByOne = firstStrict
            )
        else ->
            null
    }

    private fun generateRangeExpressionData(
        value: KtExpression, min: KtExpression, max: KtExpression, incrementMinByOne: Boolean, decrementMaxByOne: Boolean
    ): RangeExpressionData? {
        fun KtExpression.getChangeBy(context: BindingContext, number: Int): String? {
            val type = getType(context) ?: return null
            if (!type.isValidTypeForIncrementDecrementByOne()) return null

            when (this) {
                is KtConstantExpression -> {
                    val constantValue = ConstantExpressionEvaluator.getConstant(this, context)?.getValue(type) ?: return null
                    return when {
                        KotlinBuiltIns.isInt(type) -> (constantValue as Int + number).let {
                            val text = this.text
                            when {
                                text.startsWith("0x") -> "0x${it.toString(16)}"
                                text.startsWith("0b") -> "0b${it.toString(2)}"
                                else -> it.toString()
                            }
                        }
                        KotlinBuiltIns.isLong(type) -> (constantValue as Long + number).let {
                            val text = this.text
                            when {
                                text.startsWith("0x") -> "0x${it.toString(16)}"
                                text.startsWith("0b") -> "0b${it.toString(2)}"
                                else -> it.toString()
                            }
                        }
                        KotlinBuiltIns.isChar(type) -> "'${constantValue as Char + number}'"
                        else -> null
                    }
                }
                else -> return if (number >= 0) "($text + $number)" else "($text - ${-number})"
            }
        }

        // To avoid possible side effects
        if (!min.isSimple() || !max.isSimple()) return null

        val context = value.analyze()
        val valType = value.getType(context)
        val minType = min.getType(context)
        val maxType = max.getType(context)

        if (valType == null || minType == null || maxType == null) return null

        if (!valType.isComparable()) return null

        var minVal = min
        var maxVal = max

        if (minType != valType || maxType != valType) {
            //numbers can be compared to numbers of different types
            if (valType.isPrimitiveNumberType() && minType.isPrimitiveNumberType() && maxType.isPrimitiveNumberType()) {
                //char is comparable to chars only
                if (KotlinBuiltIns.isChar(valType) || KotlinBuiltIns.isChar(minType) || KotlinBuiltIns.isChar(maxType)) return null
                //floating point ranges can't contain integer types and vise versa
                if (valType.isInteger() && (minType.isFloatingPoint() || maxType.isFloatingPoint())) return null

                if (valType.isFloatingPoint()) {
                    if (minType.isInteger())
                        minVal = KtPsiFactory(minVal).createExpression(getDoubleConstant(min, minType, context) ?: return null)
                    if (maxType.isInteger())
                        maxVal = KtPsiFactory(maxVal).createExpression(getDoubleConstant(max, maxType, context) ?: return null)
                }
            } else {
                return null
            }
        }

        if (incrementMinByOne || decrementMaxByOne) {
            if (!valType.isValidTypeForIncrementDecrementByOne()) return null
        }

        val minText = if (incrementMinByOne) minVal.getChangeBy(context, 1) else minVal.text
        val maxText = if (decrementMaxByOne) maxVal.getChangeBy(context, -1) else maxVal.text
        return RangeExpressionData(value, minText ?: return null, maxText ?: return null)
    }

    private fun getDoubleConstant(intExpr: KtExpression, type: KotlinType, context: BindingContext): String? {
        val intConst = ConstantExpressionEvaluator.getConstant(intExpr, context)?.getValue(type) ?: return null
        return (intConst as? Number)?.toDouble()?.toString()
    }

    private fun KotlinType.isComparable() = DescriptorUtils.isSubtypeOfClass(this, this.builtIns.comparable)

    private fun KotlinType.isFloatingPoint(): Boolean {
        return KotlinBuiltIns.isFloat(this) || KotlinBuiltIns.isDouble(this)
    }

    private fun KotlinType.isInteger(): Boolean {
        return KotlinBuiltIns.isInt(this) ||
                KotlinBuiltIns.isLong(this) ||
                KotlinBuiltIns.isShort(this) ||
                KotlinBuiltIns.isByte(this)
    }

    private fun KotlinType?.isValidTypeForIncrementDecrementByOne(): Boolean {
        this ?: return false
        return this.isInteger() || KotlinBuiltIns.isChar(this)
    }
}