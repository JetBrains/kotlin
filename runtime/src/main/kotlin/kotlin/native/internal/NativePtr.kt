/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.native.internal

@Intrinsic external fun getNativeNullPtr(): NativePtr

class NativePtr @PublishedApi internal constructor(private val value: NonNullNativePtr?) {
    companion object {
        val NULL = getNativeNullPtr()
    }

    @Intrinsic external operator fun plus(offset: Long): NativePtr

    @Intrinsic external fun toLong(): Long

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
