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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsNumberLiteral
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
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
    val COMPARE_TO_CHAR = pattern("Int|Short|Byte|Double|Float.compareTo(Char)")
    val CHAR_COMPARE_TO = pattern("Char.compareTo(Int|Short|Byte|Double|Float)")
    val PRIMITIVE_COMPARE_TO = pattern("Int|Short|Byte|Double|Float|Char|String|Boolean.compareTo")

    private object CompareToIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, right)
        }
    }

    private object CompareToCharIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, JsAstUtils.charToInt(right))
        }
    }

    private object CompareCharToPrimitiveIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, JsAstUtils.charToInt(left), right)
        }
    }

    private object CompareToFunctionIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            val compareTo = JsAstUtils.compareTo(left, right)
            return JsBinaryOperation(operator, compareTo, JsNumberLiteral.ZERO)
        }
    }

    override fun getSupportTokens(): Set<KtSingleValueToken> = OperatorConventions.COMPARISON_OPERATIONS

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        if (descriptor.isDynamic()) return CompareToIntrinsic

        if (!KotlinBuiltIns.isBuiltIn(descriptor)) return null

        return when {
            COMPARE_TO_CHAR.test(descriptor) ->
                CompareToCharIntrinsic
            CHAR_COMPARE_TO.test(descriptor) ->
                CompareCharToPrimitiveIntrinsic
            PRIMITIVE_COMPARE_TO.test(descriptor) ->
                CompareToIntrinsic
            else ->
                CompareToFunctionIntrinsic
        }
    }
}
