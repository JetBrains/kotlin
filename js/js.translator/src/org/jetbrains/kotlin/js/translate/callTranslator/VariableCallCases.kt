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

package org.jetbrains.kotlin.js.translate.callTranslator

import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsLiteral
import com.google.dart.compiler.backend.js.ast.JsInvocation
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import java.util.Collections
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContextUtils.isVarCapturedInClosure
import org.jetbrains.kotlin.js.translate.context.Namer.getCapturedVarAccessor


object NativeVariableAccessCase : VariableAccessCase() {
    override fun VariableAccessInfo.extensionReceiver(): JsExpression {
        return constructAccessExpression(JsNameRef(variableName, extensionReceiver!!))
    }

    override fun VariableAccessInfo.dispatchReceiver(): JsExpression {
        return constructAccessExpression(JsNameRef(variableName, dispatchReceiver!!))
    }

    override fun VariableAccessInfo.noReceivers(): JsExpression {
        return constructAccessExpression(variableName.makeRef())
    }
}

object DefaultVariableAccessCase : VariableAccessCase() {
    override fun VariableAccessInfo.noReceivers(): JsExpression {
        val functionRef = context.aliasOrValue(callableDescriptor) {
            context.getQualifiedReference(variableDescriptor)
        }

        val ref =
                if (isVarCapturedInClosure(context.bindingContext(), callableDescriptor)) {
                    getCapturedVarAccessor(functionRef)
                } else {
                    functionRef
                }

        return constructAccessExpression(ref)
    }

    override fun VariableAccessInfo.dispatchReceiver(): JsExpression {
        return constructAccessExpression(JsNameRef(variableName, dispatchReceiver!!))
    }

    override fun VariableAccessInfo.extensionReceiver(): JsExpression {
        val functionRef = context.aliasOrValue(callableDescriptor) {
            JsNameRef(getAccessFunctionName(), context.getQualifierForDescriptor(variableDescriptor))
        }
        return if (isGetAccess()) {
            JsInvocation(functionRef, extensionReceiver!!)
        } else {
            JsInvocation(functionRef, extensionReceiver!!, value!!)
        }
    }

    override fun VariableAccessInfo.bothReceivers(): JsExpression {
        val funRef = JsNameRef(getAccessFunctionName(), dispatchReceiver!!)
        return if (isGetAccess()) {
            JsInvocation(funRef, extensionReceiver!!)
        } else {
            JsInvocation(funRef, extensionReceiver!!, value!!)
        }
    }
}

object DelegatePropertyAccessIntrinsic : DelegateIntrinsic<VariableAccessInfo> {
    override fun VariableAccessInfo.canBeApply(): Boolean {
        if(variableDescriptor is PropertyDescriptor) {
            return isGetAccess() || (variableDescriptor as PropertyDescriptor).isVar
        }
        return false
    }

    override fun VariableAccessInfo.getArgs(): List<JsExpression> {
        return if (isGetAccess())
            Collections.emptyList<JsExpression>()
        else
            Collections.singletonList(value!!)
    }

    override fun VariableAccessInfo.getDescriptor(): CallableDescriptor {
        val propertyDescriptor = variableDescriptor as PropertyDescriptor
        return if (isGetAccess()) {
            propertyDescriptor.getter!!
        } else {
            propertyDescriptor.setter!!
        }
    }
}

object SuperPropertyAccessCase : VariableAccessCase() {
    override fun VariableAccessInfo.dispatchReceiver(): JsExpression {
        val variableName = context.program().getStringLiteral(this.variableName.ident)
        val jsReceiver = this.superCallReceiver
        var propertyOwner = when (jsReceiver) {
            null -> JsLiteral.THIS
            else -> jsReceiver
        }
        return if (isGetAccess())
            JsInvocation(context.namer().callGetProperty, propertyOwner, dispatchReceiver!!, variableName)
        else
            JsInvocation(context.namer().callSetProperty, propertyOwner, dispatchReceiver!!, variableName, value!!)
    }
}

fun VariableAccessInfo.translateVariableAccess(): JsExpression {
    val intrinsic = DelegatePropertyAccessIntrinsic.intrinsic(this)

    return when {
        intrinsic != null ->
            intrinsic
        isSuperInvocation() ->
            SuperPropertyAccessCase.translate(this)
        isNative() ->
            NativeVariableAccessCase.translate(this)
        else ->
            DefaultVariableAccessCase.translate(this)
    }
}
