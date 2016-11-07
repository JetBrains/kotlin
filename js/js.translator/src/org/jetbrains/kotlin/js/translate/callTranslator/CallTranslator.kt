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
import com.google.dart.compiler.backend.js.ast.metadata.isSuspend
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.reference.CallArgumentTranslator
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.Call.CallType
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInvokeCallOnVariable
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

object CallTranslator {
    @JvmOverloads
    @JvmStatic
    fun translate(context: TranslationContext,
                  resolvedCall: ResolvedCall<out FunctionDescriptor>,
                  extensionOrDispatchReceiver: JsExpression? = null
    ): JsExpression {
        return translateCall(context, resolvedCall, ExplicitReceivers(extensionOrDispatchReceiver))
    }

    fun translateGet(context: TranslationContext,
                     resolvedCall: ResolvedCall<out VariableDescriptor>,
                     extensionOrDispatchReceiver: JsExpression? = null
    ): JsExpression {
        val variableAccessInfo = VariableAccessInfo(context.getCallInfo(resolvedCall, extensionOrDispatchReceiver), null)
        return variableAccessInfo.translateVariableAccess()
    }

    fun translateSet(context: TranslationContext,
                     resolvedCall: ResolvedCall<out VariableDescriptor>,
                     value: JsExpression,
                     extensionOrDispatchReceiver: JsExpression? = null
    ): JsExpression {
        val variableAccessInfo = VariableAccessInfo(context.getCallInfo(resolvedCall, extensionOrDispatchReceiver), value)
        return variableAccessInfo.translateVariableAccess()
    }

    fun buildCall(context: TranslationContext,
                  functionDescriptor: FunctionDescriptor,
                  args: List<JsExpression>,
                  dispatchReceiver: JsExpression?
    ): JsExpression {
        val argumentsInfo = CallArgumentTranslator.ArgumentsInfo(args, false, null)
        val functionName = context.getNameForDescriptor(functionDescriptor)
        val isNative = AnnotationsUtils.isNativeObject(functionDescriptor)
        val hasSpreadOperator = false
        if (dispatchReceiver != null) {
            return DefaultFunctionCallCase.buildDefaultCallWithDispatchReceiver(argumentsInfo, dispatchReceiver, functionName, isNative,
                                                                                hasSpreadOperator)
        } else {
            return DefaultFunctionCallCase.buildDefaultCallWithoutReceiver(context, argumentsInfo, functionDescriptor, isNative,
                                                                           hasSpreadOperator)
        }
    }
}

private fun ResolvedCall<out CallableDescriptor>.expectedReceivers(): Boolean {
    return this.explicitReceiverKind != NO_EXPLICIT_RECEIVER
}

private fun translateCall(context: TranslationContext,
                          resolvedCall: ResolvedCall<out FunctionDescriptor>,
                          explicitReceivers: ExplicitReceivers
): JsExpression {
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        assert(explicitReceivers.extensionReceiver == null) { "VariableAsFunctionResolvedCall must have one receiver" }
        val variableCall = resolvedCall.variableCall
        if (variableCall.expectedReceivers()) {
            val newReceiver = CallTranslator.translateGet(context, variableCall, explicitReceivers.extensionOrDispatchReceiver)
            return translateFunctionCall(context, resolvedCall.functionCall, ExplicitReceivers(newReceiver))
        } else {
            val dispatchReceiver = CallTranslator.translateGet(context, variableCall, null)
            if (explicitReceivers.extensionOrDispatchReceiver == null)
                return translateFunctionCall(context, resolvedCall.functionCall, ExplicitReceivers(dispatchReceiver))
            else
                return translateFunctionCall(context, resolvedCall.functionCall, ExplicitReceivers(dispatchReceiver, explicitReceivers.extensionOrDispatchReceiver))
        }
    }

    val call = resolvedCall.call
    if (call.callType == CallType.INVOKE && !isInvokeCallOnVariable(call)) {
        val explicitReceiversForInvoke = computeExplicitReceiversForInvoke(context, resolvedCall, explicitReceivers)
        return translateFunctionCall(context, resolvedCall, explicitReceiversForInvoke)
    }

    return translateFunctionCall(context, resolvedCall, explicitReceivers)
}

private fun translateFunctionCall(context: TranslationContext,
                                  resolvedCall: ResolvedCall<out FunctionDescriptor>,
                                  explicitReceivers: ExplicitReceivers
): JsExpression {
    val callExpression = context.getCallInfo(resolvedCall, explicitReceivers).translateFunctionCall()
    if (resolvedCall.resultingDescriptor.isSuspend) {
        (callExpression as JsInvocation).isSuspend = true
    }
    return callExpression
}

fun computeExplicitReceiversForInvoke(
        context: TranslationContext,
        resolvedCall: ResolvedCall<out FunctionDescriptor>,
        explicitReceivers: ExplicitReceivers
): ExplicitReceivers {
    val callElement = resolvedCall.call.callElement
    assert(explicitReceivers.extensionReceiver == null) { "'Invoke' call must have one receiver: $callElement" }

    fun translateReceiverAsExpression(receiver: ReceiverValue?): JsExpression? =
            (receiver as? ExpressionReceiver)?.let { Translation.translateAsExpression(it.expression, context) }

    val dispatchReceiver = resolvedCall.dispatchReceiver
    val extensionReceiver = resolvedCall.extensionReceiver

    if (dispatchReceiver != null && extensionReceiver != null && resolvedCall.explicitReceiverKind == ExplicitReceiverKind.BOTH_RECEIVERS) {
        assert(explicitReceivers.extensionOrDispatchReceiver != null) {
            "No explicit receiver for 'invoke' resolved call with both receivers: $callElement, text: ${callElement.text}" +
            "Dispatch receiver: $dispatchReceiver Extension receiver: $extensionReceiver"
        }
    }
    else {
        assert(explicitReceivers.extensionOrDispatchReceiver == null) {
            "Non trivial explicit receiver ${explicitReceivers.extensionOrDispatchReceiver}\n" +
            "for 'invoke' resolved call: $callElement, text: ${callElement.text}\n" +
            "Dispatch receiver: $dispatchReceiver Extension receiver: $extensionReceiver"
        }
    }

    val dispatchReceiverExpression = translateReceiverAsExpression(dispatchReceiver)
    return when (Pair(dispatchReceiver != null, extensionReceiver != null)) {
        Pair(true, true)  -> ExplicitReceivers(dispatchReceiverExpression, explicitReceivers.extensionOrDispatchReceiver)
        Pair(true, false) -> ExplicitReceivers(dispatchReceiverExpression)
        Pair(false, true) -> ExplicitReceivers(translateReceiverAsExpression(extensionReceiver))
        else -> throw AssertionError("'Invoke' resolved call without receivers: $callElement")
    }
}

abstract class CallCase<in I : CallInfo> {

    protected open fun I.unsupported(message: String = "") : Nothing = throw IllegalStateException("this case unsupported. $this")

    protected open fun I.noReceivers(): JsExpression = unsupported()

    protected open fun I.dispatchReceiver(): JsExpression = unsupported()

    protected open fun I.extensionReceiver(): JsExpression = unsupported()

    protected open fun I.bothReceivers(): JsExpression = unsupported()

    fun translate(callInfo: I): JsExpression {
        val result = if (callInfo.dispatchReceiver == null) {
            if (callInfo.extensionReceiver == null)
                callInfo.noReceivers()
            else
                callInfo.extensionReceiver()
        } else {
            if (callInfo.extensionReceiver == null) {
                callInfo.dispatchReceiver()
            } else
                callInfo.bothReceivers()
        }

        return callInfo.constructSafeCallIsNeeded(result)
    }
}

abstract class FunctionCallCase : CallCase<FunctionCallInfo>()

abstract class VariableAccessCase : CallCase<VariableAccessInfo>()

interface DelegateIntrinsic<in I : CallInfo> {
    fun I.canBeApply(): Boolean = true
    fun I.getDescriptor(): CallableDescriptor
    fun I.getArgs(): List<JsExpression>

    fun intrinsic(callInfo: I): JsExpression? {
        val result =
                if (callInfo.canBeApply()) {
                    callInfo.getIntrinsic()
                } else {
                    null
                }

        if (result != null) {
            return callInfo.constructSafeCallIsNeeded(result)
        } else {
            return null
        }
    }

    private fun I.getIntrinsic(): JsExpression? {
        val descriptor = getDescriptor()

        // Now intrinsic support only FunctionDescriptor. See DelegatePropertyAccessIntrinsic.getDescriptor()
        if (descriptor is FunctionDescriptor) {
            val intrinsic = context.intrinsics().getFunctionIntrinsic(descriptor)
            if (intrinsic.exists()) {
                return intrinsic.apply(this, getArgs(), context)
            }
        }
        return null
    }
}
