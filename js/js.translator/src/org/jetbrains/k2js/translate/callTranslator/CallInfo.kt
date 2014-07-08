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

import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getReceiverParameterForReceiver
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.k2js.translate.utils.AnnotationsUtils
import org.jetbrains.jet.lang.psi.JetSuperExpression
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import com.google.dart.compiler.backend.js.ast.JsName
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils
import com.google.dart.compiler.backend.js.ast.JsNameRef
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.k2js.translate.reference.CallArgumentTranslator
import org.jetbrains.k2js.translate.utils.TranslationUtils
import com.google.dart.compiler.backend.js.ast.JsLiteral
import com.google.dart.compiler.backend.js.ast.JsConditional


trait CallInfo {
    val context: TranslationContext
    val resolvedCall: ResolvedCall<out CallableDescriptor>

    val thisObject: JsExpression?
    val receiverObject: JsExpression?

    fun constructSafeCallIsNeeded(result: JsExpression): JsExpression

    override fun toString(): String {
        val location = DiagnosticUtils.atLocation(callableDescriptor)
        val name = callableDescriptor.getName().asString()
        return "callableDescriptor: $name at $location; thisObject: $thisObject; receiverObject: $receiverObject"
    }
}

// if value == null, it is get access
class VariableAccessInfo(callInfo: CallInfo, val value: JsExpression? = null) : CallInfo by callInfo

class FunctionCallInfo(callInfo: CallInfo, val argumentsInfo: CallArgumentTranslator.ArgumentsInfo) : CallInfo by callInfo

/**
 * no receivers - thisObjectOrReceiverObject = null,     receiverObject = null
 * this -         thisObjectOrReceiverObject = this,     receiverObject = null
 * receiver -     thisObjectOrReceiverObject = receiver, receiverObject = null
 * both -         thisObjectOrReceiverObject = this,     receiverObject = receiver
 */
class ExplicitReceivers(val receiverOrThisObject: JsExpression?, val receiverObject: JsExpression? = null)

fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out CallableDescriptor>, receiverOrThisObject: JsExpression?): CallInfo {
    return createCallInfo(resolvedCall, ExplicitReceivers(receiverOrThisObject))
}

fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out FunctionDescriptor>, receiverOrThisObject: JsExpression?): FunctionCallInfo {
    return getCallInfo(resolvedCall, ExplicitReceivers(receiverOrThisObject));
}

// two receiver need only for FunctionCall in VariableAsFunctionResolvedCall
fun TranslationContext.getCallInfo(resolvedCall: ResolvedCall<out FunctionDescriptor>, explicitReceivers: ExplicitReceivers): FunctionCallInfo {
    val callInfo = createCallInfo(resolvedCall, explicitReceivers)
    val argumentsInfo = CallArgumentTranslator.translate(resolvedCall, explicitReceivers.receiverOrThisObject, this)
    return FunctionCallInfo(callInfo, argumentsInfo)
}

private fun TranslationContext.getThisObject(receiverValue: ReceiverValue): JsExpression {
    assert(receiverValue.exists(), "receiverValue must be exist here")
    return getThisObject(getReceiverParameterForReceiver(receiverValue))
}

private fun TranslationContext.createCallInfo(resolvedCall: ResolvedCall<out CallableDescriptor>, explicitReceivers: ExplicitReceivers): CallInfo {
    val receiverKind = resolvedCall.getExplicitReceiverKind()

    fun getThisObject(): JsExpression? {
        val receiverValue = resolvedCall.getThisObject()
        if (!receiverValue.exists()) {
            return null
        }
        return when (receiverKind) {
            THIS_OBJECT, BOTH_RECEIVERS -> explicitReceivers.receiverOrThisObject
            else -> this.getThisObject(receiverValue)
        }
    }

    fun getReceiverObject(): JsExpression? {
        val receiverValue = resolvedCall.getReceiverArgument()
        if (!receiverValue.exists()) {
            return null
        }
        return when (receiverKind) {
            RECEIVER_ARGUMENT -> explicitReceivers.receiverOrThisObject
            BOTH_RECEIVERS -> explicitReceivers.receiverObject
            else -> this.getThisObject(receiverValue)
        }
    }

    var thisObject = getThisObject()
    var receiverObject = getReceiverObject()
    var notNullConditional: JsConditional? = null

    if (resolvedCall.isSafeCall()) {
        when (resolvedCall.getExplicitReceiverKind()) {
            BOTH_RECEIVERS, RECEIVER_ARGUMENT -> {
                notNullConditional = TranslationUtils.notNullConditional(receiverObject!!, JsLiteral.NULL, this)
                receiverObject = notNullConditional!!.getThenExpression()
            }
            else -> {
                notNullConditional = TranslationUtils.notNullConditional(thisObject!!, JsLiteral.NULL, this)
                thisObject = notNullConditional!!.getThenExpression()
            }
        }
    }
    return object : CallInfo {
        override val context: TranslationContext = this@createCallInfo
        override val resolvedCall: ResolvedCall<out CallableDescriptor> = resolvedCall
        override val thisObject: JsExpression? = thisObject
        override val receiverObject: JsExpression? = receiverObject

        val notNullConditionalForSafeCall: JsConditional? = notNullConditional

        override fun constructSafeCallIsNeeded(result: JsExpression): JsExpression {
            if (notNullConditionalForSafeCall == null) {
                return result
            } else {
                notNullConditionalForSafeCall.setThenExpression(result)
                return notNullConditionalForSafeCall
            }
        }
    };
}