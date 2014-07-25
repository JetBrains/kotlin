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

package org.jetbrains.k2js.translate.declaration


import com.google.dart.compiler.backend.js.ast.*
import com.intellij.util.SmartList
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.bindingContextUtil.*
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.general.AbstractTranslator
import org.jetbrains.k2js.translate.general.Translation
import org.jetbrains.k2js.translate.utils.BindingUtils
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.k2js.translate.utils.TranslationUtils

import java.util.LinkedHashMap

public class ExplicitDelegationTranslator(classDeclaration: JetClassOrObject, private val classDescriptor: ClassDescriptor, context: TranslationContext) : AbstractTranslator(context) {

    private val delegationFieldsInfo: DelegationFieldsInfo

    {
        this.delegationFieldsInfo = DelegationFieldsInfo()
        for (specifier in classDeclaration.getDelegationSpecifiers())
            if (specifier is JetDelegatorByExpressionSpecifier) {
                val expression = specifier.getDelegateExpression() ?: throw IllegalArgumentException("delegate expression should not be null: ${specifier.getText()}")
                val typeRef = specifier.getTypeReference() ?: throw IllegalArgumentException("type reference should not be null: ${specifier.getText()}")
                val descriptor = BindingUtils.getClassDescriptorForTypeReference(bindingContext(), typeRef)
                val propertyDescriptor = getDelegatePropertyIfAny(expression)

                if (propertyDescriptor != null && !propertyDescriptor.isVar())
                    delegationFieldsInfo.addField(expression, descriptor, propertyDescriptor)
                else {
                    val typeName = descriptor.getName().asString()
                    val delegateName = TranslationUtils.getMangledMemberNameForExplicitDelegation("\$delegate", classDeclaration.getFqName(), typeName)
                    delegationFieldsInfo.addField(expression, descriptor, delegateName)
                }
            }
    }

    public fun addInitCode(statements: MutableList<JsStatement>) {
        for (field in delegationFieldsInfo.fields.values())
            if (field.generateField) {
                val delegateInitExpr = Translation.translateAsExpression(field.expression, context())
                statements.add(JsAstUtils.defineSimpleProperty(field.name, delegateInitExpr))
            }
    }

    public fun generateDelegated(descriptor: ClassDescriptor, properties: MutableList<JsPropertyInitializer>) {
        if (descriptor is MutableClassDescriptor)
            for (callableMember in descriptor.getDeclaredCallableMembers())
                if (callableMember.getKind() == CallableMemberDescriptor.Kind.DELEGATION)
                    when (callableMember) {
                        is FunctionDescriptor ->
                            properties.add(generateDelegateCallForFunctionMember(callableMember))
                        is PropertyDescriptor ->
                            PropertyTranslator.translateAccessorsForDelegate(callableMember, getDelegateName(callableMember), properties, context())
                        else ->
                            throw IllegalArgumentException("Expected member or property: ${callableMember}")
                    }
    }

    private fun getClassDescriptor(callableMember: CallableMemberDescriptor): ClassDescriptor {
        assert(!callableMember.getOverriddenDescriptors().isEmpty(), "delegated member should have overridden descriptors: ${callableMember}")
        val descriptor = callableMember.getOverriddenDescriptors().iterator().next().getContainingDeclaration()
        assert(descriptor is ClassDescriptor, "descriptor should be instance of ClassDescriptor: ${callableMember}")
        return descriptor as ClassDescriptor
    }

    private fun getDelegateName(callableMember: CallableMemberDescriptor): String {
        val descriptor = getClassDescriptor(callableMember)
        return delegationFieldsInfo.getInfo(descriptor).name
    }

    private fun generateDelegateCallForFunctionMember(descriptor: CallableMemberDescriptor): JsPropertyInitializer {
        val delegateName = getDelegateName(descriptor)
        val functionName = context().getNameForDescriptor(descriptor)
        val functionObject = context().getFunctionObject(descriptor)
        val args = SmartList<JsExpression>()

        if (JsDescriptorUtils.isExtension(descriptor)) {
            val extensionFunctionReceiverName = functionObject.getScope().declareName(Namer.getReceiverParameterName())
            functionObject.getParameters().add(JsParameter(extensionFunctionReceiverName))
            args.add(JsNameRef(extensionFunctionReceiverName))
        }

        val delegateRefName = context().getScopeForDescriptor(descriptor).declareName(delegateName)
        val delegateRef = JsNameRef(delegateRefName, JsLiteral.THIS)
        val funRef = JsNameRef(functionName, delegateRef)

        val params = descriptor.getValueParameters()
        for (param in params) {
            val paramName = param.getName().asString()
            val jsParamName = functionObject.getScope().declareName(paramName)
            functionObject.getParameters().add(JsParameter(jsParamName))
            args.add(JsNameRef(jsParamName))
        }

        val jsInvocation = JsInvocation(funRef, args)
        val jsReturn = JsReturn(jsInvocation)
        functionObject.getBody().getStatements().add(jsReturn)
        return JsPropertyInitializer(functionName.makeRef(), functionObject)
    }

    // Adapted from org.jetbrains.jet.codegen.ImplementationBodyCodegen
    private fun getDelegatePropertyIfAny(expression: JetExpression): PropertyDescriptor? {
        var propertyDescriptor: PropertyDescriptor? = null
        if (expression is JetSimpleNameExpression) {
            val call = expression.getResolvedCall(bindingContext())
            if (call != null) {
                val callResultingDescriptor = call.getResultingDescriptor()
                if (callResultingDescriptor is ValueParameterDescriptor) {
                    val valueParameterDescriptor = callResultingDescriptor as ValueParameterDescriptor
                    // constructor parameter
                    if (valueParameterDescriptor.getContainingDeclaration() is ConstructorDescriptor) {
                        // constructor of my class
                        if (valueParameterDescriptor.getContainingDeclaration().getContainingDeclaration() == classDescriptor) {
                            propertyDescriptor = bindingContext().get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameterDescriptor)
                        }
                    }
                }
            }
        }
        return propertyDescriptor
    }

    // Adapted from DelegationFieldsInfo in org.jetbrains.jet.codegen.ImplementationBodyCodegen
    private class DelegationFieldsInfo {
        class Field (public val expression: JetExpression, public val name: String, public val generateField: Boolean)
        val fields = LinkedHashMap<ClassDescriptor, Field>()

        public fun getInfo(descriptor: ClassDescriptor): Field {
            return fields.get(descriptor) ?: throw IllegalArgumentException("Expected field for ${descriptor}")
        }

        fun addField(expression: JetExpression, descriptor: ClassDescriptor, propertyDescriptor: PropertyDescriptor) {
            fields.put(descriptor, Field(expression, propertyDescriptor.getName().asString(), false))
        }

        fun addField(expression: JetExpression, descriptor: ClassDescriptor, name: String) {
            fields.put(descriptor, Field(expression, name, true))
        }
    }
}
