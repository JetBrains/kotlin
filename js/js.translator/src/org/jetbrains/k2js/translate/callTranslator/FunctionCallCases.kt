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
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsInvocation
import java.util.Collections
import java.util.ArrayList
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import com.google.dart.compiler.backend.js.ast.JsNew
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.k2js.translate.general.Translation
import com.google.dart.compiler.backend.js.ast.JsLiteral
import com.google.dart.compiler.backend.js.ast.JsName
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.reference.CallArgumentTranslator
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.psi.Call.CallType
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.jet.lang.resolve.DescriptorUtils

public fun addReceiverToArgs(receiver: JsExpression, arguments: List<JsExpression>): List<JsExpression> {
    if (arguments.isEmpty())
        return Collections.singletonList(receiver)

    val argumentList = ArrayList<JsExpression>(1 + arguments.size())
    argumentList.add(receiver)
    argumentList.addAll(arguments)
    return argumentList
}


// call may be native and|or with spreadOperator
object DefaultFunctionCallCase : FunctionCallCase {
    // TODO: refactor after fix ArgumentsInfo - duplicate code
    private fun nativeSpreadFunWithThisObjectOrReceiver(argumentsInfo: CallArgumentTranslator.ArgumentsInfo, functionName: JsName): JsExpression {
        val cachedReceiver = argumentsInfo.getCachedReceiver()!!
        val functionCallRef = Namer.getFunctionApplyRef(JsNameRef(functionName, cachedReceiver.assignmentExpression()))
        return JsInvocation(functionCallRef, argumentsInfo.getTranslateArguments())
    }

    fun buildDefaultCallWithThisObject(argumentsInfo: CallArgumentTranslator.ArgumentsInfo,
                                       thisObject: JsExpression,
                                       functionName: JsName,
                                       isNative: Boolean,
                                       hasSpreadOperator: Boolean): JsExpression {
        if (isNative && hasSpreadOperator) {
            return nativeSpreadFunWithThisObjectOrReceiver(argumentsInfo, functionName)
        }
        val functionRef = JsNameRef(functionName, thisObject)
        return JsInvocation(functionRef, argumentsInfo.getTranslateArguments())
    }

    fun buildDefaultCallWithoutReceiver(context: TranslationContext,
                                        argumentsInfo: CallArgumentTranslator.ArgumentsInfo,
                                        callableDescriptor: CallableDescriptor,
                                        functionName: JsName,
                                        isNative: Boolean,
                                        hasSpreadOperator: Boolean): JsExpression {
        if (isNative && hasSpreadOperator) {
            val functionCallRef = Namer.getFunctionApplyRef(JsNameRef(functionName))
            return JsInvocation(functionCallRef, argumentsInfo.getTranslateArguments())
        }
        if (isNative) {
            return JsInvocation(JsNameRef(functionName), argumentsInfo.getTranslateArguments())
        }

        val functionRef = context.aliasOrValue(callableDescriptor) {
            val qualifierForFunction = context.getQualifierForDescriptor(it)
            JsNameRef(functionName, qualifierForFunction)
        }
        return JsInvocation(functionRef, argumentsInfo.getTranslateArguments())
    }

    override fun FunctionCallInfo.noReceivers(): JsExpression {
        return buildDefaultCallWithoutReceiver(context, argumentsInfo, callableDescriptor, functionName, isNative(), hasSpreadOperator())
    }

    override fun FunctionCallInfo.thisObject(): JsExpression {
        return buildDefaultCallWithThisObject(argumentsInfo, thisObject!!, functionName, isNative(), hasSpreadOperator())
    }

    override fun FunctionCallInfo.receiverArgument(): JsExpression {
        if (isNative() && hasSpreadOperator()) {
            return nativeSpreadFunWithThisObjectOrReceiver(argumentsInfo, functionName)
        }
        if (isNative()) {
            return JsInvocation(JsNameRef(functionName, receiverObject), argumentsInfo.getTranslateArguments())
        }

        val functionRef = context.aliasOrValue(callableDescriptor) {
            val qualifierForFunction = context.getQualifierForDescriptor(it)
            JsNameRef(functionName, qualifierForFunction) // TODO: remake to call
        }

        val referenceToCall =
                if (callableDescriptor.getVisibility() == Visibilities.LOCAL) {
                    Namer.getFunctionCallRef(functionRef)
                }
                else {
                    functionRef
                }

        return JsInvocation(referenceToCall, addReceiverToArgs(receiverObject!!, argumentsInfo.getTranslateArguments()))
    }

    override fun FunctionCallInfo.bothReceivers(): JsExpression {
        // TODO: think about crazy case: spreadOperator + native
        val functionRef = JsNameRef(functionName, thisObject!!)
        return JsInvocation(functionRef, addReceiverToArgs(receiverObject!!, argumentsInfo.getTranslateArguments()))
    }
}


object DelegateFunctionIntrinsic : DelegateIntrinsic<FunctionCallInfo> {
    override fun FunctionCallInfo.getArgs(): List<JsExpression> {
        return argumentsInfo.getTranslateArguments()
    }
    override fun FunctionCallInfo.getDescriptor(): CallableDescriptor {
        return callableDescriptor
    }
}

object InvokeIntrinsic : FunctionCallCase {
    fun canApply(callInfo: FunctionCallInfo): Boolean {
        if (!callInfo.callableDescriptor.getName().asString().equals("invoke"))
            return false
        val parameterCount = callInfo.callableDescriptor.getValueParameters().size()
        val funDeclaration = callInfo.callableDescriptor.getContainingDeclaration()

        val reflectionTypes = callInfo.context.getReflectionTypes()
        return if (callInfo.callableDescriptor.getReceiverParameter() == null)
            funDeclaration == KotlinBuiltIns.getInstance().getFunction(parameterCount) ||
            funDeclaration == reflectionTypes.getKFunction(parameterCount)
        else
            funDeclaration == KotlinBuiltIns.getInstance().getExtensionFunction(parameterCount) ||
            funDeclaration == reflectionTypes.getKExtensionFunction(parameterCount) ||
            funDeclaration == reflectionTypes.getKMemberFunction(parameterCount)
    }

    override fun FunctionCallInfo.thisObject(): JsExpression {
        return JsInvocation(thisObject, argumentsInfo.getTranslateArguments())
    }
    override fun FunctionCallInfo.bothReceivers(): JsExpression {
        return JsInvocation(Namer.getFunctionCallRef(thisObject!!), addReceiverToArgs(receiverObject!!, argumentsInfo.getTranslateArguments()))
    }
}

object ConstructorCallCase : FunctionCallCase {
    fun canApply(callInfo: FunctionCallInfo): Boolean {
        return callInfo.callableDescriptor is ConstructorDescriptor
    }

    override fun FunctionCallInfo.noReceivers(): JsExpression {
        val fqName = context.getQualifiedReference(callableDescriptor)

        val functionRef = if (isNative()) fqName else context.aliasOrValue(callableDescriptor) { fqName }

        return JsNew(functionRef, argumentsInfo.getTranslateArguments())
    }
}

object SuperCallCase : FunctionCallCase {
    fun canApply(callInfo: FunctionCallInfo): Boolean {
        return callInfo.isSuperInvocation()
    }

    override fun FunctionCallInfo.thisObject(): JsExpression {
        // TODO: spread operator
        val prototypeClass = JsNameRef(Namer.getPrototypeName(), thisObject!!)
        val functionRef = Namer.getFunctionCallRef(JsNameRef(functionName, prototypeClass))
        return JsInvocation(functionRef, addReceiverToArgs(JsLiteral.THIS, argumentsInfo.getTranslateArguments()))
    }
}

fun FunctionCallInfo.translateFunctionCall(): JsExpression {
    val intrinsic = DelegateFunctionIntrinsic.intrinsic(this)

    return when {
        intrinsic != null ->
            intrinsic
        InvokeIntrinsic.canApply(this) ->
            InvokeIntrinsic.translate(this)
        ConstructorCallCase.canApply(this) ->
            ConstructorCallCase.translate(this)
        SuperCallCase.canApply(this) ->
            SuperCallCase.translate(this)
        else ->
            DefaultFunctionCallCase.translate(this)
    }
}
