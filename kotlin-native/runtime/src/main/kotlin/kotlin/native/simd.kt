/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.native.internal.GCCritical
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType


public final class Vector128 private constructor() {
    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external fun getByteAt(index: Int): Byte

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external fun getIntAt(index: Int): Int

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external fun getLongAt(index: Int): Long

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external fun getFloatAt(index: Int): Float

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external fun getDoubleAt(index: Int): Double

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external fun getUByteAt(index: Int): UByte

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external fun getUIntAt(index: Int): UInt

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external fun getULongAt(index: Int): ULong

    public override fun toString() =
            "(0x${getUIntAt(0).toString(16)}, 0x${getUIntAt(1).toString(16)}, 0x${getUIntAt(2).toString(16)}, 0x${getUIntAt(3).toString(16)})"

    // Not as good for floating types
    public fun equals(other: Vector128): Boolean =
            getLongAt(0) == other.getLongAt(0) && getLongAt(1) == other.getLongAt(1)

    public override fun equals(other: Any?): Boolean =
            other is Vector128 && this.equals(other)

    override fun hashCode(): Int {
        val x0 = getLongAt(0)
        val x1 = getLongAt(1)
        return 31 * (x0 xor (x0 shr 32)).toInt() + (x1 xor (x1 shr 32)).toInt()
    }
}

@SymbolName("Kotlin_Vector4f_of")
@GCCritical
external fun vectorOf(f0: Float, f1: Float, f2: Float, f3: Float): Vector128

@SymbolName("Kotlin_Vector4i32_of")
@GCCritical
external fun vectorOf(f0: Int, f1: Int, f2: Int, f3: Int): Vector128
