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

package org.jetbrains.k2js.translate.declaration.propertyTranslator

import com.google.dart.compiler.backend.js.ast.*
import com.intellij.util.SmartList
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.k2js.translate.callTranslator.CallTranslator
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.general.AbstractTranslator
import org.jetbrains.k2js.translate.general.Translation
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils

import org.jetbrains.k2js.translate.context.Namer.getDelegateNameRef
import org.jetbrains.k2js.translate.utils.TranslationUtils.*

/**
 * Translates single property /w accessors.
 */

public fun translateAccessors(
        descriptor: PropertyDescriptor,
        declaration: JetProperty?,
        result: MutableList<JsPropertyInitializer>,
        context: TranslationContext
) {
    if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor)) {
        PropertyTranslator(descriptor, declaration, context).translate(result)
    }
}

public fun translateAccessors(
        descriptor: PropertyDescriptor,
        result: MutableList<JsPropertyInitializer>,
        context: TranslationContext
) {
    translateAccessors(descriptor, null, result, context)
}

public fun MutableList<JsPropertyInitializer>.addGetterAndSetter(
        descriptor: PropertyDescriptor,
        context: TranslationContext,
        generateGetter: () -> JsPropertyInitializer,
        generateSetter: () -> JsPropertyInitializer
) {
    val to: MutableList<JsPropertyInitializer>
    if (!JsDescriptorUtils.isExtension(descriptor)) {
        to = SmartList<JsPropertyInitializer>()
        this.add(JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), JsObjectLiteral(to, true)))
    }
    else {
        to = this
    }

    to.add(generateGetter())
    if (descriptor.isVar()) {
        to.add(generateSetter())
    }
}

private class PropertyTranslator(
        val descriptor: PropertyDescriptor,
        val declaration: JetProperty?,
        context: TranslationContext
) : AbstractTranslator(context) {

    private val propertyName: String = descriptor.getName().asString()

    fun translate(result: MutableList<JsPropertyInitializer>) {
        result.addGetterAndSetter(descriptor, context(), { generateGetter() }, { generateSetter() })
    }

    private fun generateGetter(): JsPropertyInitializer =
            if (hasCustomGetter()) translateCustomAccessor(getCustomGetterDeclaration()) else generateDefaultGetter()

    private fun generateSetter(): JsPropertyInitializer =
            if (hasCustomSetter()) translateCustomAccessor(getCustomSetterDeclaration()) else generateDefaultSetter()

    private fun hasCustomGetter() = declaration?.getGetter() != null && getCustomGetterDeclaration().hasBody()

    private fun hasCustomSetter() = declaration?.getSetter() != null && getCustomSetterDeclaration().hasBody()

    private fun getCustomGetterDeclaration(): JetPropertyAccessor =
            declaration?.getGetter() ?:
                    throw IllegalStateException("declaration and getter should not be null descriptor=${descriptor} declaration=${declaration}")

    private fun getCustomSetterDeclaration(): JetPropertyAccessor =
            declaration?.getSetter() ?:
                    throw IllegalStateException("declaration and setter should not be null descriptor=${descriptor} declaration=${declaration}")

    private fun generateDefaultGetter(): JsPropertyInitializer {
        val getterDescriptor = descriptor.getGetter() ?: throw IllegalStateException("Getter descriptor should not be null")
        return generateDefaultAccessor(getterDescriptor, generateDefaultGetterFunction(getterDescriptor))
    }

    private fun generateDefaultGetterFunction(getterDescriptor: PropertyGetterDescriptor): JsFunction {
        val delegatedCall = bindingContext().get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor)

        val value = if (delegatedCall != null)
            CallTranslator.translate(context(), delegatedCall, getDelegateNameRef(propertyName))
        else
            backingFieldReference(context(), this.descriptor)

        return simpleReturnFunction(context().getScopeForDescriptor(getterDescriptor.getContainingDeclaration()), value)
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
        val contextWithAliased = context().innerContextWithAliased(valueParameterDescriptor, defaultParameterRef)

        val delegatedCall = bindingContext().get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, setterDescriptor)

        val setExpression = if (delegatedCall != null)
            CallTranslator.translate(contextWithAliased, delegatedCall, getDelegateNameRef(propertyName))
        else
            assignmentToBackingField(contextWithAliased, descriptor, defaultParameterRef)

        jsFunction.getParameters().add(defaultParameter)
        jsFunction.setBody(JsBlock(setExpression.makeStmt()))
        return jsFunction
    }

    private fun generateDefaultAccessor(accessorDescriptor: PropertyAccessorDescriptor, function: JsFunction): JsPropertyInitializer =
            translateFunctionAsEcma5PropertyDescriptor(function, accessorDescriptor, context())

    private fun translateCustomAccessor(expression: JetPropertyAccessor): JsPropertyInitializer =
            Translation.functionTranslator(expression, context()).translateAsEcma5PropertyDescriptor()
}
