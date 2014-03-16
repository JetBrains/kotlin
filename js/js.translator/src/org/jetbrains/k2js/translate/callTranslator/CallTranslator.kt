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

package org.jetbrains.k2js.translate.callTranslator

import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.k2js.translate.context.TranslationContext
import java.util.ArrayList
import org.jetbrains.k2js.facade.exceptions.UnsupportedFeatureException
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind.*
import com.google.dart.compiler.backend.js.ast.JsNameRef
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.k2js.translate.utils.ErrorReportingUtils
import org.jetbrains.k2js.translate.utils.AnnotationsUtils
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils
import org.jetbrains.k2js.translate.reference.CallArgumentTranslator

object CallTranslator {
    fun translate(context: TranslationContext,
                  resolvedCall: ResolvedCall<out FunctionDescriptor>,
                  receiverOrThisObject: JsExpression? = null
    ): JsExpression {
        return translateCall(context, resolvedCall, ExplicitReceivers(receiverOrThisObject))
    }

    fun translateGet(context: TranslationContext,
                     resolvedCall: ResolvedCall<out VariableDescriptor>,
                     receiverOrThisObject: JsExpression? = null
    ): JsExpression {
        val variableAccessInfo = VariableAccessInfo(context.getCallInfo(resolvedCall, receiverOrThisObject), null);
        return variableAccessInfo.translateVariableAccess()
    }

    fun translateSet(context: TranslationContext,
                     resolvedCall: ResolvedCall<out VariableDescriptor>,
                     value: JsExpression,
                     receiverOrThisObject: JsExpression? = null
    ): JsExpression {
        val variableAccessInfo = VariableAccessInfo(context.getCallInfo(resolvedCall, receiverOrThisObject), value);
        return variableAccessInfo.translateVariableAccess()
    }

    fun buildCall(context: TranslationContext,
                  functionDescriptor: FunctionDescriptor,
                  args: List<JsExpression>,
                  thisObject: JsExpression?
    ): JsExpression {
        val argumentsInfo = CallArgumentTranslator.ArgumentsInfo(args, false, null);
        val functionName = context.getNameForDescriptor(functionDescriptor)
        val isNative = AnnotationsUtils.isNativeObject(functionDescriptor)
        val hasSpreadOperator = false
        if (thisObject != null) {
            return DefaultFunctionCallCase.buildDefaultCallWithThisObject(argumentsInfo, thisObject, functionName, isNative, hasSpreadOperator)
        } else {
            return DefaultFunctionCallCase.buildDefaultCallWithoutReceiver(context, argumentsInfo, functionDescriptor, functionName, isNative, hasSpreadOperator)
        }
    }
}

private fun ResolvedCall<out CallableDescriptor>.expectedReceivers(): Boolean {
    return this.getExplicitReceiverKind() != NO_EXPLICIT_RECEIVER
}

private fun translateCall(context: TranslationContext,
                          resolvedCall: ResolvedCall<out FunctionDescriptor>,
                          explicitReceivers: ExplicitReceivers
): JsExpression {
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        assert(explicitReceivers.receiverObject == null, "VariableAsFunctionResolvedCall must have one receiver")
        val variableCall = resolvedCall.getVariableCall()
        if (variableCall.expectedReceivers()) {
            val newReceiver = CallTranslator.translateGet(context, variableCall, explicitReceivers.receiverOrThisObject)
            return translateCall(context, resolvedCall.getFunctionCall(), ExplicitReceivers(newReceiver))
        } else {
            val thisObject = CallTranslator.translateGet(context, variableCall, null)
            if (explicitReceivers.receiverOrThisObject == null)
                return translateCall(context, resolvedCall.getFunctionCall(), ExplicitReceivers(thisObject))
            else
                return translateCall(context, resolvedCall.getFunctionCall(), ExplicitReceivers(thisObject, explicitReceivers.receiverOrThisObject))
        }
    }

    val functionCallInfo = context.getCallInfo(resolvedCall, explicitReceivers)
    return functionCallInfo.translateFunctionCall()
}


trait CallCase<I : CallInfo> {

    protected fun I.unsupported(message: String = "") : Nothing = throw UnsupportedOperationException("this case unsopported. $this")

    protected fun I.noReceivers(): JsExpression = unsupported()

    protected fun I.thisObject(): JsExpression = unsupported()

    protected fun I.receiverArgument(): JsExpression = unsupported()

    protected fun I.bothReceivers(): JsExpression = unsupported()

    final fun translate(callInfo: I): JsExpression {
        val result = if (callInfo.thisObject == null) {
            if (callInfo.receiverObject == null)
                callInfo.noReceivers()
            else
                callInfo.receiverArgument()
        } else {
            if (callInfo.receiverObject == null) {
                callInfo.thisObject()
            } else
                callInfo.bothReceivers()
        }

        return callInfo.constructSafeCallIsNeeded(result)
    }
}

trait FunctionCallCase : CallCase<FunctionCallInfo>

trait VariableAccessCase : CallCase<VariableAccessInfo>

trait DelegateIntrinsic<I : CallInfo> {
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
        val descriptor = getDescriptor();

        // Now intrinsic support only FunctionDescriptor. See DelegatePropertyAccessIntrinsic.getDescriptor()
        if (descriptor is FunctionDescriptor) {
            val intrinsic = context.intrinsics().getFunctionIntrinsics().getIntrinsic(descriptor)
            if (intrinsic.exists()) {
                return intrinsic.apply(this, getArgs(), context)
            }
        }
        return null
    }
}
