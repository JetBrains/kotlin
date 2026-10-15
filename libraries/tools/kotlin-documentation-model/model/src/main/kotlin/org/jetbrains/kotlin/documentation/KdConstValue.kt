/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A compile-time-known value, e.g., of a `const val` or an annotation argument.
 */
@Serializable
sealed class KdConstValue {
    @Serializable
    @SerialName("null")
    data object Null : KdConstValue()

    @Serializable
    @SerialName("string")
    data class String(val value: kotlin.String) : KdConstValue()

    @Serializable
    @SerialName("boolean")
    data class Boolean(val value: kotlin.Boolean) : KdConstValue()

    @Serializable
    @SerialName("char")
    data class Char(val value: kotlin.Char) : KdConstValue()

    @Serializable
    @SerialName("byte")
    data class Byte(val value: kotlin.Byte) : KdConstValue()

    @Serializable
    @SerialName("ubyte")
    data class UByte(val value: kotlin.UByte) : KdConstValue()

    @Serializable
    @SerialName("short")
    data class Short(val value: kotlin.Short) : KdConstValue()

    @Serializable
    @SerialName("ushort")
    data class UShort(val value: kotlin.UShort) : KdConstValue()

    @Serializable
    @SerialName("int")
    data class Int(val value: kotlin.Int) : KdConstValue()

    @Serializable
    @SerialName("uint")
    data class UInt(val value: kotlin.UInt) : KdConstValue()

    @Serializable
    @SerialName("long")
    data class Long(val value: kotlin.Long) : KdConstValue()

    @Serializable
    @SerialName("ulong")
    data class ULong(val value: kotlin.ULong) : KdConstValue()

    @Serializable
    @SerialName("float")
    data class Float(val value: kotlin.Float) : KdConstValue()

    @Serializable
    @SerialName("double")
    data class Double(val value: kotlin.Double) : KdConstValue()
}

/**
 * A compile-time-known [value], together with the source [expression] that produced it when
 * available (e.g. not available for inherited/overridden or non-const declarations).
 */
@Serializable
data class KdConstExpression(
    val value: KdConstValue,
    val expression: KdExpression?,
)
