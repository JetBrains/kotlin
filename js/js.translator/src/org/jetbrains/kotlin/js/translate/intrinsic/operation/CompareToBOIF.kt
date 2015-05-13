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

import com.google.common.collect.ImmutableSet
import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.operation.OperatorTable
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic

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

    override public fun getSupportTokens() = OperatorConventions.COMPARISON_OPERATIONS

    override public fun getIntrinsic(descriptor: FunctionDescriptor): BinaryOperationIntrinsic? {
        if (descriptor.isDynamic()) return CompareToIntrinsic

        if (!JsDescriptorUtils.isBuiltin(descriptor)) return null

        return when {
            COMPARE_TO_CHAR.apply(descriptor) ->
                CompareToCharIntrinsic
            CHAR_COMPARE_TO.apply(descriptor) ->
                CompareCharToPrimitiveIntrinsic
            PRIMITIVE_COMPARE_TO.apply(descriptor) ->
                CompareToIntrinsic
            else ->
                CompareToFunctionIntrinsic
        }
    }
}
