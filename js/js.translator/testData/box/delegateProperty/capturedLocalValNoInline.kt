// EXPECTED_REACHABLE_NODES: 495
//TODO: reuse same tests from JVM backend
package foo

import kotlin.reflect.KProperty

fun <T> myRun(f: () -> T) = f()

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun box(): String {
    val prop: Int by Delegate()
    return myRun { if (prop == 1) "OK" else "fail" }
}
