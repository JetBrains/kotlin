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

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.isCallableReference
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.calls.callUtil.getFunctionResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.callUtil.getPropertyResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.DelegatingResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS

object CallableReferenceTranslator {

    fun translate(expression: KtCallableReferenceExpression, context: TranslationContext): JsExpression {
        val descriptor = BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), expression.callableReference)

        val receiver = expression.receiverExpression?.let { r ->
            if (context.bindingContext().get(BindingContext.DOUBLE_COLON_LHS, r) is DoubleColonLHS.Expression &&
                descriptor is CallableMemberDescriptor &&
                descriptor.dispatchReceiverParameter ?: descriptor.extensionReceiverParameter != null
            ) {
                val block = JsBlock()
                val e = Translation.translateAsExpression(r, context, block)
                if (!block.isEmpty) {
                    context.addStatementsToCurrentBlockFrom(block)
                }
                e
            }
            else {
                null
            }
        } ?: (descriptor as? PropertyImportedFromObject)?.let {
            ReferenceTranslator.translateAsValueReference(it.containingObject, context)
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
        val fakeExpression = CodegenUtil.constructFakeFunctionCall(expression.project, descriptor)

        val fakeCall = CallMaker.makeCall(fakeExpression, null, null, fakeExpression, fakeExpression.valueArguments)
        val fakeResolvedCall = object : DelegatingResolvedCall<FunctionDescriptor>(realResolvedCall) {
            val valueArgumentList = fakeCall.valueArguments.map(::ExpressionValueArgument)
            val valueArgumentMap = valueArgumentList.withIndex().associate { (index, arg) -> descriptor.valueParameters[index] to arg }

            override fun getCall() = fakeCall

            override fun getValueArgumentsByIndex(): List<ResolvedValueArgument> = valueArgumentList

            override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> = valueArgumentMap
        }

        val function = JsFunction(context.scope(), JsBlock(), "")
        val receiverParam = if (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null) {
            val paramName = JsScope.declareTemporaryName(Namer.getReceiverParameterName())
            function.parameters += JsParameter(paramName)
            paramName.makeRef()
        }
        else {
            null
        }

        val aliases = mutableMapOf<KtExpression, JsExpression>()
        for ((index, valueArg) in fakeCall.valueArguments.withIndex()) {
            val paramName = JsScope.declareTemporaryName(descriptor.valueParameters[index].name.asString())
            function.parameters += JsParameter(paramName)
            aliases[valueArg.getArgumentExpression()!!] = paramName.makeRef()
        }
        val functionContext = context.innerBlock(function.body).innerContextWithAliasesForExpressions(aliases)
        val invocation = CallTranslator.translate(functionContext, fakeResolvedCall, receiverParam)
        function.body.statements += JsReturn(invocation)

        val rawCallableRef = bindIfNecessary(function, receiver)
        return wrapFunctionCallableRef(expression.callableReference.getReferencedName(), rawCallableRef)
    }

    private fun translateForProperty(
            descriptor: PropertyDescriptor,
            context: TranslationContext,
            expression: KtCallableReferenceExpression,
            receiver: JsExpression?
    ): JsExpression {
        val call = expression.callableReference.getPropertyResolvedCallWithAssert(context.bindingContext())

        val getter = translateForPropertyAccessor(call, descriptor, context, receiver, false) { context, call, _, receiverParam ->
            CallTranslator.translateGet(context, call, receiverParam)
        }

        val setter = if (isSetterVisible(descriptor, context)) {
            translateForPropertyAccessor(call, descriptor, context, receiver, true, CallTranslator::translateSet)
        }
        else {
            null
        }

        return wrapPropertyCallableRef(receiver, descriptor, expression.callableReference.getReferencedName(), getter, setter)
    }

    private fun isSetterVisible(descriptor: PropertyDescriptor, context: TranslationContext): Boolean {
        val setter = descriptor.setter ?: return false
        if (setter.visibility != Visibilities.PRIVATE) return true
        val classDescriptor = context.classDescriptor ?: return false

        val outerClasses = generateSequence<DeclarationDescriptor>(classDescriptor) { it.containingDeclaration }
                .filterIsInstance<ClassDescriptor>()
        return descriptor.containingDeclaration in outerClasses
    }

    private fun translateForPropertyAccessor(
            call: ResolvedCall<out PropertyDescriptor>,
            descriptor: PropertyDescriptor,
            context: TranslationContext,
            receiver: JsExpression?,
            isSetter: Boolean,
            translator: (TranslationContext, ResolvedCall<out PropertyDescriptor>, JsExpression, JsExpression?) -> JsExpression
    ): JsExpression {
        val accessorFunction = JsFunction(context.scope(), JsBlock(), "")
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

    private fun wrapPropertyCallableRef(
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
        val invokeFun = JsNameRef(Namer.PROPERTY_CALLABLE_REF, Namer.kotlinObject())
        val invocation = JsInvocation(invokeFun, nameLiteral, argCountLiteral, getter)
        if (setter != null) {
            invocation.arguments += setter
        }
        return invocation
    }

    private fun wrapFunctionCallableRef(
            name: String,
            function: JsExpression
    ): JsExpression {
        val nameLiteral = JsStringLiteral(name)
        val invokeName = Namer.FUNCTION_CALLABLE_REF
        val invokeFun = JsNameRef(invokeName, Namer.kotlinObject())
        invokeFun.sideEffects = SideEffectKind.PURE
        val invocation = JsInvocation(invokeFun, nameLiteral, function)
        invocation.isCallableReference = true
        invocation.sideEffects = SideEffectKind.PURE
        return invocation
    }
}
