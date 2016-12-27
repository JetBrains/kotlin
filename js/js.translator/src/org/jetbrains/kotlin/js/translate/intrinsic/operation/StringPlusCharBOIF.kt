/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF
import org.jetbrains.kotlin.js.translate.operation.OperatorTable
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.types.KotlinType

object StringPlusCharBOIF : BinaryOperationIntrinsicFactory {

    private object StringPlusCharIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, JsAstUtils.charToString(right))
        }
    }

    private object StringPlusAnyIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, TopLevelFIF.TO_STRING.apply(right, listOf(), context))
        }
    }

    private object StringPlusStringIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, right)
        }
    }

    override fun getSupportTokens() = ImmutableSet.of(KtTokens.PLUS)

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        if (KotlinBuiltIns.isString(leftType) && rightType != null) {
            if (KotlinBuiltIns.isCharOrNullableChar(rightType)) {
                return StringPlusCharIntrinsic
            }
            if (KotlinBuiltIns.isStringOrNullableString(rightType)) {
                return StringPlusStringIntrinsic
            }
            if (KotlinBuiltIns.isAnyOrNullableAny(rightType)) {
                return StringPlusAnyIntrinsic
            }
        }
        return null
    }
}
