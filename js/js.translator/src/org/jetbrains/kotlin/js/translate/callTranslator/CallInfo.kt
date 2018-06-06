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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.type
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.CallArgumentTranslator
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getReceiverParameterForReceiver
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.js.translate.utils.createCoroutineResult
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.typeUtil.makeNullable

interface CallInfo {
    val context: TranslationContext
    val resolvedCall: ResolvedCall<out CallableDescriptor>

    val dispatchReceiver: JsExpression?
    val extensionReceiver: JsExpression?

    fun constructSafeCallIfNeeded(result: JsExpression): JsExpression

    fun constructSuspendSafeCallIfNeeded(result: JsStatement): JsStatement
}

abstract class AbstractCallInfo : CallInfo {
    override fun toString(): String {
        val location = DiagnosticUtils.atLocation(callableDescriptor)
        val name = callableDescriptor.name.asString()
        return "callableDescriptor: $name at $location; dispatchReceiver: $dispatchReceiver; extensionReceiver: $extensionReceiver"
    }
}

// if value == null, it is get access
class VariableAccessInfo(callInfo: CallInfo, val value: JsExpression? = null) : AbstractCallInfo(), CallInfo by callInfo

class FunctionCallInfo(
        callInfo: CallInfo,
        val argumentsInfo: CallArgumentTranslator.ArgumentsInfo
) : AbstractCallInfo(), CallInfo by callInfo


/**
 * no receivers - extensionOrDispatchReceiver = null,     extensionReceiver = null
 * this -         extensionOrDispatchReceiver = this,     extensionReceiver = null
 * receiver -     extensionOrDispatchReceiver = receiver, extensionReceiver = null
 * both -         extensionOrDispatchReceiver = this,     extensionReceiver = receiver
 */
class ExplicitReceivers(val extensionOrDispatchReceiver: JsExpression?, val extensionReceiver: JsExpression? = null)

fun TranslationContext.getCallInfo(
        resolvedCall: ResolvedCall<out CallableDescriptor>,
        extensionOrDispatchReceiver: JsExpression?
): CallInfo {
    return createCallInfo(resolvedCall, ExplicitReceivers(extensionOrDispatchReceiver))
}

// two receiver need only for FunctionCall in VariableAsFunctionResolvedCall
fun TranslationContext.getCallInfo(
        resolvedCall: ResolvedCall<out FunctionDescriptor>,
        explicitReceivers: ExplicitReceivers
): FunctionCallInfo {
    val argsBlock = JsBlock()
    val argumentsInfo = CallArgumentTranslator.translate(resolvedCall, explicitReceivers.extensionOrDispatchReceiver, this, argsBlock)

    val explicitReceiversCorrected =
            if (!argsBlock.isEmpty && explicitReceivers.extensionOrDispatchReceiver != null) {
                val receiverOrThisRef = cacheExpressionIfNeeded(explicitReceivers.extensionOrDispatchReceiver)
                var receiverRef = explicitReceivers.extensionReceiver
                if (receiverRef != null) {
                    receiverRef = defineTemporary(explicitReceivers.extensionReceiver!!)
                }
                ExplicitReceivers(receiverOrThisRef, receiverRef)
            }
            else {
                explicitReceivers
            }
    this.addStatementsToCurrentBlockFrom(argsBlock)
    val callInfo = createCallInfo(resolvedCall, explicitReceiversCorrected)
    return FunctionCallInfo(callInfo, argumentsInfo)
}

private fun TranslationContext.getDispatchReceiver(receiverValue: ReceiverValue): JsExpression {
    return getDispatchReceiver(getReceiverParameterForReceiver(receiverValue))
}

private fun TranslationContext.createCallInfo(
        resolvedCall: ResolvedCall<out CallableDescriptor>,
        explicitReceivers: ExplicitReceivers
): CallInfo {
    val receiverKind = resolvedCall.explicitReceiverKind

    // I'm not sure if it's a proper code, and why it should work. Just copied similar logic from ExpressionCodegen.generateConstructorCall.
    // See box/classes/inner/instantiateInDerived.kt
    // TODO: revisit this code later, write more tests (or borrow them from JVM backend)
    fun getDispatchReceiver(): JsExpression? {
        val receiverValue = resolvedCall.dispatchReceiver ?: return null
        return when (receiverKind) {
            DISPATCH_RECEIVER, BOTH_RECEIVERS -> explicitReceivers.extensionOrDispatchReceiver
            else -> getDispatchReceiver(receiverValue)
        }
    }

    fun getExtensionReceiver(): JsExpression? {
        val receiverValue = resolvedCall.extensionReceiver ?: return null
        return when (receiverKind) {
            EXTENSION_RECEIVER -> explicitReceivers.extensionOrDispatchReceiver
            BOTH_RECEIVERS -> explicitReceivers.extensionReceiver
            else -> getDispatchReceiver(receiverValue)
        }
    }

    var dispatchReceiver = getDispatchReceiver()
    var dispatchReceiverType = resolvedCall.smartCastDispatchReceiverType ?: resolvedCall.dispatchReceiver?.type
    if (dispatchReceiverType != null && (resolvedCall.resultingDescriptor as? FunctionDescriptor)?.kind?.isReal == false) {
        dispatchReceiverType = TranslationUtils.getDispatchReceiverTypeForCoercion(resolvedCall.resultingDescriptor)
    }
    var extensionReceiver = getExtensionReceiver()
    var notNullConditional: JsConditional? = null

    if (resolvedCall.call.isSafeCall()) {
        when (resolvedCall.explicitReceiverKind) {
            BOTH_RECEIVERS, EXTENSION_RECEIVER -> {
                notNullConditional = TranslationUtils.notNullConditional(extensionReceiver!!, JsNullLiteral(), this)
                extensionReceiver = notNullConditional.thenExpression
            }
            else -> {
                notNullConditional = TranslationUtils.notNullConditional(dispatchReceiver!!, JsNullLiteral(), this)
                dispatchReceiver = notNullConditional.thenExpression
            }
        }
    }

    if (dispatchReceiver == null) {
        val container = resolvedCall.resultingDescriptor.containingDeclaration
        if (DescriptorUtils.isObject(container)) {
            dispatchReceiver = ReferenceTranslator.translateAsValueReference(container, this)
            dispatchReceiverType = (container as ClassDescriptor).defaultType
        }
    }

    if (dispatchReceiverType != null) {
        dispatchReceiver = dispatchReceiver?.let {
            TranslationUtils.coerce(this, it, dispatchReceiverType)
        }
    }

    extensionReceiver = extensionReceiver?.let {
        TranslationUtils.coerce(this, it, resolvedCall.candidateDescriptor.extensionReceiverParameter!!.type)
    }


    return object : AbstractCallInfo(), CallInfo {
        override val context: TranslationContext = this@createCallInfo
        override val resolvedCall: ResolvedCall<out CallableDescriptor> = resolvedCall
        override val dispatchReceiver: JsExpression? = dispatchReceiver
        override val extensionReceiver: JsExpression? = extensionReceiver

        val notNullConditionalForSafeCall: JsConditional? = notNullConditional

        override fun constructSafeCallIfNeeded(result: JsExpression): JsExpression {
            return if (notNullConditionalForSafeCall == null) {
                result
            }
            else {
                val type = resolvedCall.getReturnType()
                result.type = type
                notNullConditionalForSafeCall.thenExpression = TranslationUtils.coerce(context, result, type.makeNullable())
                notNullConditionalForSafeCall
            }
        }

        override fun constructSuspendSafeCallIfNeeded(result: JsStatement): JsStatement {
            return if (notNullConditionalForSafeCall == null) {
                result
            }
            else {
                val callElement = resolvedCall.call.callElement
                val coroutineResult = context.createCoroutineResult(resolvedCall)
                val nullAssignment = JsAstUtils.assignment(coroutineResult, JsNullLiteral()).source(callElement)

                val thenBlock = JsBlock()
                thenBlock.statements += result
                val thenContext = context.innerBlock(thenBlock)
                val lhs = coroutineResult.deepCopy()
                val rhsOriginal = coroutineResult.deepCopy().apply { type = resolvedCall.getReturnType() }
                val rhs = TranslationUtils.coerce(thenContext, rhsOriginal, resolvedCall.getReturnType().makeNullable())
                if (rhs != rhsOriginal) {
                    thenBlock.statements += JsAstUtils.asSyntheticStatement(JsAstUtils.assignment(lhs, rhs).source(callElement))
                }

                val thenStatement = if (thenBlock.statements.size == 1) thenBlock.statements.first() else thenBlock
                JsIf(notNullConditionalForSafeCall.testExpression, thenStatement, nullAssignment.makeStmt())
            }
        }
    }
}
