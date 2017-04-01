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
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.operation.OperatorTable
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.identity as ID

object LongCompareToBOIF : BinaryOperationIntrinsicFactory {

    val FLOATING_POINT_COMPARE_TO_LONG_PATTERN = pattern("Double|Float.compareTo(Long)")
    val LONG_COMPARE_TO_FLOATING_POINT_PATTERN = pattern("Long.compareTo(Float|Double)")
    val INTEGER_COMPARE_TO_LONG_PATTERN = pattern("Int|Short|Byte.compareTo(Long)")
    val CHAR_COMPARE_TO_LONG_PATTERN = pattern("Char.compareTo(Long)")
    val LONG_COMPARE_TO_INTEGER_PATTERN = pattern("Long.compareTo(Int|Short|Byte)")
    val LONG_COMPARE_TO_CHAR_PATTERN = pattern("Long.compareTo(Char)")
    val LONG_COMPARE_TO_LONG_PATTERN = pattern("Long.compareTo(Long)")

    private object FLOATING_POINT_COMPARE_TO_LONG : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, invokeMethod(right, Namer.LONG_TO_NUMBER))
        }
    }

    private object LONG_COMPARE_TO_FLOATING_POINT : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, invokeMethod(left, Namer.LONG_TO_NUMBER), right)
        }
    }

    private class CompareToBinaryIntrinsic(val toLeft: (JsExpression) -> JsExpression, val toRight: (JsExpression) -> JsExpression) : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            val compareInvocation = compareForObject(toLeft(left), toRight(right))
            return JsBinaryOperation(operator, compareInvocation, JsNumberLiteral.ZERO)
        }
    }

    private val INTEGER_COMPARE_TO_LONG = CompareToBinaryIntrinsic(::longFromInt, ID())
    private val CHAR_COMPARE_TO_LONG  = CompareToBinaryIntrinsic( { longFromInt(charToInt(it)) }, ID())
    private val LONG_COMPARE_TO_INTEGER  = CompareToBinaryIntrinsic(ID(), ::longFromInt)
    private val LONG_COMPARE_TO_CHAR  = CompareToBinaryIntrinsic( ID(), { longFromInt(charToInt(it)) })
    private val LONG_COMPARE_TO_LONG  = CompareToBinaryIntrinsic( ID(), ID() )

    override fun getSupportTokens() = OperatorConventions.COMPARISON_OPERATIONS

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        if (KotlinBuiltIns.isBuiltIn(descriptor)) {
            return when {
                FLOATING_POINT_COMPARE_TO_LONG_PATTERN.test(descriptor) -> FLOATING_POINT_COMPARE_TO_LONG
                LONG_COMPARE_TO_FLOATING_POINT_PATTERN.test(descriptor) -> LONG_COMPARE_TO_FLOATING_POINT
                INTEGER_COMPARE_TO_LONG_PATTERN.test(descriptor) -> INTEGER_COMPARE_TO_LONG
                CHAR_COMPARE_TO_LONG_PATTERN.test(descriptor) -> CHAR_COMPARE_TO_LONG
                LONG_COMPARE_TO_INTEGER_PATTERN.test(descriptor) -> LONG_COMPARE_TO_INTEGER
                LONG_COMPARE_TO_CHAR_PATTERN.test(descriptor) -> LONG_COMPARE_TO_CHAR
                LONG_COMPARE_TO_LONG_PATTERN.test(descriptor) -> LONG_COMPARE_TO_LONG
                else -> null
            }
        }
        return null
    }
}
