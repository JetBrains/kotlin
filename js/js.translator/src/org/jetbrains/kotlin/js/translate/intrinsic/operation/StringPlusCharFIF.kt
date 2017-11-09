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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.FunctionIntrinsicFactory
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.TypeUtils

object StringPlusCharFIF : FunctionIntrinsicFactory {
    private class StringPlusAnyIntrinsic(private val leftTypeNullable: Boolean) : FunctionIntrinsic() {
        override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            val receiver = callInfo.dispatchReceiver ?: callInfo.extensionReceiver!!
            val rightType = context.bindingContext().getType(callInfo.resolvedCall.call.valueArguments[0].getArgumentExpression()!!)
                            ?: callInfo.resolvedCall.resultingDescriptor.valueParameters[0].type
            val rightTypeNullable = TypeUtils.isNullableType(rightType)
            val hasNonNullArg = !leftTypeNullable || !rightTypeNullable
            val rightExpr = when {
                KotlinBuiltIns.isChar(rightType) -> {
                    JsAstUtils.charToString(arguments[0])
                }
                KotlinBuiltIns.isStringOrNullableString(rightType) && hasNonNullArg -> {
                    arguments[0]
                }
                else -> {
                    TopLevelFIF.TO_STRING.apply(arguments[0], listOf(), context)
                }
            }

            return JsBinaryOperation(JsBinaryOperator.ADD, receiver, rightExpr)
        }
    }

    override fun getIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? {
        val fqName = descriptor.fqNameUnsafe.asString()
        if (fqName != "kotlin.String.plus" && fqName != "kotlin.plus") return null

        val leftType = (descriptor.dispatchReceiverParameter ?: descriptor.extensionReceiverParameter ?: return null).type

        return if (KotlinBuiltIns.isStringOrNullableString(leftType)) {
            StringPlusAnyIntrinsic(TypeUtils.isNullableType(leftType))
        }
        else {
            null
        }
    }
}
