// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class ArrayElement(val id: Int, val label: String)

fun runStackArrayComputation(): Boolean {
    val e0 = ArrayElement(10, "first")
    val e1 = ArrayElement(20, "second")
    val e2 = ArrayElement(30, "third")

    val weak0 = WeakReference(e0)
    val weak1 = WeakReference(e1)
    val weak2 = WeakReference(e2)

    val arr = arrayOf(e0, e1, e2)

    GC.collect()

    if (weak0.value == null || weak1.value == null || weak2.value == null) return false
    if (arr[0].id != 10 || arr[1].id != 20 || arr[2].id != 30) return false

    arr[1] = ArrayElement(25, "second_updated")
    GC.collect()

    return arr[1].id == 25 && arr[1].label == "second_updated"
}

fun box(): String {
    val ok = runStackArrayComputation()
    if (!ok) return "FAIL: stack array reference elements not preserved"

    GC.collect()

    return "OK"
}
