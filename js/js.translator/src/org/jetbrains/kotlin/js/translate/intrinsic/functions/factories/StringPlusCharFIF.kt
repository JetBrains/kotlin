/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
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
