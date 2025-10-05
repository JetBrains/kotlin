/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.cgen.*
import org.jetbrains.kotlin.ir.util.getAnnotationStringValue
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.IntrinsicType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*

private class InteropCallContext(
        val symbols: KonanSymbols,
        val builder: IrBuilderWithScope,
        val failCompilation: (String) -> Nothing
) {
    fun IrType.isCPointer() = this.isCPointer(symbols)

    fun IrType.isNativePointed() = this.isNativePointed(symbols)

    fun IrType.isSupportedReference() = this.isCStructFieldSupportedReferenceType(symbols)

    val irBuiltIns: IrBuiltIns = builder.context.irBuiltIns
}

private inline fun <T> generateInteropCall(
        symbols: KonanSymbols,
        builder: IrBuilderWithScope,
        noinline failCompilation: (String) -> Nothing,
        block: InteropCallContext.() -> T
) = InteropCallContext(symbols, builder, failCompilation).block()

/**
 * Search for memory read/write function in [kotlinx.cinterop.nativeMemUtils] of a given [valueType].
 */
private fun InteropCallContext.findMemoryAccessFunction(isRead: Boolean, valueType: IrType): IrFunction {
    val requiredType = if (isRead) {
        IntrinsicType.INTEROP_READ_PRIMITIVE
    } else {
        IntrinsicType.INTEROP_WRITE_PRIMITIVE
    }
    val nativeMemUtilsClass = symbols.nativeMemUtils.owner
    return nativeMemUtilsClass.functions.filter {
        val annotationArgument = it.annotations
                .findAnnotation(RuntimeNames.typedIntrinsicAnnotation)
                ?.getAnnotationStringValue()
        annotationArgument == requiredType.name
    }.firstOrNull {
        if (isRead) {
            it.returnType.classOrNull == valueType.classOrNull
        } else {
            it.parameters.last().type.classOrNull == valueType.classOrNull
        }
    } ?: error("No memory access function for ${valueType.classOrNull?.owner?.name}")
}

private fun InteropCallContext.readValueFromMemory(
        nativePtr: IrExpression,
        returnType: IrType
): IrExpression {
    val memoryValueType = determineInMemoryType(returnType)
    val memReadFn = findMemoryAccessFunction(isRead = true, valueType = memoryValueType)
    val memRead = builder.irCall(memReadFn).also { memRead ->
        memRead.arguments[0] = builder.irGetObject(symbols.nativeMemUtils)
        memRead.arguments[1] = readPointed(nativePtr, symbols.nativePointed.defaultType)
    }
    return castPrimitiveIfNeeded(memRead, memoryValueType, returnType)
}

private fun InteropCallContext.writeValueToMemory(
        nativePtr: IrExpression,
        value: IrExpression,
        targetType: IrType
): IrExpression {
    val memoryValueType = determineInMemoryType(targetType)
    val memWriteFn = findMemoryAccessFunction(isRead = false, valueType = memoryValueType)
    val valueToWrite = castPrimitiveIfNeeded(value, targetType, memoryValueType)
    return with(builder) {
        irCall(memWriteFn).also { memWrite ->
            memWrite.arguments[0] = irGetObject(symbols.nativeMemUtils)
            memWrite.arguments[1] = readPointed(nativePtr, symbols.nativePointed.defaultType)
            memWrite.arguments[2] = valueToWrite
        }
    }
}

private fun InteropCallContext.determineInMemoryType(type: IrType): IrType {
    val classifier = type.classOrNull!!
    return when (classifier) {
        in symbols.unsignedIntegerClasses -> {
            symbols.unsignedToSignedOfSameBitWidth.getValue(classifier).owner.defaultType
        }
        // Assuming that _Bool is stored as single byte.
        irBuiltIns.booleanClass -> symbols.byte.defaultType
        else -> type
    }
}

private fun InteropCallContext.castPrimitiveIfNeeded(
        value: IrExpression,
        fromType: IrType,
        toType: IrType
): IrExpression {
    val sourceClass = fromType.classOrNull!!
    val targetClass = toType.classOrNull!!
    return if (sourceClass != targetClass) {
        when {
            targetClass == irBuiltIns.booleanClass -> castToBoolean(sourceClass, value)
            sourceClass == irBuiltIns.booleanClass -> castFromBoolean(targetClass, value)
            else -> {
                val conversion = symbols.integerConversions[sourceClass to targetClass]
                        ?: error("There is no conversion from ${sourceClass.owner.name} to ${targetClass.owner.name}")
                builder.irCall(conversion.owner).apply {
                    arguments[0] = value
                }
            }
        }
    } else {
        value
    }
}

/**
 * Perform (value != 0)
 */
private fun InteropCallContext.castToBoolean(sourceClass: IrClassSymbol, value: IrExpression): IrExpression {
    val (primitiveBinaryType, immZero) = when (sourceClass) {
        // Case of regular struct field.
        symbols.byte -> PrimitiveBinaryType.BYTE to builder.irByte(0)
        // Case of bitfield.
        symbols.long -> PrimitiveBinaryType.LONG to builder.irLong(0)
        else -> error("Unsupported cast to boolean from ${sourceClass.owner.name}")
    }
    val areEqualByValuesBytes = symbols.areEqualByValue.getValue(primitiveBinaryType)
    val compareToZero = builder.irCall(areEqualByValuesBytes).apply {
        arguments[0] = value
        arguments[1] = immZero
    }
    return builder.irCall(irBuiltIns.booleanNotSymbol).apply {
        arguments[0] = compareToZero
    }
}

/**
 * Perform if (value) 1 else 0
 */
private fun InteropCallContext.castFromBoolean(targetClass: IrClassSymbol, value: IrExpression): IrExpression {
    val (thenPart, elsePart) = when (targetClass) {
        // Case of regular struct field.
        symbols.byte -> builder.irByte(1) to builder.irByte(0)
        // Case of bitfield.
        symbols.long -> builder.irLong(1) to builder.irLong(0)
        else -> error("Unsupported cast from boolean to ${targetClass.owner.name}")
    }
    return builder.irIfThenElse(targetClass.defaultType, value, thenPart, elsePart)
}

private fun InteropCallContext.convertEnumToIntegral(enumValue: IrExpression, targetEnumType: IrType): IrExpression {
    val enumClass = targetEnumType.getClass()!!
    val valueProperty = enumClass.properties.single { it.name.asString() == "value" }
    return builder.irCall(valueProperty.getter!!).also {
        it.dispatchReceiver = enumValue
    }
}

private fun InteropCallContext.convertIntegralToEnum(
        value: IrExpression,
        intergralType: IrType,
        enumType: IrType
): IrExpression {
    val enumClass = enumType.getClass()!!
    val companionClass = enumClass.companionObject()!!
    val byValue = companionClass.simpleFunctions().single { it.name.asString() == "byValue" }
    val byValueArg = castPrimitiveIfNeeded(value, intergralType, byValue.parameters[1].type)
    return builder.irCall(byValue).apply {
        arguments[0] = builder.irGetObject(companionClass.symbol)
        arguments[1] = byValueArg
    }
}

private fun IrType.getCEnumPrimitiveType(): IrType {
    assert(this.isCEnumType())
    val enumClass = this.getClass()!!
    return enumClass.properties.single { it.name.asString() == "value" }
            .getter!!.returnType
}

private fun InteropCallContext.readEnumValueFromMemory(nativePtr: IrExpression, enumType: IrType): IrExpression {
    val enumPrimitiveType = enumType.getCEnumPrimitiveType()
    val readMemory = readValueFromMemory(nativePtr, enumPrimitiveType)
    return convertIntegralToEnum(readMemory, readMemory.type, enumType)
}

private fun InteropCallContext.writeEnumValueToMemory(
        nativePtr: IrExpression,
        value: IrExpression,
        targetEnumType: IrType
): IrExpression {
    val valueToWrite = convertEnumToIntegral(value, targetEnumType)
    return writeValueToMemory(nativePtr, valueToWrite, targetEnumType.getCEnumPrimitiveType())
}

private fun InteropCallContext.convertCPointerToNativePtr(cPointer: IrExpression): IrExpression {
    return builder.irCall(symbols.interopCPointerGetRawValue).also {
        it.arguments[0] = cPointer
    }
}


private fun InteropCallContext.writePointerToMemory(
        nativePtr: IrExpression,
        value: IrExpression,
        pointerType: IrType
): IrExpression {
    val valueToWrite = when {
        pointerType.isCPointer() -> convertCPointerToNativePtr(value)
        else -> error("Unsupported pointer type")
    }
    return writeValueToMemory(nativePtr, valueToWrite, valueToWrite.type)
}

private fun InteropCallContext.writeObjCReferenceToMemory(
        nativePtr: IrExpression,
        value: IrExpression
): IrExpression {
    val valueToWrite = builder.irCall(symbols.interopObjCObjectRawValueGetter).also {
        it.arguments[0] = value
    }
    return writeValueToMemory(nativePtr, valueToWrite, valueToWrite.type)
}

private fun InteropCallContext.calculateFieldPointer(receiver: IrExpression, offset: Long): IrExpression {
    val base = builder.irCall(symbols.interopNativePointedRawPtrGetter).also {
        it.dispatchReceiver = receiver
    }
    val nativePtrPlusLong = symbols.nativePtrType.getClass()!!
            .functions.single { it.name.identifier == "plus" }
    return with (builder) {
        irCall(nativePtrPlusLong).also {
            it.arguments[0] = base
            it.arguments[1] = irLong(offset)
        }
    }
}

private fun InteropCallContext.interpretCPointer(nativePtr: IrExpression, type: IrType): IrMemberAccessExpression<*> {
    require(type.isCPointer()) { "A CPointer expected but was: ${type.render()}" }
    return builder.irCallWithSubstitutedType(
            symbols.interopInterpretCPointer, listOf((type as IrSimpleType).arguments[0].typeOrFail)
    ).also { it.arguments[0] = nativePtr }
}

private fun InteropCallContext.readPointed(nativePtr: IrExpression, type: IrType) =
        builder.irCallWithSubstitutedType(symbols.interopInterpretNullablePointed, listOf(type)).also {
            it.arguments[0] = nativePtr
        }

private fun InteropCallContext.readObjectiveCReferenceFromMemory(
        nativePtr: IrExpression,
        type: IrType
): IrExpression {
    val readMemory = readValueFromMemory(nativePtr, symbols.nativePtrType)
    return builder.irCallWithSubstitutedType(symbols.interopInterpretObjCPointerOrNull, listOf(type)).apply {
        arguments[0] = readMemory
    }
}

/** Returns non-null result if [callSite] is accessor to:
 *  1. T.value, T : CEnumVar
 *  2. T.<field-name>, T : CStructVar and accessor is annotated with
 *      [kotlinx.cinterop.internal.CStruct.MemberAt] or [kotlinx.cinterop.internal.CStruct.BitField]
 */
internal fun tryGenerateInteropMemberAccess(
        callSite: IrCall,
        symbols: KonanSymbols,
        builder: IrBuilderWithScope,
        failCompilation: (String) -> Nothing
): IrExpression? = when {
    callSite.symbol.owner.isCEnumVarValueAccessor(symbols) ->
        generateInteropCall(symbols, builder, failCompilation) { generateEnumVarValueAccess(callSite) }
    callSite.symbol.owner.isCStructMemberAtAccessor() ->
        generateInteropCall(symbols, builder, failCompilation) { generateMemberAtAccess(callSite) }
    callSite.symbol.owner.isCStructBitFieldAccessor() ->
        generateInteropCall(symbols, builder, failCompilation) { generateBitFieldAccess(callSite) }
    callSite.symbol.owner.isCStructArrayMemberAtAccessor() ->
        generateInteropCall(symbols, builder, failCompilation) { generateArrayMemberAtAccess(callSite) }
    else -> null
}

private fun InteropCallContext.generateEnumVarValueAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val nativePtr = builder.irCall(symbols.interopNativePointedRawPtrGetter).also {
        it.arguments[0] = callSite.arguments[0]!!
    }
    return when {
        accessor.isGetter -> readEnumValueFromMemory(nativePtr, accessor.returnType)
        accessor.isSetter -> {
            val type = accessor.parameters[1].type
            writeEnumValueToMemory(nativePtr, callSite.arguments[1]!!, type)
        }
        else -> error("")
    }
}

private fun InteropCallContext.generateMemberAtAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val memberAt = accessor.getAnnotation(RuntimeNames.cStructMemberAt)!!
    val offset = (memberAt.arguments[0] as IrConst).value as Long
    val fieldPointer = calculateFieldPointer(callSite.arguments[0]!!, offset)
    return when {
        accessor.isGetter -> {
            val type = accessor.returnType
            when {
                type.isCEnumType() -> readEnumValueFromMemory(fieldPointer, type)
                type.isCStructFieldTypeStoredInMemoryDirectly() -> readValueFromMemory(fieldPointer, type)
                type.isCPointer() -> interpretCPointer(readValueFromMemory(fieldPointer, symbols.nativePtrType), type)
                type.isNativePointed() -> readPointed(fieldPointer, type)
                type.isSupportedReference() -> readObjectiveCReferenceFromMemory(fieldPointer, type)
                else -> failCompilation("Unsupported struct field type: ${type.getClass()?.name}")
            }
        }
        accessor.isSetter -> {
            val value = callSite.arguments[1]!!
            val type = accessor.parameters[1].type
            when {
                type.isCEnumType() -> writeEnumValueToMemory(fieldPointer, value, type)
                type.isCStructFieldTypeStoredInMemoryDirectly() -> writeValueToMemory(fieldPointer, value, type)
                type.isCPointer() -> writePointerToMemory(fieldPointer, value, type)
                type.isSupportedReference() -> writeObjCReferenceToMemory(fieldPointer, value)
                else -> failCompilation("Unsupported struct field type: ${type.getClass()?.name}")
            }
        }
        else -> failCompilation("Unexpected accessor function: ${accessor.name}")
    }
}

private fun InteropCallContext.generateArrayMemberAtAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val memberAt = accessor.getAnnotation(RuntimeNames.cStructArrayMemberAt)!!
    val offset = (memberAt.arguments[0] as IrConst).value as Long
    val fieldPointer = calculateFieldPointer(callSite.dispatchReceiver!!, offset)
    return interpretCPointer(fieldPointer, accessor.returnType)
}

private fun InteropCallContext.writeBits(
        base: IrExpression,
        offset: Long,
        size: Int,
        value: IrExpression,
        type: IrType
): IrExpression {
    val (integralValue, fromType) = when {
        type.isCEnumType() -> convertEnumToIntegral(value, type) to type.getCEnumPrimitiveType()
        else -> value to type
    }
    val targetType = symbols.writeBits.owner.parameters.last().type
    val valueToWrite = castPrimitiveIfNeeded(integralValue, fromType, targetType)
    return with(builder) {
        irCall(symbols.writeBits).also {
            it.arguments[0] = base
            it.arguments[1] = irLong(offset)
            it.arguments[2] = irInt(size)
            it.arguments[3] = valueToWrite
        }
    }
}

private fun InteropCallContext.readBits(
        base: IrExpression,
        offset: Long,
        size: Int,
        type: IrType
): IrExpression {
    val isSigned = when {
        type.isCEnumType() ->
            !type.getCEnumPrimitiveType().isUnsigned()
        else ->
            !type.isUnsigned()
    }
    val integralValue = with (builder) {
        irCall(symbols.readBits).also {
            it.arguments[0] = base
            it.arguments[1] = irLong(offset)
            it.arguments[2] = irInt(size)
            it.arguments[3] = irBoolean(isSigned)
        }
    }
    return when {
        type.isCEnumType() -> convertIntegralToEnum(integralValue, integralValue.type, type)
        else -> castPrimitiveIfNeeded(integralValue, integralValue.type, type)
    }
}

private fun InteropCallContext.generateBitFieldAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val bitField = accessor.getAnnotation(RuntimeNames.cStructBitField)!!
    val offset = (bitField.arguments[0] as IrConst).value as Long
    val size = (bitField.arguments[1] as IrConst).value as Int
    val base = builder.irCall(symbols.interopNativePointedRawPtrGetter).also {
        it.dispatchReceiver = callSite.dispatchReceiver!!
    }
    return when {
        accessor.isSetter -> {
            val argument = callSite.arguments[1]!!
            val type = accessor.parameters[1].type
            writeBits(base, offset, size, argument, type)
        }
        accessor.isGetter -> {
            val type = accessor.returnType
            readBits(base, offset, size, type)
        }
        else -> error("Unexpected accessor function: ${accessor.name}")
    }
}
