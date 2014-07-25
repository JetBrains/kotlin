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
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.k2js.translate.callTranslator.CallTranslator
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.general.AbstractTranslator
import org.jetbrains.k2js.translate.general.Translation
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.k2js.translate.utils.TranslationUtils

import org.jetbrains.k2js.translate.context.Namer.getDelegateNameRef
import org.jetbrains.k2js.translate.utils.TranslationUtils.*

/**
 * Translates single property /w accessors.
 */

public class PropertyTranslator private(private val descriptor: PropertyDescriptor, private val declaration: JetProperty?, private val delegateName: String?, context: TranslationContext) : AbstractTranslator(context) {

    class object {

        public fun translateAccessors(descriptor: PropertyDescriptor, result: MutableList<JsPropertyInitializer>, context: TranslationContext) {
            translateAccessors(descriptor, null, result, context)
        }

        public fun translateAccessors(descriptor: PropertyDescriptor, declaration: JetProperty?, result: MutableList<JsPropertyInitializer>, context: TranslationContext) {
            if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor))
                PropertyTranslator(descriptor, declaration, null, context).translate(result)
        }

        public fun translateAccessorsForDelegate(descriptor: PropertyDescriptor, delegateName: String?, result: MutableList<JsPropertyInitializer>, context: TranslationContext) {
            if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor))
                PropertyTranslator(descriptor, null, delegateName, context).translate(result)
        }
    }

    private fun translate(result: MutableList<JsPropertyInitializer>) {
        val to: MutableList<JsPropertyInitializer>
        if (!JsDescriptorUtils.isExtension(descriptor)) {
            to = SmartList<JsPropertyInitializer>()
            result.add(JsPropertyInitializer(context().getNameForDescriptor(descriptor).makeRef(), JsObjectLiteral(to, true)))
        }
        else
            to = result

        to.add(generateGetter())
        if (descriptor.isVar())
            to.add(generateSetter())
    }

    private fun generateGetter(): JsPropertyInitializer = if (hasCustomGetter()) translateCustomAccessor(getCustomGetterDeclaration()) else generateDefaultGetter()

    private fun generateSetter(): JsPropertyInitializer = if (hasCustomSetter()) translateCustomAccessor(getCustomSetterDeclaration()) else generateDefaultSetter()

    private fun hasCustomGetter(): Boolean = declaration != null && declaration.getGetter() != null && getCustomGetterDeclaration().hasBody()

    private fun hasCustomSetter(): Boolean = declaration != null && declaration.getSetter() != null && getCustomSetterDeclaration().hasBody()

    private fun getCustomGetterDeclaration(): JetPropertyAccessor =
            declaration?.getGetter() ?: throw IllegalStateException("declaration and getter should not be null descriptor=${descriptor} declaration=${declaration}")

    private fun getCustomSetterDeclaration(): JetPropertyAccessor =
            declaration?.getSetter() ?: throw IllegalStateException("declaration and setter should not be null descriptor=${descriptor} declaration=${declaration}")

    private fun generateDefaultGetter(): JsPropertyInitializer {
        val getterDescriptor = descriptor.getGetter() ?: throw IllegalStateException("Getter descriptor should not be null")
        return generateDefaultAccessor(getterDescriptor, generateDefaultGetterFunction(getterDescriptor))
    }

    private fun getPropertyName(): String = descriptor.getName().asString()

    private fun generateDefaultGetterFunction(getterDescriptor: PropertyGetterDescriptor): JsFunction {
        var value: JsExpression
        if (delegateName != null) {
            val propertyName = context().getNameForDescriptor(descriptor)
            val delegateRefName = context().getScopeForDescriptor(getterDescriptor).declareName(delegateName)
            val delegateRef = JsNameRef(delegateRefName, JsLiteral.THIS)
            value = JsNameRef(propertyName, delegateRef)
            if (JsDescriptorUtils.isExtension(descriptor)) {
                val getterName = context().getNameForDescriptor(getterDescriptor)
                val receiver = Namer.getReceiverParameterName()
                value = JsInvocation(JsNameRef(getterName, delegateRef), JsNameRef(receiver))
            }
        }
        else {
            val delegatedCall = bindingContext().get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor)
            value = if (delegatedCall != null)
                CallTranslator.translate(context(), delegatedCall, getDelegateNameRef(getPropertyName()))
            else
                backingFieldReference(context(), this.descriptor)
        }
        val jsFunction = simpleReturnFunction(context().getScopeForDescriptor(getterDescriptor.getContainingDeclaration()), value)
        if (delegateName != null && JsDescriptorUtils.isExtension(descriptor)) {
            val receiverName = jsFunction.getScope().declareName(Namer.getReceiverParameterName())
            jsFunction.getParameters().add(JsParameter(receiverName))
        }
        return jsFunction
    }

    private fun generateDefaultSetter(): JsPropertyInitializer {
        val setterDescriptor = descriptor.getSetter() ?: throw IllegalStateException("Setter descriptor should not be null")
        return generateDefaultAccessor(setterDescriptor, generateDefaultSetterFunction(setterDescriptor))
    }

    private fun generateDefaultSetterFunction(setterDescriptor: PropertySetterDescriptor): JsFunction {
        val jsFunction = JsFunction(context().getScopeForDescriptor(setterDescriptor.getContainingDeclaration()))

        assert(setterDescriptor.getValueParameters().size() == 1, "Setter must have 1 parameter")
        val valueParameterDescriptor = setterDescriptor.getValueParameters().get(0)
        val defaultParameter = JsParameter(jsFunction.getScope().declareTemporary())
        val defaultParameterRef = defaultParameter.getName().makeRef()

        val setExpression: JsExpression
        if (delegateName != null) {
            val delegateRefName = context().getScopeForDescriptor(setterDescriptor).declareName(delegateName)
            val delegateRef = JsNameRef(delegateRefName, JsLiteral.THIS)
            if (JsDescriptorUtils.isExtension(descriptor)) {
                val setterName = context().getNameForDescriptor(setterDescriptor)
                val setterNameRef = JsNameRef(setterName, delegateRef)
                val extensionFunctionReceiverName = jsFunction.getScope().declareName(Namer.getReceiverParameterName())
                jsFunction.getParameters().add(JsParameter(extensionFunctionReceiverName))
                setExpression = JsInvocation(setterNameRef, JsNameRef(extensionFunctionReceiverName), defaultParameterRef)
            }
            else {
                val propertyName = context().getNameForDescriptor(descriptor)
                val propertyNameRef = JsNameRef(propertyName, delegateRef)
                setExpression = JsAstUtils.assignment(propertyNameRef, defaultParameterRef)
            }
            jsFunction.getParameters().add(defaultParameter)
        }
        else {
            jsFunction.getParameters().add(defaultParameter)
            val contextWithAliased = context().innerContextWithAliased(valueParameterDescriptor, defaultParameterRef)

            val delegatedCall = bindingContext().get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, setterDescriptor)
            setExpression = if (delegatedCall != null)
                CallTranslator.translate(contextWithAliased, delegatedCall, getDelegateNameRef(getPropertyName()))
            else
                assignmentToBackingField(contextWithAliased, descriptor, defaultParameterRef)
        }
        jsFunction.setBody(JsBlock(setExpression.makeStmt()))
        return jsFunction
    }

    private fun generateDefaultAccessor(accessorDescriptor: PropertyAccessorDescriptor, function: JsFunction): JsPropertyInitializer =
        TranslationUtils.translateFunctionAsEcma5PropertyDescriptor(function, accessorDescriptor, context())

    private fun translateCustomAccessor(expression: JetPropertyAccessor): JsPropertyInitializer =
        Translation.functionTranslator(expression, context()).translateAsEcma5PropertyDescriptor()
}
