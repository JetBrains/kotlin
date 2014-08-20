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

package org.jetbrains.k2js.translate.reference

import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import com.google.dart.compiler.backend.js.ast.JsInvocation
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.k2js.translate.utils.AnnotationsUtils
import org.jetbrains.jet.lang.psi.JetCallableReferenceExpression
import org.jetbrains.k2js.translate.utils.BindingUtils
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import com.google.dart.compiler.backend.js.ast.JsLiteral
import java.util.ArrayList
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import com.google.dart.compiler.backend.js.ast.JsNameRef
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

object CallableReferenceTranslator {

    fun translate(expression: JetCallableReferenceExpression, context: TranslationContext): JsExpression {
        val descriptor = BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), expression.getCallableReference())
        return when (descriptor) {
            is PropertyDescriptor ->
                translateForProperty(descriptor as PropertyDescriptor, context)
            is FunctionDescriptor ->
                translateForFunction(descriptor as FunctionDescriptor, context)
            else ->
                throw IllegalArgumentException("Expected property or function: ${descriptor}, expression=${expression.getText()}")
        }
    }

    private fun translateForFunction(descriptor: FunctionDescriptor, context: TranslationContext): JsExpression {
        return when {
        // TODO Support for callable reference to builtin functions and members
            JsDescriptorUtils.isBuiltin(descriptor) ->
                throw UnsupportedOperationException("callable references for builtin functions are not supported yet")
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

    private fun translateForProperty(descriptor: PropertyDescriptor, context: TranslationContext): JsExpression {
        return when {
        // TODO Support for callable reference to builtin properties
            JsDescriptorUtils.isBuiltin(descriptor) ->
                throw UnsupportedOperationException("callable references for builtin properties are not supported yet")
            isExtension(descriptor) ->
                translateForExtensionProperty(descriptor, context)
            isMember(descriptor) ->
                translateForMemberProperty(descriptor, context)
            else ->
                translateForTopLevelProperty(descriptor, context)
        }
    }

    private fun isConstructor(descriptor: CallableDescriptor): Boolean = descriptor is ConstructorDescriptor

    private fun isExtension(descriptor: CallableDescriptor): Boolean = JsDescriptorUtils.isExtension(descriptor)

    private fun isMember(descriptor: CallableDescriptor): Boolean = JsDescriptorUtils.getContainingDeclaration(descriptor) is ClassDescriptor

    private fun isVar(descriptor: PropertyDescriptor): JsExpression = if (descriptor.isVar()) JsLiteral.TRUE else JsLiteral.FALSE

    private fun translateForTopLevelProperty(descriptor: PropertyDescriptor, context: TranslationContext): JsExpression {
        val packageDescriptor = JsDescriptorUtils.getContainingDeclaration(descriptor)
        assert(packageDescriptor is PackageFragmentDescriptor, "Expected PackageFragmentDescriptor: ${packageDescriptor}")

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
        val receiverParameterDescriptor = descriptor.getReceiverParameter()
        assert(receiverParameterDescriptor != null, "receiverParameter for extension should not be null")

        val jsFunctionRef = ReferenceTranslator.translateAsFQReference(descriptor, context)
        if (descriptor.getVisibility() == Visibilities.LOCAL)
            return jsFunctionRef
        else if (AnnotationsUtils.isNativeObject(descriptor)) {
            val jetType = receiverParameterDescriptor!!.getType()
            val receiverClassDescriptor = DescriptorUtils.getClassDescriptorForType(jetType)
            return translateAsMemberFunctionReference(descriptor, receiverClassDescriptor, context)
        }
        else
            return JsInvocation(context.namer().callableRefForExtensionFunctionReference(), jsFunctionRef)
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