/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class SerializerForEnumsGenerator(
    irClass: IrClass,
    compilerContext: SerializationPluginContext,
    bindingContext: BindingContext
) :
    SerializerIrGenerator(irClass, compilerContext, bindingContext) {

    override fun generateSave(function: FunctionDescriptor) = irClass.contributeFunction(function) { saveFunc ->
        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, saveFunc.dispatchReceiverParameter!!.symbol)

        val encoderClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.ENCODER_CLASS)
        val descriptorGetterSymbol = compilerContext.symbolTable.referenceFunction(anySerialDescProperty?.getter!!)
        val encodeEnum = encoderClass.referenceMethod(CallingConventions.encodeEnum)
        val serialDescGetter = irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol)

        val serializableIrClass = requireNotNull(serializableIrClass) { "Enums do not support external serialization" }
        val ordinalProp = serializableIrClass.properties.single { it.name == Name.identifier("ordinal") }.getter!!
        val getOrdinal = irInvoke(irGet(saveFunc.valueParameters[1]), ordinalProp.symbol)
        val call = irInvoke(irGet(saveFunc.valueParameters[0]), encodeEnum, serialDescGetter, getOrdinal)
        +call
    }

    override fun generateLoad(function: FunctionDescriptor) = irClass.contributeFunction(function) { loadFunc ->
        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, loadFunc.dispatchReceiverParameter!!.symbol)

        val decoderClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.DECODER_CLASS)
        val descriptorGetterSymbol = compilerContext.symbolTable.referenceFunction(anySerialDescProperty?.getter!!)
        val decode = decoderClass.referenceMethod(CallingConventions.decodeEnum)
        val serialDescGetter = irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol)

        val serializableIrClass = requireNotNull(serializableIrClass) { "Enums do not support external serialization" }
        val valuesF = serializableIrClass.functions.single { it.name == DescriptorUtils.ENUM_VALUES }
        val getValues = irInvoke(dispatchReceiver = null, callee = valuesF.symbol)

        val arrayGet =
            compilerContext.builtIns.array.getFuncDesc("get").single()
        val arrayGetSymbol = compilerContext.symbolTable.referenceFunction(arrayGet)
        val getValueByOrdinal =
            irInvoke(
                getValues,
                arrayGetSymbol,
                irInvoke(irGet(loadFunc.valueParameters[0]), decode, serialDescGetter),
                typeHint = serializableIrClass.defaultType
            )
        +irReturn(getValueByOrdinal)
    }

    override fun IrBlockBodyBuilder.instantiateNewDescriptor(
        serialDescImplClass: ClassDescriptor,
        correctThis: IrExpression
    ): IrExpression {
        val serialDescForEnums = serializerDescriptor
            .getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_FOR_ENUM)
        val ctor =
            compilerContext.symbolTable.referenceConstructor(serialDescForEnums.unsubstitutedPrimaryConstructor!!)
        return irInvoke(
            null, ctor,
            irString(serialName),
            irInt(serializableDescriptor.enumEntries().size),
            typeHint = ctor.descriptor.returnType.toIrType()
        )
    }

    override fun IrBlockBodyBuilder.addElementsContentToDescriptor(
        serialDescImplClass: ClassDescriptor,
        localDescriptor: IrVariable,
        addFunction: IrFunctionSymbol
    ) {
        val enumEntries = serializableDescriptor.enumEntries()
        for (entry in enumEntries) {
            // regular .serialName() produces fqName here, which is kinda inconvenient for enum entry
            val serialName = entry.annotations.serialNameValue ?: entry.name.toString()
            val call = irInvoke(
                irGet(localDescriptor),
                addFunction,
                irString(serialName),
                irBoolean(false),
                typeHint = compilerContext.irBuiltIns.unitType
            )
            +call
            // serialDesc.pushAnnotation(...)
            copySerialInfoAnnotationsToDescriptor(
                entry.annotations.mapNotNull(compilerContext.typeTranslator.constantValueGenerator::generateAnnotationConstructorCall),
                localDescriptor,
                serialDescImplClass.referenceMethod(CallingConventions.addAnnotation)
            )
        }
    }
}