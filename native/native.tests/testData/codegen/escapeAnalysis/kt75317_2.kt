// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO

// Enable runtime assertions:
// ASSERTIONS_MODE: always-enable

@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.internal.isStack
import kotlin.native.runtime.GC

class Node {
    var next: Node? = null
}

fun box(): String {
    val head = Node()
    if (!head.isStack()) return "FAIL 1"

    val tail = Node()
    if (!tail.isStack()) return "FAIL 2"

    repeat(1) {
        val middle = Node()
        if (middle.isStack()) return "FAIL 3"

        head.next = middle
        middle.next = tail
    }

    // A runtime assertion should fail in GC if we have a reference
    // from a heap non-local object (`middle`) to a stack object (`tail`):
    GC.collect()

    return "OK"
}
