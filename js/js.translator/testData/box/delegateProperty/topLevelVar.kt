// EXPECTED_REACHABLE_NODES: 498
package foo

import kotlin.reflect.KProperty

class Delegate {
    var inner = 1
    operator fun getValue(t: Any?, p: KProperty<*>): Int = inner
    operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {
        inner = i
    }
}

var prop: Int by Delegate()

fun box(): String {
    if (prop != 1) return "fail get"
    prop = 2
    if (prop != 2) return "fail set"
    return "OK"
}
