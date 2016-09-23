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

package org.jetbrains.kotlin.js.translate.declaration


import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.simpleReturnFunction
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.translateFunctionAsEcma5PropertyDescriptor
import org.jetbrains.kotlin.js.translate.utils.generateDelegateCall
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.resolve.DescriptorUtils

class DelegationTranslator(
        classDeclaration: KtClassOrObject,
        context: TranslationContext
) : AbstractTranslator(context) {

    private val classDescriptor: ClassDescriptor =
            BindingUtils.getClassDescriptor(context.bindingContext(), classDeclaration)

    private val delegationBySpecifiers =
            classDeclaration.getSuperTypeListEntries().filterIsInstance<KtDelegatedSuperTypeEntry>()

    private class Field (val name: JsName, val generateField: Boolean)
    private val fields = mutableMapOf<KtDelegatedSuperTypeEntry, Field>()

    init {
        for (specifier in delegationBySpecifiers) {
            val expression = specifier.delegateExpression ?:
                             throw IllegalArgumentException("delegate expression should not be null: ${specifier.text}")
            val descriptor = getSuperClass(specifier)
            val propertyDescriptor = CodegenUtil.getDelegatePropertyIfAny(expression, classDescriptor, bindingContext())

            if (CodegenUtil.isFinalPropertyWithBackingField(propertyDescriptor, bindingContext())) {
                val delegateName = context.getNameForDescriptor(propertyDescriptor!!)
                fields[specifier] = Field(delegateName, false)
            }
            else {
                val classFqName = DescriptorUtils.getFqName(classDescriptor)
                val idForMangling = classFqName.asString()
                val suggestedName = NameSuggestion.getStableMangledName(Namer.getDelegatePrefix(), idForMangling)
                val delegateName = context.getScopeForDescriptor(classDescriptor).declareFreshName("${suggestedName}_0")
                fields[specifier] = Field(delegateName, true)
            }
        }
    }

    fun addInitCode(statements: MutableList<JsStatement>) {
        for (specifier in delegationBySpecifiers) {
            val field = fields[specifier]!!
            if (field.generateField) {
                val expression = specifier.delegateExpression!!
                val context = context().innerBlock()
                val delegateInitExpr = Translation.translateAsExpression(expression, context)
                statements += context.dynamicContext().jsBlock().statements
                val lhs = JsAstUtils.pureFqn(field.name, JsLiteral.THIS)
                statements += JsAstUtils.assignment(lhs, delegateInitExpr).makeStmt()
            }
        }
    }

    fun generateDelegated(properties: MutableList<JsPropertyInitializer>) {
        for (specifier in delegationBySpecifiers) {
            generateDelegates(getSuperClass(specifier), fields[specifier]!!, properties)
        }
    }

    private fun getSuperClass(specifier: KtSuperTypeListEntry): ClassDescriptor =
        CodegenUtil.getSuperClassBySuperTypeListEntry(specifier, bindingContext())

    private fun generateDelegates(toClass: ClassDescriptor, field: Field, properties: MutableList<JsPropertyInitializer>) {
        for ((descriptor, overriddenDescriptor) in CodegenUtil.getDelegates(classDescriptor, toClass)) {
            when (descriptor) {
                is PropertyDescriptor ->
                    generateDelegateCallForPropertyMember(descriptor, field.name, properties)
                is FunctionDescriptor ->
                    generateDelegateCallForFunctionMember(descriptor, overriddenDescriptor as FunctionDescriptor, field.name, properties)
                else ->
                    throw IllegalArgumentException("Expected property or function $descriptor")
            }
        }
    }

    private fun generateDelegateCallForPropertyMember(
            descriptor: PropertyDescriptor,
            delegateName: JsName,
            properties: MutableList<JsPropertyInitializer>
    ) {
        val propertyName: String = descriptor.name.asString()

        fun generateDelegateGetterFunction(getterDescriptor: PropertyGetterDescriptor): JsFunction {
            val delegateRef = JsNameRef(delegateName, JsLiteral.THIS)

            val returnExpression: JsExpression = if (DescriptorUtils.isExtension(descriptor)) {
                val getterName = context().getNameForDescriptor(getterDescriptor)
                val receiver = Namer.getReceiverParameterName()
                JsInvocation(JsNameRef(getterName, delegateRef), JsNameRef(receiver))
            }
            else {
                JsNameRef(propertyName, delegateRef)
            }

            val jsFunction = simpleReturnFunction(context().getScopeForDescriptor(getterDescriptor.containingDeclaration), returnExpression)
            if (DescriptorUtils.isExtension(descriptor)) {
                val receiverName = jsFunction.scope.declareName(Namer.getReceiverParameterName())
                jsFunction.parameters.add(JsParameter(receiverName))
            }
            return jsFunction
        }

        fun generateDelegateSetterFunction(setterDescriptor: PropertySetterDescriptor): JsFunction {
            val jsFunction = JsFunction(context().program().rootScope,
                                        "setter for " + setterDescriptor.name.asString())

            assert(setterDescriptor.valueParameters.size == 1) { "Setter must have 1 parameter" }
            val defaultParameter = JsParameter(jsFunction.scope.declareTemporary())
            val defaultParameterRef = defaultParameter.name.makeRef()

            val delegateRef = JsNameRef(delegateName, JsLiteral.THIS)

            // TODO: remove explicit type annotation when Kotlin compiler works this out
            val setExpression: JsExpression = if (DescriptorUtils.isExtension(descriptor)) {
                val setterName = context().getNameForDescriptor(setterDescriptor)
                val setterNameRef = JsNameRef(setterName, delegateRef)
                val extensionFunctionReceiverName = jsFunction.scope.declareName(Namer.getReceiverParameterName())
                jsFunction.parameters.add(JsParameter(extensionFunctionReceiverName))
                JsInvocation(setterNameRef, JsNameRef(extensionFunctionReceiverName), defaultParameterRef)
            }
            else {
                val propertyNameRef = JsNameRef(propertyName, delegateRef)
                JsAstUtils.assignment(propertyNameRef, defaultParameterRef)
            }

            jsFunction.parameters.add(defaultParameter)
            jsFunction.body = JsBlock(setExpression.makeStmt())
            return jsFunction
        }

        fun generateDelegateAccessor(accessorDescriptor: PropertyAccessorDescriptor, function: JsFunction): JsPropertyInitializer =
                translateFunctionAsEcma5PropertyDescriptor(function, accessorDescriptor, context())

        fun generateDelegateGetter(): JsPropertyInitializer {
            val getterDescriptor = descriptor.getter ?: throw IllegalStateException("Getter descriptor should not be null")
            return generateDelegateAccessor(getterDescriptor, generateDelegateGetterFunction(getterDescriptor))
        }

        fun generateDelegateSetter(): JsPropertyInitializer {
            val setterDescriptor = descriptor.setter ?: throw IllegalStateException("Setter descriptor should not be null")
            return generateDelegateAccessor(setterDescriptor, generateDelegateSetterFunction(setterDescriptor))
        }

        properties.addGetterAndSetter(descriptor, context(), ::generateDelegateGetter, ::generateDelegateSetter)
    }


    private fun generateDelegateCallForFunctionMember(
            descriptor: FunctionDescriptor,
            overriddenDescriptor: FunctionDescriptor,
            delegateName: JsName,
            properties: MutableList<JsPropertyInitializer>
    ) {
        val delegateRef = JsNameRef(delegateName, JsLiteral.THIS)
        properties.add(generateDelegateCall(descriptor, overriddenDescriptor, delegateRef, context()));
    }
}
