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

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.anonymousInitializers
import org.jetbrains.kotlinx.serialization.compiler.backend.common.bodyPropertiesDescriptorsMap
import org.jetbrains.kotlinx.serialization.compiler.backend.common.primaryPropertiesDescriptorsMap
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.getClassFromSerializationPackage
import org.jetbrains.kotlinx.serialization.compiler.resolve.isInternalSerializable

class SerializableJsTranslator(
    val declaration: KtPureClassOrObject,
    val descriptor: ClassDescriptor,
    val translator: DeclarationBodyVisitor,
    val context: TranslationContext
) : SerializableCodegen(descriptor, context.bindingContext()) {

    private val initMap: Map<PropertyDescriptor, KtExpression?> = declaration.run {
        (bodyPropertiesDescriptorsMap(context.bindingContext()).mapValues { it.value.delegateExpressionOrInitializer } +
                primaryPropertiesDescriptorsMap(context.bindingContext()).mapValues { it.value.defaultValue })
    }

    override fun generateInternalConstructor(constructorDescriptor: ClassConstructorDescriptor) {

        val missingExceptionClassRef = serializableDescriptor.getClassFromSerializationPackage(MISSING_FIELD_EXC)
            .let { context.translateQualifiedReference(it) }

        val f = context.buildFunction(constructorDescriptor) { jsFun, context ->
            val thiz = jsFun.scope.declareName(Namer.ANOTHER_THIS_PARAMETER_NAME).makeRef()
            @Suppress("NAME_SHADOWING")
            val context = context.innerContextWithAliased(serializableDescriptor.thisAsReceiverParameter, thiz)

            +JsVars(
                JsVars.JsVar(
                    thiz.name,
                    Namer.createObjectWithPrototypeFrom(context.getInnerNameForDescriptor(serializableDescriptor).makeRef())
                )
            )
            val seenVar = jsFun.parameters[0].name.makeRef()
            for ((index, prop) in properties.serializableProperties.withIndex()) {
                val paramRef = jsFun.parameters[index + 1].name.makeRef()
                // assign this.a = a in else branch
                val assignParamStmt = TranslationUtils.assignmentToBackingField(context, prop.descriptor, paramRef).makeStmt()

                val ifNotSeenStmt: JsStatement = if (prop.optional) {
                    val initializer = initMap.getValue(prop.descriptor) ?: throw IllegalArgumentException("optional without an initializer")
                    val initExpr = Translation.translateAsExpression(initializer, context)
                    TranslationUtils.assignmentToBackingField(context, prop.descriptor, initExpr).makeStmt()
                } else {
                    JsThrow(JsNew(missingExceptionClassRef, listOf(JsStringLiteral(prop.name))))
                }
                // (seen & 1 << i == 0) -- not seen
                val notSeenTest = propNotSeenTest(seenVar, index)
                +JsIf(notSeenTest, ifNotSeenStmt, assignParamStmt)
            }

            //transient initializers and init blocks
            val serialDescs = properties.serializableProperties.map { it.descriptor }
            (initMap - serialDescs).forEach { (desc, expr) ->
                val e = requireNotNull(expr) { "transient without an initializer" }
                val initExpr = Translation.translateAsExpression(e, context)
                +TranslationUtils.assignmentToBackingField(context, desc, initExpr).makeStmt()
            }

            declaration.anonymousInitializers()
                .forEach { Translation.translateAsExpression(it, context, this.block) }

            +JsReturn(thiz)
        }

        f.name = context.getInnerNameForDescriptor(constructorDescriptor)
        context.addDeclarationStatement(f.makeStmt())
    }

    override fun generateWriteSelfMethod(methodDescriptor: FunctionDescriptor) {
        // no-op yet
    }

    companion object {
        fun translate(
            declaration: KtPureClassOrObject,
            descriptor: ClassDescriptor,
            translator: DeclarationBodyVisitor,
            context: TranslationContext
        ) {
            if (descriptor.isInternalSerializable)
                SerializableJsTranslator(declaration, descriptor, translator, context).generate()
        }
    }
}