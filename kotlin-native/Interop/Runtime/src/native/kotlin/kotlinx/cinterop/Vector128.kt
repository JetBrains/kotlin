/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlinx.cinterop

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType
import kotlinx.cinterop.ExperimentalForeignApi


@SinceKotlin("1.9")
@ExperimentalForeignApi
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
    public override fun equals(other: Any?): Boolean =
            other is Vector128 && getLongAt(0) == other.getLongAt(0) && getLongAt(1) == other.getLongAt(1)

    override fun hashCode(): Int {
        val x0 = getLongAt(0)
        val x1 = getLongAt(1)
        return 31 * (x0 xor (x0 shr 32)).toInt() + (x1 xor (x1 shr 32)).toInt()
    }
}


@SinceKotlin("1.9")
@ExperimentalForeignApi
@GCUnsafeCall("Kotlin_Interop_Vector4f_of")
public external fun vectorOf(f0: Float, f1: Float, f2: Float, f3: Float): Vector128

@SinceKotlin("1.9")
@ExperimentalForeignApi
@GCUnsafeCall("Kotlin_Interop_Vector4i32_of")
public external fun vectorOf(f0: Int, f1: Int, f2: Int, f3: Int): Vector128


@SinceKotlin("1.9")
@ExperimentalForeignApi
@Suppress("FINAL_UPPER_BOUND")
public class Vector128VarOf<T : Vector128>(rawPtr: NativePtr) : CVariable(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(size = 16, align = 16)
}

@SinceKotlin("1.9")
@ExperimentalForeignApi
public typealias Vector128Var = Vector128VarOf<Vector128>

@SinceKotlin("1.9")
@ExperimentalForeignApi
@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : Vector128> Vector128VarOf<T>.value: T
    get() = nativeMemUtils.getVector(this) as T
    set(value) = nativeMemUtils.putVector(this, value)

