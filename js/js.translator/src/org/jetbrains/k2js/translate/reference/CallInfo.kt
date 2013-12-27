/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.reference

import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getDeclarationDescriptorForReceiver
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.k2js.translate.utils.AnnotationsUtils
import org.jetbrains.jet.lang.psi.JetSuperExpression
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver


open class BaseCallInfo(
        val context: TranslationContext,
        val resolvedCall: ResolvedCall<out CallableDescriptor>,

        val thisObject: JsExpression?,
        val receiverObject: JsExpression?,
        val nullableReceiverForSafeCall: JsExpression?
) {
    val callableDescriptor = resolvedCall.getResultingDescriptor().getOriginal()
    val functionName = context.getNameForDescriptor(callableDescriptor)

    fun isExtension(): Boolean = receiverObject != null
    fun isMemberCall(): Boolean = thisObject != null
    fun isSafeCall(): Boolean = nullableReceiverForSafeCall != null
    fun isNative(): Boolean = AnnotationsUtils.isNativeObject(callableDescriptor)

    fun isSuperInvocation() : Boolean {
        val thisObject = resolvedCall.getThisObject()
        return thisObject is ExpressionReceiver && ((thisObject as ExpressionReceiver)).getExpression() is JetSuperExpression
    }

    // TODO: toString for debug
}

class BaseFunctionCallInfo(
        callInfo: BaseCallInfo,
        val argumentsInfo: CallArgumentTranslator.ArgumentsInfo
) : BaseCallInfo(callInfo.context, callInfo.resolvedCall, callInfo.thisObject, callInfo.receiverObject, callInfo.nullableReceiverForSafeCall)

private fun TranslationContext.getThisObject(receiverValue: ReceiverValue): JsExpression {
    assert(receiverValue.exists(), "receiverValue must be exist here")
    return getThisObject(getDeclarationDescriptorForReceiver(receiverValue))
}

private fun TranslationContext.mainGetCallInfo(resolvedCall: ResolvedCall<out CallableDescriptor>, receiver1: JsExpression?, receiver2: JsExpression?): BaseCallInfo {
    val receiverKind = resolvedCall.getExplicitReceiverKind()
    fun getNotNullReceiver1(): JsExpression {
        assert(receiver1 != null, "ResolvedCall say, that receiver(1) must be not null")
        return receiver1!!
    }
    fun getNotNullReceiver2(): JsExpression {
        assert(receiver2 != null, "ResolvedCall say, that receiver(2) must be not null")
        return receiver2!!
    }

    fun getThisObject(): JsExpression? {
        val receiverValue = resolvedCall.getThisObject()
        if (!receiverValue.exists()) {
            return null
        }
        return when (receiverKind) {
            THIS_OBJECT -> getNotNullReceiver1()
            BOTH_RECEIVERS -> getNotNullReceiver2()
            else -> this.getThisObject(receiverValue)
        }
    }

    fun getReceiverObject(): JsExpression? {
        val receiverValue = resolvedCall.getReceiverArgument()
        if (!receiverValue.exists()) {
            return null
        }
        return when (receiverKind) {
            RECEIVER_ARGUMENT, BOTH_RECEIVERS -> getNotNullReceiver1()
            else -> this.getThisObject(receiverValue)
        }
    }

    fun getNullableReceiverForSafeCall(): JsExpression? {
        if (!resolvedCall.isSafeCall()) {
            return null
        }
        return when (receiverKind) {
            BOTH_RECEIVERS -> getNotNullReceiver1()
            else -> getNotNullReceiver1()
        }
    }
    return BaseCallInfo(this, resolvedCall, getThisObject(), getReceiverObject(), getNullableReceiverForSafeCall())
}

fun ResolvedCall<out CallableDescriptor>.expectedReceivers(): Boolean {
    return this.getExplicitReceiverKind() != NO_EXPLICIT_RECEIVER
}

fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out CallableDescriptor>, receiver: JsExpression?): BaseCallInfo {
    return mainGetCallInfo(resolvedCall, receiver, null)
}

fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out FunctionDescriptor>, receiver: JsExpression?): BaseFunctionCallInfo {
    return getCallInfo(resolvedCall, receiver, null);
}

// two receiver need only for FunctionCall in VariableAsFunctionResolvedCall
fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out FunctionDescriptor>, receiver1: JsExpression?, receiver2: JsExpression?): BaseFunctionCallInfo {
    val callInfo = mainGetCallInfo(resolvedCall, receiver1, receiver2)
    val argumentsInfo = CallArgumentTranslator.translate(resolvedCall, receiver1, this)
    return BaseFunctionCallInfo(callInfo, argumentsInfo)
}
