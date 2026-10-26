@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
import kotlin.native.ref.WeakReference

class Node {
    var next: Node? = null
    val value = "OK"
}

fun box(): String {
    val node1 = Node()
    lateinit var weakNode3: WeakReference<Node>
    repeat(2) {
        val node2 = Node()
        val node3 = Node()
        if (it == 0) {
            weakNode3 = WeakReference(node3)
            node1.next = node2
            node2.next = node3
        }
    }

    repeat(2) {
        @OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
        kotlin.native.runtime.GC.collect()
    }

    val result = weakNode3.value?.value ?: "FAIL"
    // node1 keeps node3 reachable. Without this use node1 is dead after the loops, and a precise GC
    // (the delta-main scheme) may collect node3, while the shadow stack keeps node1 in its frame.
    if (node1.next == null) return "FAIL"
    return result
}