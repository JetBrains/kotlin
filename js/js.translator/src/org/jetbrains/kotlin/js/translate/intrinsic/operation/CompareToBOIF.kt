/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.operation.OperatorTable
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.OperatorConventions

object CompareToBOIF : BinaryOperationIntrinsicFactory {
    private object CompareToIntrinsic : BinaryOperationIntrinsic {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, right)
        }
    }

    private object CompareToCharIntrinsic : BinaryOperationIntrinsic {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, JsAstUtils.charToInt(right))
        }
    }

    private object CompareCharToPrimitiveIntrinsic : BinaryOperationIntrinsic {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, JsAstUtils.charToInt(left), right)
        }
    }

    private object CompareToFunctionIntrinsic : BinaryOperationIntrinsic{
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            val compareTo = JsAstUtils.compareTo(left, right)
            return JsBinaryOperation(operator, compareTo, JsIntLiteral(0))
        }
    }

    override fun getSupportTokens(): Set<KtSingleValueToken> = OperatorConventions.COMPARISON_OPERATIONS

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        if (descriptor.isDynamic()) return CompareToIntrinsic

        if (leftType == null || rightType == null) return null

        if (!KotlinBuiltIns.isBuiltIn(descriptor)) return null

        // Types may be nullable if properIeeeComparisons are switched off, e.g. fun foo(a: Double?) = a != null && a < 0.0
        return when {
            KotlinBuiltIns.isCharOrNullableChar(rightType) -> CompareToCharIntrinsic
            KotlinBuiltIns.isCharOrNullableChar(leftType) -> CompareCharToPrimitiveIntrinsic
            KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(leftType) &&
                    KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(rightType) -> CompareToIntrinsic
            else -> CompareToFunctionIntrinsic
        }
    }
}
