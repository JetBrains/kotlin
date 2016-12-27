/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.operation

import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsLiteral
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.patterns.NamePredicate
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isDynamic
import java.util.*

object EqualsBOIF : BinaryOperationIntrinsicFactory {
    private object EqualsIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val isNegated = expression.isNegated()
            if (right == JsLiteral.NULL || left == JsLiteral.NULL) {
                return TranslationUtils.nullCheck(if (right == JsLiteral.NULL) left else right, isNegated)
            }
            else if (canUseSimpleEquals(expression, context)) {
                return JsBinaryOperation(if (isNegated) JsBinaryOperator.REF_NEQ else JsBinaryOperator.REF_EQ, left, right)
            }

            val resolvedCall = expression.getResolvedCall(context.bindingContext())
            val appliedToDynamic =
                    resolvedCall != null &&
                    with(resolvedCall.dispatchReceiver) {
                        if (this != null) type.isDynamic() else false
                    }

            if (appliedToDynamic) {
                return JsBinaryOperation(if (isNegated) JsBinaryOperator.NEQ else JsBinaryOperator.EQ, left, right)
            }

            val result = TopLevelFIF.KOTLIN_EQUALS.apply(left, Arrays.asList<JsExpression>(right), context)
            return if (isNegated) JsAstUtils.not(result) else result
        }

        private fun canUseSimpleEquals(expression: KtBinaryExpression, context: TranslationContext): Boolean {
            val left = expression.left
            assert(left != null) { "No left-hand side: " + expression.text }
            val typeName = JsDescriptorUtils.getNameIfStandardType(left!!, context)
            return typeName != null && NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS.apply(typeName)
        }
    }

    object EnumEqualsIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsBinaryOperation {
            val operator = if (expression.isNegated()) JsBinaryOperator.REF_NEQ else JsBinaryOperator.REF_EQ
            return JsBinaryOperation(operator, left, right)
        }
    }

    override fun getSupportTokens() = OperatorConventions.EQUALS_OPERATIONS!!

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        val result = when {
            isEnumIntrinsicApplicable(descriptor, leftType, rightType) -> EnumEqualsIntrinsic

            KotlinBuiltIns.isBuiltIn(descriptor) ||
            TopLevelFIF.EQUALS_IN_ANY.apply(descriptor) -> EqualsIntrinsic

            else -> null
        }

        if (result != null && leftType != rightType) {
            val leftChar = (leftType != null && KotlinBuiltIns.isCharOrNullableChar(leftType))
            val rightChar = (rightType != null && KotlinBuiltIns.isCharOrNullableChar(rightType))

            if (leftChar xor rightChar) {
                return object : AbstractBinaryOperationIntrinsic() {
                    override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
                        val maybeBoxedLeft = if (leftChar) JsAstUtils.charToBoxedChar(left) else left
                        val maybeBoxedRight = if (rightChar) JsAstUtils.charToBoxedChar(right) else right
                        return result.apply(expression, maybeBoxedLeft, maybeBoxedRight, context)
                    }
                }
            }
        }

        return result
    }

    private fun isEnumIntrinsicApplicable(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): Boolean {
        return DescriptorUtils.isEnumClass(descriptor.containingDeclaration) && leftType != null && rightType != null &&
               !TypeUtils.isNullableType(leftType) && !TypeUtils.isNullableType(rightType)
    }

    private fun KtBinaryExpression.isNegated() = getOperationToken(this) == KtTokens.EXCLEQ
}
