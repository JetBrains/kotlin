// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true

@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.runtime.GC

class Payload(val name: String, val value: Int)

fun divergentWorker(cond: Boolean, argA: Payload, argB: Payload): String {
    if (cond) {
        GC.collect()
        return "BranchA: ${argA.name}_${argA.value}"
    } else {
        GC.collect()
        return "BranchB: ${argB.name}_${argB.value}"
    }
}

fun box(): String {
    val a = Payload("Alpha", 10)
    val b = Payload("Beta", 20)

    val resA = divergentWorker(true, a, b)
    if (resA != "BranchA: Alpha_10") return "FAIL resA: $resA"

    val resB = divergentWorker(false, a, b)
    if (resB != "BranchB: Beta_20") return "FAIL resB: $resB"

    return "OK"
}
