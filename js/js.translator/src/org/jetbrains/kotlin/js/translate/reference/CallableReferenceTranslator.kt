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

import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsInvocation
import com.google.dart.compiler.backend.js.ast.JsLiteral
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.psi.JetCallableReferenceExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.ArrayList

object CallableReferenceTranslator {

    fun translate(expression: JetCallableReferenceExpression, context: TranslationContext): JsExpression {
        val descriptor = BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), expression.getCallableReference())
        return when (descriptor) {
            is PropertyDescriptor ->
                translateForProperty(descriptor, context, expression)
            is FunctionDescriptor ->
                translateForFunction(descriptor, context, expression)
            else ->
                throw IllegalArgumentException("Expected property or function: ${descriptor}, expression=${expression.getText()}")
        }
    }

    private fun reportNotSupported(context: TranslationContext, expression: JetCallableReferenceExpression): JsExpression {
        context.bindingTrace().report(ErrorsJs.REFERENCE_TO_BUILTIN_MEMBERS_NOT_SUPPORTED.on(expression, expression))
        return context.getEmptyExpression()
    }

    private fun translateForFunction(descriptor: FunctionDescriptor, context: TranslationContext, expression: JetCallableReferenceExpression): JsExpression {
        return when {
        // TODO Support for callable reference to builtin functions and members
            JsDescriptorUtils.isBuiltin(descriptor) ->
                reportNotSupported(context, expression)
            isConstructor(descriptor) ->
                translateForConstructor(descriptor, context)
            isExtension(descriptor) ->
                translateForExtensionFunction(descriptor, context)
            isMember(descriptor) ->
                translateForMemberFunction(descriptor, context)
            else ->
                ReferenceTranslator.translateAsFQReference(descriptor, context)
        }
    }

    private fun translateForProperty(descriptor: PropertyDescriptor, context: TranslationContext, expression: JetCallableReferenceExpression): JsExpression {
        return when {
        // TODO Support for callable reference to builtin properties
            JsDescriptorUtils.isBuiltin(descriptor) ->
                reportNotSupported(context, expression)
            isExtension(descriptor) ->
                translateForExtensionProperty(descriptor, context)
            isMember(descriptor) ->
                translateForMemberProperty(descriptor, context)
            else ->
                translateForTopLevelProperty(descriptor, context)
        }
    }

    private fun isConstructor(descriptor: CallableDescriptor): Boolean = descriptor is ConstructorDescriptor

    private fun isExtension(descriptor: CallableDescriptor): Boolean = DescriptorUtils.isExtension(descriptor)

    private fun isMember(descriptor: CallableDescriptor): Boolean = JsDescriptorUtils.getContainingDeclaration(descriptor) is ClassDescriptor

    private fun isVar(descriptor: PropertyDescriptor): JsExpression = if (descriptor.isVar()) JsLiteral.TRUE else JsLiteral.FALSE

    private fun translateForTopLevelProperty(descriptor: PropertyDescriptor, context: TranslationContext): JsExpression {
        val packageDescriptor = JsDescriptorUtils.getContainingDeclaration(descriptor)
        assert(packageDescriptor is PackageFragmentDescriptor) { "Expected PackageFragmentDescriptor: $packageDescriptor" }

        val jsPackageNameRef = context.getQualifiedReference(packageDescriptor)
        val jsPropertyName = context.getNameForDescriptor(descriptor)
        val jsPropertyNameAsString = context.program().getStringLiteral(jsPropertyName.toString())

        return JsInvocation(context.namer().callableRefForTopLevelPropertyReference(), jsPackageNameRef, jsPropertyNameAsString, isVar(descriptor))
    }

    private fun translateForMemberProperty(descriptor: PropertyDescriptor, context: TranslationContext): JsExpression {
        val jsPropertyName = context.getNameForDescriptor(descriptor)
        val jsPropertyNameAsString = context.program().getStringLiteral(jsPropertyName.toString())
        return JsInvocation(context.namer().callableRefForMemberPropertyReference(), jsPropertyNameAsString, isVar(descriptor))
    }

    private fun translateForExtensionProperty(descriptor: PropertyDescriptor, context: TranslationContext): JsExpression {
        val jsGetterNameRef = context.getQualifiedReference(descriptor.getGetter()!!)
        val propertyName = descriptor.getName()
        val jsPropertyNameAsString = context.program().getStringLiteral(propertyName.asString())
        val argumentList = ArrayList<JsExpression>(3)
        argumentList.add(jsPropertyNameAsString)
        argumentList.add(jsGetterNameRef)
        if (descriptor.isVar()) {
            val jsSetterNameRef = context.getQualifiedReference(descriptor.getSetter()!!)
            argumentList.add(jsSetterNameRef)
        }
        if (AnnotationsUtils.isNativeObject(descriptor))
            return translateForMemberProperty(descriptor, context)
        else
            return JsInvocation(context.namer().callableRefForExtensionPropertyReference(), argumentList)
    }

    private fun translateForConstructor(descriptor: FunctionDescriptor, context: TranslationContext): JsExpression {
        val jsFunctionRef = ReferenceTranslator.translateAsFQReference(descriptor, context)
        return JsInvocation(context.namer().callableRefForConstructorReference(), jsFunctionRef)
    }

    private fun translateForExtensionFunction(descriptor: FunctionDescriptor, context: TranslationContext): JsExpression {
        val receiverParameterDescriptor = descriptor.getExtensionReceiverParameter()
        assert(receiverParameterDescriptor != null, "receiverParameter for extension should not be null")

        val jsFunctionRef = ReferenceTranslator.translateAsFQReference(descriptor, context)
        if (descriptor.getVisibility() == Visibilities.LOCAL) {
            return JsInvocation(context.namer().callableRefForLocalExtensionFunctionReference(), jsFunctionRef)
        }

        else if (AnnotationsUtils.isNativeObject(descriptor)) {
            val jetType = receiverParameterDescriptor!!.getType()
            val receiverClassDescriptor = DescriptorUtils.getClassDescriptorForType(jetType)
            return translateAsMemberFunctionReference(descriptor, receiverClassDescriptor, context)
        }
        else {
            return JsInvocation(context.namer().callableRefForExtensionFunctionReference(), jsFunctionRef)
        }
    }

    private fun translateForMemberFunction(descriptor: FunctionDescriptor, context: TranslationContext): JsExpression {
        val classDescriptor = JsDescriptorUtils.getContainingDeclaration(descriptor) as? ClassDescriptor ?: throw IllegalArgumentException("Expected ClassDescriptor: ${descriptor}")
        return translateAsMemberFunctionReference(descriptor, classDescriptor, context)
    }

    private fun translateAsMemberFunctionReference(descriptor: CallableDescriptor, classDescriptor: ClassDescriptor, context: TranslationContext): JsExpression {
        val jsClassNameRef = context.getQualifiedReference(classDescriptor)
        val funName = context.getNameForDescriptor(descriptor)
        val funNameAsString = context.program().getStringLiteral(funName.toString())
        return JsInvocation(context.namer().callableRefForMemberFunctionReference(), jsClassNameRef, funNameAsString)
    }
}
