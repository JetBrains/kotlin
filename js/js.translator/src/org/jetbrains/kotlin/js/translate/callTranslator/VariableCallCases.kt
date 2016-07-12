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

package org.jetbrains.kotlin.js.translate.callTranslator

import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsInvocation
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.metadata.SideEffectKind
import com.google.dart.compiler.backend.js.ast.metadata.sideEffects
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.js.translate.context.Namer.getCapturedVarAccessor
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.resolve.BindingContextUtils.isVarCapturedInClosure
import java.util.*


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

        val localVariableDescriptor = resolvedCall.resultingDescriptor as? LocalVariableDescriptor
        val accessorDescriptor = if (isGetAccess()) localVariableDescriptor?.getter else localVariableDescriptor?.setter
        if (accessorDescriptor != null) {
            val funRef = JsNameRef(TranslationUtils.getAccessorFunctionName(accessorDescriptor), ref)
            return if (isGetAccess()) {
                JsInvocation(funRef)
            } else {
                JsInvocation(funRef, value!!)
            }
        }

        return constructAccessExpression(ref)
    }

    override fun VariableAccessInfo.dispatchReceiver(): JsExpression {
        val accessor = JsNameRef(variableName, dispatchReceiver!!)
        val descriptor = callableDescriptor
        if (descriptor is PropertyDescriptor && !JsDescriptorUtils.sideEffectsPossibleOnRead(descriptor)) {
            accessor.sideEffects = SideEffectKind.DEPENDS_ON_STATE
        }
        return constructAccessExpression(accessor)
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

        return if (isGetAccess())
            JsInvocation(context.namer().callGetProperty, dispatchReceiver!!, calleeOwner, variableName)
        else
            JsInvocation(context.namer().callSetProperty, dispatchReceiver!!, calleeOwner, variableName, value!!)
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
