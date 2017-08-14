/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlinx.serialization.compiler.backend.js

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.ClassTranslator
import org.jetbrains.kotlin.js.translate.utils.addFunctionToPrototype
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializerCodegen
import org.jetbrains.kotlinx.serialization.compiler.resolve.getSerializableClassDescriptorBySerializer

/**
 *  @author Leonid Startsev
 *          sandwwraith@gmail.com
 */

class SerializerJsTranslator(declaration: KtPureClassOrObject,
                             val translator: ClassTranslator,
                             val context: TranslationContext) : SerializerCodegen(declaration, context.bindingContext()) {

    protected fun generateFunction(descriptor: FunctionDescriptor, bodyGen: () -> JsBlock) {
        val functionObject = context.createRootScopedFunction(descriptor)
        val containingClass = descriptor.containingDeclaration as ClassDescriptor
        context.addDeclarationStatement(context.addFunctionToPrototype(containingClass, descriptor, functionObject))
        val stmts = bodyGen().statements
        functionObject.body.statements += stmts
    }

    override fun generateSerialDesc() {
        // todo
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        // todo
    }

    override fun generateSave(function: FunctionDescriptor) {
        generateFunction(function) {
            JsBlock()
        }
    }


    override fun generateLoad(function: FunctionDescriptor) {
        // todo
    }

    companion object {
        fun translate(declaration: KtPureClassOrObject, descriptor: ClassDescriptor, translator: ClassTranslator, context: TranslationContext) {
            if (getSerializableClassDescriptorBySerializer(descriptor) != null)
                    SerializerJsTranslator(declaration, translator, context).generate()
        }
    }
}