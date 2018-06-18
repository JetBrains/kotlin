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

package konan.internal

import kotlinx.cinterop.*

@Frozen
class NativePtrBox(val value: NativePtr) {
    override fun equals(other: Any?): Boolean {
        if (other !is NativePtrBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()
}

fun boxNativePtr(value: NativePtr) = NativePtrBox(value)

@Frozen
class NativePointedBox(val value: NativePointed) {
    override fun equals(other: Any?): Boolean {
        if (other !is NativePointedBox) {
            return false
        }

        return this.value == other.value
    }

    // TODO: can't delegate the following methods to `this.value`
    // because `NativePointed` doesn't provide them.

    override fun hashCode() = this.value.rawPtr.hashCode()

    override fun toString() = "NativePointed(raw=${this.value.rawPtr})"
}

fun boxNativePointed(value: NativePointed?) = if (value != null) NativePointedBox(value) else null
fun unboxNativePointed(box: NativePointedBox?) = box?.value

@Frozen
class CPointerBox(val value: CPointer<CPointed>) : CValuesRef<CPointed>() {
    override fun equals(other: Any?): Boolean {
        if (other !is CPointerBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun getPointer(scope: AutofreeScope) = value.getPointer(scope)
}

fun boxCPointer(value: CPointer<CPointed>?) = if (value != null) CPointerBox(value) else null
fun unboxCPointer(box: CPointerBox?) = box?.value