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

package org.jetbrains.kotlin.js.translate.callTranslator

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.reference.CallArgumentTranslator
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.Call.CallType
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInvokeCallOnVariable
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable

object CallTranslator {
    @JvmOverloads
    @JvmStatic
    fun translate(context: TranslationContext,
                  resolvedCall: ResolvedCall<out FunctionDescriptor>,
                  extensionOrDispatchReceiver: JsExpression? = null
    ): JsExpression {
        return translateCall(context, resolvedCall, ExplicitReceivers(extensionOrDispatchReceiver)).source(resolvedCall.call.callElement)
    }

    fun translateGet(context: TranslationContext,
                     resolvedCall: ResolvedCall<out VariableDescriptor>,
                     extensionOrDispatchReceiver: JsExpression? = null
    ): JsExpression {
        val variableAccessInfo = VariableAccessInfo(context.getCallInfo(resolvedCall, extensionOrDispatchReceiver), null)
        val result = variableAccessInfo.constructSafeCallIfNeeded(variableAccessInfo.translateVariableAccess())
                .source(resolvedCall.call.callElement)
        result.type = TranslationUtils.getReturnTypeForCoercion(resolvedCall.resultingDescriptor.original)
        return result
    }

    fun translateSet(context: TranslationContext,
                     resolvedCall: ResolvedCall<out VariableDescriptor>,
                     value: JsExpression,
                     extensionOrDispatchReceiver: JsExpression? = null
    ): JsExpression {
        val type = TranslationUtils.getReturnTypeForCoercion(resolvedCall.resultingDescriptor)
        val coerceValue = TranslationUtils.coerce(context, value, type)
        val variableAccessInfo = VariableAccessInfo(context.getCallInfo(resolvedCall, extensionOrDispatchReceiver), coerceValue)
        val result = variableAccessInfo.constructSafeCallIfNeeded(variableAccessInfo.translateVariableAccess())
                .source(resolvedCall.call.callElement)
        result.type = context.currentModule.builtIns.unitType
        return result
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
        return if (dispatchReceiver != null) {
            DefaultFunctionCallCase.buildDefaultCallWithDispatchReceiver(argumentsInfo, dispatchReceiver, functionName, isNative,
                                                                         hasSpreadOperator)
        } else {
            DefaultFunctionCallCase.buildDefaultCallWithoutReceiver(context, argumentsInfo, functionDescriptor, isNative, hasSpreadOperator)
        }
    }
}

private fun ResolvedCall<out CallableDescriptor>.expectedReceivers(): Boolean {
    return this.explicitReceiverKind != NO_EXPLICIT_RECEIVER
}

private fun translateCall(
        context: TranslationContext,
        resolvedCall: ResolvedCall<out FunctionDescriptor>,
        explicitReceivers: ExplicitReceivers
): JsExpression {
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        assert(explicitReceivers.extensionReceiver == null) { "VariableAsFunctionResolvedCall must have one receiver" }
        val variableCall = resolvedCall.variableCall
        val isFunctionType = variableCall.resultingDescriptor.type.run { isFunctionTypeOrSubtype || isSuspendFunctionTypeOrSubtype }
        val inlineCall = if (isFunctionType) variableCall else resolvedCall

        val newExplicitReceivers = if (variableCall.expectedReceivers()) {
            val newReceiver = CallTranslator.translateGet(context, variableCall, explicitReceivers.extensionOrDispatchReceiver)
            ExplicitReceivers(newReceiver)
        } else {
            val dispatchReceiver = CallTranslator.translateGet(context, variableCall, null)
            if (explicitReceivers.extensionOrDispatchReceiver == null) {
                ExplicitReceivers(dispatchReceiver)
            } else {
                ExplicitReceivers(dispatchReceiver, explicitReceivers.extensionOrDispatchReceiver)
            }
        }

        return translateFunctionCall(context, resolvedCall.functionCall, inlineCall, newExplicitReceivers)
    }

    val call = resolvedCall.call
    if (call.callType == CallType.INVOKE && !isInvokeCallOnVariable(call)) {
        val explicitReceiversForInvoke = computeExplicitReceiversForInvoke(context, resolvedCall, explicitReceivers)
        return translateFunctionCall(context, resolvedCall, resolvedCall, explicitReceiversForInvoke)
    }

    return translateFunctionCall(context, resolvedCall, resolvedCall, explicitReceivers)
}

private fun translateFunctionCall(
        context: TranslationContext,
        resolvedCall: ResolvedCall<out FunctionDescriptor>,
        inlineResolvedCall: ResolvedCall<out CallableDescriptor>,
        explicitReceivers: ExplicitReceivers
): JsExpression {
    val rangeCheck = RangeCheckTranslator(context).translateAsRangeCheck(resolvedCall, explicitReceivers)
    if (rangeCheck != null) return rangeCheck

    val callInfo = context.getCallInfo(resolvedCall, explicitReceivers)
    var callExpression = callInfo.translateFunctionCall()

    if (CallExpressionTranslator.shouldBeInlined(inlineResolvedCall.resultingDescriptor, context)) {
        setInlineCallMetadata(callExpression, resolvedCall.call.callElement, inlineResolvedCall.resultingDescriptor, context)
    }

    if (resolvedCall.resultingDescriptor.isSuspend) {
        val statement = callInfo.constructSuspendSafeCallIfNeeded(JsAstUtils.asSyntheticStatement(callExpression.apply {
            isSuspend = true
            source = resolvedCall.call.callElement
        }))
        context.currentBlock.statements += statement
        return context.createCoroutineResult(resolvedCall)
    }
    else {
        callExpression = callInfo.constructSafeCallIfNeeded(callExpression)
    }

    callExpression.type = resolvedCall.getReturnType().let { if (resolvedCall.call.isSafeCall()) it.makeNullable() else it }
    mayBeMarkByRangeMetadata(resolvedCall, callExpression)
    return callExpression
}

private fun mayBeMarkByRangeMetadata(resolvedCall: ResolvedCall<out FunctionDescriptor>, callExpression: JsExpression) {
    when (resolvedCall.resultingDescriptor.fqNameSafe) {
        intRangeToFqName -> {
            callExpression.range = Pair(RangeType.INT, RangeKind.RANGE_TO)
        }
        longRangeToFqName -> {
            callExpression.range = Pair(RangeType.LONG, RangeKind.RANGE_TO)
        }
        untilFqName -> when (resolvedCall.resultingDescriptor.returnType?.constructor?.declarationDescriptor?.fqNameUnsafe) {
            KotlinBuiltIns.FQ_NAMES.intRange -> {
                callExpression.range = Pair(RangeType.INT, RangeKind.UNTIL)
            }
            KotlinBuiltIns.FQ_NAMES.longRange -> {
                callExpression.range = Pair(RangeType.LONG, RangeKind.UNTIL)
            }
        }
    }
}

private val intRangeToFqName = FqName("kotlin.Int.rangeTo")
private val longRangeToFqName = FqName("kotlin.Long.rangeTo")
private val untilFqName = FqName("kotlin.ranges.until")

fun ResolvedCall<out CallableDescriptor>.getReturnType(): KotlinType = TranslationUtils.getReturnTypeForCoercion(resultingDescriptor)

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

    if (dispatchReceiver != null && extensionReceiver != null && resolvedCall.explicitReceiverKind == BOTH_RECEIVERS) {
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

    return when (resolvedCall.explicitReceiverKind) {
        NO_EXPLICIT_RECEIVER -> ExplicitReceivers(null)
        DISPATCH_RECEIVER -> ExplicitReceivers(translateReceiverAsExpression(dispatchReceiver))
        EXTENSION_RECEIVER -> ExplicitReceivers(translateReceiverAsExpression(extensionReceiver))
        BOTH_RECEIVERS -> ExplicitReceivers(translateReceiverAsExpression(dispatchReceiver),
                                            translateReceiverAsExpression(extensionReceiver))
    }
}

abstract class CallCase<in I : CallInfo> {

    protected open fun I.unsupported(message: String = "") : Nothing = throw IllegalStateException("this case unsupported. $this")

    protected open fun I.noReceivers(): JsExpression = unsupported()

    protected open fun I.dispatchReceiver(): JsExpression = unsupported()

    protected open fun I.extensionReceiver(): JsExpression = unsupported()

    protected open fun I.bothReceivers(): JsExpression = unsupported()

    fun translate(callInfo: I): JsExpression {
        return if (callInfo.dispatchReceiver == null) {
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
    }
}

abstract class FunctionCallCase : CallCase<FunctionCallInfo>()

abstract class VariableAccessCase : CallCase<VariableAccessInfo>()

interface DelegateIntrinsic<in I : CallInfo> {
    fun I.canBeApply(): Boolean = true
    fun I.getDescriptor(): CallableDescriptor
    fun I.getArgs(): List<JsExpression>

    fun intrinsic(callInfo: I, context: TranslationContext): JsExpression? = if (callInfo.canBeApply()) callInfo.getIntrinsic(context) else null

    private fun I.getIntrinsic(context: TranslationContext): JsExpression? {
        val descriptor = getDescriptor()

        // Now intrinsic support only FunctionDescriptor. See DelegatePropertyAccessIntrinsic.getDescriptor()
        if (descriptor is FunctionDescriptor) {
            val intrinsic = context.intrinsics().getFunctionIntrinsic(descriptor, context)
            if (intrinsic != null) {
                return intrinsic.apply(this, getArgs(), context)
            }
        }
        return null
    }
}
