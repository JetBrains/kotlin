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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.expression.ExpressionVisitor
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCompanionCodegen
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class SerializableCompanionJsTranslator(
    declaration: ClassDescriptor,
    val translator: DeclarationBodyVisitor,
    val context: TranslationContext
): SerializableCompanionCodegen(declaration, context.bindingContext()) {

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) {
        val f = context.buildFunction(methodDescriptor) {jsFun, context ->
            val serializer = serializableDescriptor.classSerializer!!
            val stmt: JsExpression = when {
                serializer.kind == ClassKind.OBJECT -> context.serializerObjectGetter(serializer)
                serializer.isSerializerWhichRequiersKClass() -> JsNew(
                    context.translateQualifiedReference(serializer),
                    listOf(ExpressionVisitor.getObjectKClass(context, serializableDescriptor))
                )
                else -> {
                    val args = jsFun.parameters.map { JsNameRef(it.name) }
                    val ref = context.getInnerNameForDescriptor(
                        requireNotNull(
                            KSerializerDescriptorResolver.findSerializerConstructorForTypeArgumentsSerializers(serializer)
                        ) { "Generated serializer does not have constructor with required number of arguments" })
                    JsInvocation(ref.makeRef(), args)
                }
            }
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