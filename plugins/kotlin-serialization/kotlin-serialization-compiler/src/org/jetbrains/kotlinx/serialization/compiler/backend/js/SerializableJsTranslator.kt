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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.anonymousInitializers
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.serializableAnnotationIsUseless
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC

class SerializableJsTranslator(
    val declaration: KtPureClassOrObject,
    val descriptor: ClassDescriptor,
    val context: TranslationContext
) : SerializableCodegen(descriptor, context.bindingContext()) {

    private val initMap: Map<PropertyDescriptor, KtExpression?> = context.buildInitializersRemapping(declaration, descriptor.getSuperClassNotAny())

    override fun generateInternalConstructor(constructorDescriptor: ClassConstructorDescriptor) {

        val missingExceptionClassRef = serializableDescriptor.getClassFromSerializationPackage(MISSING_FIELD_EXC)
            .let { context.translateQualifiedReference(it) }

        val f = context.buildFunction(constructorDescriptor) { jsFun, context ->
            val thiz = jsFun.scope.declareName(Namer.ANOTHER_THIS_PARAMETER_NAME).makeRef()
            @Suppress("NAME_SHADOWING")
            val context = context.innerContextWithAliased(serializableDescriptor.thisAsReceiverParameter, thiz)

            // use serializationConstructorMarker for passing "this" from inheritors to base class
            val markerAsThis = jsFun.parameters.last().name.makeRef()

            +JsVars(
                JsVars.JsVar(
                    thiz.name,
                    JsAstUtils.or(
                        markerAsThis,
                        Namer.createObjectWithPrototypeFrom(context.getInnerNameForDescriptor(serializableDescriptor).makeRef())
                    )
                )
            )
            val serializableProperties = properties.serializableProperties
            val seenVarsOffset = serializableProperties.bitMaskSlotCount()
            val seenVars = (0 until seenVarsOffset).map { jsFun.parameters[it].name.makeRef() }
            val superClass = serializableDescriptor.getSuperClassOrAny()
            var startPropOffset: Int = 0
            when {
                KotlinBuiltIns.isAny(superClass) -> { /* no=op */ }
                superClass.isInternalSerializable -> {
                    startPropOffset = generateSuperSerializableCall(
                        superClass,
                        jsFun.parameters.map { it.name.makeRef() },
                        thiz,
                        seenVarsOffset
                    )
                }
                else -> generateSuperNonSerializableCall(superClass, thiz)
            }

            for (index in startPropOffset until serializableProperties.size) {
                val prop = serializableProperties[index]
                val paramRef = jsFun.parameters[index + seenVarsOffset].name.makeRef()
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
                val notSeenTest = propNotSeenTest(seenVars[bitMaskSlotAt(index)], index)
                +JsIf(notSeenTest, ifNotSeenStmt, assignParamStmt)
            }

            //transient initializers and init blocks
            val serialDescs = serializableProperties.map { it.descriptor }
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
        context.export(constructorDescriptor)
    }

    private fun JsBlockBuilder.generateSuperNonSerializableCall(superClass: ClassDescriptor, thisParameter: JsExpression) {
        val suitableCtor = superClass.constructors.singleOrNull { it.valueParameters.size == 0 }
            ?: throw IllegalArgumentException("Non-serializable parent of serializable $serializableDescriptor must have no arg constructor")
        if (suitableCtor.isPrimary) {
            +JsInvocation(Namer.getFunctionCallRef(context.getInnerReference(superClass)), thisParameter).makeStmt()
        } else {
            +JsAstUtils.assignment(thisParameter, JsInvocation(context.getInnerReference(suitableCtor), thisParameter)).makeStmt()
        }
    }

    private fun JsBlockBuilder.generateSuperSerializableCall(
        superClass: ClassDescriptor,
        parameters: List<JsExpression>,
        thisParameter: JsExpression,
        propertiesStart: Int
    ): Int {
        val constrDesc = superClass.constructors.single(ClassConstructorDescriptor::isSerializationCtor)
        val constrRef = context.getInnerNameForDescriptor(constrDesc).makeRef()
        val superProperties = bindingContext.serializablePropertiesFor(superClass).serializableProperties
        val superSlots = superProperties.bitMaskSlotCount()
        val arguments = parameters.subList(0, superSlots) +
                parameters.subList(propertiesStart, propertiesStart + superProperties.size) +
                thisParameter // SerializationConstructorMarker
        +JsAstUtils.assignment(thisParameter, JsInvocation(constrRef, arguments)).makeStmt()
        return superProperties.size
    }

    override fun generateWriteSelfMethod(methodDescriptor: FunctionDescriptor) {
        // no-op yet
    }

    companion object {
        fun translate(
            declaration: KtPureClassOrObject,
            serializableClass: ClassDescriptor,
            translator: DeclarationBodyVisitor,
            context: TranslationContext
        ) {
            if (serializableClass.isInternalSerializable)
                SerializableJsTranslator(declaration, serializableClass, context).generate()
            else if (serializableClass.serializableAnnotationIsUseless) {
                throw CompilationException(
                    "@Serializable annotation on $serializableClass would be ignored because it is impossible to serialize it automatically. " +
                            "Provide serializer manually via e.g. companion object", null, serializableClass.findPsi()
                )
            }
        }
    }
}
