package konan.internal

import kotlinx.cinterop.*

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

class NativePointedBox(val value: NativePointed) {
    override fun equals(other: Any?): Boolean {
        if (other !is NativePointedBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()
}

fun boxNativePointed(value: NativePointed?) = if (value != null) NativePointedBox(value) else null
fun unboxNativePointed(box: NativePointedBox?) = box?.value

class CPointerBox(val value: CPointer<*>) {
    override fun equals(other: Any?): Boolean {
        if (other !is CPointerBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()
}

fun boxCPointer(value: CPointer<*>?) = if (value != null) CPointerBox(value) else null
fun unboxCPointer(box: CPointerBox?) = box?.value