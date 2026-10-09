// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class TraceableElement(val id: Int, var status: String)

fun runClosureMutationLoop(): String {
    var capturedRoot = TraceableElement(0, "init")

    val mutator: (Int) -> Unit = { step ->
        val oldRef = WeakReference(capturedRoot)
        capturedRoot = TraceableElement(step, "step_$step")

        GC.collect()

        if (capturedRoot.id != step || capturedRoot.status != "step_$step") {
            throw IllegalStateException("Closure captured variable corrupted at step $step")
        }
    }

    for (i in 1..10) {
        mutator(i)
    }

    GC.collect()

    if (capturedRoot.id != 10 || capturedRoot.status != "step_10") {
        return "FAIL: final captured root corrupted: id=${capturedRoot.id}, status=${capturedRoot.status}"
    }

    return "OK"
}

fun box(): String {
    try {
        val res = runClosureMutationLoop()
        if (res != "OK") return res
    } catch (e: Exception) {
        return "FAIL exception: ${e.message}"
    }
    return "OK"
}
