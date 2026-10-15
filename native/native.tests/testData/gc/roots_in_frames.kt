// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.runtime.NativeRuntimeApi,kotlin.experimental.ExperimentalNativeApi

// A reference that lives only in a local variable, a parameter, a receiver, a field of such an
// object, or a captured variable must keep its object alive over a collection. An object that
// nothing references must be collected.
//
// Every test calls `GC.collect()` while the object is referenced only from the frame under test,
// and uses the object (or a weak reference to it) afterwards.
//
// The way the collector finds the roots of Kotlin frames depends on the GC stack map scheme
// (shadow stack or delta-main stack maps); the tests are the same for both.

import kotlin.native.NoInline
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC
import kotlin.test.*

class Node(var next: Node?, val data: IntArray) {
    fun sum(): Int = data.sum() + (next?.sum() ?: 0)
}

// 0 + 1 + ... + 15
private const val NODE_SUM = 120

@NoInline
private fun newNode(next: Node? = null): Node = Node(next, IntArray(16) { it })

@Test
fun localSurvivesCollection() {
    val node = newNode()
    val weak = WeakReference(node)

    GC.collect()

    assertNotNull(weak.value)
    assertEquals(NODE_SUM, node.sum())
}

@NoInline
private fun collectAndSum(node: Node, weakOut: Array<WeakReference<Node>?>): Int {
    weakOut[0] = WeakReference(node)
    GC.collect()
    return node.sum()
}

@Test
fun parameterSurvivesCollection() {
    val weakOut = arrayOfNulls<WeakReference<Node>>(1)

    // The caller does not keep the argument: only the parameter of the callee is a root.
    val sum = collectAndSum(newNode(), weakOut)

    assertEquals(NODE_SUM, sum)
    assertNotNull(weakOut[0])
}

class Holder {
    val node = newNode()

    @NoInline
    fun collectAndSum(): Int {
        GC.collect()
        return node.sum()
    }
}

@Test
fun receiverSurvivesCollection() {
    // A temporary receiver, nobody else references it.
    assertEquals(NODE_SUM, Holder().collectAndSum())
}

@Test
fun objectReachableOnlyThroughFieldSurvivesCollection() {
    val tail = newNode()
    val head = Node(tail, IntArray(1))
    val weakTail = WeakReference(tail)

    // `tail` is not used below: the field of `head` is the only path.
    GC.collect()

    assertNotNull(weakTail.value)
    assertEquals(NODE_SUM, head.next!!.sum())
}

@Test
fun elementsOfLocalArraySurviveCollection() {
    val array = Array<Node?>(10) { newNode() }
    val weaks = array.map { WeakReference(it!!) }

    GC.collect()

    assertTrue(weaks.all { it.value != null })
    assertEquals(10 * NODE_SUM, array.sumOf { it!!.sum() })
}

@Test
fun capturedVariableSurvivesCollection() {
    val node = newNode()
    val weak = WeakReference(node)
    val lambda: () -> Int = { node.sum() }

    GC.collect()

    assertNotNull(weak.value)
    assertEquals(NODE_SUM, lambda())
}

@NoInline
private fun descend(depth: Int, weaks: ArrayList<WeakReference<Node>>): Int {
    val node = newNode()
    weaks.add(WeakReference(node))
    val aliveAtTheBottom = if (depth == 0) {
        GC.collect()
        weaks.count { it.value != null }
    } else {
        descend(depth - 1, weaks)
    }
    // `node` is live across the call above.
    check(node.sum() == NODE_SUM)
    return aliveAtTheBottom
}

@Test
fun everyFrameOfDeepChainKeepsItsObject() {
    val weaks = ArrayList<WeakReference<Node>>()

    val alive = descend(60, weaks)

    assertEquals(61, weaks.size)
    assertEquals(61, alive)
}

class Tagged(val tag: Int)

@NoInline
private fun newTagged(tag: Int) = Tagged(tag)

@Test
fun variableAssignedInLoopStaysRooted() {
    // The same local variable holds a different object in every iteration. At every collection the
    // collector must see the object that the variable holds at that moment.
    var current = newTagged(0)
    repeat(30) { i ->
        val weak = WeakReference(current)

        GC.collect()

        assertNotNull(weak.value)
        assertEquals(i, current.tag)
        current = newTagged(i + 1)
    }
}

@NoInline
private fun weakOfUnreferenced(): WeakReference<Node> {
    val node = newNode()
    val weak = WeakReference(node)
    node.data[0] = 0
    return weak
}

@Test
fun unreferencedObjectIsCollected() {
    // The control of the tests above: when nothing references the object, the weak reference is cleared.
    val weak = weakOfUnreferenced()

    GC.collect()

    assertNull(weak.value)
}
