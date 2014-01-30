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


val functionCallCases: CallCaseDispatcher<FunctionCallCase, FunctionCallInfo> = createFunctionCases()
val variableAccessCases: CallCaseDispatcher<VariableAccessCase, VariableAccessInfo> = createVariableAccessCases()

fun TranslationContext.buildCall(resolvedCall: ResolvedCall<out FunctionDescriptor>, receiverOrThisObject: JsExpression? = null): JsExpression {
    return buildCall(resolvedCall, ExplicitReceivers(receiverOrThisObject))
}

fun TranslationContext.buildGet(resolvedCall: ResolvedCall<out VariableDescriptor>, receiverOrThisObject: JsExpression? = null): JsExpression {
    val variableAccessInfo = VariableAccessInfo(getCallInfo(resolvedCall, receiverOrThisObject), null);
    return variableAccessCases.translate(variableAccessInfo)
}

fun TranslationContext.buildSet(resolvedCall: ResolvedCall<out VariableDescriptor>, value: JsExpression, receiverOrThisObject: JsExpression? = null): JsExpression {
    val variableAccessInfo = VariableAccessInfo(getCallInfo(resolvedCall, receiverOrThisObject), value);
    return variableAccessCases.translate(variableAccessInfo)
}

fun TranslationContext.buildFakeCall(functionDescriptor: FunctionDescriptor, args: List<JsExpression>, thisObject: JsExpression?): JsExpression {
    val argumentsInfo = CallArgumentTranslator.ArgumentsInfo(args, false, null);
    val functionName = getNameForDescriptor(functionDescriptor)
    val isNative = AnnotationsUtils.isNativeObject(functionDescriptor)
    val hasSpreadOperator = false
    if (thisObject != null) {
        return DefaultCallCase.buildDefaultCallWithThisObject(argumentsInfo, thisObject, functionName, isNative, hasSpreadOperator)
    } else {
        return DefaultCallCase.buildDefaultCallWithoutReceiver(this, argumentsInfo, functionDescriptor, functionName, isNative, hasSpreadOperator)
    }
}

fun ResolvedCall<out CallableDescriptor>.expectedReceivers(): Boolean {
    return this.getExplicitReceiverKind() != NO_EXPLICIT_RECEIVER
}

private fun TranslationContext.buildCall(resolvedCall: ResolvedCall<out FunctionDescriptor>, explicitReceivers: ExplicitReceivers): JsExpression {
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        assert(explicitReceivers.receiverObject == null, "VariableAsFunctionResolvedCall must have one receiver") // TODO: add debug info
        val variableCall = resolvedCall.getVariableCall()
        if (variableCall.expectedReceivers()) {
            val newReceiver = buildGet(variableCall, explicitReceivers.receiverOrThisObject)
            return buildCall(resolvedCall.getFunctionCall(), ExplicitReceivers(newReceiver))
        } else {
            val thisObject = buildGet(variableCall, null)
            if (explicitReceivers.receiverOrThisObject == null)
                return buildCall(resolvedCall.getFunctionCall(), ExplicitReceivers(thisObject))
            else
                return buildCall(resolvedCall.getFunctionCall(), ExplicitReceivers(thisObject, explicitReceivers.receiverOrThisObject))
        }
    }

    val functionCallInfo = getCallInfo(resolvedCall, explicitReceivers)
    return functionCallCases.translate(functionCallInfo)
}


trait CallCase<I : CallInfo> {
    val callInfo: I

    protected fun unsupported(message: String = "") : Exception {
        val stackTrace = Thread.currentThread().getStackTrace()
        val methodName = stackTrace.get(stackTrace.lastIndex - 1).getMethodName()
        val caseName = javaClass.getName()
        return UnsupportedOperationException("this case unsopported: $message [$methodName; $caseName; $callInfo]")
    }

    protected fun I.noReceivers(): JsExpression {
        throw unsupported()
    }

    protected fun I.thisObject(): JsExpression {
        throw unsupported()
    }

    protected fun I.receiverArgument(): JsExpression {
        throw unsupported()
    }

    protected fun I.bothReceivers(): JsExpression {
        throw unsupported()
    }

    final fun translate(): JsExpression {
        val result = if (callInfo.thisObject == null) {
            if (callInfo.receiverObject == null)
                callInfo.noReceivers()
            else
                callInfo.receiverArgument()
        } else {
            if (callInfo.receiverObject == null)
                callInfo.thisObject()
            else
                callInfo.bothReceivers()
        }

        return callInfo.constructSafeCallIsNeeded(result)
    }
}

open class FunctionCallCase(override val callInfo: FunctionCallInfo) : CallCase<FunctionCallInfo>

open class VariableAccessCase(override val callInfo: VariableAccessInfo) : CallCase<VariableAccessInfo> {
    protected fun VariableAccessInfo.getAccessFunctionName(): String {
        return Namer.getNameForAccessor(variableName.getIdent()!!, isGetAccess(), false)
    }

    protected fun VariableAccessInfo.constructAccessExpression(ref: JsNameRef): JsExpression {
        if (isGetAccess()) {
            return ref
        } else {
            return JsAstUtils.assignment(ref, value!!)
        }
    }
}

class CallCaseDispatcher<C : CallCase<I>, I : CallInfo> {
    private val cases: MutableList<(I) -> JsExpression?> = ArrayList()

    fun addCase(canBeApplyCase: (I) -> JsExpression?) {
        cases.add(canBeApplyCase)
    }

    fun addCase(caseConstructor: (I) -> C, canApply: (I) -> Boolean) {
        cases.add({
            if (canApply(it))
                caseConstructor(it).translate()
            else
                null
        })
    }

    fun translate(callInfo: I): JsExpression {
        for (case in cases) {
            val result = case(callInfo)
            if (result != null)
                return result
        }
        throw UnsupportedOperationException("This case of call unsopported. CallInfo: $callInfo")
    }
}

trait DelegateIntrinsic<I : CallInfo> : CallCase<I> {
    fun I.canBeApply(): Boolean = true
    fun I.getReceiver(): JsExpression? {
        return when (resolvedCall.getExplicitReceiverKind()) {
            THIS_OBJECT -> thisObject
            RECEIVER_ARGUMENT, BOTH_RECEIVERS -> receiverObject
            else -> null
        }
    }

    fun I.getDescriptor(): CallableDescriptor
    fun I.getArgs(): List<JsExpression>

    fun I.intrinsic(): JsExpression? {
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

    fun intrinsic(): JsExpression? {
        return if (callInfo.canBeApply())
            callInfo.intrinsic()
        else
            null
    }
}
