import kotlin.test.*
import kotlin.native.ref.*

class Node(var next: Node?)

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@Test fun runTest1() {
    val node1 = Node(null)
    val node2 = Node(node1)
    node1.next = node2

    WeakReference(node1)
}
