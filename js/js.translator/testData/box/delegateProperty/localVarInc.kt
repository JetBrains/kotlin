// EXPECTED_REACHABLE_NODES: 494
//TODO: reuse same tests from JVM backend
package foo

import kotlin.reflect.KProperty

class Delegate {
    var inner = 1
    operator fun getValue(t: Any?, p: KProperty<*>): Int = inner
    operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {
        inner = i
    }
}

fun box(): String {
    var prop: Int by Delegate()
    var result = prop++
    if (result != 1) return "fail increment result: $prop"
    if (prop != 2) return "fail increment: $prop"
    return "OK"
}
