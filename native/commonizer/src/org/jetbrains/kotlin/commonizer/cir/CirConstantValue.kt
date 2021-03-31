/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

sealed class CirConstantValue {
    sealed class LiteralValue<out T> : CirConstantValue() {
        abstract val value: T
    }

    data class StringValue(override val value: String) : LiteralValue<String>()
    data class CharValue(override val value: Char) : LiteralValue<Char>()

    data class ByteValue(override val value: Byte) : LiteralValue<Byte>()
    data class ShortValue(override val value: Short) : LiteralValue<Short>()
    data class IntValue(override val value: Int) : LiteralValue<Int>()
    data class LongValue(override val value: Long) : LiteralValue<Long>()

    // TODO: remove @ExperimentalUnsignedTypes once bootstrap stdlib has stable unsigned types.
    @ExperimentalUnsignedTypes
    data class UByteValue(override val value: UByte) : LiteralValue<UByte>()

    @ExperimentalUnsignedTypes
    data class UShortValue(override val value: UShort) : LiteralValue<UShort>()

    @ExperimentalUnsignedTypes
    data class UIntValue(override val value: UInt) : LiteralValue<UInt>()

    @ExperimentalUnsignedTypes
    data class ULongValue(override val value: ULong) : LiteralValue<ULong>()

    data class FloatValue(override val value: Float) : LiteralValue<Float>()
    data class DoubleValue(override val value: Double) : LiteralValue<Double>()
    data class BooleanValue(override val value: Boolean) : LiteralValue<Boolean>()

    data class EnumValue(val enumClassId: CirEntityId, val enumEntryName: CirName) : CirConstantValue()

    data class ArrayValue(val elements: List<CirConstantValue>) : CirConstantValue()

    object NullValue : CirConstantValue() {
        override fun toString() = "NullValue(value=null)"
    }
}
