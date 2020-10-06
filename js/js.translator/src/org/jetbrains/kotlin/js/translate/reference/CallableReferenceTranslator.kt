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

package org.jetbrains.kotlin.js.translate.reference

import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getFunctionResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.callUtil.getPropertyResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver

object CallableReferenceTranslator {

    fun translate(expression: KtCallableReferenceExpression, context: TranslationContext): JsExpression {
        val referencedFunction = expression.callableReference.getResolvedCallWithAssert(context.bindingContext())
        val descriptor = referencedFunction.getResultingDescriptor()

        val extensionReceiver = referencedFunction.extensionReceiver
        val dispatchReceiver = referencedFunction.dispatchReceiver
        assert(dispatchReceiver == null || extensionReceiver == null) { "Cannot generate reference with both receivers: " + descriptor }

        val receiver = (dispatchReceiver ?: extensionReceiver)?.let {
            when (it) {
                is TransientReceiver -> null
                is ImplicitClassReceiver, is ExtensionReceiver ->
                    context.getDispatchReceiver(JsDescriptorUtils.getReceiverParameterForReceiver(it))
                is ExpressionReceiver -> Translation.translateAsExpression(it.expression, context)
                else -> throw UnsupportedOperationException("Unsupported receiver value: " + it)
            }
        }

        return when (descriptor) {
            is PropertyDescriptor ->
                translateForProperty(descriptor, context, expression, receiver)
            is FunctionDescriptor ->
                translateForFunction(descriptor, context, expression, receiver)
            else ->
                throw IllegalArgumentException("Expected property or function: $descriptor, expression=${expression.text}")
        }
    }

    private fun translateForFunction(
        descriptor: FunctionDescriptor,
        context: TranslationContext,
        expression: KtCallableReferenceExpression,
        receiver: JsExpression?
    ): JsExpression {
        val realResolvedCall = expression.callableReference.getFunctionResolvedCallWithAssert(context.bindingContext())
        val functionDescriptor = context.bindingContext().get(BindingContext.FUNCTION, expression)!!

        val receivers =
            if (receiver == null && (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null)) 1 else 0
        val fakeArgCount = functionDescriptor.valueParameters.size - receivers

        val fakeExpression = CodegenUtil.constructFakeFunctionCall(expression.project, fakeArgCount)
        val fakeArguments = fakeExpression.valueArguments

        val fakeCall = CallMaker.makeCall(fakeExpression, null, null, fakeExpression, fakeArguments)
        val fakeResolvedCall = object : DelegatingResolvedCall<FunctionDescriptor>(realResolvedCall) {
            val valueArgumentMap = mutableMapOf<ValueParameterDescriptor, ResolvedValueArgument>().also { argumentMap ->
                var i = 0

                for (parameter in descriptor.valueParameters) {
                    if (parameter.varargElementType != null) {
                        // Two cases are possible for a function reference with a vararg parameter of type T: either several arguments
                        // of type T are bound to that parameter, or one argument of type Array<out T>. In the former case the argument
                        // is bound as a VarargValueArgument, in the latter it's an ExpressionValueArgument
                        if (i == fakeArgCount) {
                            // If we've exhausted the argument list of the reference and we still have one vararg parameter left,
                            // we should use its default value if present, or simply an empty vararg instead
                            argumentMap[parameter] =
                                if (parameter.hasDefaultValue()) DefaultValueArgument.DEFAULT else VarargValueArgument()
                            continue
                        }
                        if (functionDescriptor.valueParameters[receivers + i].type == parameter.varargElementType) {
                            argumentMap[parameter] = VarargValueArgument(fakeArguments.subList(i, fakeArgCount))
                            i = fakeArgCount
                            continue
                        }
                    }
                    if (i < fakeArgCount) {
                        argumentMap[parameter] = ExpressionValueArgument(fakeArguments.get(i++))
                    } else {
                        assert(parameter.hasDefaultValue()) {
                            "Parameter should be either vararg or expression or default: " + parameter +
                                    " (reference in: " + functionDescriptor.containingDeclaration + ")"
                        }
                        argumentMap[parameter] = DefaultValueArgument.DEFAULT
                    }
                }
            }

            val valueArgumentList = valueArgumentMap.values.toList()

            override fun getCall() = fakeCall

            override fun getValueArgumentsByIndex(): List<ResolvedValueArgument> = valueArgumentList

            override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> = valueArgumentMap

            override fun getExplicitReceiverKind(): ExplicitReceiverKind {
                if (receiver != null) {
                    return if (descriptor.isExtension) ExplicitReceiverKind.EXTENSION_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER
                } else {
                    return super.getExplicitReceiverKind()
                }
            }
        }

        val function = JsFunction(context.scope(), JsBlock(), "")
        function.source = expression
        val receiverParam = if (descriptor.dispatchReceiverParameter != null ||
            descriptor.extensionReceiverParameter != null ||
            receiver != null
        ) {
            val paramName = JsScope.declareTemporaryName(Namer.getReceiverParameterName())
            function.parameters += JsParameter(paramName)
            paramName.makeRef()
        } else {
            null
        }

        val aliases = mutableMapOf<KtExpression, JsExpression>()
        for ((index, valueArg) in fakeCall.valueArguments.withIndex()) {
            val paramName = JsScope.declareTemporaryName(functionDescriptor.valueParameters[index].name.asString())
            function.parameters += JsParameter(paramName)
            val paramRef = paramName.makeRef()
            paramRef.type = context.currentModule.builtIns.anyType
            aliases[valueArg.getArgumentExpression()!!] = paramRef
        }

        var functionContext = context.innerBlock(function.body).innerContextWithAliasesForExpressions(aliases).inner(descriptor)

        functionContext.continuationParameterDescriptor?.let { continuationDescriptor ->
            function.parameters += JsParameter(context.getNameForDescriptor(continuationDescriptor))
            functionContext =
                functionContext.innerContextWithDescriptorsAliased(mapOf(continuationDescriptor to JsAstUtils.stateMachineReceiver()))
        }

        if (descriptor.isSuspend) {
            function.fillCoroutineMetadata(functionContext, functionDescriptor, hasController = false)
        }

        val invocation = CallTranslator.translate(functionContext, fakeResolvedCall, receiverParam)
        function.body.statements += JsReturn(TranslationUtils.coerce(context, invocation, context.currentModule.builtIns.anyType))

        val rawCallableRef = bindIfNecessary(function, receiver)
        return context.wrapFunctionCallableRef(receiver, expression.callableReference.getReferencedName(), rawCallableRef)
    }

    private fun translateForProperty(
            descriptor: PropertyDescriptor,
            context: TranslationContext,
            expression: KtCallableReferenceExpression,
            receiver: JsExpression?
    ): JsExpression {
        val realCall = expression.callableReference.getPropertyResolvedCallWithAssert(context.bindingContext())

        val call = object : DelegatingResolvedCall<PropertyDescriptor>(realCall) {
            override fun getExplicitReceiverKind(): ExplicitReceiverKind {
                if (receiver != null) {
                    return if (descriptor.isExtension) ExplicitReceiverKind.EXTENSION_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER
                }
                else {
                    return super.getExplicitReceiverKind()
                }
            }
        }

        val getter = translateForPropertyAccessor(
            call,
            expression,
            descriptor,
            context,
            receiver,
            false
        ) { translationContext, resolvedCall, _, receiverParam ->
            CallTranslator.translateGet(translationContext, resolvedCall, receiverParam)
        }

        val setter = if (isSetterVisible(descriptor, context)) {
            translateForPropertyAccessor(call, expression, descriptor, context, receiver, true, CallTranslator::translateSet)
        }
        else {
            null
        }

        return context.wrapPropertyCallableRef(receiver, descriptor, expression.callableReference.getReferencedName(), getter, setter)
    }

    private fun isSetterVisible(descriptor: PropertyDescriptor, context: TranslationContext): Boolean {
        val setter = descriptor.setter ?: return false
        if (setter.visibility != DescriptorVisibilities.PRIVATE) return true
        val classDescriptor = context.classDescriptor ?: return false

        val outerClasses = generateSequence<DeclarationDescriptor>(classDescriptor) { it.containingDeclaration }
                .filterIsInstance<ClassDescriptor>()
        return descriptor.containingDeclaration in outerClasses
    }

    private fun translateForPropertyAccessor(
            call: ResolvedCall<out PropertyDescriptor>,
            expression: KtExpression,
            descriptor: PropertyDescriptor,
            context: TranslationContext,
            receiver: JsExpression?,
            isSetter: Boolean,
            translator: (TranslationContext, ResolvedCall<out PropertyDescriptor>, JsExpression, JsExpression?) -> JsExpression
    ): JsExpression {
        val accessorFunction = JsFunction(context.scope(), JsBlock(), "")
        accessorFunction.source = expression
        val accessorContext = context.innerBlock(accessorFunction.body)
        val receiverParam = if (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null) {
            val name = JsScope.declareTemporaryName(Namer.getReceiverParameterName())
            accessorFunction.parameters += JsParameter(name)
            name.makeRef()
        }
        else {
            null
        }

        val valueParam = if (isSetter) {
            val name = JsScope.declareTemporaryName("value")
            accessorFunction.parameters += JsParameter(name)
            name.makeRef()
        }
        else {
            JsNullLiteral()
        }

        val accessorResult = translator(accessorContext, call, valueParam, receiverParam)
        accessorFunction.body.statements += if (isSetter) accessorResult.makeStmt() else JsReturn(accessorResult)
        accessorFunction.body.source = expression.finalElement as? LeafPsiElement
        return bindIfNecessary(accessorFunction, receiver)
    }

    private fun bindIfNecessary(function: JsFunction, receiver: JsExpression?): JsExpression {
        return if (receiver != null) {
            JsInvocation(JsNameRef("bind", function), JsNullLiteral(), receiver)
        }
        else {
            function
        }
    }

    private fun TranslationContext.wrapPropertyCallableRef(
            receiver: JsExpression?,
            descriptor: PropertyDescriptor,
            name: String,
            getter: JsExpression,
            setter: JsExpression?
    ): JsExpression {
        var argCount = if (descriptor.containingDeclaration is ClassDescriptor || descriptor.extensionReceiverParameter != null) 1 else 0
        if (receiver != null) {
            argCount--
        }
        val nameLiteral = JsStringLiteral(name)
        val argCountLiteral = JsIntLiteral(argCount)
        val invokeFun = getReferenceToIntrinsic(Namer.PROPERTY_CALLABLE_REF)
        val invocation = JsInvocation(invokeFun, nameLiteral, argCountLiteral, getter)
        if (setter != null) {
            invocation.arguments += setter
        }
        invocation.callableReferenceReceiver = receiver
        return invocation
    }

    private fun TranslationContext.wrapFunctionCallableRef(
            receiver: JsExpression?,
            name: String,
            function: JsExpression
    ): JsExpression {
        val nameLiteral = JsStringLiteral(name)
        val invokeFun = getReferenceToIntrinsic(Namer.FUNCTION_CALLABLE_REF)
        invokeFun.sideEffects = SideEffectKind.PURE
        val invocation = JsInvocation(invokeFun, nameLiteral, function)
        invocation.isCallableReference = true
        invocation.sideEffects = SideEffectKind.PURE
        invocation.callableReferenceReceiver = receiver
        return invocation
    }
}
