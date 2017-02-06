package konan.internal

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.NativePointed
import kotlinx.cinterop.NativePtr

@Intrinsic external fun areEqualByValue(first: Boolean, second: Boolean): Boolean
@Intrinsic external fun areEqualByValue(first: Char, second: Char): Boolean
@Intrinsic external fun areEqualByValue(first: Byte, second: Byte): Boolean
@Intrinsic external fun areEqualByValue(first: Short, second: Short): Boolean
@Intrinsic external fun areEqualByValue(first: Int, second: Int): Boolean
@Intrinsic external fun areEqualByValue(first: Long, second: Long): Boolean
@Intrinsic external fun areEqualByValue(first: Float, second: Float): Boolean
@Intrinsic external fun areEqualByValue(first: Double, second: Double): Boolean

@Intrinsic external fun areEqualByValue(first: NativePtr, second: NativePtr): Boolean
@Intrinsic external fun areEqualByValue(first: NativePointed?, second: NativePointed?): Boolean
@Intrinsic external fun areEqualByValue(first: CPointer<*>?, second: CPointer<*>?): Boolean

inline fun areEqual(first: Any?, second: Any?): Boolean {
    return if (first == null) second == null else first.equals(second)
}