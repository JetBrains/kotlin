// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier4_workloads

import kotlin.native.runtime.GC

class WorkloadPayload(val id: Int, val data: IntArray)

fun box(): String {
    val anchors = ArrayList<WorkloadPayload>()

    for (cycle in 0 until 10) {
        anchors.add(WorkloadPayload(cycle, IntArray(64) { it + cycle }))

        for (i in 0 until 500) {
            val garbage = WorkloadPayload(i, IntArray(32) { it * 2 })
        }

        GC.collect()

        for (a in anchors) {
            if (a.data.size != 64 || a.data[0] != a.id) {
                return "FAIL anchor ${a.id} corrupted at cycle $cycle"
            }
        }
    }

    if (anchors.size != 10) return "FAIL total anchors: ${anchors.size}"
    return "OK"
}
