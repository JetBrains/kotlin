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
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsReturn
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCompanionCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.getSerializableClassDescriptorByCompanion
import org.jetbrains.kotlinx.serialization.compiler.resolve.shouldHaveGeneratedMethodsInCompanion
import org.jetbrains.kotlinx.serialization.compiler.resolve.toSimpleType

class SerializableCompanionJsTranslator(
    declaration: ClassDescriptor,
    val translator: DeclarationBodyVisitor,
    val context: TranslationContext
) : SerializableCompanionCodegen(declaration, context.bindingContext()) {

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) {
        val f = context.buildFunction(methodDescriptor) { jsFun, context ->
            val serializer = requireNotNull(
                findTypeSerializer(
                    serializableDescriptor.module,
                    serializableDescriptor.toSimpleType()
                )
            )
            val args = jsFun.parameters.map { JsNameRef(it.name) }
            val stmt =
                requireNotNull(
                    serializerInstance(
                        context,
                        serializer,
                        serializableDescriptor.module,
                        serializableDescriptor.defaultType,
                        genericGetter = { it, _ ->
                            args[it]
                        })
                )
            +JsReturn(stmt)
        }
        translator.addFunction(methodDescriptor, f, null)
    }

    companion object {
        fun translate(declaration: KtPureClassOrObject, descriptor: ClassDescriptor, translator: DeclarationBodyVisitor, context: TranslationContext) {
            val serializableClass = getSerializableClassDescriptorByCompanion(descriptor) ?: return
            if (serializableClass.shouldHaveGeneratedMethodsInCompanion)
                SerializableCompanionJsTranslator(descriptor, translator, context).generate()
        }
    }
}