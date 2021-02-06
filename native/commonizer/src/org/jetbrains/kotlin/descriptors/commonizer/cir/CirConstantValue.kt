/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

sealed class CirConstantValue<out T> {
    abstract val value: T

    data class StringValue(override val value: String) : CirConstantValue<String>()
    data class CharValue(override val value: Char) : CirConstantValue<Char>()

    data class ByteValue(override val value: Byte) : CirConstantValue<Byte>()
    data class ShortValue(override val value: Short) : CirConstantValue<Short>()
    data class IntValue(override val value: Int) : CirConstantValue<Int>()
    data class LongValue(override val value: Long) : CirConstantValue<Long>()

    data class UByteValue(override val value: Byte) : CirConstantValue<Byte>()
    data class UShortValue(override val value: Short) : CirConstantValue<Short>()
    data class UIntValue(override val value: Int) : CirConstantValue<Int>()
    data class ULongValue(override val value: Long) : CirConstantValue<Long>()

    data class FloatValue(override val value: Float) : CirConstantValue<Float>()
    data class DoubleValue(override val value: Double) : CirConstantValue<Double>()
    data class BooleanValue(override val value: Boolean) : CirConstantValue<Boolean>()

    data class EnumValue(val enumClassId: CirEntityId, val enumEntryName: CirName) : CirConstantValue<String>() {
        override val value: String = "${enumClassId}.${enumEntryName}"
    }

    data class ArrayValue(override val value: List<CirConstantValue<*>>) : CirConstantValue<List<CirConstantValue<*>>>()

    object NullValue : CirConstantValue<Void?>() {
        override val value: Void? get() = null
        override fun toString() = "NullValue(value=null)"
    }
}
