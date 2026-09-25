/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.util

import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
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

    val constType = this.type.makeNotNull()
    return when (kind) {
        IrConstKind.Null -> NullValue
        IrConstKind.Boolean -> BooleanValue(this.value as Boolean)
        IrConstKind.Char -> CharValue(this.value as Char)
        IrConstKind.Byte -> if (constType.isUByte()) UByteValue(this.value as Byte) else ByteValue(this.value as Byte)
        IrConstKind.Short -> if (constType.isUShort()) UShortValue(this.value as Short) else ShortValue(this.value as Short)
        IrConstKind.Int -> if (constType.isUInt()) UIntValue(this.value as Int) else IntValue(this.value as Int)
        IrConstKind.Long -> if (constType.isULong()) ULongValue(this.value as Long) else LongValue(this.value as Long)
        IrConstKind.Double -> DoubleValue(this.value as Double)
        IrConstKind.Float -> FloatValue(this.value as Float)
        IrConstKind.String -> StringValue(this.value as String)
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
