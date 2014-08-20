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
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.general.AbstractTranslator
import org.jetbrains.k2js.translate.general.Translation
import org.jetbrains.k2js.translate.utils.BindingUtils
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.k2js.translate.utils.TranslationUtils.*

import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.backend.common.CodegenUtil
import java.util.HashMap
import com.google.dart.compiler.backend.js.ast.JsLiteral
import org.jetbrains.k2js.translate.declaration.propertyTranslator.addGetterAndSetter

public class DelegationTranslator(
        private val classDeclaration: JetClassOrObject,
        context: TranslationContext
) : AbstractTranslator(context) {

    private val classDescriptor: ClassDescriptor =
            BindingUtils.getClassDescriptor(context.bindingContext(), classDeclaration);

    private val delegationBySpecifiers =
            classDeclaration.getDelegationSpecifiers().filterIsInstance(javaClass<JetDelegatorByExpressionSpecifier>());

    private class Field (val name: String, val generateField: Boolean)
    private val fields = HashMap<JetDelegatorByExpressionSpecifier, Field>();

    {
        for (specifier in delegationBySpecifiers) {
            val expression = specifier.getDelegateExpression() ?:
                    throw IllegalArgumentException("delegate expression should not be null: ${specifier.getText()}")
            val descriptor = getSuperClass(specifier)
            val propertyDescriptor = CodegenUtil.getDelegatePropertyIfAny(expression, classDescriptor, bindingContext())

            if (CodegenUtil.isFinalPropertyWithBackingField(propertyDescriptor, bindingContext())) {
                fields.put(specifier, Field(propertyDescriptor!!.getName().asString(), false))
            }
            else {
                val typeFqName = DescriptorUtils.getFqNameSafe(descriptor)
                val delegateName = getMangledMemberNameForExplicitDelegation(Namer.getDelegatePrefix(), classDeclaration.getFqName(), typeFqName)
                fields.put(specifier, Field(delegateName, true))
            }
        }
    }

    public fun addInitCode(statements: MutableList<JsStatement>) {
        for (specifier in delegationBySpecifiers) {
            val field = fields.get(specifier)!!
            if (field.generateField) {
                val expression = specifier.getDelegateExpression()!!
                val delegateInitExpr = Translation.translateAsExpression(expression, context())
                statements.add(JsAstUtils.defineSimpleProperty(field.name, delegateInitExpr))
            }
        }
    }

    public fun generateDelegated(properties: MutableList<JsPropertyInitializer>) {
        for (specifier in delegationBySpecifiers) {
            generateDelegates(getSuperClass(specifier), fields.get(specifier)!!, properties)
        }
    }

    private fun getSuperClass(specifier: JetDelegationSpecifier): ClassDescriptor =
        CodegenUtil.getSuperClassByDelegationSpecifier(specifier, bindingContext())

    private fun generateDelegates(toClass: ClassDescriptor, field: Field, properties: MutableList<JsPropertyInitializer>) {
        for ((descriptor, overriddenDescriptor) in CodegenUtil.getDelegates(classDescriptor, toClass)) {
            when (descriptor) {
                is PropertyDescriptor ->
                    generateDelegateCallForPropertyMember(descriptor, field.name, properties)
                is FunctionDescriptor ->
                    generateDelegateCallForFunctionMember(descriptor, overriddenDescriptor as FunctionDescriptor, field.name, properties)
                else ->
                    throw IllegalArgumentException("Expected property or function ${descriptor}")
            }
        }
    }

    private fun generateDelegateCallForPropertyMember(
            descriptor: PropertyDescriptor,
            delegateName: String,
            properties: MutableList<JsPropertyInitializer>
    ) {
        val propertyName: String = descriptor.getName().asString()

        fun generateDelegateGetterFunction(getterDescriptor: PropertyGetterDescriptor): JsFunction {
            val delegateRefName = context().getScopeForDescriptor(getterDescriptor).declareName(delegateName)
            val delegateRef = JsNameRef(delegateRefName, JsLiteral.THIS)

            val returnExpression = if (JsDescriptorUtils.isExtension(descriptor)) {
                val getterName = context().getNameForDescriptor(getterDescriptor)
                val receiver = Namer.getReceiverParameterName()
                JsInvocation(JsNameRef(getterName, delegateRef), JsNameRef(receiver))
            }
            else {
                JsNameRef(propertyName, delegateRef): JsExpression // TODO remove explicit type specification after resolving KT-5569
            }

            val jsFunction = simpleReturnFunction(context().getScopeForDescriptor(getterDescriptor.getContainingDeclaration()), returnExpression)
            if (JsDescriptorUtils.isExtension(descriptor)) {
                val receiverName = jsFunction.getScope().declareName(Namer.getReceiverParameterName())
                jsFunction.getParameters().add(JsParameter(receiverName))
            }
            return jsFunction
        }

        fun generateDelegateSetterFunction(setterDescriptor: PropertySetterDescriptor): JsFunction {
            val jsFunction = JsFunction(context().getScopeForDescriptor(setterDescriptor.getContainingDeclaration()))

            assert(setterDescriptor.getValueParameters().size() == 1, "Setter must have 1 parameter")
            val defaultParameter = JsParameter(jsFunction.getScope().declareTemporary())
            val defaultParameterRef = defaultParameter.getName().makeRef()

            val delegateRefName = context().getScopeForDescriptor(setterDescriptor).declareName(delegateName)
            val delegateRef = JsNameRef(delegateRefName, JsLiteral.THIS)

            val setExpression = if (JsDescriptorUtils.isExtension(descriptor)) {
                val setterName = context().getNameForDescriptor(setterDescriptor)
                val setterNameRef = JsNameRef(setterName, delegateRef)
                val extensionFunctionReceiverName = jsFunction.getScope().declareName(Namer.getReceiverParameterName())
                jsFunction.getParameters().add(JsParameter(extensionFunctionReceiverName))
                JsInvocation(setterNameRef, JsNameRef(extensionFunctionReceiverName), defaultParameterRef)
            }
            else {
                val propertyNameRef = JsNameRef(propertyName, delegateRef)
                JsAstUtils.assignment(propertyNameRef, defaultParameterRef)
            }

            jsFunction.getParameters().add(defaultParameter)
            jsFunction.setBody(JsBlock(setExpression.makeStmt()))
            return jsFunction
        }

        fun generateDelegateAccessor(accessorDescriptor: PropertyAccessorDescriptor, function: JsFunction): JsPropertyInitializer =
                translateFunctionAsEcma5PropertyDescriptor(function, accessorDescriptor, context())

        fun generateDelegateGetter(): JsPropertyInitializer {
            val getterDescriptor = descriptor.getGetter() ?: throw IllegalStateException("Getter descriptor should not be null")
            return generateDelegateAccessor(getterDescriptor, generateDelegateGetterFunction(getterDescriptor))
        }

        fun generateDelegateSetter(): JsPropertyInitializer {
            val setterDescriptor = descriptor.getSetter() ?: throw IllegalStateException("Setter descriptor should not be null")
            return generateDelegateAccessor(setterDescriptor, generateDelegateSetterFunction(setterDescriptor))
        }

        properties.addGetterAndSetter(descriptor, context(), ::generateDelegateGetter, ::generateDelegateSetter
        )
    }


    private fun generateDelegateCallForFunctionMember(
            descriptor: FunctionDescriptor,
            overriddenDescriptor: FunctionDescriptor,
            delegateName: String,
            properties: MutableList<JsPropertyInitializer>
    ) {
        val delegateMemberFunctionName = context().getNameForDescriptor(descriptor)
        val overriddenMemberFunctionName = context().getNameForDescriptor(overriddenDescriptor)
        val delegateRefName = context().getScopeForDescriptor(descriptor).declareName(delegateName)
        val delegateRef = JsNameRef(delegateRefName, JsLiteral.THIS)
        val overriddenMemberFunctionRef = JsNameRef(overriddenMemberFunctionName, delegateRef)

        val parameters = SmartList<JsParameter>()
        val args = SmartList<JsExpression>()
        val functionScope = context().getScopeForDescriptor(descriptor);

        if (JsDescriptorUtils.isExtension(descriptor)) {
            val extensionFunctionReceiverName = functionScope.declareName(Namer.getReceiverParameterName())
            parameters.add(JsParameter(extensionFunctionReceiverName))
            args.add(JsNameRef(extensionFunctionReceiverName))
        }

        for (param in descriptor.getValueParameters()) {
            val paramName = param.getName().asString()
            val jsParamName = functionScope.declareName(paramName)
            parameters.add(JsParameter(jsParamName))
            args.add(JsNameRef(jsParamName))
        }

        val functionObject = simpleReturnFunction(context().getScopeForDescriptor(descriptor), JsInvocation(overriddenMemberFunctionRef, args))
        functionObject.getParameters().addAll(parameters)
        properties.add(JsPropertyInitializer(delegateMemberFunctionName.makeRef(), functionObject))
    }
}
