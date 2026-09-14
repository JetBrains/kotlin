// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class TargetNode(val name: String, var count: Int)

class NonEscapingPair(val left: TargetNode, val right: TargetNode)

fun runStackObjectScope(): Boolean {
    val nodeA = TargetNode("NodeA", 100)
    val nodeB = TargetNode("NodeB", 200)

    val weakA = WeakReference(nodeA)
    val weakB = WeakReference(nodeB)

    val pair = NonEscapingPair(nodeA, nodeB)

    GC.collect()

    if (weakA.value == null || weakB.value == null) return false
    if (pair.left.count != 100 || pair.right.count != 200) return false

    pair.left.count += 50
    pair.right.count += 50

    GC.collect()

    return pair.left.count == 150 && pair.right.count == 250
}

fun box(): String {
    val ok = runStackObjectScope()
    if (!ok) return "FAIL: stack object reference fields failed GC root preservation"

    GC.collect()

    return "OK"
}
