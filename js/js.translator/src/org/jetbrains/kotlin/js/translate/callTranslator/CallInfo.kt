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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getReceiverParameterForReceiver
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.reference.CallArgumentTranslator
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import com.google.dart.compiler.backend.js.ast.JsLiteral
import com.google.dart.compiler.backend.js.ast.JsConditional
import com.google.dart.compiler.backend.js.ast.JsBlock
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver

interface CallInfo {
    val context: TranslationContext
    val resolvedCall: ResolvedCall<out CallableDescriptor>

    val dispatchReceiver: JsExpression?
    val extensionReceiver: JsExpression?

    fun constructSafeCallIsNeeded(result: JsExpression): JsExpression
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

class FunctionCallInfo(callInfo: CallInfo, val argumentsInfo: CallArgumentTranslator.ArgumentsInfo) : AbstractCallInfo(), CallInfo by callInfo

val CallInfo.superCallReceiver : JsExpression?
    get() {
        val receiver = this.resolvedCall.dispatchReceiver
        return when (receiver) {
            is ExpressionReceiver -> {
                val expr = receiver.expression
                when (expr) {
                    is KtSuperExpression -> {
                        val superDescriptor = context.getSuperTarget(expr);
                        context.getDispatchReceiver(JsDescriptorUtils.getReceiverParameterForDeclaration(superDescriptor))
                    }
                    else -> {
                        Translation.translateAsExpression(receiver.expression, context)
                    }
                }
            }
            is ThisClassReceiver -> {
                JsLiteral.THIS
            }
            else -> {
                null
            }
        }
    }

/**
 * no receivers - extensionOrDispatchReceiver = null,     extensionReceiver = null
 * this -         extensionOrDispatchReceiver = this,     extensionReceiver = null
 * receiver -     extensionOrDispatchReceiver = receiver, extensionReceiver = null
 * both -         extensionOrDispatchReceiver = this,     extensionReceiver = receiver
 */
class ExplicitReceivers(val extensionOrDispatchReceiver: JsExpression?, val extensionReceiver: JsExpression? = null)

fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out CallableDescriptor>, extensionOrDispatchReceiver: JsExpression?): CallInfo {
    return createCallInfo(resolvedCall, ExplicitReceivers(extensionOrDispatchReceiver))
}

fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out FunctionDescriptor>, extensionOrDispatchReceiver: JsExpression?): FunctionCallInfo {
    return getCallInfo(resolvedCall, ExplicitReceivers(extensionOrDispatchReceiver));
}

// two receiver need only for FunctionCall in VariableAsFunctionResolvedCall
fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out FunctionDescriptor>, explicitReceivers: ExplicitReceivers): FunctionCallInfo {
    val argsBlock = JsBlock()
    val argumentsInfo = CallArgumentTranslator.translate(resolvedCall, explicitReceivers.extensionOrDispatchReceiver, this, argsBlock)
    val explicitReceiversCorrected =
        if (!argsBlock.isEmpty && explicitReceivers.extensionOrDispatchReceiver != null) {
            val receiverOrThisRef =
                if (TranslationUtils.isCacheNeeded(explicitReceivers.extensionOrDispatchReceiver)) {
                    val receiverOrThisRefVar = this.declareTemporary(explicitReceivers.extensionOrDispatchReceiver)
                    this.addStatementToCurrentBlock(receiverOrThisRefVar.assignmentExpression().makeStmt())
                    receiverOrThisRefVar.reference()
                }
                else {
                    explicitReceivers.extensionOrDispatchReceiver
                }
            var receiverRef = explicitReceivers.extensionReceiver
            if (receiverRef != null) {
                receiverRef = this.declareTemporary(null).reference()
                this.addStatementToCurrentBlock(JsAstUtils.assignment(receiverRef, explicitReceivers.extensionReceiver!!).makeStmt())
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

private fun TranslationContext.createCallInfo(resolvedCall: ResolvedCall<out CallableDescriptor>, explicitReceivers: ExplicitReceivers): CallInfo {
    val receiverKind = resolvedCall.explicitReceiverKind

    fun getDispatchReceiver(): JsExpression? {
        val receiverValue = resolvedCall.dispatchReceiver ?: return null
        return when (receiverKind) {
            DISPATCH_RECEIVER, BOTH_RECEIVERS -> explicitReceivers.extensionOrDispatchReceiver
            else -> this.getDispatchReceiver(receiverValue)
        }
    }

    fun getExtensionReceiver(): JsExpression? {
        val receiverValue = resolvedCall.extensionReceiver ?: return null
        return when (receiverKind) {
            EXTENSION_RECEIVER -> explicitReceivers.extensionOrDispatchReceiver
            BOTH_RECEIVERS -> explicitReceivers.extensionReceiver
            else -> this.getDispatchReceiver(receiverValue as ReceiverValue)
        }
    }

    var dispatchReceiver = getDispatchReceiver()
    var extensionReceiver = getExtensionReceiver()
    var notNullConditional: JsConditional? = null

    if (resolvedCall.isSafeCall) {
        when (resolvedCall.explicitReceiverKind) {
            BOTH_RECEIVERS, EXTENSION_RECEIVER -> {
                notNullConditional = TranslationUtils.notNullConditional(extensionReceiver!!, JsLiteral.NULL, this)
                extensionReceiver = notNullConditional.thenExpression
            }
            else -> {
                notNullConditional = TranslationUtils.notNullConditional(dispatchReceiver!!, JsLiteral.NULL, this)
                dispatchReceiver = notNullConditional.thenExpression
            }
        }
    }
    return object : AbstractCallInfo(), CallInfo {
        override val context: TranslationContext = this@createCallInfo
        override val resolvedCall: ResolvedCall<out CallableDescriptor> = resolvedCall
        override val dispatchReceiver: JsExpression? = dispatchReceiver
        override val extensionReceiver: JsExpression? = extensionReceiver

        val notNullConditionalForSafeCall: JsConditional? = notNullConditional

        override fun constructSafeCallIsNeeded(result: JsExpression): JsExpression {
            if (notNullConditionalForSafeCall == null) {
                return result
            } else {
                notNullConditionalForSafeCall.thenExpression = result
                return notNullConditionalForSafeCall
            }
        }
    };
}
