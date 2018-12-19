/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("RESERVED_MEMBER_INSIDE_INLINE_CLASS")

package kotlin.native.internal

@TypedIntrinsic(IntrinsicType.GET_NATIVE_NULL_PTR)
external fun getNativeNullPtr(): NativePtr

class NativePtr @PublishedApi internal constructor(private val value: NonNullNativePtr?) {
    companion object {
        val NULL = getNativeNullPtr()
    }

    @TypedIntrinsic(IntrinsicType.NATIVE_PTR_PLUS_LONG)
    external operator fun plus(offset: Long): NativePtr

    @TypedIntrinsic(IntrinsicType.NATIVE_PTR_TO_LONG)
    external fun toLong(): Long

    override fun equals(other: Any?) = (other is NativePtr) && kotlin.native.internal.areEqualByValue(this, other)

    override fun hashCode() = this.toLong().hashCode()

    override fun toString() = "0x${this.toLong().toString(16)}"
}

@PublishedApi
internal inline class NonNullNativePtr(val value: NotNullPointerValue) { // TODO: refactor to use this type widely.
    @Suppress("NOTHING_TO_INLINE")
    inline fun toNativePtr() = NativePtr(this)
    // TODO: fixme.
    override fun toString() = ""

    override fun hashCode() = 0

    override fun equals(other: Any?) = false
}

@ExportTypeInfo("theNativePtrArrayTypeInfo")
internal class NativePtrArray {

    @SymbolName("Kotlin_NativePtrArray_get")
    external public operator fun get(index: Int): NativePtr

    @SymbolName("Kotlin_NativePtrArray_set")
    external public operator fun set(index: Int, value: NativePtr): Unit

    @SymbolName("Kotlin_NativePtrArray_getArrayLength")
    external private fun getArrayLength(): Int
}
