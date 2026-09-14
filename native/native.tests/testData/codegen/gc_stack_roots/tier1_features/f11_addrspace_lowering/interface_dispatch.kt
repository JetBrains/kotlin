// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f11_addrspace_lowering

import kotlin.native.runtime.GC

interface WorkerTask {
    fun execute(): String
}

class FastTask : WorkerTask {
    override fun execute(): String = "fast_done"
}

class SlowTask : WorkerTask {
    override fun execute(): String = "slow_done"
}

fun box(): String {
    val tasks: Array<WorkerTask> = arrayOf(FastTask(), SlowTask())

    GC.collect()

    val r1 = tasks[0].execute()
    val r2 = tasks[1].execute()

    if (r1 != "fast_done" || r2 != "slow_done") {
        return "FAIL interface dispatch: r1=$r1, r2=$r2"
    }
    return "OK"
}
