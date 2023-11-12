/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("RESERVED_MEMBER_INSIDE_VALUE_CLASS")

package kotlin.native.internal

import kotlinx.cinterop.*

@TypedIntrinsic(IntrinsicType.INTEROP_GET_NATIVE_NULL_PTR)
@PublishedApi
internal external fun getNativeNullPtr(): NativePtr

@ExperimentalForeignApi
public class NativePtr @PublishedApi internal constructor(private val value: NonNullNativePtr?) {
    public companion object {
        // TODO: make it properly precreated, maybe use an intrinsic for that.
        public val NULL: NativePtr = getNativeNullPtr()
    }

    @TypedIntrinsic(IntrinsicType.INTEROP_NATIVE_PTR_PLUS_LONG)
    public external operator fun plus(offset: Long): NativePtr

    @TypedIntrinsic(IntrinsicType.INTEROP_NATIVE_PTR_TO_LONG)
    public external fun toLong(): Long

    override fun equals(other: Any?): Boolean = (other is NativePtr) && kotlin.native.internal.areEqualByValue(this, other)

    override fun hashCode(): Int = this.toLong().hashCode()

    override fun toString(): String = "0x${this.toLong().toString(16)}"

    internal fun isNull(): Boolean = (value == null)
}

@PublishedApi
internal class NonNullNativePtr private constructor() { // TODO: refactor to use this type widely.
    @Suppress("NOTHING_TO_INLINE")
    inline fun toNativePtr() = NativePtr(this)

    override fun toString(): String = toNativePtr().toString()

    override fun hashCode(): Int = toNativePtr().hashCode()

    override fun equals(other: Any?): Boolean = other is NonNullNativePtr
            && kotlin.native.internal.areEqualByValue(this.toNativePtr(), other.toNativePtr())
}

@ExportTypeInfo("theNativePtrArrayTypeInfo")
internal class NativePtrArray {

    @GCUnsafeCall("Kotlin_NativePtrArray_get")
    public external operator fun get(index: Int): NativePtr

    @GCUnsafeCall("Kotlin_NativePtrArray_set")
    public external operator fun set(index: Int, value: NativePtr): Unit

    val size: Int
        get() = getArrayLength()

    @GCUnsafeCall("Kotlin_NativePtrArray_getArrayLength")
    private external fun getArrayLength(): Int
}
