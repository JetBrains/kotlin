// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class, kotlin.native.concurrent.ObsoleteWorkersApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.TransferMode
import kotlin.native.runtime.GC

class MutatorContext(val workerId: Int)

fun box(): String {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { 99 }) { input ->
        val ctx = MutatorContext(input)
        if (input == 99) {
            throw IllegalArgumentException("worker_throw_${ctx.workerId}")
        }
        ctx.workerId * 2
    }

    val mainRoots = Array(20) { MutatorContext(it) }
    GC.collect()

    var threw = false
    try {
        future.result
    } catch (e: Throwable) {
        threw = true
        GC.collect()
    }

    worker.requestTermination().result

    if (!threw) return "FAIL worker should have thrown"
    if (mainRoots.size != 20) return "FAIL mainRoots corrupted"

    return "OK"
}
