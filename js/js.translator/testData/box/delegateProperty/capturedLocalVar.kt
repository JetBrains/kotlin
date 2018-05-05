// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1115
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
    run { prop = 2 }
    if (prop != 2) return "fail get"
    return run { if (prop != 2) "fail set" else "OK" }
}
