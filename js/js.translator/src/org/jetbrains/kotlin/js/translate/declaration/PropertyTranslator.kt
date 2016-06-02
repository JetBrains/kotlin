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
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.Namer.getDelegateNameRef
import org.jetbrains.kotlin.js.translate.context.Namer.getReceiverParameterName
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.assignmentToBackingField
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.backingFieldReference
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.translateFunctionAsEcma5PropertyDescriptor
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.addParameter
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.addStatement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

/**
 * Translates single property /w accessors.
 */

fun translateAccessors(
        descriptor: PropertyDescriptor,
        declaration: KtProperty?,
        result: MutableList<JsPropertyInitializer>,
        context: TranslationContext
) {
    if (descriptor.modality == Modality.ABSTRACT || JsDescriptorUtils.isSimpleFinalProperty(descriptor)) return

    PropertyTranslator(descriptor, declaration, context).translate(result)
}

fun translateAccessors(
        descriptor: PropertyDescriptor,
        result: MutableList<JsPropertyInitializer>,
        context: TranslationContext
) {
    translateAccessors(descriptor, null, result, context)
}

fun MutableList<JsPropertyInitializer>.addGetterAndSetter(
        descriptor: PropertyDescriptor,
        context: TranslationContext,
        generateGetter: () -> JsPropertyInitializer,
        generateSetter: () -> JsPropertyInitializer
) {
    val to: MutableList<JsPropertyInitializer>
    if (!descriptor.isExtension && !TranslationUtils.shouldGenerateAccessors(descriptor)) {
        to = SmartList<JsPropertyInitializer>()
        this.add(JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), JsObjectLiteral(to, true)))
    }
    else {
        to = this
    }

    to.add(generateGetter())
    if (descriptor.isVar) {
        to.add(generateSetter())
    }
}

private class PropertyTranslator(
        val descriptor: PropertyDescriptor,
        val declaration: KtProperty?,
        context: TranslationContext
) : AbstractTranslator(context) {

    private val propertyName: String = descriptor.name.asString()

    fun translate(result: MutableList<JsPropertyInitializer>) {
        result.addGetterAndSetter(descriptor, context(), { generateGetter() }, { generateSetter() })
    }

    private fun generateGetter(): JsPropertyInitializer =
            if (hasCustomGetter()) translateCustomAccessor(getCustomGetterDeclaration()) else generateDefaultGetter()

    private fun generateSetter(): JsPropertyInitializer =
            if (hasCustomSetter()) translateCustomAccessor(getCustomSetterDeclaration()) else generateDefaultSetter()

    private fun hasCustomGetter() = declaration?.getter != null && getCustomGetterDeclaration().hasBody()

    private fun hasCustomSetter() = declaration?.setter != null && getCustomSetterDeclaration().hasBody()

    private fun getCustomGetterDeclaration(): KtPropertyAccessor =
            declaration?.getter ?:
            throw IllegalStateException("declaration and getter should not be null descriptor=$descriptor declaration=$declaration")

    private fun getCustomSetterDeclaration(): KtPropertyAccessor =
            declaration?.setter ?:
            throw IllegalStateException("declaration and setter should not be null descriptor=$descriptor declaration=$declaration")

    private fun generateDefaultGetter(): JsPropertyInitializer {
        val getterDescriptor = descriptor.getter ?: throw IllegalStateException("Getter descriptor should not be null")
        return generateDefaultAccessor(getterDescriptor, generateDefaultGetterFunction(getterDescriptor))
    }

    private fun generateDefaultGetterFunction(getterDescriptor: PropertyGetterDescriptor): JsFunction {
        val delegatedCall = bindingContext().get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor)

        if (delegatedCall != null) {
            return generateDelegatedGetterFunction(getterDescriptor, delegatedCall)
        }

        assert(!descriptor.isExtension) { "Unexpected extension property $descriptor}" }
        val scope = context().getScopeForDescriptor(getterDescriptor.containingDeclaration)
        val result = backingFieldReference(context(), descriptor)
        val body = JsBlock(JsReturn(result))

        return JsFunction(scope, body, accessorDescription(getterDescriptor))
    }

    private fun generateDelegatedGetterFunction(
            getterDescriptor: PropertyGetterDescriptor,
            delegatedCall: ResolvedCall<FunctionDescriptor>
    ): JsFunction {
        val scope = context().getScopeForDescriptor(getterDescriptor.containingDeclaration)
        val function = JsFunction(scope, JsBlock(), accessorDescription(getterDescriptor))

        val delegateRef = getDelegateNameRef(propertyName)
        val delegatedJsCall = CallTranslator.translate(
                contextWithPropertyMetadataCreationIntrinsified(context(), delegatedCall, getterDescriptor), delegatedCall, delegateRef
        )

        if (getterDescriptor.isExtension) {
            val receiver = function.addParameter(getReceiverParameterName()).name
            val arguments = (delegatedJsCall as JsInvocation).arguments
            arguments[0] = receiver.makeRef()
        }

        val returnResult = JsReturn(delegatedJsCall)
        function.addStatement(returnResult)
        return function
    }

    private fun contextWithPropertyMetadataCreationIntrinsified(
            context: TranslationContext, delegatedCall: ResolvedCall<FunctionDescriptor>, accessor: PropertyAccessorDescriptor
    ): TranslationContext {
        val propertyNameLiteral = context.program().getStringLiteral(accessor.correspondingProperty.name.asString())
        // 0th argument is instance, 1st is KProperty, 2nd (for setter) is value
        val fakeArgumentExpression =
                (delegatedCall.valueArgumentsByIndex!![1] as ExpressionValueArgument).valueArgument!!.getArgumentExpression()
        return context.innerContextWithAliasesForExpressions(mapOf(
                fakeArgumentExpression to JsNew(pureFqn("PropertyMetadata", Namer.kotlinObject()), listOf(propertyNameLiteral))
        ))
    }

    private fun generateDefaultSetter(): JsPropertyInitializer {
        val setterDescriptor = descriptor.setter ?: throw IllegalStateException("Setter descriptor should not be null")
        return generateDefaultAccessor(setterDescriptor, generateDefaultSetterFunction(setterDescriptor))
    }

    private fun generateDefaultSetterFunction(setterDescriptor: PropertySetterDescriptor): JsFunction {
        val containingScope = context().getScopeForDescriptor(setterDescriptor.containingDeclaration)
        val function = JsFunction(containingScope, JsBlock(), accessorDescription(setterDescriptor))

        assert(setterDescriptor.valueParameters.size == 1) { "Setter must have 1 parameter" }
        val correspondingPropertyName = setterDescriptor.correspondingProperty.name.asString()
        val valueParameter = function.addParameter(correspondingPropertyName).name
        val withAliased = context().innerContextWithAliased(setterDescriptor.valueParameters[0], valueParameter.makeRef())
        val delegatedCall = context().bindingContext().get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, setterDescriptor)

        if (delegatedCall != null) {
            val delegatedJsCall = CallTranslator.translate(
                    contextWithPropertyMetadataCreationIntrinsified(withAliased, delegatedCall, setterDescriptor),
                    delegatedCall, getDelegateNameRef(correspondingPropertyName)
            )
            function.addStatement(delegatedJsCall.makeStmt())

            if (setterDescriptor.isExtension) {
                val receiver = function.addParameter(getReceiverParameterName(), 0).name
                (delegatedJsCall as JsInvocation).arguments[0] = receiver.makeRef()
            }
        }
        else {
            assert(!descriptor.isExtension) { "Unexpected extension property $descriptor}" }
            val assignment = assignmentToBackingField(withAliased, descriptor, valueParameter.makeRef())
            function.addStatement(assignment.makeStmt())
        }

        return function
    }

    private fun generateDefaultAccessor(accessorDescriptor: PropertyAccessorDescriptor, function: JsFunction): JsPropertyInitializer =
            translateFunctionAsEcma5PropertyDescriptor(function, accessorDescriptor, context())

    private fun translateCustomAccessor(expression: KtPropertyAccessor): JsPropertyInitializer =
            Translation.functionTranslator(expression, context()).translateAsEcma5PropertyDescriptor()

    private fun accessorDescription(accessorDescriptor: PropertyAccessorDescriptor): String {
        val accessorType =
                when(accessorDescriptor) {
                    is PropertyGetterDescriptor ->
                        "getter"
                    is PropertySetterDescriptor ->
                        "setter"
                    else ->
                        throw IllegalArgumentException("Unknown accessor type ${accessorDescriptor.javaClass}")
                }

        val name = accessorDescriptor.name.asString()
        return "$accessorType for $name"
    }
}
