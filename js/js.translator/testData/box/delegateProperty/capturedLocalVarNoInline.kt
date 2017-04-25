// EXPECTED_REACHABLE_NODES: 497
//TODO: reuse same tests from JVM backend
package foo

import kotlin.reflect.KProperty

fun <T> myRun(f: () -> T) = f()

class Delegate {
    var inner = 1
    operator fun getValue(t: Any?, p: KProperty<*>): Int = inner
    operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {
        inner = i
    }
}

fun box(): String {
    var prop: Int by Delegate()
    myRun { prop = 2 }
    if (prop != 2) return "fail get"
    return myRun { if (prop != 2) "fail set" else "OK" }
}
