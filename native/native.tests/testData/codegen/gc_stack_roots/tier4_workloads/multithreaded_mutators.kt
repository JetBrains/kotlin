// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class, kotlin.native.concurrent.ObsoleteWorkersApi::class)
package gc_stack_roots.tier4_workloads

import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.TransferMode
import kotlin.native.runtime.GC

class TaskInput(val taskId: Int, val payloadSize: Int)
class TaskOutput(val taskId: Int, val checksum: Long)

fun box(): String {
    val workers = Array(4) { Worker.start() }
    val futures = ArrayList<kotlin.native.concurrent.Future<TaskOutput>>()

    for (i in 0 until 12) {
        val worker = workers[i % workers.size]
        val input = TaskInput(i, 200)
        val f = worker.execute(TransferMode.SAFE, { input }) { task ->
            val localGraph = Array(task.payloadSize) { idx -> "str_${task.taskId}_$idx" }
            var chk = 0L
            for (s in localGraph) {
                chk += s.length
            }
            TaskOutput(task.taskId, chk)
        }
        futures.add(f)
    }

    for (step in 0 until 5) {
        val mainObj = Array(100) { "main_step_$step" }
        GC.collect()
    }

    for (f in futures) {
        val out = f.result
        if (out.checksum <= 0L) {
            workers.forEach { it.requestTermination().result }
            return "FAIL worker task ${out.taskId} returned invalid checksum"
        }
    }

    workers.forEach { it.requestTermination().result }
    return "OK"
}
