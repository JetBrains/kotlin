/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.general.TranslatorVisitor
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty

abstract class AbstractDeclarationVisitor : TranslatorVisitor<Unit>()  {
    override fun emptyResult(context: TranslationContext) { }

    override fun visitClassOrObject(classOrObject: KtClassOrObject, context: TranslationContext) {
        ClassTranslator.translate(classOrObject, context)
        val descriptor = BindingUtils.getClassDescriptor(context.bindingContext(), classOrObject)
        addClass(descriptor)
    }

    override fun visitProperty(expression: KtProperty, context: TranslationContext) {
        val descriptor = BindingUtils.getPropertyDescriptor(context.bindingContext(), expression)
        if (descriptor.modality === Modality.ABSTRACT) return

        val propertyContext = context.newDeclaration(descriptor)

        val defaultTranslator = DefaultPropertyTranslator(descriptor, context, getBackingFieldReference(descriptor))
        val getter = descriptor.getter!!
        val getterExpr = if (expression.hasCustomGetter()) {
            translateFunction(getter, expression.getter!!, propertyContext)
        }
        else {
            val function = context.getFunctionObject(getter)
            defaultTranslator.generateDefaultGetterFunction(getter, function)
            function
        }

        val setterExpr = if (descriptor.isVar) {
            val setter = descriptor.setter!!
            if (expression.hasCustomSetter()) {
                translateFunction(setter, expression.setter!!, propertyContext)
            }
            else {
                val function = context.getFunctionObject(setter)
                defaultTranslator.generateDefaultSetterFunction(setter, function)
                function
            }
        }
        else {
            null
        }

        if (TranslationUtils.shouldAccessViaFunctions(descriptor) || descriptor.isExtensionProperty) {
            addFunction(descriptor.getter!!, getterExpr)
            descriptor.setter?.let { addFunction(it, setterExpr!!) }
        }
        else {
            addProperty(descriptor, getterExpr, setterExpr)
        }
    }

    override fun visitNamedFunction(expression: KtNamedFunction, context: TranslationContext) {
        val descriptor = BindingUtils.getFunctionDescriptor(context.bindingContext(), expression)
        if (descriptor.modality === Modality.ABSTRACT) return

        addFunction(descriptor, translateFunction(descriptor, expression, context))
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias, data: TranslationContext?) {}

    private fun translateFunction(
            descriptor: FunctionDescriptor,
            expression: KtDeclarationWithBody,
            context: TranslationContext
    ): JsExpression {
        val innerContext = context.newDeclaration(descriptor)
        innerContext.getInnerNameForDescriptor(descriptor)
        val function = context.getFunctionObject(descriptor)
        return Translation.functionTranslator(expression, innerContext, function).translateAsMethod()
    }

    protected abstract fun addClass(descriptor: ClassDescriptor)

    protected abstract fun addFunction(
            descriptor: FunctionDescriptor,
            expression: JsExpression
    )

    protected abstract fun addProperty(
            descriptor: PropertyDescriptor,
            getter: JsExpression,
            setter: JsExpression?
    )

    protected abstract fun getBackingFieldReference(descriptor: PropertyDescriptor): JsExpression
}