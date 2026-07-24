/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.util

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.constant.AnnotationValue
import org.jetbrains.kotlin.constant.ArrayValue
import org.jetbrains.kotlin.constant.BooleanValue
import org.jetbrains.kotlin.constant.ByteValue
import org.jetbrains.kotlin.constant.CharValue
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.constant.DoubleValue
import org.jetbrains.kotlin.constant.EnumValue
import org.jetbrains.kotlin.constant.FloatValue
import org.jetbrains.kotlin.constant.IntValue
import org.jetbrains.kotlin.constant.KClassValue
import org.jetbrains.kotlin.constant.LongValue
import org.jetbrains.kotlin.constant.NullValue
import org.jetbrains.kotlin.constant.ShortValue
import org.jetbrains.kotlin.constant.StringValue
import org.jetbrains.kotlin.constant.UByteValue
import org.jetbrains.kotlin.constant.UIntValue
import org.jetbrains.kotlin.constant.ULongValue
import org.jetbrains.kotlin.constant.UShortValue
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.getPrimitiveType
import org.jetbrains.kotlin.ir.types.getUnsignedType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.removeAnnotations
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.getAllArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal fun IrElement.toConstantValue(): ConstantValue<*> {
    return this.toConstantValueOrNull() ?: errorWithAttachment("Cannot convert IrExpression to ConstantValue") {
        withEntry("IrExpression", this@toConstantValue.render())
    }
}

private fun IrConst.toConstantValue(): ConstantValue<*> {
    if (value == null) return NullValue

    val constType = this.type.makeNotNull().removeAnnotations()
    return when (this.type.getPrimitiveType()) {
        PrimitiveType.BOOLEAN -> BooleanValue(this.value as Boolean)
        PrimitiveType.CHAR -> CharValue(this.value as Char)
        PrimitiveType.BYTE -> ByteValue((this.value as Number).toByte())
        PrimitiveType.SHORT -> ShortValue((this.value as Number).toShort())
        PrimitiveType.INT -> IntValue((this.value as Number).toInt())
        PrimitiveType.FLOAT -> FloatValue((this.value as Number).toFloat())
        PrimitiveType.LONG -> LongValue((this.value as Number).toLong())
        PrimitiveType.DOUBLE -> DoubleValue((this.value as Number).toDouble())
        null -> when (constType.getUnsignedType()) {
            UnsignedType.UBYTE -> UByteValue((this.value as Number).toByte())
            UnsignedType.USHORT -> UShortValue((this.value as Number).toShort())
            UnsignedType.UINT -> UIntValue((this.value as Number).toInt())
            UnsignedType.ULONG -> ULongValue((this.value as Number).toLong())
            null -> when {
                constType.isString() -> StringValue(this.value as String)
                else -> error("Cannot convert IrConst ${this.render()} to ConstantValue")
            }
        }
    }
}

private fun IrElement.toConstantValueOrNull(): ConstantValue<*>? {
    fun createKClassValue(possiblyArrayType: IrType): KClassValue? {
        var type = possiblyArrayType
        var arrayDimensions = 0
        while (type.isArray()) {
            arrayDimensions++
            type = (type as? IrSimpleType)?.arguments?.singleOrNull()?.typeOrNull
                ?: return KClassValue(StandardClassIds.Any, arrayDimensions) // `kotlin/Array<*>`
        }

        if (type is IrErrorType) {
            return KClassValue(type.symbol.owner.classId!!, arrayDimensions)
        }

        val irClass = type.getClass() ?: return null
        if (irClass.isLocal) {
            return KClassValue(KClassValue.Value.LocalClass(firClassSymbol = (irClass.metadata as? MetadataSource.Class)?.asFirSymbol()))
        }
        val classId = irClass.classId ?: return null
        return KClassValue(classId, arrayDimensions)
    }

    return when (this) {
        is IrConst -> this.toConstantValue()
        is IrAnnotation -> {
            val classId = this.classId
            val rawArguments = this.getAllArgumentsWithIr()
            val argumentMapping = rawArguments
                .filter { it.second != null }
                .associate { [parameter, expression] -> parameter.name to expression!!.toConstantValue() }
            AnnotationValue.create(classId, argumentMapping)
        }
        is IrGetEnumValue -> {
            val classId = this.type.getClass()?.classId ?: return null
            EnumValue(classId, this.symbol.owner.name)
        }
        is IrClassReference -> createKClassValue(this.classType)
        is IrVararg -> ArrayValue(this.elements.map { it.toConstantValue() })
        else -> null
    }
}
