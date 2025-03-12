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

    return weakNode3.value?.value ?: "FAIL"
}