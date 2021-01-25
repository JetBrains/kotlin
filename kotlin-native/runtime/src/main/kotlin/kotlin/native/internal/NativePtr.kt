/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("RESERVED_MEMBER_INSIDE_INLINE_CLASS")

package kotlin.native.internal

@TypedIntrinsic(IntrinsicType.INTEROP_GET_NATIVE_NULL_PTR)
external fun getNativeNullPtr(): NativePtr

class NativePtr @PublishedApi internal constructor(private val value: NonNullNativePtr?) {
    companion object {
        // TODO: make it properly precreated, maybe use an intrinsic for that.
        val NULL = getNativeNullPtr()
    }

    @TypedIntrinsic(IntrinsicType.INTEROP_NATIVE_PTR_PLUS_LONG)
    external operator fun plus(offset: Long): NativePtr

    @TypedIntrinsic(IntrinsicType.INTEROP_NATIVE_PTR_TO_LONG)
    external fun toLong(): Long

    override fun equals(other: Any?) = (other is NativePtr) && kotlin.native.internal.areEqualByValue(this, other)

    override fun hashCode() = this.toLong().hashCode()

    override fun toString() = "0x${this.toLong().toString(16)}"

    internal fun isNull(): Boolean = (value == null)
}

@PublishedApi
internal class NonNullNativePtr private constructor() { // TODO: refactor to use this type widely.
    @Suppress("NOTHING_TO_INLINE")
    inline fun toNativePtr() = NativePtr(this)

    override fun toString() = toNativePtr().toString()

    override fun hashCode() = toNativePtr().hashCode()

    override fun equals(other: Any?) = other is NonNullNativePtr
            && kotlin.native.internal.areEqualByValue(this.toNativePtr(), other.toNativePtr())
}

@ExportTypeInfo("theNativePtrArrayTypeInfo")
internal class NativePtrArray {

    @SymbolName("Kotlin_NativePtrArray_get")
    @GCCritical
    external public operator fun get(index: Int): NativePtr

    @SymbolName("Kotlin_NativePtrArray_set")
    @GCCritical
    external public operator fun set(index: Int, value: NativePtr): Unit

    val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_NativePtrArray_getArrayLength")
    @GCCritical
    external private fun getArrayLength(): Int
}
