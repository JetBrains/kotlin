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

package org.jetbrains.kotlin.js.translate.reference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.calls.callUtil.getPropertyResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS

object CallableReferenceTranslator {

    fun translate(expression: KtCallableReferenceExpression, context: TranslationContext): JsExpression {
        val descriptor = BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), expression.callableReference)

        val receiver = expression.receiverExpression?.let { r ->
            if (context.bindingContext().get(BindingContext.DOUBLE_COLON_LHS, r) is DoubleColonLHS.Expression) {
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

    private fun reportNotSupported(context: TranslationContext, expression: KtCallableReferenceExpression): JsExpression {
        context.bindingTrace().report(ErrorsJs.REFERENCE_TO_BUILTIN_MEMBERS_NOT_SUPPORTED.on(expression, expression))
        return JsLiteral.NULL
    }

    private fun translateForFunction(
            descriptor: FunctionDescriptor,
            context: TranslationContext,
            expression: KtCallableReferenceExpression,
            receiver: JsExpression?
    ): JsExpression {
        return when {
        // TODO Support for callable reference to builtin functions and members
            KotlinBuiltIns.isBuiltIn(descriptor) ->
                reportNotSupported(context, expression)
            isConstructor(descriptor) ->
                translateForConstructor(descriptor, context)
            isExtension(descriptor) ->
                translateForExtensionFunction(descriptor, context, receiver)
            isMember(descriptor) ->
                translateForMemberFunction(descriptor, context, receiver)
            else ->
                ReferenceTranslator.translateAsValueReference(descriptor, context)
        }
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

        return wrapPropertyCallableRef(context, receiver, descriptor, descriptor.name.identifier, getter, setter)
    }

    private fun isSetterVisible(descriptor: PropertyDescriptor, context: TranslationContext): Boolean {
        val setter = descriptor.setter ?: return false
        if (setter.visibility != Visibilities.PRIVATE) return true
        val classDescriptor = context.classDescriptor ?: return false
        return classDescriptor == descriptor.containingDeclaration
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
            val name = accessorFunction.scope.declareTemporaryName(Namer.getReceiverParameterName())
            accessorFunction.parameters += JsParameter(name)
            name.makeRef()
        }
        else {
            null
        }

        val valueParam = if (isSetter) {
            val name = accessorFunction.scope.declareTemporaryName("value")
            accessorFunction.parameters += JsParameter(name)
            name.makeRef()
        }
        else {
            JsLiteral.NULL
        }

        val accessorResult = translator(accessorContext, call, valueParam, receiverParam)
        accessorFunction.body.statements += if (isSetter) accessorResult.makeStmt() else JsReturn(accessorResult)
        return if (receiver != null) {
            JsInvocation(JsNameRef("bind", accessorFunction), JsLiteral.NULL, receiver)
        }
        else {
            accessorFunction
        }
    }

    private fun wrapPropertyCallableRef(
            context: TranslationContext,
            receiver: JsExpression?,
            descriptor: PropertyDescriptor,
            name: String,
            getter: JsExpression,
            setter: JsExpression?
    ): JsExpression {
        var argCount = if (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null) 1 else 0
        if (receiver != null) {
            argCount--
        }
        val nameLiteral = context.program().getStringLiteral(name)
        val invokeName = if (argCount == 0) Namer.PROPERTY_CALLABLE_REF_ZERO_ARG else Namer.PROPERTY_CALLABLE_REF_ONE_ARG
        val invokeFun = JsNameRef(invokeName, Namer.kotlinObject())
        val invocation = JsInvocation(invokeFun, nameLiteral, getter)
        if (setter != null) {
            invocation.arguments += setter
        }
        return invocation
    }

    private fun isConstructor(descriptor: CallableDescriptor) = descriptor is ConstructorDescriptor

    private fun isExtension(descriptor: CallableDescriptor) = DescriptorUtils.isExtension(descriptor)

    private fun isMember(descriptor: CallableDescriptor) = JsDescriptorUtils.getContainingDeclaration(descriptor) is ClassDescriptor

    private fun translateForConstructor(descriptor: FunctionDescriptor, context: TranslationContext): JsExpression {
        val jsFunctionRef = ReferenceTranslator.translateAsValueReference(descriptor, context)
        return JsInvocation(context.namer().callableRefForConstructorReference(), jsFunctionRef)
    }

    private fun translateForExtensionFunction(descriptor: FunctionDescriptor,
                                              context: TranslationContext,
                                              receiver: JsExpression?
    ): JsExpression {
        val jsFunctionRef = ReferenceTranslator.translateAsValueReference(descriptor, context)
        if (AnnotationsUtils.isNativeObject(descriptor)) {
            return translateForMemberFunction(descriptor, context, receiver)
        }
        else {
            if (receiver == null) {
                return JsInvocation(context.namer().callableRefForExtensionFunctionReference(), jsFunctionRef)
            }
            else {
                return JsInvocation(context.namer().boundCallableRefForExtensionFunctionReference(), receiver, jsFunctionRef)
            }
        }
    }

    private fun translateForMemberFunction(
            descriptor: CallableDescriptor,
            context: TranslationContext,
            receiver: JsExpression?
    ): JsExpression {
        val funName = context.getNameForDescriptor(descriptor)
        val funNameAsString = context.program().getStringLiteral(funName.toString())
        if (receiver == null) {
            return JsInvocation(context.namer().callableRefForMemberFunctionReference(), funNameAsString)
        }
        else {
            return JsInvocation(context.namer().boundCallableRefForMemberFunctionReference(), receiver, funNameAsString)
        }
    }
}
