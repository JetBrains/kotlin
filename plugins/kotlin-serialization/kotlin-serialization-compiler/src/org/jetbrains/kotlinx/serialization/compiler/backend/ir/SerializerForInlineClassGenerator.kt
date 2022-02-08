/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class SerializerForInlineClassGenerator(
    irClass: IrClass,
    compilerContext: SerializationPluginContext,
    bindingContext: BindingContext,
    serialInfoJvmGenerator: SerialInfoImplJvmIrGenerator,
) : SerializerIrGenerator(irClass, compilerContext, bindingContext, null, serialInfoJvmGenerator) {
    override fun generateSave(function: FunctionDescriptor) = irClass.contributeFunction(function) { saveFunc ->
        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, saveFunc.dispatchReceiverParameter!!.symbol)

        val encoderClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.ENCODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.owner?.getter!!.symbol
        val encodeInline = encoderClass.referenceFunctionSymbol(CallingConventions.encodeInline)
        val serialDescGetter = irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol)

        // val inlineEncoder = encoder.encodeInline()
        val encodeInlineCall: IrExpression = irInvoke(irGet(saveFunc.valueParameters[0]), encodeInline, serialDescGetter)
        val inlineEncoder = irTemporary(encodeInlineCall, nameHint = "inlineEncoder")

        val property = serializableProperties.first()
        val value = getFromBox(irGet(saveFunc.valueParameters[1]), property)

        // inlineEncoder.encodeInt/String/SerializableValue
        val elementCall = formEncodeDecodePropertyCall(irGet(inlineEncoder), saveFunc.dispatchReceiverParameter!!, property, {innerSerial, sti ->
            val f =
                encoderClass.referenceFunctionSymbol("${CallingConventions.encode}${sti.elementMethodPrefix}SerializableValue")
            f to listOf(
                innerSerial,
                value
            )
        }, {
            val f =
                encoderClass.referenceFunctionSymbol("${CallingConventions.encode}${it.elementMethodPrefix}")
            val args = if (it.elementMethodPrefix != "Unit") listOf(value) else emptyList()
            f to args
        })

        val actualEncodeCall = irIfNull(compilerContext.irBuiltIns.unitType, irGet(inlineEncoder), irNull(), elementCall)
        +actualEncodeCall
    }

    override fun generateLoad(function: FunctionDescriptor) = irClass.contributeFunction(function) { loadFunc ->
        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, loadFunc.dispatchReceiverParameter!!.symbol)

        val decoderClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.DECODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.owner?.getter!!.symbol
        val decodeInline = decoderClass.referenceFunctionSymbol(CallingConventions.decodeInline)
        val serialDescGetter = irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol)

        // val inlineDecoder = decoder.decodeInline()
        val inlineDecoder: IrExpression = irInvoke(irGet(loadFunc.valueParameters[0]), decodeInline, serialDescGetter)

        val property = serializableProperties.first()
        val inlinedType = property.type.toIrType()
        val actualCall = formEncodeDecodePropertyCall(inlineDecoder, loadFunc.dispatchReceiverParameter!!, property, { innerSerial, sti ->
            decoderClass.referenceFunctionSymbol( "${CallingConventions.decode}${sti.elementMethodPrefix}SerializableValue") to listOf(innerSerial)
        }, {
            decoderClass.referenceFunctionSymbol("${CallingConventions.decode}${it.elementMethodPrefix}") to listOf()
        }, returnTypeHint = inlinedType)
        val value = coerceToBox(actualCall, loadFunc.returnType)
        +irReturn(value)
    }

    override val serialDescImplClass: ClassDescriptor = serializerDescriptor
        .getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_FOR_INLINE)

    override fun IrBlockBodyBuilder.instantiateNewDescriptor(
        serialDescImplClass: ClassDescriptor,
        correctThis: IrExpression
    ): IrExpression {
        val ctor = compilerContext.referenceConstructors(serialDescImplClass.fqNameSafe).single { it.owner.isPrimary }
        return irInvoke(
            null, ctor,
            irString(serialName),
            correctThis
        )
    }

    // Compiler will elide these in corresponding inline class lowerings (when serialize/deserialize functions will be split in two)

    private fun IrBlockBodyBuilder.coerceToBox(expression: IrExpression, inlineClassBoxType: IrType): IrExpression =
        irInvoke(
            null,
            serializableIrClass.constructors.single { it.isPrimary }.symbol,
            (inlineClassBoxType as IrSimpleType).arguments.map { it.typeOrNull },
            listOf(expression)
        )

    private fun IrBlockBodyBuilder.getFromBox(expression: IrExpression, serializableProperty: SerializableProperty): IrExpression =
        getProperty(expression, serializableProperty.getIrPropertyFrom(serializableIrClass))
}
