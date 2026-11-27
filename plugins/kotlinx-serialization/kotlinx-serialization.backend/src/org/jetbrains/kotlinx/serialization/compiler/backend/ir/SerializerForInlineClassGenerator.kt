/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.v2.irVariable
import org.jetbrains.kotlin.ir.builders.expressions.v2.irBranch
import org.jetbrains.kotlin.ir.builders.expressions.v2.irCall
import org.jetbrains.kotlin.ir.builders.expressions.v2.irElseBranch
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetValue
import org.jetbrains.kotlin.ir.builders.expressions.v2.irReturn
import org.jetbrains.kotlin.ir.builders.expressions.v2.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.CallingConventions
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames

class SerializerForInlineClassGenerator(
    irClass: IrClass,
    compilerContext: SerializationPluginContext,
) : SerializerIrGenerator(irClass, compilerContext) {
    override fun generateSave(function: IrSimpleFunction) = addFunctionBody(function) { saveFunc ->
        fun irThis(): IrExpression =
            irGetValue(saveFunc.dispatchReceiverParameter!!.symbol)

        val encoderClass = compilerContext.getClassFromRuntime(SerialEntityNames.ENCODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.getter!!.symbol
        val encodeInline = encoderClass.functionByName(CallingConventions.encodeInline)
        val serialDescGetter = irCall(descriptorGetterSymbol) {
            origin = IrStatementOrigin.GET_PROPERTY
            arguments[0] = irThis()
        }

        // val inlineEncoder = encoder.encodeInline()
        val encodeInlineCall: IrExpression = irInvoke(encodeInline, irGetValue(saveFunc.parameters[1].symbol), serialDescGetter)
        val inlineEncoder = irVariable {
            initializer = encodeInlineCall
            name = Name.identifier(scope.inventNameForTemporary(nameHint = "inlineEncoder"))
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
        }.also { +it }

        val property = serializableProperties.first()
        val value = getProperty(irGetValue(saveFunc.parameters[2].symbol), property.ir, property.getTypeInScope())

        // inlineEncoder.encodeInt/String/SerializableValue
        val elementCall = formEncodeDecodePropertyCall(irGetValue(inlineEncoder.symbol), saveFunc.dispatchReceiverParameter!!, property, {innerSerial, sti ->
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
        }, null)

        val actualEncodeCall = irWhen {
            type = compilerContext.irBuiltIns.unitType
            branches += irBranch {
                condition = irCall(context.irBuiltIns.eqeqSymbol) {
                    origin = IrStatementOrigin.EQEQ
                    arguments[0] = irGetValue(inlineEncoder.symbol)
                    arguments[1] = irNull()
                }
                result = irNull()
            }
            branches += irElseBranch {
                result = elementCall
            }
        }
        +actualEncodeCall
    }

    override fun generateLoad(function: IrSimpleFunction) = addFunctionBody(function) { loadFunc ->
        fun irThis(): IrExpression =
            irGetValue(loadFunc.dispatchReceiverParameter!!.symbol)

        val decoderClass = compilerContext.getClassFromRuntime(SerialEntityNames.DECODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.getter!!.symbol
        val decodeInline = decoderClass.functionByName(CallingConventions.decodeInline)
        val serialDescGetter = irCall(descriptorGetterSymbol) {
            origin = IrStatementOrigin.GET_PROPERTY
            arguments[0] = irThis()
        }

        // val inlineDecoder = decoder.decodeInline()
        val inlineDecoder: IrExpression = irInvoke(decodeInline, irGetValue(loadFunc.parameters[1].symbol), serialDescGetter)

        val property = serializableProperties.first()
        val inlinedType = property.getTypeInScope()
        val actualCall = formEncodeDecodePropertyCall(inlineDecoder, loadFunc.dispatchReceiverParameter!!, property, { innerSerial, sti ->
            decoderClass.functionByName( "${CallingConventions.decode}${sti.elementMethodPrefix}SerializableValue") to listOf(innerSerial)
        }, {
            decoderClass.functionByName("${CallingConventions.decode}${it.elementMethodPrefix}") to listOf()
        }, null, returnTypeHint = inlinedType)
        val value = coerceToBox(actualCall, loadFunc.returnType)
        +irReturn { this.value = value }
    }

    override val serialDescImplClass: IrClassSymbol = compilerContext.getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_FOR_INLINE)

    override fun IrBlockBodyBuilder.instantiateNewDescriptor(serialDescImplClass: IrClassSymbol, correctThis: IrExpression): IrExpression {
        val ctor = serialDescImplClass.constructors.single { it.owner.isPrimary }
        return irInvoke(
             ctor,
            irString(serialName),
            correctThis
        )
    }

    // Compiler will elide these in corresponding inline class lowerings (when serialize/deserialize functions will be split in two)

    private fun IrBlockBodyBuilder.coerceToBox(expression: IrExpression, inlineClassBoxType: IrType): IrExpression =
        irInvoke(
            serializableIrClass.constructors.single { it.isPrimary }.symbol,
            listOf(expression),
            (inlineClassBoxType as IrSimpleType).arguments.map { it.typeOrNull },
            returnTypeHint = inlineClassBoxType,
        )

}
