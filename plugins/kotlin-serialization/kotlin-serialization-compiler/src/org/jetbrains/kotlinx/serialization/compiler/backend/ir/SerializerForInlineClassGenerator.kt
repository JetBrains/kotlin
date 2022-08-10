/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.CallingConventions
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames

class SerializerForInlineClassGenerator(
    irClass: IrClass,
    compilerContext: SerializationPluginContext,
) : SerializerIrGenerator(irClass, compilerContext, null) {
    override fun generateSave(function: IrSimpleFunction) = addFunctionBody(function) { saveFunc ->
        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, saveFunc.dispatchReceiverParameter!!.symbol)

        val encoderClass = compilerContext.getClassFromRuntime(SerialEntityNames.ENCODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.getter!!.symbol
        val encodeInline = encoderClass.functionByName(CallingConventions.encodeInline)
        val serialDescGetter = irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol)

        // val inlineEncoder = encoder.encodeInline()
        val encodeInlineCall: IrExpression = irInvoke(irGet(saveFunc.valueParameters[0]), encodeInline, serialDescGetter)
        val inlineEncoder = irTemporary(encodeInlineCall, nameHint = "inlineEncoder")

        val property = serializableProperties.first()
        val value = getProperty(irGet(saveFunc.valueParameters[1]), property.ir)

        // inlineEncoder.encodeInt/String/SerializableValue
        val elementCall = formEncodeDecodePropertyCall(irGet(inlineEncoder), saveFunc.dispatchReceiverParameter!!, property, {innerSerial, sti ->
            val f =
                encoderClass.functionByName("${CallingConventions.encode}${sti.elementMethodPrefix}SerializableValue")
            f to listOf(
                innerSerial,
                value
            )
        }, {
            val f =
                encoderClass.functionByName("${CallingConventions.encode}${it.elementMethodPrefix}")
            val args = if (it.elementMethodPrefix != "Unit") listOf(value) else emptyList()
            f to args
        })

        val actualEncodeCall = irIfNull(compilerContext.irBuiltIns.unitType, irGet(inlineEncoder), irNull(), elementCall)
        +actualEncodeCall
    }

    override fun generateLoad(function: IrSimpleFunction) = addFunctionBody(function) { loadFunc ->
        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, loadFunc.dispatchReceiverParameter!!.symbol)

        val decoderClass = compilerContext.getClassFromRuntime(SerialEntityNames.DECODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.getter!!.symbol
        val decodeInline = decoderClass.functionByName(CallingConventions.decodeInline)
        val serialDescGetter = irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol)

        // val inlineDecoder = decoder.decodeInline()
        val inlineDecoder: IrExpression = irInvoke(irGet(loadFunc.valueParameters[0]), decodeInline, serialDescGetter)

        val property = serializableProperties.first()
        val inlinedType = property.type
        val actualCall = formEncodeDecodePropertyCall(inlineDecoder, loadFunc.dispatchReceiverParameter!!, property, { innerSerial, sti ->
            decoderClass.functionByName( "${CallingConventions.decode}${sti.elementMethodPrefix}SerializableValue") to listOf(innerSerial)
        }, {
            decoderClass.functionByName("${CallingConventions.decode}${it.elementMethodPrefix}") to listOf()
        }, returnTypeHint = inlinedType)
        val value = coerceToBox(actualCall, loadFunc.returnType)
        +irReturn(value)
    }

    override val serialDescImplClass: IrClassSymbol = compilerContext.getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_FOR_INLINE)

    override fun IrBlockBodyBuilder.instantiateNewDescriptor(serialDescImplClass: IrClassSymbol, correctThis: IrExpression): IrExpression {
        val ctor = serialDescImplClass.constructors.single { it.owner.isPrimary }
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

}
