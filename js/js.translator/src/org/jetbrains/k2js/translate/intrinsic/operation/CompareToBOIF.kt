/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.intrinsic.operation

import com.google.common.collect.ImmutableSet
import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern
import org.jetbrains.k2js.translate.operation.OperatorTable
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken


object CompareToBOIF : BinaryOperationIntrinsicFactory {
    val COMPARE_TO_CHAR = pattern("Int|Short|Byte|Double|Float.compareTo(Char)")
    val CHAR_COMPARE_TO = pattern("Char.compareTo(Int|Short|Byte|Double|Float)")
    val PRIMITIVE_COMPARE_TO = pattern("Int|Short|Byte|Double|Float|Char|String.compareTo")

    private object CompareToIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, right)
        }
    }

    private object CompareToCharIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, JsAstUtils.charToInt(right))
        }
    }

    private object CompareCharToPrimitiveIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, JsAstUtils.charToInt(left), right)
        }
    }

    private object CompareToFunctionIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            val compareTo = JsAstUtils.compareTo(left, right)
            return JsBinaryOperation(operator, compareTo, JsNumberLiteral.ZERO)
        }
    }

    override public fun getSupportTokens(): ImmutableSet<JetToken> = OperatorConventions.COMPARISON_OPERATIONS

    override public fun getIntrinsic(descriptor: FunctionDescriptor): BinaryOperationIntrinsic? {
        if (JsDescriptorUtils.isBuiltin(descriptor))
            when {
                COMPARE_TO_CHAR.apply(descriptor) ->
                    return CompareToCharIntrinsic
                CHAR_COMPARE_TO.apply(descriptor) ->
                    return CompareCharToPrimitiveIntrinsic
                PRIMITIVE_COMPARE_TO.apply(descriptor) ->
                    return CompareToIntrinsic
                else ->
                    return CompareToFunctionIntrinsic
            }
        return null
    }
}
