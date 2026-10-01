// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inspectObjectWithWeakReference.in
// OUTPUT_DATA_FILE: inspectObjectWithWeakReference.out

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

@OptIn(ExperimentalNativeApi::class)
fun main() {
    val box = Box(42)
    val weakReference = WeakReference(box)
    val string = buildString {
        append("weak ")
        append("string")
    }
    val stringWeakReference = WeakReference(string)
    check(weakReference.value === box)
    check(stringWeakReference.value === string)
}

data class Box(val payload: Int)
