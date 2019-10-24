/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.js

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class SerializerForEnumsTranslator(
    descriptor: ClassDescriptor,
    translator: DeclarationBodyVisitor,
    context: TranslationContext
) : SerializerJsTranslator(descriptor, translator, context) {
    override fun generateSave(function: FunctionDescriptor) = generateFunction(function) { jsFun, ctx ->
        val encoderClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.ENCODER_CLASS)
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(anySerialDescProperty!!), JsThisRef())
        val ordinalProp = serializableDescriptor.unsubstitutedMemberScope.getContributedVariables(
            Name.identifier("ordinal"),
            NoLookupLocation.FROM_BACKEND
        ).single()
        val ordinalRef = JsNameRef(context.getNameForDescriptor(ordinalProp), JsNameRef(jsFun.parameters[1].name))
        val encodeEnumF = ctx.getNameForDescriptor(encoderClass.getFuncDesc(CallingConventions.encodeEnum).single())
        val call = JsInvocation(JsNameRef(encodeEnumF, JsNameRef(jsFun.parameters[0].name)), serialClassDescRef, ordinalRef)
        +call.makeStmt()
    }

    override fun generateLoad(function: FunctionDescriptor) = generateFunction(function) { jsFun, ctx ->
        val decoderClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.DECODER_CLASS)
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(anySerialDescProperty!!), JsThisRef())
        val decodeEnumF = ctx.getNameForDescriptor(decoderClass.getFuncDesc(CallingConventions.decodeEnum).single())
        val valuesFunc = DescriptorUtils.getFunctionByName(serializableDescriptor.staticScope, DescriptorUtils.ENUM_VALUES)
        val decodeEnumCall = JsInvocation(JsNameRef(decodeEnumF, JsNameRef(jsFun.parameters[0].name)), serialClassDescRef)
        val resultCall = JsArrayAccess(JsInvocation(ctx.getInnerNameForDescriptor(valuesFunc).makeRef()), decodeEnumCall)
        +JsReturn(resultCall)
    }

    override fun instantiateNewDescriptor(
        context: TranslationContext,
        correctThis: JsExpression,
        baseSerialDescImplClass: ClassDescriptor
    ): JsExpression {
        val serialDescForEnums = serializerDescriptor
            .getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_FOR_ENUM)
        val ctor = serialDescForEnums.unsubstitutedPrimaryConstructor!!
        return JsNew(
            context.getInnerReference(ctor),
            listOf(JsStringLiteral(serialName))
        )
    }

    override fun addElementsContentToDescriptor(
        context: TranslationContext,
        serialDescriptorInThis: JsNameRef,
        addElementFunction: FunctionDescriptor,
        pushAnnotationFunction: FunctionDescriptor
    ) {
        val enumEntries = serializableDescriptor.enumEntries()
        for (entry in enumEntries) {
            // regular .serialName() produces fqName here, which is kinda inconvenient for enum entry
            val serialName = entry.annotations.serialNameValue ?: entry.name.toString()
            val call = JsInvocation(
                JsNameRef(context.getNameForDescriptor(addElementFunction), serialDescriptorInThis),
                JsStringLiteral(serialName)
            )
            translator.addInitializerStatement(call.makeStmt())
            // serialDesc.pushAnnotation(...)
            pushAnnotationsInto(entry, pushAnnotationFunction, serialDescriptorInThis)
        }
    }
}