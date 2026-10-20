// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class, kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference
import kotlin.concurrent.AtomicReference
import kotlin.concurrent.Volatile

class SharedData(val tag: String, var iteration: Int)

class VolatileHolder {
    @Volatile
    var volatileRef: SharedData? = null
}

fun testVolatileFieldRoots(): String {
    val holder = VolatileHolder()
    val d1 = SharedData("d1", 1)
    holder.volatileRef = d1

    GC.collect()

    val read1 = holder.volatileRef
    if (read1 == null || read1.tag != "d1" || read1.iteration != 1) {
        return "FAIL: volatile field read corrupted after GC"
    }

    val d2 = SharedData("d2", 2)
    val weakD1 = WeakReference(d1)
    holder.volatileRef = d2

    GC.collect()

    val read2 = holder.volatileRef
    if (read2 == null || read2.tag != "d2" || read2.iteration != 2) {
        return "FAIL: reassigned volatile field read corrupted"
    }

    return "OK"
}

fun testAtomicReferenceCAS(): String {
    val initial = SharedData("init", 0)
    val atomic = AtomicReference(initial)

    GC.collect()

    val next = SharedData("next", 100)
    val success = atomic.compareAndSet(initial, next)
    if (!success) return "FAIL: AtomicReference CAS returned false"

    GC.collect()

    val current = atomic.value
    if (current.tag != "next" || current.iteration != 100) {
        return "FAIL: AtomicReference value corrupted after CAS and GC"
    }

    return "OK"
}

fun box(): String {
    var res = testVolatileFieldRoots()
    if (res != "OK") return res

    res = testAtomicReferenceCAS()
    if (res != "OK") return res

    return "OK"
}
