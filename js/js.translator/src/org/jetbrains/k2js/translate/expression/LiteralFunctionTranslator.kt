/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.expression

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.k2js.translate.context.*
import org.jetbrains.k2js.translate.general.AbstractTranslator
import org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptor
import org.jetbrains.k2js.translate.utils.FunctionBodyTranslator.translateFunctionBody
import org.jetbrains.k2js.translate.utils.TranslationUtils.getSuggestedName
import org.jetbrains.k2js.translate.utils.TranslationUtils.simpleReturnFunction
import org.jetbrains.jet.lang.descriptors.MemberDescriptor

class LiteralFunctionTranslator(context: TranslationContext) : AbstractTranslator(context) {
    fun translate(declaration: JetDeclarationWithBody): JsExpression {
        val invokingContext = context()
        val descriptor = getFunctionDescriptor(invokingContext.bindingContext(), declaration)

        val lambda = invokingContext.getFunctionObject(descriptor)
        val functionContext = invokingContext.newFunctionBodyWithUsageTracker(lambda, descriptor)

        val receiverDescriptor = descriptor.getReceiverParameter()
        if (receiverDescriptor != null) {
            val receiverName = lambda.getScope()?.declareName(Namer.getReceiverParameterName())
            lambda.getParameters()?.add(JsParameter(receiverName))
            functionContext.aliasingContext().registerAlias(receiverDescriptor, receiverName!!.makeRef())
        }

        FunctionTranslator.addParameters(lambda.getParameters(), descriptor, functionContext)
        val functionBody = translateFunctionBody(descriptor, declaration, functionContext)
        lambda.getBody()?.getStatements()?.addAll(functionBody.getStatements()!!)

        val tracker = functionContext.usageTracker()!!

        val isRecursive = tracker.isCaptured(descriptor)

        if (isRecursive) {
            lambda.setName(tracker.getNameForCapturedDescriptor(descriptor))
        }

        if (tracker.hasCapturedExceptContaining()) {
            val lambdaCreator = simpleReturnFunction(invokingContext.scope(), lambda)
            return lambdaCreator.withCapturedParameters(functionContext, invokingContext, descriptor)
        }

        return invokingContext.define(descriptor, lambda)
    }
}

fun JsFunction.withCapturedParameters(context: TranslationContext, invokingContext: TranslationContext, descriptor: MemberDescriptor): JsExpression {

    fun getParameterNameRefForInvocation(callableDescriptor: CallableDescriptor): JsExpression {
        val alias = invokingContext.getAliasForDescriptor(callableDescriptor)
        if (alias != null) return alias

        if (callableDescriptor is ReceiverParameterDescriptor) return JsLiteral.THIS

        return invokingContext.getNameForDescriptor(callableDescriptor).makeRef()
    }

    val ref = invokingContext.define(descriptor, this)
    val invocation = JsInvocation(ref)

    val invocationArguments = invocation.getArguments()!!
    val functionParameters = this.getParameters()!!

    val tracker = context.usageTracker()!!

    for ((capturedDescriptor, name) in tracker.capturedDescriptorToJsName) {
        if (capturedDescriptor == tracker.containingDescriptor) continue

        functionParameters.add(JsParameter(name))
        invocationArguments.add(getParameterNameRefForInvocation(capturedDescriptor))
    }

    return invocation
}