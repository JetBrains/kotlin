// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.internal.isStack
import kotlin.native.runtime.GC

open class Node {
    var next: Node? = null
}

class Middle : Node() {
    var next2: Node? = null
    var next3: Node? = null
    var next4: Node? = null
    var next5: Node? = null
}

var count = 0

fun left(depth: Int) {
    if (depth <= 0) return

    // Schedule the GC often enough to find the problem sooner:
    if (count++ % 500 == 0) GC.schedule()

    val head = Node()
    check(head.isStack())

    // Use many stack objects to increase the probability of hitting one of them with zeroes:
    val tail1 = Node()
    val tail2 = Node()
    val tail3 = Node()
    val tail4 = Node()
    val tail5 = Node()

    check(tail1.isStack())
    check(tail2.isStack())
    check(tail3.isStack())
    check(tail4.isStack())
    check(tail5.isStack())

    repeat(1) {
        val middle = Middle()
        check(!middle.isStack())

        head.next = middle

        middle.next = tail1
        middle.next2 = tail2
        middle.next3 = tail3
        middle.next4 = tail4
        middle.next5 = tail5
    }

    // The bug is that `middle` is marked as a normal heap object, not as a local object.
    // So, we have a heap object referencing a stack object.
    // In CMS, when it marks concurrently, by the time it scans `middle`,
    // the referred stack objects might be overwritten.
    // That's exactly what this test tries to achieve.
    //
    // `left` and `right` are similar, but the former allocates such stack objects,
    // while the latter zeroes the stack.
    // So, if the stack trace changes between scanning stack roots (in STW)
    // and marking `middle` (concurrently), then the deepest frame changes
    // from `left` to `right`, so left's objects are overwritten by right's zeroing.

    left(depth - 1)
    right(depth - 1)
}

fun right(depth: Int) {
    if (depth <= 0) return

    val zeroes = IntArray(16)
    check(zeroes.isStack()) // Zeroes the stack.

    left(depth - 1)
    right(depth - 1)
}

fun box(): String {
    left(20)

    // Wait till the scheduled GC finishes:
    GC.collect()

    return "OK"
}
