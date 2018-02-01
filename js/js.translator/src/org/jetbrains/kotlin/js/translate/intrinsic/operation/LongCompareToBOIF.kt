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
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral
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

    private val intrinsics = arrayOf(
            pattern("Double|Float.compareTo(Long)") to PrimitiveCompare({ it }, { invokeMethod(it, Namer.LONG_TO_NUMBER) }),
            pattern("Long.compareTo(Float|Double)") to PrimitiveCompare({ invokeMethod(it, Namer.LONG_TO_NUMBER) }, { it }),
            pattern("Int|Short|Byte.compareTo(Long)") to Compare(::longFromInt, { it }),
            pattern("Char.compareTo(Long)") to Compare({ longFromInt(charToInt(it)) }, { it }),
            pattern("Long.compareTo(Int|Short|Byte)") to Compare({ it }, ::longFromInt),
            pattern("Long.compareTo(Char)") to Compare({ it }, { longFromInt(charToInt(it)) }),
            pattern("Long.compareTo(Long)") to Compare({ it }, { it })
    )

    private class PrimitiveCompare(val toLeft: (JsExpression) -> JsExpression, val toRight: (JsExpression) -> JsExpression): AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, toLeft(left), toRight(right))
        }
    }


    private class Compare(val toLeft: (JsExpression) -> JsExpression, val toRight: (JsExpression) -> JsExpression) : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, compareForObject(toLeft(left), toRight(right)), JsIntLiteral(0))
        }
    }

    override fun getSupportTokens() = OperatorConventions.COMPARISON_OPERATIONS

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        if (KotlinBuiltIns.isBuiltIn(descriptor)) {
            intrinsics.forEach { (predicate, intrinsic) ->
                if (predicate.test(descriptor)) return intrinsic
            }
        }
        return null
    }
}
