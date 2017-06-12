// EXPECTED_REACHABLE_NODES: 493
//TODO: reuse same tests from JVM backend
package foo

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun box(): String {
    val prop: Int by Delegate()
    return run { if (prop == 1) "OK" else "fail" }
}
