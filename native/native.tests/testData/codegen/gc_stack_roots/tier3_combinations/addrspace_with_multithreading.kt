// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class, kotlin.native.concurrent.ObsoleteWorkersApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.TransferMode
import kotlin.native.runtime.GC

class WorkerResult(val value: Int)

fun box(): String {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { 42 }) { input ->
        WorkerResult(input * 2)
    }

    val localRoots = Array(10) { WorkerResult(it) }
    GC.collect()

    val result = future.result
    worker.requestTermination().result

    if (result.value != 84) {
        return "FAIL addrspace with multithreading: ${result.value}"
    }
    return "OK"
}
