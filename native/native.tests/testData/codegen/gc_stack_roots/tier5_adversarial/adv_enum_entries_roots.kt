// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.runtime.GC

enum class WorkPriority(val weight: Int, val description: String) {
    LOW(1, "Low Priority"),
    MEDIUM(5, "Medium Priority"),
    HIGH(10, "High Priority"),
    CRITICAL(50, "Critical Priority")
}

class Task(val name: String, val priority: WorkPriority)

fun dispatchTask(task: Task, depth: Int): WorkPriority {
    if (depth <= 0) {
        GC.collect()
        return task.priority
    }
    return dispatchTask(task, depth - 1)
}

fun testEnumIdentityAcrossGC(): String {
    val p1 = WorkPriority.CRITICAL
    val p2 = WorkPriority.CRITICAL

    GC.collect()

    if (p1 !== p2) return "FAIL: Enum CRITICAL referential identity lost"
    if (p1.weight != 50 || p1.description != "Critical Priority") {
        return "FAIL: Enum properties corrupted"
    }

    val task = Task("CriticalTask", WorkPriority.HIGH)
    val resultPriority = dispatchTask(task, 20)

    GC.collect()

    if (resultPriority !== WorkPriority.HIGH) {
        return "FAIL: Dispatched task enum priority mismatch: $resultPriority"
    }

    return "OK"
}

fun testEnumEntriesArray(): String {
    val entries = WorkPriority.entries

    GC.collect()

    if (entries.size != 4) return "FAIL: WorkPriority entries count != 4"
    if (entries[0] !== WorkPriority.LOW || entries[3] !== WorkPriority.CRITICAL) {
        return "FAIL: WorkPriority entries content mismatch"
    }

    return "OK"
}

fun box(): String {
    var res = testEnumIdentityAcrossGC()
    if (res != "OK") return res

    res = testEnumEntriesArray()
    if (res != "OK") return res

    return "OK"
}
