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
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsInvocation
import java.util.Collections
import java.util.ArrayList
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind
import com.google.dart.compiler.backend.js.ast.JsNew
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor
import org.jetbrains.k2js.translate.utils.TranslationUtils
import org.jetbrains.k2js.translate.general.Translation
import org.jetbrains.k2js.translate.utils.PsiUtils
import com.google.dart.compiler.backend.js.ast.JsLiteral

public fun addReceiverToArgs(receiver: JsExpression, arguments: List<JsExpression>) : List<JsExpression> {
    if (arguments.isEmpty())
        return Collections.singletonList(receiver)

    val argumentList = ArrayList<JsExpression>(1 + arguments.size())
    argumentList.add(receiver)
    argumentList.addAll(arguments)
    return argumentList
}


// call may be native and|or with spreadOperator
class DefaultCallCase(callInfo: FunctionCallInfo): FunctionCallCase(callInfo) { //TODO: check spreadOperator

    override fun FunctionCallInfo.bothReceivers(): JsExpression { // TODO: think about crazy case: spreadOperator + native
        val functionRef = JsNameRef(functionName, thisObject!!)
        return JsInvocation(functionRef, addReceiverToArgs(receiverObject!!, argumentsInfo.getTranslateArguments()))
    }

    override fun FunctionCallInfo.thisObject(): JsExpression {
        if (isNative() && hasSpreadOperator()) {
            val cachedReceiver = argumentsInfo.getCachedReceiver()!!
            val functionCallRef = Namer.getFunctionCallRef(JsNameRef(functionName, cachedReceiver.assignmentExpression()))
            return JsInvocation(functionCallRef, argumentsInfo.getTranslateArguments())
        }
        val functionRef = JsNameRef(functionName, thisObject!!)
        return JsInvocation(functionRef, argumentsInfo.getTranslateArguments())
    }

    // TODO: refactor after fix ArgumentsInfo - duplicate code
    override fun FunctionCallInfo.receiverArgument(): JsExpression {
        val qualifierForFunction = context.getQualifierForDescriptor(callableDescriptor)
        if (isNative() && hasSpreadOperator()) {
            val functionCallRef = Namer.getFunctionCallRef(JsNameRef(functionName, qualifierForFunction))
            return JsInvocation(functionCallRef, argumentsInfo.getTranslateArguments())
        }
        if (isNative()) {
            return JsInvocation(JsNameRef(functionName, receiverObject), argumentsInfo.getTranslateArguments())
        }
        val functionCall = JsNameRef(functionName, qualifierForFunction) // TODO: remake to call
        return JsInvocation(functionCall, addReceiverToArgs(receiverObject!!, argumentsInfo.getTranslateArguments()))
    }
    override fun FunctionCallInfo.noReceivers(): JsExpression {
        val qualifierForFunction = context.getQualifierForDescriptor(callableDescriptor)
        if (isNative() && hasSpreadOperator()) {
            val functionCallRef = Namer.getFunctionCallRef(JsNameRef(functionName, qualifierForFunction))
            return JsInvocation(functionCallRef, argumentsInfo.getTranslateArguments())
        }
        val functionCall = JsNameRef(functionName, qualifierForFunction)
        return JsInvocation(functionCall, argumentsInfo.getTranslateArguments())
    }
}


class DelegateFunctionIntrinsic(callInfo: FunctionCallInfo) : FunctionCallCase(callInfo), DelegateIntrinsic<FunctionCallInfo> {
    override fun FunctionCallInfo.getArgs(): List<JsExpression> {
        return argumentsInfo.getTranslateArguments()
    }
    override fun FunctionCallInfo.getDescriptor(): CallableDescriptor {
        return callableDescriptor
    }
}

class InvokeIntrinsic(callInfo: FunctionCallInfo) : FunctionCallCase(callInfo) {
    class object {
        fun canApply(callInfo: FunctionCallInfo): Boolean {
            if (!callInfo.callableDescriptor.getName().asString().equals("invoke"))
                return false
            val parameterCount = callInfo.callableDescriptor.getValueParameters().size()
            val funDeclaration = callInfo.callableDescriptor.getContainingDeclaration()
            return funDeclaration == ((if (callInfo.callableDescriptor.getReceiverParameter() == null)
                KotlinBuiltIns.getInstance().getFunction(parameterCount)
            else
                KotlinBuiltIns.getInstance().getExtensionFunction(parameterCount)))
        }
    }

    override fun FunctionCallInfo.thisObject(): JsExpression {
        return JsInvocation(thisObject, argumentsInfo.getTranslateArguments())
    }
    override fun FunctionCallInfo.bothReceivers(): JsExpression {
        return JsInvocation(thisObject, addReceiverToArgs(receiverObject!!, argumentsInfo.getTranslateArguments()))
    }
}

class ConstructorCallCase(callInfo: FunctionCallInfo) : FunctionCallCase(callInfo) {
    override fun FunctionCallInfo.noReceivers(): JsExpression {
        return JsNew(context.getQualifiedReference(callableDescriptor), argumentsInfo.getTranslateArguments())
    }
}

class ExpressionAsFunctionDescriptorIntrinsic(callInfo: FunctionCallInfo) : FunctionCallCase(callInfo) {
    class object {
        fun canApply(callInfo: FunctionCallInfo): Boolean {
            return callInfo.callableDescriptor is ExpressionAsFunctionDescriptor
        }
    }

    override fun FunctionCallInfo.noReceivers(): JsExpression {
        if (callableDescriptor !is ExpressionAsFunctionDescriptor) {
            throw IllegalStateException("callableDescriptor must be ExpressionAsFunctionDescriptor $callInfo")
        }
        val funRef = Translation.translateAsExpression(callableDescriptor.getExpression()!!, context)
        return JsInvocation(funRef, argumentsInfo.getTranslateArguments())

    }
}

class SuperCallCase(callInfo: FunctionCallInfo) : FunctionCallCase(callInfo) {
    class object {
        fun canApply(callInfo: FunctionCallInfo): Boolean {
            return callInfo.isSuperInvocation()
        }
    }

    override fun FunctionCallInfo.thisObject(): JsExpression { // TODO: spread operator
        val prototypeClass = JsNameRef(Namer.getPrototypeName(), thisObject!!)
        val functionRef =  Namer.getFunctionCallRef(JsNameRef(functionName, prototypeClass))
        return JsInvocation(functionRef, addReceiverToArgs(JsLiteral.THIS, argumentsInfo.getTranslateArguments()))
    }
}

fun createFunctionCases(): CallCaseDispatcher<FunctionCallCase, FunctionCallInfo> {
    val caseDispatcher = CallCaseDispatcher<FunctionCallCase, FunctionCallInfo>()

    caseDispatcher.addCase(::ExpressionAsFunctionDescriptorIntrinsic) {ExpressionAsFunctionDescriptorIntrinsic.canApply(it)}
    caseDispatcher.addCase(::InvokeIntrinsic) {InvokeIntrinsic.canApply(it)}

    caseDispatcher.addCase { DelegateFunctionIntrinsic(it).intrinsic() }

    caseDispatcher.addCase(::ConstructorCallCase) {it.callableDescriptor is ConstructorDescriptor}
    caseDispatcher.addCase(::SuperCallCase) {SuperCallCase.canApply(it)}

    caseDispatcher.addCase(::DefaultCallCase) { true } // TODO: fix this
    return caseDispatcher
}